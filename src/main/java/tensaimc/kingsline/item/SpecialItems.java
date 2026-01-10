package tensaimc.kingsline.item;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;

import java.util.*;

/**
 * 特殊・ユニーク系アイテム
 */
public class SpecialItems {
    
    // ゴーストマント効果中のプレイヤー
    private static final Set<UUID> ghostCloakActive = new HashSet<>();
    
    // ゴーストマント発動時に一時保存した防具
    private static final Map<UUID, ItemStack[]> storedArmor = new HashMap<>();
    
    // ミラーシールド効果中のプレイヤー
    private static final Set<UUID> mirrorShieldActive = new HashSet<>();
    
    // リバイバルチャーム所持中のプレイヤー
    private static final Set<UUID> revivalCharmActive = new HashSet<>();
    
    // リバイバルチャーム発動時に保存したインベントリ
    private static final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private static final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    
    public static void registerAll(ShopItemRegistry registry) {
        registry.register(new GhostCloak());
        registry.register(new MirrorShield());
        registry.register(new RevivalCharm());
    }
    
    // ========== ゴーストマント ==========
    public static class GhostCloak extends AbstractShopItem {
        
        public GhostCloak() {
            super(
                "ghost_cloak",
                "ゴーストマント",
                "5秒間透明+スピードIII",
                Material.GHAST_TEAR,
                5,
                1,
                ShopItemCategory.SPECIAL
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.LIGHT_PURPLE + "👻 姿を消して高速移動");
            lore.add(ChatColor.RED + "※攻撃されると解除");
            lore.add(ChatColor.YELLOW + "右クリックで使用");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            UUID uuid = player.getUniqueId();
            
            if (ghostCloakActive.contains(uuid)) {
                player.sendMessage(ChatColor.RED + "既にゴーストマント効果中です。");
                return false;
            }
            
            ghostCloakActive.add(uuid);
            
            // 防具を一時保存して外す（透明化のため）
            ItemStack[] armor = player.getInventory().getArmorContents().clone();
            storedArmor.put(uuid, armor);
            player.getInventory().setArmorContents(new ItemStack[4]);
            
            // 透明 + Speed III (5秒)
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2)); // Speed III
            
