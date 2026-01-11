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
 * コマンドヘルプGUI
 */
public class CommandHelpGUI {
    
    public static final String TITLE = ChatColor.YELLOW + "コマンド一覧";
    private static final int GUI_SIZE = 54; // 6行
    
    private final KingsLine plugin;
    
    public CommandHelpGUI(KingsLine plugin) {
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
        
        // タイトル (slot 4)
        inv.setItem(4, createTitleItem());
        
        // パーティーコマンド (slot 10)
        inv.setItem(10, createPartyCommandItem());
        
        // チャットコマンド (slot 12)
        inv.setItem(12, createChatCommandItem());
        
        // キング立候補 (slot 14)
        inv.setItem(14, createKingCommandItem());
        
        // SP技の使い方 (slot 16)
        inv.setItem(16, createSPCommandItem());
        
        // その他のヒント (slot 31)
        inv.setItem(31, createTipsItem());
        
        // 戻るボタン (slot 49)
        inv.setItem(49, createBackItem());
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.COMMAND_HELP);
    }
    
    /**
     * タイトルアイテムを作成
     */
    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "コマンド一覧");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "使えるコマンドを確認しよう！");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * パーティーコマンドアイテムを作成
     */
    private ItemStack createPartyCommandItem() {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "パーティーコマンド");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.WHITE + "/p invite <プレイヤー>");
        lore.add(ChatColor.GRAY + "  → パーティーに招待");
        lore.add("");
        lore.add(ChatColor.WHITE + "/p accept");
        lore.add(ChatColor.GRAY + "  → 招待を承諾");
        lore.add("");
        lore.add(ChatColor.WHITE + "/p deny");
        lore.add(ChatColor.GRAY + "  → 招待を拒否");
        lore.add("");
        lore.add(ChatColor.WHITE + "/p leave");
        lore.add(ChatColor.GRAY + "  → パーティーを脱退");
        lore.add("");
        lore.add(ChatColor.WHITE + "/p list  または  /pl");
        lore.add(ChatColor.GRAY + "  → メンバー一覧を表示");
        lore.add("");
        lore.add(ChatColor.WHITE + "/p disband");
        lore.add(ChatColor.GRAY + "  → パーティーを解散（リーダーのみ）");
        lore.add("");
        lore.add(ChatColor.YELLOW + "※ パーティーは同じチームになります");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * チャットコマンドアイテムを作成
     */
    private ItemStack createChatCommandItem() {
        ItemStack item = new ItemStack(Material.BOOK_AND_QUILL);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "チャットコマンド");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.WHITE + "/chat a  または  /ch a");
        lore.add(ChatColor.GRAY + "  → 全体チャット");
        lore.add("");
        lore.add(ChatColor.WHITE + "/chat t  または  /ch t");
        lore.add(ChatColor.GRAY + "  → チームチャット");
        lore.add("");
        lore.add(ChatColor.WHITE + "/chat p  または  /ch p");
        lore.add(ChatColor.GRAY + "  → パーティーチャット");
        lore.add("");
        lore.add(ChatColor.YELLOW + "現在のモードは名前の横に表示されます");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * キング立候補アイテムを作成
     */
    private ItemStack createKingCommandItem() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "👑 キング立候補");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.WHITE + "チャットで「!king」と発言");
        lore.add("");
        lore.add(ChatColor.GRAY + "  準備フェーズ中に使用可能");
        lore.add(ChatColor.GRAY + "  立候補者の中から投票で選出");
        lore.add(ChatColor.GRAY + "  立候補者がいない場合はランダム");
        lore.add("");
        lore.add(ChatColor.YELLOW + "キングになると:");
        lore.add(ChatColor.GRAY + "  • HP 2倍");
        lore.add(ChatColor.GRAY + "  • 周囲の味方にSpeed効果");
        lore.add(ChatColor.GRAY + "  • ダイヤチェストプレート装備");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * SP技の使い方アイテムを作成
     */
    private ItemStack createSPCommandItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "SP技の使い方");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.WHITE + "Shift + 剣 + 右クリック");
        lore.add("");
        lore.add(ChatColor.GRAY + "  敵に10ヒットでゲージMAX");
        lore.add(ChatColor.GRAY + "  ゲージMAX時に発動可能");
        lore.add("");
        lore.add(ChatColor.YELLOW + "エレメント別SP技:");
        lore.add(ChatColor.RED + "  Fire: Overheat（火力UP）");
        lore.add(ChatColor.AQUA + "  Ice: Ice Age（周囲凍結）");
        lore.add(ChatColor.WHITE + "  Wind: Gale Step（背後にTP）");
        lore.add(ChatColor.GOLD + "  Earth: Bulwark（防御UP）");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * その他のヒントアイテムを作成
     */
    private ItemStack createTipsItem() {
        ItemStack item = new ItemStack(Material.SIGN);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "その他のヒント");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "拠点に戻ると...");
        lore.add(ChatColor.GRAY + "  自動で通貨が貯金されます");
        lore.add("");
        lore.add(ChatColor.YELLOW + "拠点のNPCに話しかけると...");
        lore.add(ChatColor.GRAY + "  ショップ / アップグレードメニュー");
        lore.add("");
        lore.add(ChatColor.YELLOW + "死亡すると...");
        lore.add(ChatColor.GRAY + "  所持中の通貨をドロップ");
        lore.add(ChatColor.GRAY + "  （貯金済みは失われません）");
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
        if (slot == 49) {
            // 戻る
            plugin.getLobbyHelpGUI().open(player);
        }
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE.equals(title);
    }
}
