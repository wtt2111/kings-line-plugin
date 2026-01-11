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
import tensaimc.kingsline.player.KLPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * エレメント選択GUI
 */
public class ElementSelectGUI {
    
    private static final String TITLE = ChatColor.DARK_PURPLE + "エレメント選択";
    
    private final KingsLine plugin;
    
    public ElementSelectGUI(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        
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
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.ELEMENT_SELECT);
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
        lore.add(ChatColor.GREEN + "クリックで選択");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * クリック処理
     */
    public void handleClick(Player player, int slot) {
        Element selected = null;
        
        switch (slot) {
            case 10:
                selected = Element.FIRE;
                break;
            case 12:
                selected = Element.ICE;
                break;
            case 14:
                selected = Element.WIND;
                break;
            case 16:
                selected = Element.EARTH;
                break;
        }
        
        if (selected != null) {
            KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
            if (klPlayer != null) {
                klPlayer.setElement(selected);
                
                // パッシブ効果を適用
                plugin.getElementManager().applyPassiveEffects(klPlayer);
                
                // 統計: エレメント選択を記録
                plugin.getStatsDatabase().addElementPick(player.getUniqueId(), selected);
                
                player.sendMessage(selected.getColor() + selected.getName() + 
                        ChatColor.GREEN + " を選択しました！");
                player.closeInventory();
            }
        }
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE.equals(title);
    }
}
