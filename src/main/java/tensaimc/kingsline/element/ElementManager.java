package tensaimc.kingsline.element;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;

import java.util.*;

/**
 * エレメントシステム管理クラス
 */
public class ElementManager {
    
    private final KingsLine plugin;
    
    // アクティブなSP技効果
    private final Set<UUID> overheatActive;      // Fire: Overheat
    private final Set<UUID> bulwarkActive;       // Earth: Bulwark
    private final Set<UUID> galeStepBonusActive; // Wind: 最初の攻撃ボーナス
    private final Set<UUID> frozenPlayers;       // Ice Age で凍結中
    
    public ElementManager(KingsLine plugin) {
        this.plugin = plugin;
        this.overheatActive = new HashSet<>();
        this.bulwarkActive = new HashSet<>();
        this.galeStepBonusActive = new HashSet<>();
        this.frozenPlayers = new HashSet<>();
    }
    
    /**
     * リセット
     */
    public void reset() {
        overheatActive.clear();
        bulwarkActive.clear();
        galeStepBonusActive.clear();
        frozenPlayers.clear();
    }
    
    // ========== パッシブ効果 ==========
    
    /**
     * パッシブ効果を適用
     */
    public void applyPassiveEffects(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null || klPlayer.getElement() == null) {
            return;
        }
        
