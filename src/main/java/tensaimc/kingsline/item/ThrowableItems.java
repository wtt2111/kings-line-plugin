package tensaimc.kingsline.item;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 投擲・妨害系アイテム
 */
public class ThrowableItems {
    
    // アクティブな煙幕エリアの位置
    private static final Set<Location> activeSmokeAreas = new HashSet<>();
    
    public static void registerAll(ShopItemRegistry registry) {
        registry.register(new SnowballPack());
        registry.register(new Flashbang());
        registry.register(new SmokeBomb());
        registry.register(new FreezeBall());
        registry.register(new SilenceTalisman());
    }
    
    // ========== 雪玉パック ==========
    public static class SnowballPack extends AbstractShopItem {
        
        public SnowballPack() {
            super(
                "snowball_pack",
                "雪玉 x8",
                "ノックバック付き",
                Material.SNOW_BALL,
                1,
                8,
                ShopItemCategory.THROWABLE
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            // バニラの雪玉（投げられる）
            return new ItemStack(Material.SNOW_BALL, amount);
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
    
    // ========== フラッシュバン ==========
    public static class Flashbang extends AbstractShopItem {
        
        public static final String METADATA_KEY = "kl_flashbang";
        
        public Flashbang() {
            super(
                "flashbang",
                "フラッシュバン x2",
                "当たると盲目4秒",
                Material.FIREWORK_CHARGE,
                2,
                2,
                ShopItemCategory.THROWABLE
            );
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // 雪玉として投射
            Snowball projectile = player.launchProjectile(Snowball.class);
            projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
            
            player.playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 1.5f);
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
        
        /**
         * 着弾時の処理（CombatListenerから呼び出し）
         */
        public static void onHit(KingsLine plugin, Entity hitEntity) {
            if (hitEntity instanceof Player) {
                Player target = (Player) hitEntity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0)); // 4秒
                target.playSound(target.getLocation(), Sound.FIREWORK_BLAST, 1.0f, 2.0f);
                target.sendMessage(ChatColor.YELLOW + "フラッシュ！視界が奪われた！");
            }
        }
    }
    
    // ========== スモークボム ==========
    public static class SmokeBomb extends AbstractShopItem {
        
        public static final String METADATA_KEY = "kl_smokebomb";
        
        public SmokeBomb() {
            super(
                "smoke_bomb",
                "スモークボム",
                "着弾点に煙幕生成（半径8m）",
                Material.INK_SACK,
                5,
                1,
                ShopItemCategory.THROWABLE
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.INK_SACK, amount);
            item.setDurability((short) 8); // 灰色の染料
            
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(category.getColor() + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.YELLOW + "右クリックで投げる");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            Snowball projectile = player.launchProjectile(Snowball.class);
            projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
            
            player.playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 0.8f);
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
        
        /**
         * 着弾時の処理
         */
        public static void onLand(KingsLine plugin, Location location) {
            World world = location.getWorld();
            
            // 煙幕エリアを作成（5秒間）
            activeSmokeAreas.add(location);
            
            // パーティクル演出と盲目効果のタスク
            new BukkitRunnable() {
                int ticks = 0;
                final int duration = 100; // 5秒
                
                @Override
                public void run() {
                    if (ticks >= duration) {
                        activeSmokeAreas.remove(location);
                        this.cancel();
                        return;
                    }
                    
                    // 煙のパーティクル（半径8m）
                    for (int i = 0; i < 20; i++) {
                        double offsetX = (Math.random() - 0.5) * 16;
                        double offsetY = Math.random() * 3;
                        double offsetZ = (Math.random() - 0.5) * 16;
                        
                        Location particleLoc = location.clone().add(offsetX, offsetY, offsetZ);
                        world.playEffect(particleLoc, Effect.SMOKE, 4);
                    }
                    
                    // 範囲内のプレイヤーに盲目（半径8m）
                    for (Entity entity : world.getNearbyEntities(location, 8, 3, 8)) {
                        if (entity instanceof Player) {
                            Player p = (Player) entity;
                            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0), true);
                        }
                    }
                    
                    ticks += 5;
                }
            }.runTaskTimer(plugin, 0L, 5L);
            
            world.playSound(location, Sound.FIZZ, 1.0f, 0.5f);
        }
    }
    
    // ========== 氷結弾 ==========
    public static class FreezeBall extends AbstractShopItem {
        
        public static final String METADATA_KEY = "kl_freezeball";
        
        public FreezeBall() {
            super(
                "freeze_ball",
                "氷結弾 x2",
                "当たると鈍足+ジャンプ低下 3秒",
                Material.PRISMARINE_CRYSTALS,
                3,
                2,
                ShopItemCategory.THROWABLE
            );
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            Snowball projectile = player.launchProjectile(Snowball.class);
            projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));
            
            player.playSound(player.getLocation(), Sound.SHOOT_ARROW, 1.0f, 1.8f);
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
        
        /**
         * 着弾時の処理
         */
        public static void onHit(KingsLine plugin, Entity hitEntity) {
            if (hitEntity instanceof Player) {
                Player target = (Player) hitEntity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1)); // 鈍足II 3秒
                target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 60, 128)); // ジャンプ不可 3秒
                
                target.playSound(target.getLocation(), Sound.GLASS, 1.0f, 1.5f);
                target.sendMessage(ChatColor.AQUA + "凍結！動きが鈍くなった！");
            }
        }
    }
    
    // ========== 沈黙の札 ==========
    public static class SilenceTalisman extends AbstractShopItem {
        
        public SilenceTalisman() {
            super(
                "silence_talisman",
                "沈黙の札",
                "周囲5mの敵をSP使用不可 5秒",
                Material.PAPER,
                4,
                1,
                ShopItemCategory.THROWABLE
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.PAPER, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(category.getColor() + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.DARK_PURPLE + "周囲の敵のSP技を封じる");
            lore.add(ChatColor.YELLOW + "右クリックで使用");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            KLPlayer klUser = plugin.getGameManager().getPlayer(player);
            if (klUser == null) {
                return false;
            }
            
            Location loc = player.getLocation();
            World world = loc.getWorld();
            int silencedCount = 0;
            
            // 周囲5マスの敵プレイヤーを沈黙
            for (Entity entity : world.getNearbyEntities(loc, 5, 5, 5)) {
                if (entity instanceof Player && entity != player) {
                    Player target = (Player) entity;
                    KLPlayer klTarget = plugin.getGameManager().getPlayer(target);
                    
                    // 敵チームのみ
                    if (klTarget != null && klTarget.getTeam() != klUser.getTeam()) {
                        klTarget.applySilence(5000); // 5秒
                        target.playSound(target.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 0.5f);
                        target.sendMessage(ChatColor.DARK_PURPLE + "沈黙！SP技が使用不可に！(5秒)");
                        silencedCount++;
                    }
                }
            }
            
            // エフェクト
            world.playSound(loc, Sound.WITHER_SHOOT, 0.8f, 1.5f);
            for (int i = 0; i < 20; i++) {
                double offsetX = (Math.random() - 0.5) * 10;
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5) * 10;
                world.playEffect(loc.clone().add(offsetX, offsetY, offsetZ), Effect.WITCH_MAGIC, 0);
            }
            
            if (silencedCount > 0) {
                player.sendMessage(ChatColor.DARK_PURPLE + "沈黙の札発動！" + silencedCount + "人を沈黙させた！");
            } else {
                player.sendMessage(ChatColor.GRAY + "沈黙の札発動... 範囲内に敵がいなかった。");
            }
            
            return true;
        }
    }
}
