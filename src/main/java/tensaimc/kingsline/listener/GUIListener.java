package tensaimc.kingsline.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.gui.ElementSelectGUI;
import tensaimc.kingsline.gui.KingVoteGUI;
import tensaimc.kingsline.gui.NPCMenuGUI;
import tensaimc.kingsline.gui.ShopGUI;
import tensaimc.kingsline.gui.UpgradeGUI;

/**
 * GUIイベントリスナー
 */
public class GUIListener implements Listener {
    
    private final KingsLine plugin;
    
    public GUIListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        String title = inv.getTitle();
        
        // エレメント選択GUI
        if (ElementSelectGUI.isThisGUI(title)) {
            event.setCancelled(true);
            plugin.getElementSelectGUI().handleClick(player, event.getRawSlot());
            return;
        }
        
        // ショップGUI
        if (ShopGUI.isThisGUI(title)) {
            event.setCancelled(true);
            plugin.getShopGUI().handleClick(player, event.getRawSlot());
            return;
        }
        
        // アップグレードGUI
        if (UpgradeGUI.isThisGUI(title)) {
            event.setCancelled(true);
            plugin.getUpgradeGUI().handleClick(player, event.getRawSlot());
            return;
        }
        
        // NPCメニューGUI
        if (NPCMenuGUI.isThisGUI(title)) {
            event.setCancelled(true);
            plugin.getNPCMenuGUI().handleClick(player, event.getRawSlot());
            return;
        }
        
        // キング投票GUI
        if (KingVoteGUI.isThisGUI(title)) {
            event.setCancelled(true);
            plugin.getKingVoteGUI().handleClick(player, event.getRawSlot());
            return;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        plugin.getGUIManager().clearOpenGUI(player.getUniqueId());
    }
}
