package tensaimc.kingsline.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.element.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * エレメント情報GUI（情報表示のみ、選択機能なし）
 */
public class ElementInfoGUI {
    
    public static final String TITLE = ChatColor.LIGHT_PURPLE + "エレメント情報";
    private static final int GUI_SIZE = 27; // 3行
    
    private final KingsLine plugin;
    
    public ElementInfoGUI(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE);
        
        // 背景
        GUIManager.fillBorder(inv);
        
        // Fire (slot 10)
        inv.setItem(10, createElementItem(Element.FIRE));
        
        // Ice (slot 12)
        inv.setItem(12, createElementItem(Element.ICE));
        
        // Wind (slot 14)
        inv.setItem(14, createElementItem(Element.WIND));
        
        // Earth (slot 16)
        inv.setItem(16, createElementItem(Element.EARTH));
        
        // 戻るボタン (slot 22)
        inv.setItem(22, createBackItem());
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.ELEMENT_INFO);
    }
    
    /**
     * エレメントアイテムを作成
     */
    private ItemStack createElementItem(Element element) {
        ItemStack item = new ItemStack(element.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(element.getColor() + "" + ChatColor.BOLD + element.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + element.getDescription());
        lore.add("");
        
        switch (element) {
            case FIRE:
                lore.add(ChatColor.YELLOW + "パッシブ:");
                lore.add(ChatColor.GREEN + "  与ダメージ +20%");
                lore.add(ChatColor.GREEN + "  攻撃時10%で炎上付与");
                lore.add(ChatColor.RED + "  被ダメージ +15%");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Overheat" + ChatColor.GRAY + " [10HIT]");
                lore.add(ChatColor.WHITE + "  5秒間、与ダメ+40%、確定炎上");
                break;
                
            case ICE:
                lore.add(ChatColor.YELLOW + "パッシブ:");
                lore.add(ChatColor.GREEN + "  KB耐性 50%");
                lore.add(ChatColor.GREEN + "  攻撃時20%でSlow付与");
                lore.add(ChatColor.RED + "  移動速度 -30%");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Ice Age" + ChatColor.GRAY + " [10HIT]");
                lore.add(ChatColor.WHITE + "  周囲6m内の敵2人を4秒凍結");
                break;
                
            case WIND:
                lore.add(ChatColor.YELLOW + "パッシブ:");
                lore.add(ChatColor.GREEN + "  常時 Speed I + 基礎速度UP");
                lore.add(ChatColor.RED + "  被ダメージ +10%");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Gale Step" + ChatColor.GRAY + " [7HIT]");
                lore.add(ChatColor.WHITE + "  敵の背後にTP + 11秒Speed II");
                break;
                
            case EARTH:
                lore.add(ChatColor.YELLOW + "パッシブ:");
                lore.add(ChatColor.GREEN + "  被ダメージ -30%");
                lore.add(ChatColor.GREEN + "  10%でダメージ完全無効");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Bulwark" + ChatColor.GRAY + " [10HIT]");
                lore.add(ChatColor.WHITE + "  5秒間、被ダメ-80%");
                break;
        }
        
        lore.add("");
        lore.add(ChatColor.GRAY + "（ゲーム開始時に選択できます）");
        
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
     * クリック処理
     */
    public void handleClick(Player player, int slot) {
        if (slot == 22) {
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
