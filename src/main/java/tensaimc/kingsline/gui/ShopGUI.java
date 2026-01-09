package tensaimc.kingsline.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 個人ショップGUI（Lumina消費）
 * 様々なアイテムを購入可能
 */
public class ShopGUI {
    
    private static final String TITLE = ChatColor.GOLD + "✦ ショップ";
    
    private final KingsLine plugin;
    
    // ショップアイテム定義
    private static final ShopItem[] SHOP_ITEMS = {
            // 消耗品
            new ShopItem(10, Material.GOLDEN_APPLE, "金のリンゴ", 8, "回復 + 吸収効果", 1),
            new ShopItem(11, Material.POTION, "治癒のスプラッシュ", 5, "即座にHP回復", 1),
            new ShopItem(12, Material.POTION, "スピード II (30秒)", 6, "移動速度UP", 1),
            new ShopItem(13, Material.POTION, "跳躍 II (30秒)", 4, "ジャンプ力UP", 1),
            new ShopItem(14, Material.MILK_BUCKET, "牛乳", 3, "デバフ解除", 1),
            new ShopItem(15, Material.COOKED_BEEF, "ステーキ x5", 2, "満腹度回復", 5),
            
            // 武器・ツール
            new ShopItem(19, Material.BOW, "弓", 10, "遠距離攻撃", 1),
            new ShopItem(20, Material.ARROW, "矢 x16", 4, "弓の弾薬", 16),
            new ShopItem(21, Material.ARROW, "火矢 x8", 8, "当たると炎上", 8),
            new ShopItem(22, Material.ARROW, "毒矢 x8", 10, "当たると毒II (3秒)", 8),
            new ShopItem(23, Material.IRON_SWORD, "鉄の剣 (早期購入)", 25, "アップグレード前に入手可能", 1),
            new ShopItem(24, Material.IRON_CHESTPLATE, "鉄装備セット", 40, "早期入手用", 1),
            
            // 特殊アイテム
            new ShopItem(28, Material.ENDER_PEARL, "エンダーパール", 15, "テレポート（落下ダメあり）", 1),
            new ShopItem(29, Material.EYE_OF_ENDER, "ゴーストオーブ", 20, "5秒間透明+無敵、攻撃不可", 1),
            new ShopItem(30, Material.SNOW_BALL, "フラッシュバン x3", 6, "当たると盲目3秒", 3),
            new ShopItem(31, Material.FISHING_ROD, "グラップル", 12, "3回使用可、引っ張り移動", 1),
            new ShopItem(32, Material.FIREBALL, "ファイアチャージ x3", 10, "小さな火の玉を投射", 3),
            new ShopItem(33, Material.GOLD_INGOT, "シールドトーテム", 25, "次の一撃を完全無効化（1回）", 1),
            new ShopItem(34, Material.COMPASS, "デスマーク", 10, "次に攻撃した敵の位置を30秒共有", 1)
    };
    