        switch (klPlayer.getElement()) {
            case WIND:
                // 常時Speed I, Jump Boost I
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false), true);
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.JUMP, Integer.MAX_VALUE, 0, false, false), true);
                break;
                
            case EARTH:
            case ICE:
                // 移動速度低下（Slowness I相当でなく、属性で調整が理想だが簡易実装）
                // ここでは効果なし（ダメージ計算時に反映）
                break;
        }
    }
    
    /**
     * ダメージ倍率を計算（攻撃者側）
     */
    public double getAttackDamageMultiplier(KLPlayer attacker) {
        if (attacker.getElement() == null) {
            return 1.0;
        }
        
        double multiplier = 1.0;
        
        switch (attacker.getElement()) {
            case FIRE:
                multiplier *= 1.07; // +7%
                if (overheatActive.contains(attacker.getUuid())) {
                    multiplier *= 1.20; // +20%
                }
                break;
        }
        
        return multiplier;
    }
    
    /**
     * ダメージ倍率を計算（被攻撃者側）
     */
    public double getDefenseDamageMultiplier(KLPlayer victim) {
        if (victim.getElement() == null) {
            return 1.0;
        }
        
        double multiplier = 1.0;
        
        switch (victim.getElement()) {
            case FIRE:
                multiplier *= 1.05; // +5% ダメージ
                if (overheatActive.contains(victim.getUuid())) {
                    multiplier *= 1.10; // +10%
                }
                break;
                
            case WIND:
                multiplier *= 1.10; // +10% ダメージ
                break;
                
            case EARTH:
                multiplier *= 0.90; // -10% ダメージ
                if (bulwarkActive.contains(victim.getUuid())) {
                    multiplier *= 0.80; // -20%
                }
                break;
        }
        
        return multiplier;
    }
    
    /**
     * 炎上判定（Fire）
     */
    public void checkFireIgnite(KLPlayer attacker, Player victim) {
        if (attacker.getElement() != Element.FIRE) {
            return;
        }
        
        // Overheat中は確定炎上
        if (overheatActive.contains(attacker.getUuid())) {
            victim.setFireTicks(40); // 2秒
            return;
        }
        
        // 10%確率で炎上
        if (Math.random() < 0.10) {
            victim.setFireTicks(20); // 1秒
        }
    }
    
    /**
     * Slowness付与判定（Ice）
     */
    public void checkIceSlow(KLPlayer victim, Player attacker) {
        if (victim.getElement() != Element.ICE) {
            return;
        }
        
        // 20%確率で相手にSlow
        if (Math.random() < 0.20) {
            attacker.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW, 20, 0, false, false), true);
        }
    }
    
    /**
     * ノックバック耐性
     */
    public double getKnockbackResistance(KLPlayer victim) {
        if (victim.getElement() == null) {
            return 0.0;
        }
        
        double resistance = 0.0;
        
        switch (victim.getElement()) {
            case ICE:
                // エリア内にいるときのみ
                if (plugin.getAreaManager().isInBArea(victim)) {
                    resistance += 0.20;
                }
                break;
                
            case EARTH:
                resistance += 0.30;
                if (bulwarkActive.contains(victim.getUuid())) {
                    resistance += 0.25; // さらに半減
                }
                break;
        }
        
        return Math.min(1.0, resistance);
    }
    
    // ========== SP技 ==========
    
    /**
     * SP技を発動
     */
    public void activateSpecialAbility(KLPlayer klPlayer) {
        if (klPlayer.getElement() == null) {
            return;
        }
        
        Player player = klPlayer.getPlayer();
        if (player == null) {
            return;
        }
        
        long cooldown = plugin.getConfigManager().getSpCooldownMillis();
        
        switch (klPlayer.getElement()) {
            case FIRE:
                activateOverheat(klPlayer, player);
                break;
            case ICE:
                activateIceAge(klPlayer, player);
                break;
            case WIND:
                if (!activateGaleStep(klPlayer, player)) {
                    return; // 失敗した場合はゲージ消費しない
                }
                break;
            case EARTH:
                activateBulwark(klPlayer, player);
                break;
        }
        
        klPlayer.useSpAbility(cooldown);
        player.sendMessage(ChatColor.GOLD + "SP技を発動！");
    }
    
    /**
     * Fire: Overheat
     */
    private void activateOverheat(KLPlayer klPlayer, Player player) {
        UUID uuid = klPlayer.getUuid();
        overheatActive.add(uuid);
        
        player.sendMessage(ChatColor.RED + "🔥 Overheat! 5秒間、与ダメ+20%、確定炎上！");
        
        // 5秒後に解除
        new BukkitRunnable() {
            @Override
            public void run() {
                overheatActive.remove(uuid);
                Player p = klPlayer.getPlayer();
                if (p != null) {
                    p.sendMessage(ChatColor.GRAY + "Overheat終了");
                }
            }
        }.runTaskLater(plugin, 100L);
    }
    
    /**
     * Ice: Ice Age
     */
    private void activateIceAge(KLPlayer klPlayer, Player player) {
        GameManager gm = plugin.getGameManager();
        Location loc = player.getLocation();
        
        List<KLPlayer> targets = new ArrayList<>();
        
        // 半径6ブロック以内の敵を取得
        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                KLPlayer klTarget = gm.getPlayer(target);
                
                if (klTarget != null && klTarget.getTeam() != klPlayer.getTeam()) {
                    targets.add(klTarget);
                }
            }
        }
        
        // 最大2人まで
        int count = 0;
        for (KLPlayer target : targets) {
            if (count >= 2) break;
            
            freezePlayer(target);
            count++;
        }
        
        player.sendMessage(ChatColor.AQUA + "❄ Ice Age! " + count + "人を凍結！");
    }
    
    /**
     * プレイヤーを凍結
     */
    private void freezePlayer(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) {
            return;
        }
        
        UUID uuid = klPlayer.getUuid();
        frozenPlayers.add(uuid);
        
        // 移動不可（Slowness 100）
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW, 30, 100, false, false), true);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP, 30, 128, false, false), true);
        
        player.sendMessage(ChatColor.AQUA + "凍結された！");
        
        // 1.5秒後に解除
        new BukkitRunnable() {
            @Override
            public void run() {
                frozenPlayers.remove(uuid);
                Player p = klPlayer.getPlayer();
                if (p != null) {
                    p.removePotionEffect(PotionEffectType.SLOW);
                    p.removePotionEffect(PotionEffectType.JUMP);
                    p.sendMessage(ChatColor.GRAY + "凍結解除");
                }
            }
        }.runTaskLater(plugin, 30L);
    }
    
    /**
     * Wind: Gale Step
     */
    private boolean activateGaleStep(KLPlayer klPlayer, Player player) {
        // 視線の先の敵を取得（最大8ブロック）
        Player target = getTargetPlayer(player, 8);
        
        if (target == null) {
            player.sendMessage(ChatColor.RED + "ターゲットが見つかりません。");
            return false;
        }
        
        KLPlayer klTarget = plugin.getGameManager().getPlayer(target);
        if (klTarget == null || klTarget.getTeam() == klPlayer.getTeam()) {
            player.sendMessage(ChatColor.RED + "敵プレイヤーをターゲットしてください。");
            return false;
        }
        
        // 敵の背後にテレポート
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.getDirection().normalize().multiply(-2);
        Location behindLoc = targetLoc.clone().add(direction);
        behindLoc.setYaw(targetLoc.getYaw());
        behindLoc.setPitch(0);
        
        player.teleport(behindLoc);
        
        // 4秒間Speed II
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, 80, 1, false, false), true);
        
        // 最初の攻撃にKBボーナス
        galeStepBonusActive.add(klPlayer.getUuid());
        
        player.sendMessage(ChatColor.WHITE + "🌪 Gale Step! " + target.getName() + "の背後にテレポート！");
        
        // 3秒後にKBボーナス解除
        new BukkitRunnable() {
            @Override
            public void run() {
                galeStepBonusActive.remove(klPlayer.getUuid());
            }
        }.runTaskLater(plugin, 60L);
        
        return true;
    }
    
    /**
     * Earth: Bulwark
     */
    private void activateBulwark(KLPlayer klPlayer, Player player) {
        UUID uuid = klPlayer.getUuid();
        bulwarkActive.add(uuid);
        
        // 移動速度低下
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW, 100, 1, false, false), true);
        
        player.sendMessage(ChatColor.GOLD + "🪨 Bulwark! 5秒間、超高耐久！");
        
        // 5秒後に解除
        new BukkitRunnable() {
            @Override
            public void run() {
                bulwarkActive.remove(uuid);
                Player p = klPlayer.getPlayer();
                if (p != null) {
                    p.removePotionEffect(PotionEffectType.SLOW);
                    p.sendMessage(ChatColor.GRAY + "Bulwark終了");
                }
            }
        }.runTaskLater(plugin, 100L);
    }
    
    /**
     * 視線の先のプレイヤーを取得
     */
    private Player getTargetPlayer(Player player, int maxDistance) {
        for (Entity entity : player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                
                // 視線の方向と位置をチェック
                Vector toTarget = target.getLocation().toVector()
                        .subtract(player.getLocation().toVector()).normalize();
                Vector direction = player.getLocation().getDirection().normalize();
                
                double dot = direction.dot(toTarget);
                if (dot > 0.8) { // 視線の前方約36度以内
                    return target;
                }
            }
        }
        return null;
    }
    
    // ========== Getters ==========
    
    public boolean hasGaleStepBonus(UUID uuid) {
        return galeStepBonusActive.contains(uuid);
    }
    
    public void consumeGaleStepBonus(UUID uuid) {
        galeStepBonusActive.remove(uuid);
    }
    
    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }
}
