package tensaimc.kingsline.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tensaimc.kingsline.KingsLine;

import java.util.*;

/**
 * ショップアイテムの登録・管理クラス
 */
public class ShopItemRegistry {
    
    private final KingsLine plugin;
    private final Map<String, ShopItem> itemsById;
    private final List<ShopItem> allItems;
    
    public ShopItemRegistry(KingsLine plugin) {
        this.plugin = plugin;
        this.itemsById = new HashMap<>();
        this.allItems = new ArrayList<>();
        
        registerAllItems();
    }
    
    /**
     * 全アイテムを登録
     */
    private void registerAllItems() {
        // 回復系
        ConsumableItems.registerAll(this);
        
        // 投擲・妨害系
        ThrowableItems.registerAll(this);
        
        // 移動系
        MobilityItems.registerAll(this);
        
        // 弓・矢系
        BowItems.registerAll(this);
        
        // エレメントオーブ系
        ElementOrbs.registerAll(this);
        
        // 特殊系
        SpecialItems.registerAll(this);
        
        // キング連携系
        KingItems.registerAll(this);
        
        plugin.getLogger().info("Registered " + allItems.size() + " shop items.");
    }
    
    /**
     * アイテムを登録
     */
    public void register(ShopItem item) {
        itemsById.put(item.getId(), item);
        allItems.add(item);
    }
    
    /**
     * IDからアイテムを取得
     */
    public ShopItem getById(String id) {
        return itemsById.get(id);
    }
    
    /**
     * ItemStackからShopItemを取得
     */
    public ShopItem getFromItemStack(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        for (ShopItem shopItem : allItems) {
            if (shopItem.matches(item)) {
                return shopItem;
            }
        }
        
        return null;
    }
    
    /**
     * 全アイテムを取得
     */
    public List<ShopItem> getAllItems() {
        return Collections.unmodifiableList(allItems);
    }
    
    /**
     * カテゴリ別にアイテムを取得
     */
    public List<ShopItem> getByCategory(ShopItemCategory category) {
        List<ShopItem> result = new ArrayList<>();
        for (ShopItem item : allItems) {
            if (item.getCategory() == category) {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * アイテムを使用
     * @return 使用成功した場合true
     */
    public boolean useItem(Player player, ItemStack itemStack) {
        ShopItem shopItem = getFromItemStack(itemStack);
        if (shopItem == null) {
            return false;
        }
        
        return shopItem.use(plugin, player, itemStack);
    }
    
    /**
     * プラグインインスタンスを取得
     */
    public KingsLine getPlugin() {
        return plugin;
    }
}
