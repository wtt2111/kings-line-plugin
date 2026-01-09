package tensaimc.kingsline.player;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

/**
 * チームを表すenum
 */
public enum Team {
    
    BLUE("Blue", ChatColor.BLUE, Color.BLUE, DyeColor.BLUE),
    RED("Red", ChatColor.RED, Color.RED, DyeColor.RED),
    NONE("None", ChatColor.GRAY, Color.GRAY, DyeColor.GRAY);
    
    private final String displayName;
    private final ChatColor chatColor;
    private final Color armorColor;
    private final DyeColor dyeColor;
    
    Team(String displayName, ChatColor chatColor, Color armorColor, DyeColor dyeColor) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.armorColor = armorColor;
        this.dyeColor = dyeColor;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public ChatColor getChatColor() {
        return chatColor;
    }
    
    public Color getArmorColor() {
        return armorColor;
    }
    
    public DyeColor getDyeColor() {
        return dyeColor;
    }
    
    /**
     * 色付きの表示名を取得
     */
    public String getColoredName() {
        return chatColor + displayName;
    }
    
    /**
     * 相手チームを取得
     */
    public Team getOpposite() {
        switch (this) {
            case BLUE:
                return RED;
            case RED:
                return BLUE;
            default:
                return NONE;
        }
    }
    
    /**
     * 有効なチーム(BLUE/RED)かどうか
     */
    public boolean isValidTeam() {
        return this != NONE;
    }
}
