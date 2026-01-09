package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;

/**
 * NPC関連のリスナー
 */
public class NPCListener implements Listener {
    
    private final KingsLine plugin;
    
    public NPCListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        
        if (!(entity instanceof Villager)) {
            return;
        }
        
        // ゲームNPCかチェック
        if (!plugin.getNPCManager().isGameNPC(entity)) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中のみ
        if (!gm.isState(GameState.RUNNING)) {
            player.sendMessage(ChatColor.RED + "ゲーム中のみ利用可能です。");
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        // 自チームのNPCかチェック
        Team npcTeam = plugin.getNPCManager().getNPCTeam(entity);
        if (npcTeam != klPlayer.getTeam()) {
            player.sendMessage(ChatColor.RED + "これは相手チームのNPCです！");
            return;
        }
        
        // まずShard/Luminaを預ける（拠点に戻った扱い）
        if (klPlayer.getShardCarrying() > 0) {
            plugin.getShardManager().onReturnToBase(klPlayer);
        }
        if (klPlayer.getLuminaCarrying() > 0) {
            plugin.getLuminaManager().onReturnToBase(klPlayer);
        }
        
        // ショップ/アップグレードGUIを開く（スニークでアップグレード）
        if (player.isSneaking()) {
            plugin.getUpgradeGUI().open(player);
        } else {
            plugin.getShopGUI().open(player);
        }
    }
}
