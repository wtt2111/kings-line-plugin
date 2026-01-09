package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.resource.LuminaManager;
import tensaimc.kingsline.resource.ShardManager;

/**
 * アイテム関連のリスナー
 */
public class ItemListener implements Listener {
    
    private final KingsLine plugin;
    
    public ItemListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        ShardManager sm = plugin.getShardManager();
        LuminaManager lm = plugin.getLuminaManager();
        
        // Shardを拾った場合
        if (sm.isShard(item)) {
            int amount = item.getAmount();
            sm.onPickupShard(klPlayer, amount);
            
            // インベントリには入れない（データのみ管理）
            event.setCancelled(true);
            event.getItem().remove();
            return;
        }
        
        // Luminaを拾った場合
        if (lm.isLumina(item)) {
            int amount = item.getAmount();
            lm.onPickupLumina(klPlayer, amount);
            
            // インベントリには入れない
            event.setCancelled(true);
            event.getItem().remove();
            return;
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) {
            return;
        }
        
        // 右クリックのみ
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        GameManager gm = plugin.getGameManager();
        
        // エレメント選択アイテム
        if (isElementSelector(item)) {
            event.setCancelled(true);
            
            if (gm.isState(GameState.STARTING)) {
                plugin.getElementSelectGUI().open(player);
            } else {
                player.sendMessage(ChatColor.RED + "エレメントは準備フェーズ中のみ選択可能です。");
            }
            return;
        }
        
        // SP技アイテム
        if (isSPItem(item)) {
            event.setCancelled(true);
            
            if (!gm.isState(GameState.RUNNING)) {
                return;
            }
            
            KLPlayer klPlayer = gm.getPlayer(player);
            if (klPlayer == null) {
                return;
            }
            
            // SP発動処理
            if (klPlayer.isSpOnCooldown()) {
                long remaining = klPlayer.getSpCooldownRemaining() / 1000;
                player.sendMessage(ChatColor.RED + "クールダウン中です (残り" + remaining + "秒)");
                return;
            }
            
            if (!klPlayer.isSpReady()) {
                player.sendMessage(ChatColor.RED + "SPゲージが溜まっていません (" + 
                        klPlayer.getSpGauge() + "/10)");
                return;
            }
            
            // ElementManagerでSP技を発動
            plugin.getElementManager().activateSpecialAbility(klPlayer);
        }
    }
    
    /**
     * エレメント選択アイテムかどうか
     */
    private boolean isElementSelector(ItemStack item) {
        if (item == null || item.getType() != GameManager.ELEMENT_SELECT_MATERIAL) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().contains("エレメント選択");
    }
    
    /**
     * SP技アイテムかどうか
     */
    private boolean isSPItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().contains("SP技");
    }
}
