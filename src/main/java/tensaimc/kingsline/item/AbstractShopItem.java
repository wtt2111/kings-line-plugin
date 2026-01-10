package tensaimc.kingsline.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * ショップアイテムの基底クラス
 */
public abstract class AbstractShopItem implements ShopItem {
    
    protected final String id;
    protected final String displayName;
    protected final String description;
    protected final Material material;
    protected final int price;
    protected final int amount;
    protected final ShopItemCategory category;
    
    // アイテム識別用のLore接頭辞
    protected static final String SHOP_ITEM_IDENTIFIER = ChatColor.DARK_GRAY + "KL-Shop:";
    
    public AbstractShopItem(String id, String displayName, String description, 
                           Material material, int price, int amount, ShopItemCategory category) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.price = price;
        this.amount = amount;
        this.category = category;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public Material getMaterial() {
        return material;
    }
    
    @Override
    public int getPrice() {
        return price;
    }
    
    @Override
    public int getAmount() {
        return amount;
    }
    
    @Override
    public ShopItemCategory getCategory() {
        return category;
    }
    
    @Override
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(category.getColor() + displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(ChatColor.YELLOW + "右クリックで使用");
        lore.add(SHOP_ITEM_IDENTIFIER + id);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    @Override
    public boolean matches(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return false;
        }
        
        List<String> lore = meta.getLore();
        for (String line : lore) {
            if (line.equals(SHOP_ITEM_IDENTIFIER + id)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * アイテムを1つ消費
     */
    protected void consumeItem(ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            item.setAmount(0);
        }
    }
}
