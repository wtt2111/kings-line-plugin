package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.element.ElementManager;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.item.SpecialItems;
import tensaimc.kingsline.player.KLPlayer;

/**
 * 戦闘関連のリスナー
 */
public class CombatListener implements Listener {
    
    private final KingsLine plugin;
    
    public CombatListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 釣り竿（FishHook）からの味方へのノックバックをキャンセル
        if (event.getDamager() instanceof FishHook) {
            FishHook hook = (FishHook) event.getDamager();
            if (hook.getShooter() instanceof Player && event.getEntity() instanceof Player) {
                Player fisher = (Player) hook.getShooter();
                Player target = (Player) event.getEntity();
                GameManager gm = plugin.getGameManager();
                
                if (gm.isState(GameState.RUNNING)) {
                    KLPlayer klFisher = gm.getPlayer(fisher);
                    KLPlayer klTarget = gm.getPlayer(target);
                    
                    if (klFisher != null && klTarget != null && klFisher.getTeam() == klTarget.getTeam()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
        
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        GameManager gm = plugin.getGameManager();
        
        // 準備フェーズ中（STARTING）はPVP禁止
        if (gm.isState(GameState.STARTING)) {
            event.setCancelled(true);
            return;
        }
        
        // ゲーム中でなければ無視
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        KLPlayer klAttacker = gm.getPlayer(attacker);
        KLPlayer klVictim = gm.getPlayer(victim);
        
        if (klAttacker == null || klVictim == null) {
            return;
        }
        
        // 同チームへのダメージをキャンセル
        if (klAttacker.getTeam() == klVictim.getTeam()) {
            event.setCancelled(true);
            return;
        }
        
        // ゴーストマント効果を解除（被害者が透明状態なら解除）
        // ※自分が攻撃しても解除されない、攻撃されたら解除
        if (SpecialItems.GhostCloak.isActive(klVictim.getUuid())) {
            SpecialItems.GhostCloak.cancelEffect(plugin, victim, "攻撃を受けたため");
        }
        
        // ミラーシールド: ダメージ反射
        if (SpecialItems.MirrorShield.isActive(klVictim.getUuid())) {
            if (SpecialItems.MirrorShield.reflectDamage(plugin, victim, attacker, event.getDamage())) {
                event.setCancelled(true);
                return;
            }
        }
        
        // リスキル対策：自チームのスポーン地点から半径20ブロック以内は無敵
        Arena arena = gm.getCurrentArena();
        if (arena != null) {
            Location spawnLoc = arena.getSpawn(klVictim.getTeam());
            if (spawnLoc != null && victim.getWorld().equals(spawnLoc.getWorld())) {
                double distance = victim.getLocation().distance(spawnLoc);
                if (distance <= 20) {
                    event.setCancelled(true);
                    attacker.sendMessage(ChatColor.RED + "相手はスポーン保護エリア内です！");
                    return;
                }
            }
        }
        
        ElementManager em = plugin.getElementManager();
        
        // Earth: 10%でダメージ完全無視
        if (em.shouldIgnoreDamage(klVictim)) {
            event.setCancelled(true);
            
            // 被害者に通知
            victim.sendMessage(ChatColor.GOLD + "🪨 ダメージ無効化！");
            victim.playSound(victim.getLocation(), Sound.ANVIL_LAND, 0.5f, 2.0f);
            
            // 攻撃者に通知
            attacker.sendMessage(ChatColor.YELLOW + "⚠ " + victim.getName() + " がダメージを無効化した！");
            attacker.playSound(attacker.getLocation(), Sound.ITEM_BREAK, 1.0f, 1.0f);
            
            // エフェクト（周囲にも見える）
            victim.getWorld().playEffect(victim.getLocation().add(0, 1, 0), Effect.CRIT, 0);
            return;
        }
        
        // ダメージ計算
        double damage = event.getDamage();
        
        // 1.8.8のStrengthポーション効果を調整
        // バニラ1.8.8: Strength I = +3ダメージ, Strength II = +6ダメージ（壊れている）
        // 調整後: Strength I = +20%, Strength II = +40%
        PotionEffect strengthEffect = getStrengthEffect(attacker);
        if (strengthEffect != null) {
            int amplifier = strengthEffect.getAmplifier(); // 0 = I, 1 = II
            
            // バニラのStrengthボーナスを取り除く (3 * (amplifier + 1))
            double vanillaBonus = 3.0 * (amplifier + 1);
            damage -= vanillaBonus;
            
            // 新しい倍率を適用 (Strength I = 1.2, Strength II = 1.4)
            double newMultiplier = 1.0 + (0.2 * (amplifier + 1));
            damage *= newMultiplier;
        }
        
        damage *= em.getAttackDamageMultiplier(klAttacker);
        damage *= em.getDefenseDamageMultiplier(klVictim);
        event.setDamage(Math.max(0, damage));
        
        // Fire: 炎上判定
        em.checkFireIgnite(klAttacker, victim);
        
        // Ice: Slowness付与判定（Iceが攻撃した時に相手にSlow）
        em.checkIceSlow(klAttacker, victim);
        
        // SPゲージ増加（攻撃者）
        klAttacker.addSpGauge(1);
        int requiredHits = em.getSpRequiredHits(klAttacker);
        if (klAttacker.getSpGauge() >= requiredHits && !klAttacker.isSpOnCooldown()) {
            attacker.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SP READY! シフト+剣右クリックで発動！");
        }
        
        // 経験値バーでSPゲージ表示
        updateSPDisplay(attacker, klAttacker, requiredHits);
        
        // Wind: Gale Stepのノックバックボーナス
        if (em.hasGaleStepBonus(klAttacker.getUuid())) {
            em.consumeGaleStepBonus(klAttacker.getUuid());
            
            // ノックバック強化
            Vector knockback = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector())
                    .normalize()
                    .multiply(1.3);
            knockback.setY(0.3);
            victim.setVelocity(victim.getVelocity().add(knockback));
            
            attacker.sendMessage(ChatColor.WHITE + "Gale Step ノックバック！");
        }
        
        // KB耐性を適用（カスタムノックバック処理）
        final double kbResistance = em.getKnockbackResistance(klVictim);
        if (kbResistance > 0) {
            // 攻撃者からvictimへの方向を計算
            final Vector direction = victim.getLocation().toVector()
                    .subtract(attacker.getLocation().toVector())
                    .setY(0)
                    .normalize();
            
            // バニラのノックバックを上書きして、軽減したノックバックを適用
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!victim.isOnline()) return;
                    
                    // 基本ノックバック力（バニラ相当）
                    double baseKnockback = 0.4;
                    
                    // 軽減後のノックバック力
                    double reducedKnockback = baseKnockback * (1.0 - kbResistance);
                    
                    // 新しいノックバックを適用
                    Vector newVelocity = direction.clone().multiply(reducedKnockback);
                    newVelocity.setY(0.35 * (1.0 - kbResistance * 0.5)); // Y方向も少し軽減
                    
                    victim.setVelocity(newVelocity);
                }
            }.runTaskLater(plugin, 1L);
        }
    }
    
    /**
     * SPゲージを経験値バーで表示
     */
    private void updateSPDisplay(Player player, KLPlayer klPlayer, int maxHits) {
        int gauge = klPlayer.getSpGauge();
        
        player.setLevel(gauge);
        player.setExp(Math.min(0.99f, (float) gauge / maxHits));
    }
    
    /**
     * ダメージ処理（落下ダメージ無効化 + リバイバルチャーム）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        // 既にキャンセルされている場合はスキップ（Earthの無効化など）
        if (event.isCancelled()) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中のみ適用
        if (!gm.isState(GameState.RUNNING, GameState.STARTING)) {
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        // 落下ダメージを無効化
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        
        // 致死ダメージのチェック（リバイバルチャーム）
        if (gm.isState(GameState.RUNNING)) {
            double finalHealth = player.getHealth() - event.getFinalDamage();
            if (finalHealth <= 0) {
                // 死亡する前にリバイバルチャームをチェック
                if (SpecialItems.RevivalCharm.tryReviveBeforeDeath(plugin, player, klPlayer)) {
                    // 死亡をキャンセルしてHP40%で復活
                    event.setCancelled(true);
                    player.setHealth(player.getMaxHealth() * 0.4);
                }
            }
        }
    }
    
    /**
     * 天候変更を無効化（雨を防ぐ）
     */
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中（準備フェーズ含む）のみ適用
        if (!gm.isState(GameState.RUNNING, GameState.STARTING)) {
            return;
        }
        
        // アリーナのワールドかチェック
        if (gm.getCurrentArena() != null && gm.getCurrentArena().getWorld() != null) {
            if (!event.getWorld().equals(gm.getCurrentArena().getWorld())) {
                return;
            }
        }
        
        // 雨への変更をキャンセル（toWeatherState() == true は雨になるという意味）
        if (event.toWeatherState()) {
            event.setCancelled(true);
            // 念のため晴れに戻す
            event.getWorld().setStorm(false);
        }
    }
    
    /**
     * 雷を無効化
     */
    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中（準備フェーズ含む）のみ適用
        if (!gm.isState(GameState.RUNNING, GameState.STARTING)) {
            return;
        }
        
        // アリーナのワールドかチェック
        if (gm.getCurrentArena() != null && gm.getCurrentArena().getWorld() != null) {
            if (!event.getWorld().equals(gm.getCurrentArena().getWorld())) {
                return;
            }
        }
        
        // 雷への変更をキャンセル
        if (event.toThunderState()) {
            event.setCancelled(true);
            event.getWorld().setThundering(false);
        }
    }
    
    /**
     * プレイヤーのStrengthエフェクトを取得（1.8.8対応）
     */
    private PotionEffect getStrengthEffect(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
                return effect;
            }
        }
        return null;
    }
    
    /**
     * 釣り竿の味方ヒットを防止
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        
        Entity caught = event.getCaught();
        if (!(caught instanceof Player)) {
            return;
        }
        
        Player fisher = event.getPlayer();
        Player target = (Player) caught;
        GameManager gm = plugin.getGameManager();
        
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        KLPlayer klFisher = gm.getPlayer(fisher);
        KLPlayer klTarget = gm.getPlayer(target);
        
        if (klFisher == null || klTarget == null) {
            return;
        }
        
        // 同チームへの釣り竿ヒットをキャンセル
        if (klFisher.getTeam() == klTarget.getTeam()) {
            event.setCancelled(true);
            // フックを除去して引っ張り効果を完全に無効化
            if (event.getHook() != null) {
                event.getHook().remove();
            }
        }
    }
    
    /**
     * 防具の耐久値ダメージを無効化（連戦しても壊れないように）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDamage(PlayerItemDamageEvent event) {
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中のみ適用
        if (!gm.isState(GameState.RUNNING, GameState.STARTING)) {
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(event.getPlayer());
        if (klPlayer == null) {
            return;
        }
        
        ItemStack item = event.getItem();
        Material type = item.getType();
        
        // 防具の耐久値ダメージをキャンセル
        if (isArmor(type)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * 防具かどうかを判定
     */
    private boolean isArmor(Material type) {
        switch (type) {
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            case GOLD_HELMET:
            case GOLD_CHESTPLATE:
            case GOLD_LEGGINGS:
            case GOLD_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
                return true;
            default:
                return false;
        }
    }
}
