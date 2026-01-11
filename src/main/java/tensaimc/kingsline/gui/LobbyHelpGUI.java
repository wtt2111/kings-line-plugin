package tensaimc.kingsline.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;

import java.util.ArrayList;
import java.util.List;

/**
 * ロビーヘルプメインメニューGUI
 */
public class LobbyHelpGUI {
    
    public static final String TITLE = ChatColor.GOLD + "⚔ KING'S LINE ガイド ⚔";
    private static final int GUI_SIZE = 45; // 5行
    
    private final KingsLine plugin;
    
    public LobbyHelpGUI(KingsLine plugin) {
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
        
        // タイトルアイコン (slot 4)
        inv.setItem(4, createTitleItem());
        
        // ゲームルール (slot 20)
        inv.setItem(20, createGameRulesItem());
        
        // コマンド一覧 (slot 21)
        inv.setItem(21, createCommandsItem());
        
        // エレメント情報 (slot 23)
        inv.setItem(23, createElementsItem());
        
        // ショッププレビュー (slot 24)
        inv.setItem(24, createShopItem());
        
        // 閉じるボタン (slot 40)
        inv.setItem(40, createCloseItem());
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.LOBBY_HELP);
    }
    
    /**
     * タイトルアイコンを作成
     */
    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ KING'S LINE ⚔");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "2チーム対戦型 PvP ミニゲーム");
        lore.add("");
        lore.add(ChatColor.YELLOW + "下のアイコンをクリックして");
        lore.add(ChatColor.YELLOW + "詳細を確認しよう！");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * ゲームルールアイテムを作成
     */
    private ItemStack createGameRulesItem() {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "ゲームルール");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "ゲームの概要と勝利条件、");
        lore.add(ChatColor.GRAY + "ポイントの稼ぎ方などを確認");
        lore.add("");
        lore.add(ChatColor.GREEN + "» クリックで詳細を見る");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * コマンド一覧アイテムを作成
     */
    private ItemStack createCommandsItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "コマンド一覧");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "使えるコマンドと");
        lore.add(ChatColor.GRAY + "その使い方を確認");
        lore.add("");
        lore.add(ChatColor.GREEN + "» クリックで詳細を見る");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * エレメント情報アイテムを作成
     */
    private ItemStack createElementsItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "エレメント情報");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Fire, Ice, Wind, Earth");
        lore.add(ChatColor.GRAY + "各エレメントの特性を確認");
        lore.add("");
        lore.add(ChatColor.GREEN + "» クリックで詳細を見る");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * ショッププレビューアイテムを作成
     */
    private ItemStack createShopItem() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "ショップアイテム");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Luminaで購入できる");
        lore.add(ChatColor.GRAY + "アイテム一覧を確認");
        lore.add("");
        lore.add(ChatColor.GREEN + "» クリックで詳細を見る");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 閉じるアイテムを作成
     */
    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.RED + "閉じる");
        
        item.setItemMeta(meta);
        return item;
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
     * クリック処理
     */
    public void handleClick(Player player, int slot) {
        switch (slot) {
            case 20: // ゲームルール
                plugin.getGameRulesGUI().open(player);
                break;
            case 21: // コマンド一覧
                plugin.getCommandHelpGUI().open(player);
                break;
            case 23: // エレメント情報
                plugin.getElementInfoGUI().open(player);
                break;
            case 24: // ショッププレビュー
                plugin.getShopPreviewGUI().open(player);
                break;
            case 40: // 閉じる
                player.closeInventory();
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
