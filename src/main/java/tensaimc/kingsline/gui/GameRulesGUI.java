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
 * ゲームルール・概要GUI
 */
public class GameRulesGUI {
    
    public static final String TITLE = ChatColor.AQUA + "ゲームルール";
    private static final int GUI_SIZE = 54; // 6行
    
    private final KingsLine plugin;
    
    public GameRulesGUI(KingsLine plugin) {
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
        
        // ゲーム概要 (slot 10)
        inv.setItem(10, createOverviewItem());
        
        // ポイント獲得 (slot 12)
        inv.setItem(12, createPointsItem());
        
        // 通貨システム (slot 14)
        inv.setItem(14, createCurrencyItem());
        
        // キングシステム (slot 16)
        inv.setItem(16, createKingItem());
        
        // コアシステム (slot 28)
        inv.setItem(28, createCoreItem());
        
        // エリア占領 (slot 30)
        inv.setItem(30, createAreaItem());
        
        // 勝利条件 (slot 32)
        inv.setItem(32, createWinConditionItem());
        
        // アップグレード (slot 34)
        inv.setItem(34, createUpgradeItem());
        
        // 戻るボタン (slot 49)
        inv.setItem(49, createBackItem());
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.GAME_RULES);
    }
    
    /**
     * タイトルアイテムを作成
     */
    private ItemStack createTitleItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "ゲームルール");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "King's Line のルールを確認しよう！");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * ゲーム概要アイテムを作成
     */
    private ItemStack createOverviewItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "ゲーム概要");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.WHITE + "2チーム対戦PvPゲーム！");
        lore.add("");
        lore.add(ChatColor.YELLOW + "目標:");
        lore.add(ChatColor.GRAY + "  キル、コア破壊、エリア占領で");
        lore.add(ChatColor.GRAY + "  ポイントを稼ぎ、相手チームを");
        lore.add(ChatColor.GRAY + "  全滅させよう！");
        lore.add("");
        lore.add(ChatColor.YELLOW + "プレイ時間:");
        lore.add(ChatColor.GRAY + "  約15〜30分");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * ポイント獲得アイテムを作成
     */
    private ItemStack createPointsItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "ポイント獲得");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "獲得方法:");
        lore.add(ChatColor.WHITE + "  通常キル: " + ChatColor.GREEN + "+1pt");
        lore.add(ChatColor.WHITE + "  キングキル: " + ChatColor.GREEN + "+5pt");
        lore.add(ChatColor.WHITE + "  コア破壊: " + ChatColor.GREEN + "+100pt");
        lore.add(ChatColor.WHITE + "  エリア占領: " + ChatColor.GREEN + "+1pt/3秒");
        lore.add("");
        lore.add(ChatColor.RED + "ペナルティ:");
        lore.add(ChatColor.WHITE + "  キング死亡: " + ChatColor.RED + "-50pt");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 通貨システムアイテムを作成
     */
    private ItemStack createCurrencyItem() {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "通貨システム");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.AQUA + "◈ Shard（シャード）");
        lore.add(ChatColor.GRAY + "  入手: フィールドで拾う");
        lore.add(ChatColor.GRAY + "  用途: チームアップグレード");
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "✦ Lumina（ルミナ）");
        lore.add(ChatColor.GRAY + "  入手: キルで獲得");
        lore.add(ChatColor.GRAY + "  用途: 個人ショップ");
        lore.add("");
        lore.add(ChatColor.RED + "⚠ 死亡時に所持中の通貨をドロップ！");
        lore.add(ChatColor.YELLOW + "→ 拠点に戻って貯金しよう");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * キングシステムアイテムを作成
     */
    private ItemStack createKingItem() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "👑 キングシステム");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "各チームに1人のキング！");
        lore.add("");
        lore.add(ChatColor.GREEN + "キングの特徴:");
        lore.add(ChatColor.WHITE + "  • HP 1.5倍（15ハート）");
        lore.add(ChatColor.WHITE + "  • 周囲8mの味方に再生II");
        lore.add(ChatColor.WHITE + "  • ダイヤチェストプレート装備");
        lore.add("");
        lore.add(ChatColor.RED + "キングキル:");
        lore.add(ChatColor.GRAY + "  • 倒した側 +5pt");
        lore.add("");
        lore.add(ChatColor.GRAY + "準備中に !king で立候補可能");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * コアシステムアイテムを作成
     */
    private ItemStack createCoreItem() {
        ItemStack item = new ItemStack(Material.OBSIDIAN);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "コアシステム");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "各チームの拠点に黒曜石のコア！");
        lore.add("");
        lore.add(ChatColor.GREEN + "破壊すると:");
        lore.add(ChatColor.WHITE + "  • +100pt 獲得");
        lore.add(ChatColor.WHITE + "  • Shard 20個ドロップ");
        lore.add("");
        lore.add(ChatColor.GRAY + "※ 5秒後に再生成されます");
        lore.add(ChatColor.GRAY + "※ 敵接近時は味方に警告が届きます");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * エリア占領アイテムを作成
     */
    private ItemStack createAreaItem() {
        ItemStack item = new ItemStack(Material.WOOL, 1, (short) 11); // 青色羊毛
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "エリア占領");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "中央のBエリアを占領しよう！");
        lore.add("");
        lore.add(ChatColor.GREEN + "占領方法:");
        lore.add(ChatColor.GRAY + "  エリア内で相手より人数が");
        lore.add(ChatColor.GRAY + "  多い状態をキープ");
        lore.add("");
        lore.add(ChatColor.GREEN + "報酬:");
        lore.add(ChatColor.WHITE + "  3秒ごとに +1pt");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 勝利条件アイテムを作成
     */
    private ItemStack createWinConditionItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "⭐ 勝利条件");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.RED + "① 500pt 先取！");
        lore.add(ChatColor.GRAY + "   → 相手チームのリスポーン無効化");
        lore.add("");
        lore.add(ChatColor.RED + "② 相手チームを全滅！");
        lore.add(ChatColor.GRAY + "   → リスポーン無効のチームを");
        lore.add(ChatColor.GRAY + "     全員倒せば勝利！");
        lore.add("");
        lore.add(ChatColor.YELLOW + "時間切れの場合:");
        lore.add(ChatColor.GRAY + "  スコア > キング生存 > 生存者数");
        lore.add(ChatColor.GRAY + "  の順で勝敗を判定");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * アップグレードアイテムを作成
     */
    private ItemStack createUpgradeItem() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "チームアップグレード");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "Shardを投資してチームを強化！");
        lore.add("");
        lore.add(ChatColor.GREEN + "アップグレード例:");
        lore.add(ChatColor.GRAY + "  • 防具強化（皮→鉄→ダイヤ）");
        lore.add(ChatColor.GRAY + "  • 武器強化（木剣→石→鉄→ダイヤ）");
        lore.add(ChatColor.GRAY + "  • プロテクション/シャープネス");
        lore.add(ChatColor.GRAY + "  • 体力増加/移動速度UP");
        lore.add("");
        lore.add(ChatColor.GRAY + "※ 拠点のNPCから購入可能");
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
