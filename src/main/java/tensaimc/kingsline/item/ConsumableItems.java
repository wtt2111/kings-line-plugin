package tensaimc.kingsline.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tensaimc.kingsline.KingsLine;

/**
 * 回復系アイテム
 */
public class ConsumableItems {
    
    public static void registerAll(ShopItemRegistry registry) {
        registry.register(new FirstAidKit());
        registry.register(new GoldenApple());
        registry.register(new SteakPack());
    }
    
    // ========== 応急キット ==========
    public static class FirstAidKit extends AbstractShopItem {
        
        public FirstAidKit() {
            super(
                "first_aid_kit",
                "応急キット",
                "即座に4ハート回復",
                Material.PAPER,
                3,  // 価格
                1,  // 個数
                ShopItemCategory.CONSUMABLE
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = super.createItemStack();
            item.setDurability((short) 0);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            double currentHealth = player.getHealth();
            double maxHealth = player.getMaxHealth();
            
            if (currentHealth >= maxHealth) {
                player.sendMessage(ChatColor.YELLOW + "体力は既に満タンです。");
                return false;
            }
            
            // HP8回復（ハート4個分）
            double newHealth = Math.min(currentHealth + 8.0, maxHealth);
            player.setHealth(newHealth);
            
            player.sendMessage(ChatColor.GREEN + "✚ HP回復！(+4❤)");
            player.playSound(player.getLocation(), Sound.DRINK, 1.0f, 1.2f);
            
            return true; // trueを返すとItemListenerでアイテムが消費される
        }
    }
    
    // ========== 金のリンゴ ==========
    public static class GoldenApple extends AbstractShopItem {
        
        public GoldenApple() {
            super(
                "golden_apple",
                "金のリンゴ",
                "回復 + 吸収効果",
                Material.GOLDEN_APPLE,
                4,  // 価格
                1,  // 個数
                ShopItemCategory.CONSUMABLE
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            // 普通の金のリンゴを作成（使用時にバニラの効果が発動）
            ItemStack item = new ItemStack(Material.GOLDEN_APPLE, amount);
            // 金のリンゴはバニラで使用可能なのでLore不要
            return item;
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // バニラ動作で食べるアイテムなので、ShopItemとしてマッチさせない
            return false;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // バニラの動作に任せる（食べる動作）
            return false;
        }
    }
    
    // ========== ステーキパック ==========
    public static class SteakPack extends AbstractShopItem {
        
        public SteakPack() {
            super(
                "steak_pack",
                "ステーキ x3",
                "満腹度回復",
                Material.COOKED_BEEF,
                1,  // 価格
                3,  // 個数
                ShopItemCategory.CONSUMABLE
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            // バニラのステーキを作成（食べられる）
            return new ItemStack(Material.COOKED_BEEF, amount);
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // バニラ動作で食べるアイテムなので、ShopItemとしてマッチさせない
            return false;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // バニラの動作に任せる（食べる動作）
            return false;
        }
    }
}
