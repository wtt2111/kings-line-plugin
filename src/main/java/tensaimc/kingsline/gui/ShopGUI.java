package tensaimc.kingsline.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.item.ShopItem;
import tensaimc.kingsline.item.ShopItemCategory;
import tensaimc.kingsline.item.ShopItemRegistry;
import tensaimc.kingsline.player.KLPlayer;

import java.util.*;

/**
 * 個人ショップGUI（Lumina消費）
 * カテゴリ別に整理されたレイアウト
 */
public class ShopGUI {
    
    private static final String TITLE = ChatColor.GOLD + "✦ ショップ";
    private static final int GUI_SIZE = 54; // 6段
    
    private final KingsLine plugin;
    
    // プレイヤーごとのスロット→ShopItemマッピング
    private final Map<UUID, Map<Integer, ShopItem>> playerSlotMap = new HashMap<>();
    
    public ShopGUI(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        int lumina = klPlayer != null ? klPlayer.getTotalLumina() : 0;
        
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE);
        Map<Integer, ShopItem> slotMap = new HashMap<>();
        
        // 背景を黒ガラスで埋める
        ItemStack background = createGlass((short) 15, " "); // 黒
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
            inv.setItem(slot, createShopItemDisplay(item, lumina));
            slotMap.put(slot, item);
            slot++;
        }
        slot = 13; // 投擲は13から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.THROWABLE)) {
            inv.setItem(slot, createShopItemDisplay(item, lumina));
            slotMap.put(slot, item);
            slot++;
        }
        
        // ========== 行2: 移動 + 弓矢のラベル行 ==========
        inv.setItem(18, createCategoryGlass(ShopItemCategory.MOBILITY));
        inv.setItem(23, createCategoryGlass(ShopItemCategory.BOW));
        
        // ========== 行3: 移動アイテム + 弓矢アイテム ==========
        slot = 27;
        for (ShopItem item : registry.getByCategory(ShopItemCategory.MOBILITY)) {
            inv.setItem(slot, createShopItemDisplay(item, lumina));
            slotMap.put(slot, item);
            slot++;
        }
        slot = 32; // 弓矢は32から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.BOW)) {
            inv.setItem(slot, createShopItemDisplay(item, lumina));
            slotMap.put(slot, item);
            slot++;
        }
        
        // ========== 行4: エレメントオーブ + 特殊 + キングのラベル ==========
        inv.setItem(36, createCategoryGlass(ShopItemCategory.ELEMENT_ORB));
        inv.setItem(40, createCategoryGlass(ShopItemCategory.SPECIAL));
        inv.setItem(43, createCategoryGlass(ShopItemCategory.KING));
        
        // ========== 行5: オーブ + 特殊 + キングアイテム ==========
        slot = 45;
        for (ShopItem item : registry.getByCategory(ShopItemCategory.ELEMENT_ORB)) {
            inv.setItem(slot, createShopItemDisplay(item, lumina));
            slotMap.put(slot, item);
            slot++;
        }
        slot = 49; // 特殊は49から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.SPECIAL)) {
            if (slot >= 52) break; // キング用にスペース確保
            inv.setItem(slot, createShopItemDisplay(item, lumina));
            slotMap.put(slot, item);
            slot++;
        }
        slot = 52; // キングは52から
        for (ShopItem item : registry.getByCategory(ShopItemCategory.KING)) {
            if (slot >= GUI_SIZE) break;
            inv.setItem(slot, createShopItemDisplay(item, lumina));
            slotMap.put(slot, item);
            slot++;
        }
        
        // Lumina表示（右下）
        inv.setItem(8, createLuminaDisplay(klPlayer));
        
        // プレイヤーのスロットマップを保存
        playerSlotMap.put(player.getUniqueId(), slotMap);
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.SHOP);
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
     * ショップアイテムの表示用ItemStackを作成
     */
    private ItemStack createShopItemDisplay(ShopItem shopItem, int playerLumina) {
        ItemStack item = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        ItemMeta meta = item.getItemMeta();
        
        boolean canAfford = playerLumina >= shopItem.getPrice();
        
        // アイテム名
        String displayName = (canAfford ? ChatColor.GREEN : ChatColor.RED) + shopItem.getDisplayName();
        meta.setDisplayName(displayName);
        
        // Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + shopItem.getDescription());
        lore.add("");
        lore.add(ChatColor.GOLD + "価格: " + ChatColor.WHITE + shopItem.getPrice() + " ✦");
        lore.add("");
        
        if (canAfford) {
            lore.add(ChatColor.GREEN + "» クリックで購入");
        } else {
            lore.add(ChatColor.RED + "✖ Luminaが足りません");
            lore.add(ChatColor.GRAY + "  (必要: " + shopItem.getPrice() + ", 所持: " + playerLumina + ")");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Lumina表示アイテム
     */
    private ItemStack createLuminaDisplay(KLPlayer klPlayer) {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ 所持 Lumina");
        
        List<String> lore = new ArrayList<>();
        if (klPlayer != null) {
            lore.add("");
            lore.add(ChatColor.GRAY + "所持中: " + ChatColor.WHITE + klPlayer.getLuminaCarrying());
            lore.add(ChatColor.GREEN + "貯金済: " + ChatColor.WHITE + klPlayer.getLuminaSaved());
            lore.add("");
            lore.add(ChatColor.YELLOW + "合計: " + ChatColor.WHITE + ChatColor.BOLD + klPlayer.getTotalLumina() + " ✦");
        } else {
            lore.add(ChatColor.WHITE + "0 ✦");
        }
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * クリック処理
     */
    public void handleClick(Player player, int slot) {
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        // プレイヤーのスロットマップを取得
        Map<Integer, ShopItem> slotMap = playerSlotMap.get(player.getUniqueId());
        if (slotMap == null) {
            return;
        }
        
        // スロットからShopItemを取得
        ShopItem shopItem = slotMap.get(slot);
        if (shopItem == null) {
            return;
        }
        
        // ロイヤルコール（キング専用アイテム）の購入制限
        if (shopItem.getId().equals("royal_call") && !klPlayer.isKing()) {
            player.sendMessage(ChatColor.RED + "このアイテムはキングのみ購入できます。");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // 購入処理
        if (!klPlayer.hasLumina(shopItem.getPrice())) {
            player.sendMessage(ChatColor.RED + "Luminaが足りません。(必要: " + shopItem.getPrice() + 
                    ", 所持: " + klPlayer.getTotalLumina() + ")");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Lumina消費
        klPlayer.spendLumina(shopItem.getPrice());
        
        // アイテム付与
        ItemStack purchasedItem = shopItem.createItemStack();
        player.getInventory().addItem(purchasedItem);
        
        player.sendMessage(ChatColor.GREEN + "✦ " + shopItem.getDisplayName() + " を購入しました！");
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
        
        // GUIを更新
        open(player);
    }
    
    /**
     * プレイヤーのスロットマップをクリア（GUI閉じた時）
     */
    public void clearPlayerData(UUID uuid) {
        playerSlotMap.remove(uuid);
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE.equals(title);
    }
}
