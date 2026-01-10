package tensaimc.kingsline.item;

import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tensaimc.kingsline.KingsLine;

import java.util.ArrayList;
import java.util.List;

/**
 * 弓・矢系アイテム
 */
public class BowItems {
    
    public static void registerAll(ShopItemRegistry registry) {
        registry.register(new Bow());
        registry.register(new FireArrow());
        registry.register(new PoisonArrow());
        registry.register(new ExplosiveArrow());
    }
    
    // ========== 弓 ==========
    public static class Bow extends AbstractShopItem {
        
        public Bow() {
            super(
                "bow",
                "弓",
                "遠距離攻撃",
                Material.BOW,
                4,
                1,
                ShopItemCategory.BOW
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            // バニラの弓
            return new ItemStack(Material.BOW, amount);
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // バニラ動作で撃つので、ShopItemとしてマッチさせない
            return false;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // バニラの動作に任せる
            return false;
        }
    }
    
    // ========== 火矢 ==========
    public static class FireArrow extends AbstractShopItem {
        
        public static final String METADATA_KEY = "kl_fire_arrow";
        
        public FireArrow() {
            super(
                "fire_arrow",
                "火矢 x2",
                "当たると炎上",
                Material.ARROW,
                6,
                2,
                ShopItemCategory.BOW
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.ARROW, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.RED + "火矢");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.RED + "🔥 命中時に炎上付与");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // 矢はバニラの弓で発射するため、ShopItemとしてマッチさせない
            return false;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // 矢は弓で発射される
            return false;
        }
        
        /**
         * 矢が発射された時の処理（EntityShootBowEventから呼び出し）
         */
        public static void onShoot(KingsLine plugin, Arrow arrow) {
            arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
            arrow.setFireTicks(Integer.MAX_VALUE); // 見た目用
        }
        
        /**
         * 着弾時の処理
         */
        public static void onHit(KingsLine plugin, Entity hitEntity) {
            if (hitEntity instanceof Player) {
                Player target = (Player) hitEntity;
                target.setFireTicks(80); // 4秒間炎上
                target.sendMessage(ChatColor.RED + "🔥 炎上！");
            }
        }
    }
    
    // ========== 毒矢 ==========
    public static class PoisonArrow extends AbstractShopItem {
        
        public static final String METADATA_KEY = "kl_poison_arrow";
        
        public PoisonArrow() {
            super(
                "poison_arrow",
                "毒矢 x2",
                "当たると毒II 3秒",
                Material.ARROW,
                6,
                2,
                ShopItemCategory.BOW
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.ARROW, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.DARK_GREEN + "毒矢");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.DARK_GREEN + "☠ 命中時に毒付与");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // 矢はバニラの弓で発射するため、ShopItemとしてマッチさせない
            return false;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            return false;
        }
        
        /**
         * 矢が発射された時の処理
         */
        public static void onShoot(KingsLine plugin, Arrow arrow) {
            arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
        }
        
        /**
         * 着弾時の処理
         */
        public static void onHit(KingsLine plugin, Entity hitEntity) {
            if (hitEntity instanceof Player) {
                Player target = (Player) hitEntity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1)); // 毒II 3秒
                target.sendMessage(ChatColor.DARK_GREEN + "☠ 毒を受けた！");
            }
        }
    }
    
    // ========== 爆発矢 ==========
    public static class ExplosiveArrow extends AbstractShopItem {
        
        public static final String METADATA_KEY = "kl_explosive_arrow";
        
        public ExplosiveArrow() {
            super(
                "explosive_arrow",
                "爆発矢 x3",
                "着弾点で爆発（防具貫通）",
                Material.ARROW,
                10,
                3,
                ShopItemCategory.BOW
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.ARROW, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.GOLD + "爆発矢");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.GOLD + "💥 着弾時に大爆発（防具貫通）");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // 矢はバニラの弓で発射するため、ShopItemとしてマッチさせない
            return false;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            return false;
        }
        
        /**
         * 矢が発射された時の処理
         */
        public static void onShoot(KingsLine plugin, Arrow arrow) {
            arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
        }
        
        /**
         * 着弾時の処理
         */
        public static void onLand(KingsLine plugin, Location location, Player shooter) {
            World world = location.getWorld();
            
            // 爆発エフェクト（ブロック破壊なし）
            world.createExplosion(location.getX(), location.getY(), location.getZ(), 
                    4.0f, false, false);
            
            // 追加のパーティクル
            world.playEffect(location, Effect.EXPLOSION_LARGE, 0);
            world.playEffect(location, Effect.EXPLOSION_HUGE, 0);
            
            // 範囲内の敵に貫通ダメージ（防具・プロテクション無視）
            for (Entity entity : world.getNearbyEntities(location, 7, 7, 7)) {
                if (entity instanceof Player && entity != shooter) {
                    Player target = (Player) entity;
                    
                    double distance = target.getLocation().distance(location);
                    double damage;
                    
                    if (distance <= 1.5) {
                        // 直撃（1.5m以内）: 高ダメージ（防具貫通）
                        damage = 8.0;
                        target.sendMessage(ChatColor.RED + "💥 爆発矢直撃！(貫通)");
                    } else {
                        // 範囲ダメージ: 距離で減衰（防具貫通）
                        // 距離2m: 3ダメージ, 距離4m: 1.5ダメージ, 距離6m: 1ダメージ
                        damage = Math.max(1.0, 6.0 / (distance * 0.8));
                        target.sendMessage(ChatColor.GOLD + "💥 爆発に巻き込まれた！(貫通)");
                    }
                    
                    // 貫通ダメージ（防具・プロテクション無視）
                    double newHealth = target.getHealth() - damage;
                    if (newHealth <= 0) {
                        // 死亡処理（キラーを記録するためdamageを使用）
                        target.damage(999, shooter);
                    } else {
                        target.setHealth(newHealth);
                        // ダメージエフェクト（ノックバック・音）
                        target.damage(0.01, shooter);
                    }
                }
            }
        }
    }
}
