package tensaimc.kingsline.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * NPCメニューGUI（ショップ/アップグレード選択）
 */
public class NPCMenuGUI {
    
    private static final String TITLE = ChatColor.DARK_GREEN + "拠点メニュー";
    
    private final KingsLine plugin;
    
    // スロット配置
    private static final int SLOT_SHOP = 11;
    private static final int SLOT_UPGRADE = 15;
    private static final int SLOT_SHARD_INFO = 31;
    
    public NPCMenuGUI(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);
        
        // 背景
        ItemStack gray = createFillerItem(Material.STAINED_GLASS_PANE, (short) 7);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, gray);
        }
        
        // ショップボタン
        inv.setItem(SLOT_SHOP, createShopButton(klPlayer));
        
        // アップグレードボタン
        inv.setItem(SLOT_UPGRADE, createUpgradeButton(klPlayer));
        
        // Shard/Lumina情報
        inv.setItem(SLOT_SHARD_INFO, createResourceInfo(klPlayer));
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.NPC_MENU);
    }
    
    /**
     * ショップボタン
     */
    private ItemStack createShopButton(KLPlayer klPlayer) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "✦ 個人ショップ");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Luminaを使ってアイテムを購入");
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "所持Lumina: " + ChatColor.WHITE + klPlayer.getTotalLumina());
        lore.add("");
        lore.add(ChatColor.YELLOW + "クリックで開く");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * アップグレードボタン
     */
    private ItemStack createUpgradeButton(KLPlayer klPlayer) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "◈ チームアップグレード");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Shardを投資してチームを強化");
        lore.add("");
        lore.add(ChatColor.GREEN + "貯金Shard: " + ChatColor.WHITE + klPlayer.getShardSaved());
        lore.add("");
        lore.add(ChatColor.YELLOW + "クリックで開く");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * リソース情報
     */
    private ItemStack createResourceInfo(KLPlayer klPlayer) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.YELLOW + "あなたのリソース");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.AQUA + "◈ Shard:");
        lore.add(ChatColor.WHITE + "  所持: " + klPlayer.getShardCarrying());
        lore.add(ChatColor.GREEN + "  貯金: " + klPlayer.getShardSaved());
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "✦ Lumina:");
        lore.add(ChatColor.WHITE + "  所持: " + klPlayer.getLuminaCarrying());
        lore.add(ChatColor.GREEN + "  貯金: " + klPlayer.getLuminaSaved());
        lore.add("");
        lore.add(ChatColor.GRAY + "所持中は死亡時にドロップ");
        lore.add(ChatColor.GRAY + "貯金は安全に保管されます");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * フィラーアイテム作成
     */
    private ItemStack createFillerItem(Material material, short data) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * クリック処理
     */
    public void handleClick(Player player, int slot) {
        switch (slot) {
            case SLOT_SHOP:
                player.closeInventory();
                plugin.getShopGUI().open(player);
                break;
            case SLOT_UPGRADE:
                player.closeInventory();
                plugin.getUpgradeGUI().open(player);
                break;
        }
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE.equals(title);
    }
}
