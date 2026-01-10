package tensaimc.kingsline.item;

import org.bukkit.ChatColor;

/**
 * ショップアイテムのカテゴリ
 */
public enum ShopItemCategory {
    
    CONSUMABLE("消耗品", ChatColor.GREEN, 0),
    THROWABLE("投擲・妨害", ChatColor.YELLOW, 1),
    MOBILITY("移動系", ChatColor.AQUA, 2),
    BOW("弓・矢", ChatColor.GRAY, 3),
    ELEMENT_ORB("エレメントオーブ", ChatColor.LIGHT_PURPLE, 4),
    SPECIAL("特殊", ChatColor.GOLD, 5),
    KING("キング連携", ChatColor.RED, 6);
    
    private final String displayName;
    private final ChatColor color;
    private final int order;
    
    ShopItemCategory(String displayName, ChatColor color, int order) {
        this.displayName = displayName;
        this.color = color;
        this.order = order;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public String getColoredName() {
        return color + displayName;
    }
    
    public int getOrder() {
        return order;
    }
}
