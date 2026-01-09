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
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.upgrade.TeamUpgrade;
import tensaimc.kingsline.upgrade.UpgradeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * チームアップグレードGUI（Shard投資）
 * 複数人でShardを投資してアップグレードを解放
 */
public class UpgradeGUI {
    
    private static final String TITLE = ChatColor.DARK_AQUA + "チームアップグレード";
    
    private final KingsLine plugin;
    
    // スロット配置
    private static final int SLOT_ARMOR = 10;
    private static final int SLOT_WEAPON = 11;
    private static final int SLOT_PROTECTION = 12;
    private static final int SLOT_SHARPNESS = 14;
    private static final int SLOT_HEALTH = 15;
    private static final int SLOT_SPEED = 16;
    
    private static final int SLOT_INVEST_1 = 28;
    private static final int SLOT_INVEST_10 = 29;
    private static final int SLOT_INVEST_50 = 30;
    private static final int SLOT_INVEST_ALL = 31;
    
    private static final int SLOT_SHARD_INFO = 35;
    
    public UpgradeGUI(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        open(player, null);
    }
    
    /**
     * GUIを開く（特定のアップグレードを選択状態で）
     */
    public void open(Player player, TeamUpgrade selectedUpgrade) {
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        Team team = klPlayer.getTeam();
        UpgradeManager um = plugin.getUpgradeManager();
        
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        
        // 背景
        ItemStack gray = createFillerItem(Material.STAINED_GLASS_PANE, (short) 7);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, gray);
        }
        
        // アップグレード一覧
        inv.setItem(SLOT_ARMOR, createUpgradeItem(TeamUpgrade.ARMOR, team, um, selectedUpgrade));
        inv.setItem(SLOT_WEAPON, createUpgradeItem(TeamUpgrade.WEAPON, team, um, selectedUpgrade));
        inv.setItem(SLOT_PROTECTION, createUpgradeItem(TeamUpgrade.PROTECTION, team, um, selectedUpgrade));
        inv.setItem(SLOT_SHARPNESS, createUpgradeItem(TeamUpgrade.SHARPNESS, team, um, selectedUpgrade));
        inv.setItem(SLOT_HEALTH, createUpgradeItem(TeamUpgrade.HEALTH, team, um, selectedUpgrade));
        inv.setItem(SLOT_SPEED, createUpgradeItem(TeamUpgrade.SPEED, team, um, selectedUpgrade));
        
        // 選択中のアップグレードがあれば投資ボタンを表示
        if (selectedUpgrade != null) {
            inv.setItem(SLOT_INVEST_1, createInvestButton(1, klPlayer));
            inv.setItem(SLOT_INVEST_10, createInvestButton(10, klPlayer));
            inv.setItem(SLOT_INVEST_50, createInvestButton(50, klPlayer));
            inv.setItem(SLOT_INVEST_ALL, createInvestAllButton(klPlayer));
            
            // 選択中の表示
            ItemStack selectedInfo = new ItemStack(Material.BOOK);
            ItemMeta meta = selectedInfo.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "選択中: " + selectedUpgrade.getDisplayName());
            
            int currentLevel = um.getLevel(team, selectedUpgrade);
            int currentProgress = um.getProgress(team, selectedUpgrade);
            int requiredCost = um.getRequiredCost(team, selectedUpgrade);
            
            List<String> lore = new ArrayList<>();
            for (String line : selectedUpgrade.createLore(currentLevel, currentProgress, requiredCost)) {
                lore.add(line);
            }
            meta.setLore(lore);
            selectedInfo.setItemMeta(meta);
            inv.setItem(22, selectedInfo);
        }
        
        // Shard情報
        inv.setItem(SLOT_SHARD_INFO, createShardInfo(klPlayer));
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.UPGRADE);
        plugin.getGUIManager().setSelectedUpgrade(player.getUniqueId(), selectedUpgrade);
    }
    
    /**
     * アップグレードアイテムを作成
     */
    private ItemStack createUpgradeItem(TeamUpgrade upgrade, Team team, UpgradeManager um, TeamUpgrade selected) {
        ItemStack item = new ItemStack(upgrade.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = um.getLevel(team, upgrade);
        int maxLevel = upgrade.getMaxTier();
        int currentProgress = um.getProgress(team, upgrade);
        int requiredCost = um.getRequiredCost(team, upgrade);
        
        // 選択中なら光らせる
        boolean isSelected = upgrade == selected;
        String prefix = isSelected ? ChatColor.GREEN + "▶ " : "";
        
        if (currentLevel >= maxLevel) {
            meta.setDisplayName(prefix + ChatColor.GOLD + upgrade.getDisplayName() + 
                    ChatColor.GREEN + " [最大]");
        } else {
            meta.setDisplayName(prefix + ChatColor.YELLOW + upgrade.getDisplayName() + 
                    ChatColor.GRAY + " Lv" + currentLevel + "/" + maxLevel);
        }
        
        List<String> lore = new ArrayList<>();
        for (String line : upgrade.createLore(currentLevel, currentProgress, requiredCost)) {
            lore.add(line);
        }
        
        if (!isSelected && currentLevel < maxLevel) {
            lore.add("");
            lore.add(ChatColor.GRAY + "クリックで選択");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * 投資ボタンを作成
     */
    private ItemStack createInvestButton(int amount, KLPlayer klPlayer) {
        boolean canAfford = klPlayer.getShardSaved() >= amount;
        
        ItemStack item = new ItemStack(canAfford ? Material.EMERALD : Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName((canAfford ? ChatColor.GREEN : ChatColor.RED) + 
                "◈" + amount + " を投資");
        
        List<String> lore = new ArrayList<>();
        if (canAfford) {
            lore.add(ChatColor.GRAY + "クリックで投資");
        } else {
            lore.add(ChatColor.RED + "Shardが足りません");
        }
        lore.add(ChatColor.GRAY + "貯金Shard: " + klPlayer.getShardSaved());
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 全額投資ボタン
     */
    private ItemStack createInvestAllButton(KLPlayer klPlayer) {
        int saved = klPlayer.getShardSaved();
        boolean canAfford = saved > 0;
        
        ItemStack item = new ItemStack(canAfford ? Material.DIAMOND : Material.COAL);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName((canAfford ? ChatColor.AQUA : ChatColor.RED) + 
                "全額投資 (◈" + saved + ")");
        
        List<String> lore = new ArrayList<>();
        if (canAfford) {
            lore.add(ChatColor.GRAY + "貯金Shardをすべて投資");
        } else {
            lore.add(ChatColor.RED + "投資するShardがありません");
        }
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Shard情報
     */
    private ItemStack createShardInfo(KLPlayer klPlayer) {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.AQUA + "◈ あなたのShard");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "所持中: " + ChatColor.WHITE + klPlayer.getShardCarrying());
        lore.add(ChatColor.GREEN + "貯金: " + ChatColor.WHITE + klPlayer.getShardSaved());
        lore.add("");
        lore.add(ChatColor.YELLOW + "貯金Shardでアップグレードに投資");
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
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        TeamUpgrade selectedUpgrade = plugin.getGUIManager().getSelectedUpgrade(player.getUniqueId());
        
        // アップグレード選択
        TeamUpgrade clicked = getUpgradeFromSlot(slot);
        if (clicked != null) {
            open(player, clicked);
            return;
        }
        
        // 投資ボタン
        if (selectedUpgrade != null) {
            int investAmount = 0;
            
            switch (slot) {
                case SLOT_INVEST_1:
                    investAmount = 1;
                    break;
                case SLOT_INVEST_10:
                    investAmount = 10;
                    break;
                case SLOT_INVEST_50:
                    investAmount = 50;
                    break;
                case SLOT_INVEST_ALL:
                    investAmount = klPlayer.getShardSaved();
                    break;
            }
            
            if (investAmount > 0) {
                plugin.getUpgradeManager().invest(klPlayer, selectedUpgrade, investAmount);
                open(player, selectedUpgrade);
            }
        }
    }
    
    /**
     * スロットからアップグレードを取得
     */
    private TeamUpgrade getUpgradeFromSlot(int slot) {
        switch (slot) {
            case SLOT_ARMOR: return TeamUpgrade.ARMOR;
            case SLOT_WEAPON: return TeamUpgrade.WEAPON;
            case SLOT_PROTECTION: return TeamUpgrade.PROTECTION;
            case SLOT_SHARPNESS: return TeamUpgrade.SHARPNESS;
            case SLOT_HEALTH: return TeamUpgrade.HEALTH;
            case SLOT_SPEED: return TeamUpgrade.SPEED;
            default: return null;
        }
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE.equals(title);
    }
}
