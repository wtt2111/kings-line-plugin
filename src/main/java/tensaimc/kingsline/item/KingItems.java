package tensaimc.kingsline.item;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;

import java.util.*;

/**
 * キング・チーム連携系アイテム
 */
public class KingItems {
    
    // 王の加護効果中のプレイヤー
    private static final Set<UUID> royalBlessingActive = new HashSet<>();
    
    // 設置された守護の旗
    private static final Map<Location, GuardianBannerData> activeBanners = new HashMap<>();
    
    public static void registerAll(ShopItemRegistry registry) {
        registry.register(new RoyalBlessing());
        registry.register(new RoyalCall());
        registry.register(new GuardianBanner());
    }
    
    // ========== 王の加護 ==========
    public static class RoyalBlessing extends AbstractShopItem {
        
        public RoyalBlessing() {
            super(
                "royal_blessing",
                "王の加護",
                "30秒間 再生III+耐性I+スピードI",
                Material.NETHER_STAR,
                10,
                1,
                ShopItemCategory.KING
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.GOLD + "✦ " + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.LIGHT_PURPLE + "再生III + 耐性I + スピードI");
            lore.add(ChatColor.YELLOW + "右クリックで使用");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            UUID uuid = player.getUniqueId();
            KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
            
            if (klPlayer == null || klPlayer.getTeam() == null) {
                player.sendMessage(ChatColor.RED + "チームに所属していません。");
                return false;
            }
            
            if (royalBlessingActive.contains(uuid)) {
                player.sendMessage(ChatColor.RED + "既に王の加護効果中です。");
                return false;
            }
            
            royalBlessingActive.add(uuid);
            
            // 王の加護効果を付与
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 2)); // 再生III 30秒
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 0)); // 耐性I 30秒
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0)); // スピードI 30秒
            
            player.sendMessage(ChatColor.GOLD + "✦ 王の加護発動！(30秒)");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            
            // 30秒後に解除
            new BukkitRunnable() {
                @Override
                public void run() {
                    royalBlessingActive.remove(uuid);
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(ChatColor.GRAY + "王の加護効果終了");
                    }
                }
            }.runTaskLater(plugin, 600L);
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
        
        public static boolean isActive(UUID uuid) {
            return royalBlessingActive.contains(uuid);
        }
    }
    
    // ========== ロイヤルコール ==========
    public static class RoyalCall extends AbstractShopItem {
        
        public RoyalCall() {
            super(
                "royal_call",
                "ロイヤルコール",
                "味方全員を自分の元へ召喚",
                Material.EYE_OF_ENDER,
                15,
                1,
                ShopItemCategory.KING
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.RED + "♔ " + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.RED + "味方全員がキングの元へ集結");
            lore.add(ChatColor.DARK_RED + "※キング専用アイテム");
            lore.add(ChatColor.YELLOW + "右クリックで使用");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        /**
         * キング専用アイテムかどうか
         */
        public static boolean isKingOnly() {
            return true;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
            
            if (klPlayer == null || klPlayer.getTeam() == null) {
                player.sendMessage(ChatColor.RED + "チームに所属していません。");
                return false;
            }
            
            // キングのみ使用可能
            if (!klPlayer.isKing()) {
                player.sendMessage(ChatColor.RED + "このアイテムはキングのみ使用できます。");
                return false;
            }
            
            Team team = klPlayer.getTeam();
            Location kingLoc = player.getLocation();
            int teleportedCount = 0;
            
            // 味方全員をキング（自分）の元へテレポート
            for (KLPlayer teammate : plugin.getTeamManager().getTeamPlayers(
                    plugin.getGameManager().getPlayers(), team)) {
                // キング本人は除外
                if (teammate.isKing()) {
                    continue;
                }
                
                Player teammatePlayer = teammate.getPlayer();
                if (teammatePlayer != null && teammatePlayer.isOnline()) {
                    teammatePlayer.teleport(kingLoc);
                    teammatePlayer.sendMessage(ChatColor.RED + "♔ ロイヤルコール！キングの元へ召喚された！");
                    teammatePlayer.playSound(teammatePlayer.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    teleportedCount++;
                }
            }
            
            player.sendMessage(ChatColor.GOLD + "♔ ロイヤルコール発動！味方 " + teleportedCount + " 人を召喚！");
            player.playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.5f);
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
    }
    
    // ========== 守護の旗 ==========
    public static class GuardianBanner extends AbstractShopItem {
        
        public GuardianBanner() {
            super(
                "guardian_banner",
                "守護の旗",
                "設置すると周囲味方にリジェネ15秒",
                Material.BANNER,
                6,
                1,
                ShopItemCategory.KING
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.BANNER, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.GREEN + "⚑ " + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.GREEN + "味方を回復するエリアを作成");
            lore.add(ChatColor.YELLOW + "右クリックで設置");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
            
            if (klPlayer == null || klPlayer.getTeam() == null) {
                player.sendMessage(ChatColor.RED + "チームに所属していません。");
                return false;
            }
            
            // 設置位置を取得
            Block targetBlock = player.getTargetBlock((Set<Material>) null, 4);
            if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                // 足元に設置
                targetBlock = player.getLocation().getBlock();
            }
            
            Location bannerLoc = targetBlock.getLocation().add(0.5, 1, 0.5);
            Team team = klPlayer.getTeam();
            
            // 旗のエフェクトを開始
            GuardianBannerData bannerData = new GuardianBannerData(team, bannerLoc);
            activeBanners.put(bannerLoc, bannerData);
            
            // リジェネエリアのタスク（15秒間）
            new BukkitRunnable() {
                int ticks = 0;
                final int duration = 300; // 15秒
                
                @Override
                public void run() {
                    if (ticks >= duration) {
                        activeBanners.remove(bannerLoc);
                        this.cancel();
                        return;
                    }
                    
                    World world = bannerLoc.getWorld();
                    
                    // パーティクル演出
                    for (int i = 0; i < 8; i++) {
                        double angle = (ticks + i * 45) * Math.PI / 180.0;
                        double x = Math.cos(angle) * 3;
                        double z = Math.sin(angle) * 3;
                        Location particleLoc = bannerLoc.clone().add(x, 0.5, z);
                        world.playEffect(particleLoc, Effect.HAPPY_VILLAGER, 0);
                    }
                    
                    // 5tickごとにリジェネ付与
                    if (ticks % 10 == 0) {
                        for (Entity entity : world.getNearbyEntities(bannerLoc, 5, 3, 5)) {
                            if (entity instanceof Player) {
                                Player p = (Player) entity;
                                KLPlayer kl = plugin.getGameManager().getPlayer(p);
                                
                                // 味方のみ
                                if (kl != null && kl.getTeam() == team) {
                                    p.addPotionEffect(new PotionEffect(
                                            PotionEffectType.REGENERATION, 40, 0), true);
                                }
                            }
                        }
                    }
                    
                    ticks += 5;
                }
            }.runTaskTimer(plugin, 0L, 5L);
            
            player.sendMessage(ChatColor.GREEN + "⚑ 守護の旗を設置！(15秒間)");
            player.playSound(bannerLoc, Sound.LEVEL_UP, 1.0f, 0.8f);
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
    }
    
    /**
     * 守護の旗のデータ
     */
    private static class GuardianBannerData {
        final Team team;
        final Location location;
        
        GuardianBannerData(Team team, Location location) {
            this.team = team;
            this.location = location;
        }
    }
    
    /**
     * ゲーム終了時にリセット
     */
    public static void reset() {
        royalBlessingActive.clear();
        activeBanners.clear();
    }
}
