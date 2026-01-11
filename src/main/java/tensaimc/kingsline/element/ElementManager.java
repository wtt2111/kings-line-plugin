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
                // 常時Speed I + walkSpeed上昇（Speed I〜IIの中間の速さ）
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false), true);
                player.setWalkSpeed(0.22f); // デフォルト0.2 → 0.22（+10%基礎速度）
                break;
                
            case ICE:
                // 移動速度-30%（Slowness II）
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW, Integer.MAX_VALUE, 1, false, false), true);
                break;
                
            case EARTH:
            case FIRE:
                // パッシブポーション効果なし（ダメージ計算時に反映）
                break;
        }
    }
    
    /**
     * パッシブ効果を解除（ゲーム終了時などに呼び出す）
     */
    public void removePassiveEffects(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) {
            return;
        }
        
        // ポーション効果を解除
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.SLOW);
        
        // walkSpeedをデフォルトに戻す
        player.setWalkSpeed(0.2f);
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
                if (overheatActive.contains(attacker.getUuid())) {
                    multiplier *= 1.40; // SP中+40%
                } else {
                    multiplier *= 1.20; // パッシブ+20%
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
                multiplier *= 1.15; // +15% ダメージ
                break;
                
            case WIND:
                multiplier *= 1.10; // +10% ダメージ
                break;
                
            case EARTH:
                if (bulwarkActive.contains(victim.getUuid())) {
                    multiplier *= 0.20; // SP中-80%
                } else {
                    multiplier *= 0.70; // パッシブ-30%
                }
                break;
        }
        
        return multiplier;
    }
    
    /**
     * Earthの10%ダメージ完全無視判定
     */
    public boolean shouldIgnoreDamage(KLPlayer victim) {
        if (victim.getElement() != Element.EARTH) {
            return false;
        }
        // Bulwark中は無視判定なし（既に-80%）
        if (bulwarkActive.contains(victim.getUuid())) {
            return false;
        }
        return Math.random() < 0.10;
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
     * Iceエレメントが攻撃した時に相手にSlow付与
     */
    public void checkIceSlow(KLPlayer attacker, Player victim) {
        if (attacker.getElement() != Element.ICE) {
            return;
        }
        
        // 20%確率で相手にSlow（2秒間）
        if (Math.random() < 0.20) {
            victim.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW, 40, 0, false, false), true);
            
            Player attackerPlayer = attacker.getPlayer();
            if (attackerPlayer != null) {
                attackerPlayer.sendMessage(ChatColor.AQUA + "❄ Slowness付与！");
            }
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
                // 常時50%KB耐性
                resistance += 0.50;
                break;
                
            case EARTH:
                // EarthはKB耐性なし
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
        
        // 統計: SP技使用を記録
        plugin.getStatsDatabase().addSkillUse(player.getUniqueId());
        
        // 経験値バー（SPゲージ表示）をリセット
        player.setLevel(0);
        player.setExp(0f);
        
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
        
        // 移動不可（Slowness 100）- 4秒間 (80 ticks)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW, 80, 100, false, false), true);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP, 80, 128, false, false), true);
        
        player.sendMessage(ChatColor.AQUA + "凍結された！(4秒)");
        
        // 4秒後に解除
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
        }.runTaskLater(plugin, 80L);
    }
    
    /**
     * Wind: Gale Step
     */
    private boolean activateGaleStep(KLPlayer klPlayer, Player player) {
        // 視線の先の敵を取得（最大14ブロック）
        Player target = getTargetPlayer(player, 14);
        
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
        
        // 対象を見つめる方向を計算
        Vector toTarget = targetLoc.toVector().subtract(behindLoc.toVector());
        behindLoc.setDirection(toTarget);
        
        // フライハック検知回避
        allowTemporaryFlight(player, 60);
        
        player.teleport(behindLoc);
        
        // 既存のSpeed効果を削除してからSpeed IIを付与（キングのSpeed Iと競合しないように）
        player.removePotionEffect(PotionEffectType.SPEED);
        
        // 11秒間Speed II (220 ticks)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, 220, 1, false, false), true);
        
        player.sendMessage(ChatColor.WHITE + "🌪 Gale Step! " + target.getName() + "の背後にテレポート！(11秒Speed II)");
        
        // 11秒後にパッシブ効果を再適用
        final KLPlayer finalKlPlayer = klPlayer;
        new BukkitRunnable() {
            @Override
            public void run() {
                Player p = finalKlPlayer.getPlayer();
                if (p != null && p.isOnline()) {
                    applyPassiveEffects(finalKlPlayer);
                }
            }
        }.runTaskLater(plugin, 220L);
        
        return true;
    }
    
    /**
     * Earth: Bulwark
     */
    private void activateBulwark(KLPlayer klPlayer, Player player) {
        UUID uuid = klPlayer.getUuid();
        bulwarkActive.add(uuid);
        
        player.sendMessage(ChatColor.GOLD + "🪨 Bulwark! 5秒間、被ダメ-80%！");
        
        // 5秒後に解除
        new BukkitRunnable() {
            @Override
            public void run() {
                bulwarkActive.remove(uuid);
                Player p = klPlayer.getPlayer();
                if (p != null) {
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
    
    /**
     * エレメント依存のSP必要HIT数を取得
     */
    public int getSpRequiredHits(KLPlayer klPlayer) {
        if (klPlayer.getElement() == Element.WIND) {
            return 7; // Windは7HIT
        }
        return plugin.getConfigManager().getSpRequiredHits(); // 他は10HIT
    }
    
    // ========== エレメントオーブ用メソッド ==========
    
    /**
     * オーブからOverheatを発動
     */
    public void activateOrbOverheat(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) return;
        
        activateOverheat(klPlayer, player);
    }
    
    /**
     * オーブからIce Ageを発動
     * @return 凍結した人数
     */
    public int activateOrbIceAge(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) return 0;
        
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
        return count;
    }
    
    /**
     * オーブからGale Stepを発動
     * @return 成功した場合true
     */
    public boolean activateOrbGaleStep(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) return false;
        
        return activateGaleStep(klPlayer, player);
    }
    
    /**
     * オーブからBulwarkを発動
     */
    public void activateOrbBulwark(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) return;
        
        activateBulwark(klPlayer, player);
    }
    
    /**
     * フライハック検知回避のため一時的に飛行を許可
     */
    private void allowTemporaryFlight(Player player, int ticks) {
        player.setAllowFlight(true);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }.runTaskLater(plugin, ticks);
    }
}
