package tensaimc.kingsline.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.upgrade.TeamUpgrade;

import java.util.*;

/**
 * GUI管理の基盤クラス
 */
public class GUIManager {
    
    private final KingsLine plugin;
    
    // 開いているGUIの種類を追跡
    private final Map<UUID, GUIType> openGUIs;
    
    // 選択中のアップグレード
    private final Map<UUID, TeamUpgrade> selectedUpgrades;
    
    public GUIManager(KingsLine plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
        this.selectedUpgrades = new HashMap<>();
    }
    
    /**
     * GUIタイプ
     */
    public enum GUIType {
        ELEMENT_SELECT,
        SHOP,
        UPGRADE,
        KING_VOTE
    }
    
    /**
     * プレイヤーが開いているGUIを取得
     */
    public GUIType getOpenGUI(UUID uuid) {
        return openGUIs.get(uuid);
    }
    
    /**
     * GUIを開いた状態を記録
     */
    public void setOpenGUI(UUID uuid, GUIType type) {
        openGUIs.put(uuid, type);
    }
    
    /**
     * GUIを閉じた状態を記録
     */
    public void clearOpenGUI(UUID uuid) {
        openGUIs.remove(uuid);
        selectedUpgrades.remove(uuid);
    }
    
    /**
     * 選択中のアップグレードを取得
     */
    public TeamUpgrade getSelectedUpgrade(UUID uuid) {
        return selectedUpgrades.get(uuid);
    }
    
    /**
     * 選択中のアップグレードを設定
     */
    public void setSelectedUpgrade(UUID uuid, TeamUpgrade upgrade) {
        if (upgrade != null) {
            selectedUpgrades.put(uuid, upgrade);
        } else {
            selectedUpgrades.remove(uuid);
        }
    }
    
    /**
     * アイテムを作成
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * アイテムを作成（loreリスト版）
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 装飾用ガラスパネル
     */
    public static ItemStack createFiller() {
        return createItem(Material.STAINED_GLASS_PANE, " ");
    }
    
    /**
     * インベントリを装飾で埋める
     */
    public static void fillBorder(Inventory inv) {
        ItemStack filler = createFiller();
        int size = inv.getSize();
        
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
        }
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, filler);
            inv.setItem(i + 8, filler);
        }
    }
}
