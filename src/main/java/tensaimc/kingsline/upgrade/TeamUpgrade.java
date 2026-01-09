package tensaimc.kingsline.upgrade;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * チームアップグレードの種類
 */
public enum TeamUpgrade {
    
    // 防具アップグレード
    ARMOR("防具", Material.DIAMOND_CHESTPLATE, new String[]{
            "皮装備",
            "鉄装備",
            "ダイヤ装備"
    }, new int[]{0, 100, 250}),
    
    // 武器アップグレード
    WEAPON("武器", Material.DIAMOND_SWORD, new String[]{
            "木の剣",
            "石の剣",
            "鉄の剣",
            "ダイヤの剣"
    }, new int[]{0, 50, 120, 250}),
    
    // プロテクションエンチャント
    PROTECTION("プロテクション", Material.ENCHANTED_BOOK, new String[]{
            "なし",
            "プロテクション I",
            "プロテクション II",
            "プロテクション III"
    }, new int[]{0, 80, 160, 300}),
    
    // シャープネスエンチャント
    SHARPNESS("シャープネス", Material.ENCHANTED_BOOK, new String[]{
            "なし",
            "シャープネス I",
            "シャープネス II",
            "シャープネス III"
    }, new int[]{0, 80, 160, 300}),
    
    // HP強化
    HEALTH("生命力強化", Material.GOLDEN_APPLE, new String[]{
            "なし",
            "HP +2 ハート",
            "HP +4 ハート"
    }, new int[]{0, 150, 350}),
    
    // スピード強化
    SPEED("俊足", Material.SUGAR, new String[]{
            "なし",
            "移動速度 +10%",
            "移動速度 +20%"
    }, new int[]{0, 120, 280}),
    
    // 弓ダメージ
    BOW_POWER("鷹の目", Material.BOW, new String[]{
            "なし",
            "弓ダメージ +15%",
            "弓ダメージ +30%"
    }, new int[]{0, 100, 220});
    
    private final String displayName;
    private final Material icon;
    private final String[] tierDescriptions;
    private final int[] tierCosts;
    
    TeamUpgrade(String displayName, Material icon, String[] tierDescriptions, int[] tierCosts) {
        this.displayName = displayName;
        this.icon = icon;
        this.tierDescriptions = tierDescriptions;
        this.tierCosts = tierCosts;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public int getMaxTier() {
        return tierDescriptions.length - 1;
    }
    
    public String getTierDescription(int tier) {
        if (tier < 0 || tier >= tierDescriptions.length) {
            return "???";
        }
        return tierDescriptions[tier];
    }
    
    public int getTierCost(int tier) {
        if (tier < 0 || tier >= tierCosts.length) {
            return Integer.MAX_VALUE;
        }
        return tierCosts[tier];
    }
    
    /**
     * 次のティアにアップグレードするのに必要なコスト
     */
    public int getNextTierCost(int currentTier) {
        int nextTier = currentTier + 1;
        if (nextTier >= tierCosts.length) {
            return Integer.MAX_VALUE;
        }
        return tierCosts[nextTier];
    }
    
    /**
     * GUI用の説明を作成
     */
    public String[] createLore(int currentTier, int currentProgress, int requiredCost) {
        boolean isMaxed = currentTier >= getMaxTier();
        
        if (isMaxed) {
            return new String[]{
                    ChatColor.GREEN + "現在: " + getTierDescription(currentTier),
                    "",
                    ChatColor.GOLD + "最大レベルに到達！"
            };
        }
        
        int remaining = requiredCost - currentProgress;
        int progressPercent = requiredCost > 0 ? (currentProgress * 100 / requiredCost) : 0;
        
        // プログレスバー
        StringBuilder progressBar = new StringBuilder();
        int bars = 20;
        int filledBars = progressPercent * bars / 100;
        for (int i = 0; i < bars; i++) {
            if (i < filledBars) {
                progressBar.append(ChatColor.GREEN).append("█");
            } else {
                progressBar.append(ChatColor.DARK_GRAY).append("█");
            }
        }
        
        return new String[]{
                ChatColor.GRAY + "現在: " + ChatColor.WHITE + getTierDescription(currentTier),
                ChatColor.YELLOW + "次: " + ChatColor.WHITE + getTierDescription(currentTier + 1),
                "",
                progressBar.toString(),
                ChatColor.AQUA + "進捗: " + currentProgress + "/" + requiredCost + " Shard",
                ChatColor.RED + "残り: " + remaining + " Shard",
                "",
                ChatColor.YELLOW + "クリックで貯金Shardを投資"
        };
    }
}