    public ShopGUI(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        int lumina = klPlayer != null ? klPlayer.getTotalLumina() : 0;
        
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        
        // 背景
        ItemStack gray = createFillerItem(Material.STAINED_GLASS_PANE, (short) 7);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, gray);
        }
        
        // カテゴリラベル
        inv.setItem(1, createCategoryLabel(ChatColor.GREEN + "消耗品"));
        inv.setItem(4, createCategoryLabel(ChatColor.RED + "武器・ツール"));
        inv.setItem(7, createCategoryLabel(ChatColor.LIGHT_PURPLE + "特殊アイテム"));
        
        // ショップアイテム
        for (ShopItem shopItem : SHOP_ITEMS) {
            inv.setItem(shopItem.slot, createShopItem(shopItem, lumina));
        }
        
        // Lumina情報
        inv.setItem(40, createLuminaDisplay(klPlayer));
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.SHOP);
    }
    
    /**
     * カテゴリラベル
     */
    private ItemStack createCategoryLabel(String name) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * ショップアイテムを作成
     */
    private ItemStack createShopItem(ShopItem shopItem, int playerLumina) {
        ItemStack item = new ItemStack(shopItem.material, shopItem.amount);
        ItemMeta meta = item.getItemMeta();
        
        boolean canAfford = playerLumina >= shopItem.cost;
        
        meta.setDisplayName((canAfford ? ChatColor.GREEN : ChatColor.RED) + shopItem.name);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + shopItem.description);
        lore.add("");
        lore.add((canAfford ? ChatColor.YELLOW : ChatColor.RED) + 
                "価格: " + ChatColor.WHITE + shopItem.cost + " ✦");
        
        if (canAfford) {
            lore.add(ChatColor.GREEN + "クリックで購入");
        } else {
            lore.add(ChatColor.RED + "Luminaが足りません");
        }
        
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
     * Lumina表示アイテム
     */
    private ItemStack createLuminaDisplay(KLPlayer klPlayer) {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ あなたのLumina");
        
        List<String> lore = new ArrayList<>();
        if (klPlayer != null) {
            lore.add(ChatColor.GRAY + "所持: " + ChatColor.WHITE + klPlayer.getLuminaCarrying());
            lore.add(ChatColor.GREEN + "貯金: " + ChatColor.WHITE + klPlayer.getLuminaSaved());
            lore.add(ChatColor.YELLOW + "合計: " + ChatColor.WHITE + klPlayer.getTotalLumina());
        } else {
            lore.add(ChatColor.WHITE + "0");
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
        
        // ショップアイテムを探す
        ShopItem shopItem = null;
        for (ShopItem si : SHOP_ITEMS) {
            if (si.slot == slot) {
                shopItem = si;
                break;
            }
        }
        
        if (shopItem == null) {
            return;
        }
        
        // 購入処理
        if (!klPlayer.hasLumina(shopItem.cost)) {
            player.sendMessage(ChatColor.RED + "Luminaが足りません。(必要: " + shopItem.cost + 
                    ", 所持: " + klPlayer.getTotalLumina() + ")");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Lumina消費
        klPlayer.spendLumina(shopItem.cost);
        
        // アイテム付与
        ItemStack purchasedItem = createPurchasedItem(shopItem);
        if (purchasedItem != null) {
            player.getInventory().addItem(purchasedItem);
        }
        
        player.sendMessage(ChatColor.GREEN + shopItem.name + " を購入しました！");
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
        
        // GUIを更新
        open(player);
    }
    
    /**
     * 購入したアイテムを作成
     */
    private ItemStack createPurchasedItem(ShopItem shopItem) {
        switch (shopItem.name) {
            case "治癒のスプラッシュ":
                return createSplashPotion(PotionEffectType.HEAL, 0, 1);
                
            case "スピード II (30秒)":
                return createPotion(PotionEffectType.SPEED, 1, 600);
                
            case "跳躍 II (30秒)":
                return createPotion(PotionEffectType.JUMP, 1, 600);
                
            case "火矢 x8":
                return createSpecialArrow(ChatColor.RED + "火矢", "当たると炎上", shopItem.amount);
                
            case "毒矢 x8":
                return createSpecialArrow(ChatColor.GREEN + "毒矢", "当たると毒II", shopItem.amount);
                
            case "グラップル":
                return createGrapple();
                
            case "ゴーストオーブ":
                return createSpecialItem(shopItem.material, ChatColor.LIGHT_PURPLE + "ゴーストオーブ", 
                        "右クリックで5秒間透明+無敵");
                
            case "フラッシュバン x3":
                return createSpecialItem(shopItem.material, ChatColor.YELLOW + "フラッシュバン", 
                        "投げて当たると盲目3秒", shopItem.amount);
                
            case "シールドトーテム":
                return createSpecialItem(Material.GOLD_INGOT, ChatColor.GOLD + "シールドトーテム", 
                        "次の一撃を完全無効化");
                
            case "デスマーク":
                return createSpecialItem(shopItem.material, ChatColor.DARK_RED + "デスマーク", 
                        "次に攻撃した敵の位置を30秒共有");
                
            case "鉄装備セット":
                // 鉄装備一式を付与
                return new ItemStack(Material.IRON_CHESTPLATE);
                
            default:
                return new ItemStack(shopItem.material, shopItem.amount);
        }
    }
    
    /**
     * ポーションを作成
     */
    @SuppressWarnings("deprecation")
    private ItemStack createPotion(PotionEffectType type, int amplifier, int duration) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        meta.setDisplayName(ChatColor.AQUA + type.getName() + " ポーション");
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * スプラッシュポーションを作成
     */
    @SuppressWarnings("deprecation")
    private ItemStack createSplashPotion(PotionEffectType type, int amplifier, int duration) {
        Potion potion = new Potion(PotionType.INSTANT_HEAL);
        potion.setSplash(true);
        return potion.toItemStack(1);
    }
    
    /**
     * 特殊矢を作成
     */
    private ItemStack createSpecialArrow(String name, String description, int amount) {
        ItemStack item = new ItemStack(Material.ARROW, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * グラップルを作成
     */
    private ItemStack createGrapple() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "グラップル");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "引っ張り移動ができる");
        lore.add(ChatColor.YELLOW + "使用回数: 3");
        meta.setLore(lore);
        // 耐久値で使用回数を管理
        item.setDurability((short) 0);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 特殊アイテムを作成
     */
    private ItemStack createSpecialItem(Material material, String name, String description) {
        return createSpecialItem(material, name, description, 1);
    }
    
    private ItemStack createSpecialItem(Material material, String name, String description, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add(ChatColor.YELLOW + "右クリックで使用");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE.equals(title);
    }
    
    /**
     * ショップアイテム定義クラス
     */
    private static class ShopItem {
        final int slot;
        final Material material;
        final String name;
        final int cost;
        final String description;
        final int amount;
        
        ShopItem(int slot, Material material, String name, int cost, String description, int amount) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.cost = cost;
            this.description = description;
            this.amount = amount;
        }
    }
}
