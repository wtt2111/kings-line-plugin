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
                lore.add(ChatColor.WHITE + "  与ダメージ +7%");
                lore.add(ChatColor.WHITE + "  10%確率で炎上付与");
                lore.add(ChatColor.RED + "  被ダメージ +5%");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Overheat");
                lore.add(ChatColor.WHITE + "  5秒間、与ダメ+20%、確定炎上");
                break;
                
            case ICE:
                lore.add(ChatColor.YELLOW + "パッシブ:");
                lore.add(ChatColor.WHITE + "  エリア内でKB耐性 -20%");
                lore.add(ChatColor.WHITE + "  被弾時20%でSlow付与");
                lore.add(ChatColor.RED + "  移動速度 -5%");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Ice Age");
                lore.add(ChatColor.WHITE + "  周囲の敵2人を1.5秒フリーズ");
                break;
                
            case WIND:
                lore.add(ChatColor.YELLOW + "パッシブ:");
                lore.add(ChatColor.WHITE + "  常時 Speed I");
                lore.add(ChatColor.WHITE + "  常時 Jump Boost I");
                lore.add(ChatColor.RED + "  被ダメージ +10%");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Gale Step");
                lore.add(ChatColor.WHITE + "  敵の背後にテレポート");
                break;
                
            case EARTH:
                lore.add(ChatColor.YELLOW + "パッシブ:");
                lore.add(ChatColor.WHITE + "  被ダメージ -10%");
                lore.add(ChatColor.WHITE + "  KB耐性 -30%");
                lore.add(ChatColor.RED + "  移動速度 -5%");
                lore.add("");
                lore.add(ChatColor.GOLD + "SP技: Bulwark");
                lore.add(ChatColor.WHITE + "  5秒間、超高耐久の壁に");
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
