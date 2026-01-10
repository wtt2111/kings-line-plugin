package tensaimc.kingsline.element;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/**
 * エレメント（キット）を表すenum
 */
public enum Element {
    
    FIRE("Fire", "ファイア", ChatColor.RED, Material.BLAZE_POWDER,
            "与ダメ+20%, 炎上付与, 被ダメ+15%"),
    
    ICE("Ice", "アイス", ChatColor.AQUA, Material.SNOW_BALL,
            "KB耐性50%, 移動-30%, Slow付与"),
    
    WIND("Wind", "ウィンド", ChatColor.WHITE, Material.FEATHER,
            "Speed常時, SP7HIT"),
    
    EARTH("Earth", "アース", ChatColor.GOLD, Material.CLAY_BALL,
            "被ダメ-30%, 15%無効化");
    
    private final String name;
    private final String japaneseName;
    private final ChatColor color;
    private final Material icon;
    private final String description;
    
    Element(String name, String japaneseName, ChatColor color, Material icon, String description) {
        this.name = name;
        this.japaneseName = japaneseName;
        this.color = color;
        this.icon = icon;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getJapaneseName() {
        return japaneseName;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 色付きの表示名を取得
     */
    public String getColoredName() {
        return color + name;
    }
    
    /**
     * エモジ付きの表示名を取得
     */
    public String getDisplayName() {
        switch (this) {
            case FIRE:
                return ChatColor.RED + "🔥 Fire";
            case ICE:
                return ChatColor.AQUA + "❄ Ice";
            case WIND:
                return ChatColor.WHITE + "🌪 Wind";
            case EARTH:
                return ChatColor.GOLD + "🪨 Earth";
            default:
                return name;
        }
    }
}
