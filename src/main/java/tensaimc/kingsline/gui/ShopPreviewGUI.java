package tensaimc.kingsline.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.item.ShopItem;
import tensaimc.kingsline.item.ShopItemCategory;
import tensaimc.kingsline.item.ShopItemRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * ショッププレビューGUI（購入不可、情報表示のみ）
 */
public class ShopPreviewGUI {
    
    public static final String TITLE = ChatColor.GOLD + "✦ ショップ プレビュー";
    private static final int GUI_SIZE = 54; // 6段
    
    private final KingsLine plugin;
    
    public ShopPreviewGUI(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE);
        
        // 背景を黒ガラスで埋める
        ItemStack background = createGlass((short) 15, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, background);
        }
        
        ShopItemRegistry registry = plugin.getShopItemRegistry();
        
        // ========== 行0: 回復 + 投擲のラベル行 ==========
        inv.setItem(0, createCategoryGlass(ShopItemCategory.CONSUMABLE));
        inv.setItem(4, createCategoryGlass(ShopItemCategory.THROWABLE));
        
        // ========== 行1: 回復アイテム + 投擲アイテム ==========
        int slot = 9;
        for (ShopItem item : registry.getByCategory(ShopItemCategory.CONSUMABLE)) {
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        slot = 13; // 投擲は13から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.THROWABLE)) {
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        
        // ========== 行2: 移動 + 弓矢のラベル行 ==========
        inv.setItem(18, createCategoryGlass(ShopItemCategory.MOBILITY));
        inv.setItem(23, createCategoryGlass(ShopItemCategory.BOW));
        
        // ========== 行3: 移動アイテム + 弓矢アイテム ==========
        slot = 27;
        for (ShopItem item : registry.getByCategory(ShopItemCategory.MOBILITY)) {
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        slot = 32; // 弓矢は32から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.BOW)) {
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        
        // ========== 行4: エレメントオーブ + 特殊 + キングのラベル ==========
        inv.setItem(36, createCategoryGlass(ShopItemCategory.ELEMENT_ORB));
        inv.setItem(40, createCategoryGlass(ShopItemCategory.SPECIAL));
        inv.setItem(43, createCategoryGlass(ShopItemCategory.KING));
        
        // ========== 行5: オーブ + 特殊 + キングアイテム ==========
        slot = 45;
        for (ShopItem item : registry.getByCategory(ShopItemCategory.ELEMENT_ORB)) {
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        slot = 49; // 特殊は49から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.SPECIAL)) {
            if (slot >= 52) break; // キング用にスペース確保
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        slot = 52; // キングは52から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.KING)) {
            if (slot >= GUI_SIZE) break;
            inv.setItem(slot, createShopItemDisplay(item));
            slot++;
        }
        
        // 情報表示（右上）
        inv.setItem(8, createInfoDisplay());
        
        // 戻るボタン（左下）
        inv.setItem(45, createBackItem());
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.SHOP_PREVIEW);
    }
    
    /**
     * カテゴリ用の色付きガラスを作成
     */
    private ItemStack createCategoryGlass(ShopItemCategory category) {
        short color;
        switch (category) {
            case CONSUMABLE:
                color = 5; // ライム
                break;
            case THROWABLE:
                color = 4; // 黄色
                break;
            case MOBILITY:
                color = 3; // 水色
                break;
            case BOW:
                color = 7; // 灰色
                break;
            case ELEMENT_ORB:
                color = 2; // マゼンタ
                break;
            case SPECIAL:
                color = 1; // オレンジ
                break;
            case KING:
                color = 14; // 赤
                break;
            default:
                color = 0; // 白
        }
        
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(category.getColoredName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "▼ " + category.getDisplayName());
        meta.setLore(lore);
        
        glass.setItemMeta(meta);
        return glass;
    }
    
    /**
     * ガラスパネルを作成
     */
    private ItemStack createGlass(short color, String name) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(name);
        glass.setItemMeta(meta);
        return glass;
    }
    
    /**
     * ショップアイテムの表示用ItemStackを作成（プレビュー用）
     */
    private ItemStack createShopItemDisplay(ShopItem shopItem) {
        ItemStack item = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        ItemMeta meta = item.getItemMeta();
        
        // アイテム名（プレビュー表示）
        String displayName = ChatColor.WHITE + shopItem.getDisplayName();
        meta.setDisplayName(displayName);
        
        // Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + shopItem.getDescription());
        lore.add("");
        lore.add(ChatColor.GOLD + "価格: " + ChatColor.WHITE + shopItem.getPrice() + " ✦ Lumina");
        lore.add("");
        lore.add(ChatColor.GRAY + "（ゲーム中に購入可能）");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * 情報表示アイテム
     */
    private ItemStack createInfoDisplay() {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ Lumina（ルミナ）とは");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "敵をキルすると獲得できる通貨");
        lore.add("");
        lore.add(ChatColor.YELLOW + "入手方法:");
        lore.add(ChatColor.WHITE + "  キル: +1 Lumina");
        lore.add("");
        lore.add(ChatColor.RED + "⚠ 死亡時に所持中のLuminaをドロップ！");
        lore.add(ChatColor.GREEN + "→ 拠点に戻って貯金しよう");
        lore.add("");
        lore.add(ChatColor.GRAY + "ゲーム中に拠点のNPCから購入できます");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 戻るアイテムを作成
     */
    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GRAY + "« 戻る");
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * クリック処理
     */
    public void handleClick(Player player, int slot) {
        if (slot == 45) {
            // 戻る
            plugin.getLobbyHelpGUI().open(player);
        }
        // その他のクリックは何もしない（プレビューのみ）
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE.equals(title);
    }
}
