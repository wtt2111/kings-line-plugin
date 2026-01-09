package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.element.ElementManager;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
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
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        GameManager gm = plugin.getGameManager();
        
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
        
        ElementManager em = plugin.getElementManager();
        
        // ダメージ計算
        double damage = event.getDamage();
        damage *= em.getAttackDamageMultiplier(klAttacker);
        damage *= em.getDefenseDamageMultiplier(klVictim);
        event.setDamage(damage);
        
        // Fire: 炎上判定
        em.checkFireIgnite(klAttacker, victim);
        
        // Ice: Slowness付与判定
        em.checkIceSlow(klVictim, attacker);
        
        // SPゲージ増加（攻撃者）
        klAttacker.addSpGauge(1);
        if (klAttacker.getSpGauge() >= plugin.getConfigManager().getSpRequiredHits()) {
            attacker.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SP READY! 右クリックで発動！");
        }
        
        // 経験値バーでSPゲージ表示
        updateSPDisplay(attacker, klAttacker);
        
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
    }
    
    /**
     * SPゲージを経験値バーで表示
     */
    private void updateSPDisplay(Player player, KLPlayer klPlayer) {
        int gauge = klPlayer.getSpGauge();
        int max = plugin.getConfigManager().getSpRequiredHits();
        
        player.setLevel(gauge);
        player.setExp(Math.min(0.99f, (float) gauge / max));
    }
}
