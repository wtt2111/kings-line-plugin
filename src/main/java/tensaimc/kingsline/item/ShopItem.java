package tensaimc.kingsline.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tensaimc.kingsline.KingsLine;

/**
 * ショップアイテムのインターフェース
 */
public interface ShopItem {
    
    /**
     * アイテムID（一意の識別子）
     */
    String getId();
    
    /**
     * 表示名
     */
    String getDisplayName();
    
    /**
     * 説明文
     */
    String getDescription();
    
    /**
     * アイテムのMaterial
     */
    Material getMaterial();
    
    /**
     * 価格（Lumina）
     */
    int getPrice();
    
    /**
     * 購入時の個数
     */
    int getAmount();
    
    /**
     * カテゴリ
     */
    ShopItemCategory getCategory();
    
    /**
     * アイテムを使用（右クリック時）
     * @return 消費された場合true
     */
    boolean use(KingsLine plugin, Player player, ItemStack item);
    
    /**
     * 購入時に渡すItemStackを作成
     */
    ItemStack createItemStack();
    
    /**
     * このItemStackがこのショップアイテムかどうか判定
     */
    boolean matches(ItemStack item);
}
