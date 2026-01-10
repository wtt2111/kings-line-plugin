package tensaimc.kingsline.item;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import tensaimc.kingsline.KingsLine;

import java.util.ArrayList;
import java.util.List;

/**
 * 移動系アイテム
 */
public class MobilityItems {
    
    public static void registerAll(ShopItemRegistry registry) {
        registry.register(new SpeedPotion());
        registry.register(new JumpPotion());
        registry.register(new EnderPearl());
        registry.register(new GrappleHook());
        registry.register(new RocketBoost());
    }
    
    // ========== スピードポーション ==========
    public static class SpeedPotion extends AbstractShopItem {
        
        public SpeedPotion() {
            super(
                "speed_potion",
                "スピードポーション",
                "スピードII 20秒",
                Material.POTION,
                2,
                1,
                ShopItemCategory.MOBILITY
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.POTION, amount);
            item.setDurability((short) 8226); // Speed II ポーションの見た目
            
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(category.getColor() + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.AQUA + "⚡ 移動速度大幅UP");
            lore.add(ChatColor.YELLOW + "右クリックで使用");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1)); // Speed II 20秒
            
            player.playSound(player.getLocation(), Sound.DRINK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.AQUA + "⚡ スピードII発動！(20秒)");
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
    }
    
    // ========== 跳躍ポーション ==========
    public static class JumpPotion extends AbstractShopItem {
        
        public JumpPotion() {
            super(
                "jump_potion",
                "跳躍ポーション",
                "ジャンプII 20秒",
                Material.POTION,
                2,
                1,
                ShopItemCategory.MOBILITY
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.POTION, amount);
            item.setDurability((short) 8235); // Jump Boost II ポーションの見た目
            
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(category.getColor() + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.GREEN + "↑ ジャンプ力大幅UP");
            lore.add(ChatColor.YELLOW + "右クリックで使用");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 400, 1)); // Jump II 20秒
            
            player.playSound(player.getLocation(), Sound.DRINK, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "↑ ジャンプII発動！(20秒)");
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
    }
    
    // ========== エンダーパール ==========
    public static class EnderPearl extends AbstractShopItem {
        
        public EnderPearl() {
            super(
                "ender_pearl",
                "エンダーパール",
                "テレポート（落下ダメあり）",
                Material.ENDER_PEARL,
                5,
                1,
                ShopItemCategory.MOBILITY
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            // バニラのエンダーパール
            return new ItemStack(Material.ENDER_PEARL, amount);
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // バニラ動作で投げるので、ShopItemとしてマッチさせない
            return false;
        }

        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // バニラの動作に任せる
            return false;
        }
    }
    
    // ========== グラップルフック ==========
    public static class GrappleHook extends AbstractShopItem {
        
        public static final String METADATA_KEY = "kl_grapple_uses";
        
        public GrappleHook() {
            super(
                "grapple_hook",
                "グラップルフック",
                "引っ張り移動（3回使用可）",
                Material.FISHING_ROD,
                6,
                1,
                ShopItemCategory.MOBILITY
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.FISHING_ROD, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(category.getColor() + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.WHITE + "残り使用回数: " + ChatColor.YELLOW + "3");
            lore.add(ChatColor.YELLOW + "釣竿を使うと自分を引っ張る");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean matches(ItemStack item) {
            // 釣り竿はPlayerFishEventで処理するため、右クリックではマッチさせない
            return false;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // 使用処理はPlayerFishEventで行う
            return false;
        }
        
        /**
         * グラップルフックかどうかを判定
         */
        public static boolean isGrappleHook(ItemStack item) {
            if (item == null || item.getType() != Material.FISHING_ROD) {
                return false;
            }
            if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
                return false;
            }
            for (String line : item.getItemMeta().getLore()) {
                if (line.contains("KL-Shop:grapple_hook")) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * グラップル使用時の処理（PlayerFishEventから呼び出し）
         */
        public static void onUse(KingsLine plugin, Player player, ItemStack item) {
            // フライハック検知回避
            allowTemporaryFlight(plugin, player, 60); // 3秒間
            
            // プレイヤーを引っ張る
            Vector direction = player.getLocation().getDirection();
            player.setVelocity(direction.multiply(1.5).setY(0.5));
            player.playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 0.8f);
            
            // 残り回数を取得・更新
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            
            int remainingUses = 3;
            int loreIndex = -1;
            
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("残り使用回数:")) {
                    String numStr = ChatColor.stripColor(line).replaceAll("[^0-9]", "");
                    try {
                        remainingUses = Integer.parseInt(numStr);
                    } catch (NumberFormatException e) {
                        remainingUses = 3;
                    }
                    loreIndex = i;
                    break;
                }
            }
            
            remainingUses--;
            
            if (remainingUses <= 0) {
                // 壊れた
                player.setItemInHand(null);
                player.updateInventory();
                player.sendMessage(ChatColor.GRAY + "グラップルフックが壊れた！");
            } else {
                // 残り回数を更新
                if (loreIndex >= 0) {
                    lore.set(loreIndex, ChatColor.WHITE + "残り使用回数: " + ChatColor.YELLOW + remainingUses);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    player.updateInventory();
                }
                player.sendMessage(ChatColor.AQUA + "グラップル！(残り" + remainingUses + "回)");
            }
        }
    }
    
    // ========== ロケットブースト ==========
    public static class RocketBoost extends AbstractShopItem {
        
        public RocketBoost() {
            super(
                "rocket_boost",
                "ロケットブースト",
                "前方に大ジャンプ",
                Material.FIREWORK,
                4,
                1,
                ShopItemCategory.MOBILITY
            );
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // フライハック検知回避
            allowTemporaryFlight(plugin, player, 100); // 5秒間
            
            // 前方に大きく飛ぶ（3倍強化）
            Vector direction = player.getLocation().getDirection();
            Vector velocity = direction.multiply(6.0).setY(2.0);
            player.setVelocity(velocity);
            
            player.playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, 1.0f, 1.0f);
            player.getWorld().playEffect(player.getLocation(), Effect.EXPLOSION_LARGE, 0);
            player.sendMessage(ChatColor.RED + "🚀 ロケットブースト！");
            
            // 落下ダメージ軽減（5秒間に延長）
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 1));
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
    }
    
    /**
     * フライハック検知回避のため一時的に飛行を許可
     */
    public static void allowTemporaryFlight(KingsLine plugin, Player player, int ticks) {
        player.setAllowFlight(true);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }.runTaskLater(plugin, ticks);
    }
}