            player.sendMessage(ChatColor.LIGHT_PURPLE + "👻 ゴーストマント発動！(5秒)");
            player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0f, 1.5f);
            
            // 5秒後に解除
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (ghostCloakActive.remove(uuid)) {
                        restoreArmor(plugin, uuid);
                        Player p = plugin.getServer().getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            p.sendMessage(ChatColor.GRAY + "ゴーストマント効果終了");
                        }
                    }
                }
            }.runTaskLater(plugin, 100L);
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
        
        /**
         * 防具を復元
         */
        private static void restoreArmor(KingsLine plugin, UUID uuid) {
            ItemStack[] armor = storedArmor.remove(uuid);
            if (armor == null) return;
            
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.getInventory().setArmorContents(armor);
                player.updateInventory();
            }
        }
        
        /**
         * ゴーストマント効果中かどうか
         */
        public static boolean isActive(UUID uuid) {
            return ghostCloakActive.contains(uuid);
        }
        
        /**
         * ゴーストマント効果を強制解除（攻撃時または被弾時）
         */
        public static void cancelEffect(KingsLine plugin, Player player, String reason) {
            UUID uuid = player.getUniqueId();
            if (ghostCloakActive.remove(uuid)) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.removePotionEffect(PotionEffectType.SPEED);
                restoreArmor(plugin, uuid);
                player.sendMessage(ChatColor.RED + reason + "ゴーストマント解除！");
            }
        }
        
        /**
         * ゴーストマント効果を強制解除（攻撃時）- 互換用
         */
        public static void cancelEffect(KingsLine plugin, Player player) {
            cancelEffect(plugin, player, "攻撃したため");
        }
    }
    
    // ========== ミラーシールド ==========
    public static class MirrorShield extends AbstractShopItem {
        
        public MirrorShield() {
            super(
                "mirror_shield",
                "ミラーシールド",
                "次の攻撃ダメージを反射",
                Material.DIAMOND,
                8,
                1,
                ShopItemCategory.SPECIAL
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.AQUA + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.AQUA + "🛡 受けたダメージを相手に返す");
            lore.add(ChatColor.YELLOW + "右クリックで発動");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            UUID uuid = player.getUniqueId();
            
            if (mirrorShieldActive.contains(uuid)) {
                player.sendMessage(ChatColor.RED + "既にミラーシールド効果中です。");
                return false;
            }
            
            mirrorShieldActive.add(uuid);
            
            player.sendMessage(ChatColor.AQUA + "🛡 ミラーシールド発動！次の攻撃を反射します。");
            player.playSound(player.getLocation(), Sound.ANVIL_LAND, 0.5f, 1.5f);
            
            // 15秒後に効果が切れる（使われなかった場合）
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mirrorShieldActive.remove(uuid)) {
                        Player p = plugin.getServer().getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            p.sendMessage(ChatColor.GRAY + "ミラーシールド効果が切れた...");
                        }
                    }
                }
            }.runTaskLater(plugin, 300L); // 15秒
            
            // ItemListenerで消費されるのでここでは消費しない
            return true;
        }
        
        /**
         * ミラーシールド効果中かどうか
         */
        public static boolean isActive(UUID uuid) {
            return mirrorShieldActive.contains(uuid);
        }
        
        /**
         * ダメージ反射処理（CombatListenerから呼び出し）
         * @return 反射が発動した場合true
         */
        public static boolean reflectDamage(KingsLine plugin, Player victim, Player attacker, double damage) {
            UUID uuid = victim.getUniqueId();
            
            if (!mirrorShieldActive.remove(uuid)) {
                return false;
            }
            
            // ダメージを反射
            attacker.damage(damage, victim);
            
            victim.sendMessage(ChatColor.AQUA + "🛡 ダメージを反射！");
            attacker.sendMessage(ChatColor.RED + "ミラーシールドで攻撃が反射された！");
            
            victim.playSound(victim.getLocation(), Sound.BLAZE_HIT, 1.0f, 1.0f);
            attacker.playSound(attacker.getLocation(), Sound.BLAZE_HIT, 1.0f, 0.5f);
            
            return true;
        }
    }
    
    // ========== リバイバルチャーム ==========
    public static class RevivalCharm extends AbstractShopItem {
        
        public RevivalCharm() {
            super(
                "revival_charm",
                "リバイバルチャーム",
                "死亡時HP40%でその場復活",
                Material.GOLD_NUGGET, // 1.8.8にはTOTEMがないのでGOLD_NUGGET
                10,
                1,
                ShopItemCategory.SPECIAL
            );
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(Material.GOLD_NUGGET, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(ChatColor.GOLD + "✟ " + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(ChatColor.GOLD + "✟ 一度だけ死を免れる");
            lore.add(ChatColor.RED + "※インベントリに所持で自動発動");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            // 右クリックでは発動しない（死亡時に自動発動）
            player.sendMessage(ChatColor.YELLOW + "リバイバルチャームはインベントリに持っているだけで効果があります。");
            player.sendMessage(ChatColor.YELLOW + "死亡時に自動的に発動します。");
            return false;
        }
        
        /**
         * プレイヤーがリバイバルチャームを持っているか確認
         */
        public static ItemStack findCharm(Player player) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null) continue;
                if (item.getType() != Material.GOLD_NUGGET) continue;
                if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) continue;
                
                for (String line : item.getItemMeta().getLore()) {
                    if (line.contains("KL-Shop:revival_charm")) {
                        return item;
                    }
                }
            }
            return null;
        }
        
        /**
         * 致死ダメージを受ける前の復活処理（EntityDamageEventから呼び出し）
         * 死亡自体をキャンセルしてその場で復活
         * @return 復活した場合true
         */
        public static boolean tryReviveBeforeDeath(KingsLine plugin, Player player, KLPlayer klPlayer) {
            ItemStack charm = findCharm(player);
            if (charm == null) {
                return false;
            }
            
            // チャームを消費
            if (charm.getAmount() > 1) {
                charm.setAmount(charm.getAmount() - 1);
            } else {
                player.getInventory().remove(charm);
            }
            player.updateInventory();
            
            // 復活演出（周囲全員に見える）
            playRevivalEffect(plugin, player, klPlayer);
            
            // 復活タイトル表示
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    tensaimc.kingsline.util.TitleUtil.sendTitle(player, 
                            ChatColor.GOLD + "✟ REVIVED!",
                            ChatColor.YELLOW + "リバイバルチャームで死を免れた",
                            5, 30, 10);
                }
            }, 1L);
            
            return true;
        }
        
        /**
         * 死亡時の復活処理（PlayerDeathEventから呼び出し）- 旧方式（バックアップ用）
         * @return 復活した場合true
         */
        public static boolean tryRevive(KingsLine plugin, Player player, KLPlayer klPlayer) {
            ItemStack charm = findCharm(player);
            if (charm == null) {
                return false;
            }
            
            // インベントリと防具を保存（チャームを除く）
            ItemStack[] inventory = player.getInventory().getContents().clone();
            ItemStack[] armor = player.getInventory().getArmorContents().clone();
            
            // チャームを保存から除外
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] != null && inventory[i].equals(charm)) {
                    if (inventory[i].getAmount() > 1) {
                        inventory[i] = inventory[i].clone();
                        inventory[i].setAmount(inventory[i].getAmount() - 1);
                    } else {
                        inventory[i] = null;
                    }
                    break;
                }
            }
            
            savedInventory.put(player.getUniqueId(), inventory);
            savedArmor.put(player.getUniqueId(), armor);
            revivalCharmActive.add(player.getUniqueId());
            
            // 復活演出（周囲全員に見える）
            playRevivalEffect(plugin, player, klPlayer);
            
            return true;
        }
        
        /**
         * リスポーン時にインベントリを復元
         */
        public static boolean restoreInventory(Player player) {
            UUID uuid = player.getUniqueId();
            if (!revivalCharmActive.remove(uuid)) {
                return false;
            }
            
            ItemStack[] inventory = savedInventory.remove(uuid);
            ItemStack[] armor = savedArmor.remove(uuid);
            
            if (inventory != null) {
                player.getInventory().setContents(inventory);
            }
            if (armor != null) {
                player.getInventory().setArmorContents(armor);
            }
            
            player.updateInventory();
            return true;
        }
        
        /**
         * リバイバル発動中かどうか
         */
        public static boolean isReviving(UUID uuid) {
            return revivalCharmActive.contains(uuid);
        }
        
        /**
         * 復活演出（敵味方両方に見える）
         */
        private static void playRevivalEffect(KingsLine plugin, Player player, KLPlayer klPlayer) {
            Location loc = player.getLocation();
            World world = loc.getWorld();
            
            // 派手なエフェクト
            world.playSound(loc, Sound.WITHER_SPAWN, 1.0f, 1.5f);
            world.playSound(loc, Sound.ENDERDRAGON_GROWL, 0.5f, 1.5f);
            
            // パーティクル演出
            for (int i = 0; i < 30; i++) {
                double offsetX = (Math.random() - 0.5) * 2;
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5) * 2;
                world.playEffect(loc.clone().add(offsetX, offsetY, offsetZ), 
                        org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
            }
            
            // 周囲のプレイヤー全員に通知（敵味方両方）
            for (Player p : world.getPlayers()) {
                if (p.getLocation().distance(loc) <= 50) {
                    KLPlayer kl = plugin.getGameManager().getPlayer(p);
                    if (kl != null) {
                        if (kl.getTeam() == klPlayer.getTeam()) {
                            // 味方
                            p.sendMessage(ChatColor.GOLD + "✟ " + player.getName() + " がリバイバルチャームで復活！");
                        } else {
                            // 敵
                            p.sendMessage(ChatColor.RED + "⚠ " + player.getName() + " がリバイバルで復活した！");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * ゲーム終了時にリセット
     */
    public static void reset() {
        ghostCloakActive.clear();
        storedArmor.clear();
        mirrorShieldActive.clear();
        revivalCharmActive.clear();
        savedInventory.clear();
        savedArmor.clear();
    }
}
