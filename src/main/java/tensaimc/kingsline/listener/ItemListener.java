package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.item.*;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.resource.LuminaManager;
import tensaimc.kingsline.resource.ShardManager;

import java.util.List;

/**
 * アイテム関連のリスナー
 */
public class ItemListener implements Listener {
    
    private final KingsLine plugin;
    
    public ItemListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        ShardManager sm = plugin.getShardManager();
        LuminaManager lm = plugin.getLuminaManager();
        
        // Shardを拾った場合
        if (sm.isShard(item)) {
            int amount = item.getAmount();
            sm.onPickupShard(klPlayer, amount);
            
            // インベントリには入れない（データのみ管理）
            event.setCancelled(true);
            event.getItem().remove();
            return;
        }
        
        // Luminaを拾った場合
        if (lm.isLumina(item)) {
            int amount = item.getAmount();
            lm.onPickupLumina(klPlayer, amount);
            
            // インベントリには入れない
            event.setCancelled(true);
            event.getItem().remove();
            return;
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) {
            return;
        }
        
        // 右クリックのみ
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        GameManager gm = plugin.getGameManager();
        
        // エレメント選択アイテム
        if (isElementSelector(item)) {
            event.setCancelled(true);
            
            if (gm.isState(GameState.STARTING)) {
                plugin.getElementSelectGUI().open(player);
            } else {
                player.sendMessage(ChatColor.RED + "エレメントは準備フェーズ中のみ選択可能です。");
            }
            return;
        }
        
        // SP技アイテム
        if (isSPItem(item)) {
            event.setCancelled(true);
            
            if (!gm.isState(GameState.RUNNING)) {
                return;
            }
            
            KLPlayer klPlayer = gm.getPlayer(player);
            if (klPlayer == null) {
                return;
            }
            
            // SP発動処理
            if (!tryActivateSP(klPlayer, player)) {
                return;
            }
            
            // ElementManagerでSP技を発動
            plugin.getElementManager().activateSpecialAbility(klPlayer);
            return;
        }
        
        // キング投票アイテム（ジュークボックス）
        if (isKingVoteItem(item)) {
            event.setCancelled(true);
            
            if (gm.isState(GameState.STARTING) && gm.isVotingPhase()) {
                plugin.getKingVoteGUI().open(player);
            } else {
                player.sendMessage(ChatColor.RED + "投票はキング投票フェーズ中のみ可能です。");
            }
            return;
        }
        
        // シフト + 剣 + 右クリック でSP技発動
        if (player.isSneaking() && isSword(item)) {
            event.setCancelled(true);
            
            if (!gm.isState(GameState.RUNNING)) {
                player.sendMessage(ChatColor.RED + "ゲーム中のみSP技を使用できます。");
                return;
            }
            
            KLPlayer klPlayer = gm.getPlayer(player);
            if (klPlayer == null) {
                player.sendMessage(ChatColor.RED + "プレイヤーデータが見つかりません。");
                return;
            }
            
            if (!tryActivateSP(klPlayer, player)) {
                return;
            }
            
            // SP技発動！
            plugin.getElementManager().activateSpecialAbility(klPlayer);
            return;
        }
        
        // ========== ショップアイテムの使用 ==========
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        // ShopItemRegistryでアイテムをチェック
        ShopItemRegistry registry = plugin.getShopItemRegistry();
        ShopItem shopItem = registry.getFromItemStack(item);
        
        if (shopItem != null) {
            event.setCancelled(true);
            boolean consumed = shopItem.use(plugin, player, item);
            // アイテムが消費された場合、手持ちアイテムを減らす
            if (consumed) {
                ItemStack handItem = player.getItemInHand();
                if (handItem != null && handItem.getAmount() > 1) {
                    handItem.setAmount(handItem.getAmount() - 1);
                } else {
                    player.setItemInHand(null);
                }
                player.updateInventory();
            }
        }
    }
    
    /**
     * SP発動条件チェック
     */
    private boolean tryActivateSP(KLPlayer klPlayer, Player player) {
        // エレメント未選択
        if (klPlayer.getElement() == null) {
            player.sendMessage(ChatColor.RED + "エレメントを選択していません！");
            return false;
        }
        
        // 沈黙状態
        if (klPlayer.isSilenced()) {
            long remaining = klPlayer.getSilenceRemaining() / 1000;
            player.sendMessage(ChatColor.DARK_PURPLE + "沈黙状態！SP技使用不可 (残り" + remaining + "秒)");
            return false;
        }
        
        // クールダウン中
        if (klPlayer.isSpOnCooldown()) {
            long remaining = klPlayer.getSpCooldownRemaining() / 1000;
            player.sendMessage(ChatColor.RED + "SP技クールダウン中 (残り" + remaining + "秒)");
            return false;
        }
        
        // SPゲージ不足
        int requiredHits = plugin.getElementManager().getSpRequiredHits(klPlayer);
        if (klPlayer.getSpGauge() < requiredHits) {
            player.sendMessage(ChatColor.RED + "SPゲージが溜まっていません (" + 
                    klPlayer.getSpGauge() + "/" + requiredHits + ")");
            return false;
        }
        
        return true;
    }
    
    /**
     * 弓で矢を発射した時
     */
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        if (!(event.getProjectile() instanceof Arrow)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        Arrow arrow = (Arrow) event.getProjectile();
        
        // 消費された矢を特定（メインハンドかオフハンドの矢）
        ItemStack consumedArrow = findConsumedArrow(player);
        if (consumedArrow == null) {
            return;
        }
        
        // 特殊矢の判定
        if (isSpecialArrow(consumedArrow, "火矢")) {
            BowItems.FireArrow.onShoot(plugin, arrow);
        } else if (isSpecialArrow(consumedArrow, "毒矢")) {
            BowItems.PoisonArrow.onShoot(plugin, arrow);
        } else if (isSpecialArrow(consumedArrow, "爆発矢")) {
            BowItems.ExplosiveArrow.onShoot(plugin, arrow);
        }
    }
    
    /**
     * 投射物が着弾した時（地面・ブロックへの着弾のみ）
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        
        Player shooter = (Player) projectile.getShooter();
        Location hitLocation = projectile.getLocation();
        
        // スモークボム（地面着弾で発動）
        if (projectile.hasMetadata(ThrowableItems.SmokeBomb.METADATA_KEY)) {
            ThrowableItems.SmokeBomb.onLand(plugin, hitLocation);
            return;
        }
        
        // 爆発矢（地面着弾で発動）
        if (projectile instanceof Arrow) {
            Arrow arrow = (Arrow) projectile;
            if (arrow.hasMetadata(BowItems.ExplosiveArrow.METADATA_KEY)) {
                BowItems.ExplosiveArrow.onLand(plugin, hitLocation, shooter);
                arrow.remove();
                return;
            }
        }
    }
    
    /**
     * 投射物がエンティティに当たった時
     */
    @EventHandler
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) {
            return;
        }
        
        Projectile projectile = (Projectile) event.getDamager();
        Entity hitEntity = event.getEntity();
        
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        
        // フラッシュバン
        if (projectile.hasMetadata(ThrowableItems.Flashbang.METADATA_KEY)) {
            ThrowableItems.Flashbang.onHit(plugin, hitEntity);
            return;
        }
        
        // 氷結弾
        if (projectile.hasMetadata(ThrowableItems.FreezeBall.METADATA_KEY)) {
            ThrowableItems.FreezeBall.onHit(plugin, hitEntity);
            return;
        }
        
        // 火矢
        if (projectile.hasMetadata(BowItems.FireArrow.METADATA_KEY)) {
            BowItems.FireArrow.onHit(plugin, hitEntity);
            return;
        }
        
        // 毒矢
        if (projectile.hasMetadata(BowItems.PoisonArrow.METADATA_KEY)) {
            BowItems.PoisonArrow.onHit(plugin, hitEntity);
            return;
        }
    }
    
    /**
     * 釣り竿使用時（グラップルフック）
     */
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        
        if (item == null || item.getType() != Material.FISHING_ROD) {
            return;
        }
        
        // グラップルフックかどうか判定
        if (!MobilityItems.GrappleHook.isGrappleHook(item)) {
            return;
        }
        
        // フックが何かに当たった時、またはフックを戻した時に発動
        PlayerFishEvent.State state = event.getState();
        if (state == PlayerFishEvent.State.IN_GROUND || 
            state == PlayerFishEvent.State.FAILED_ATTEMPT ||
            state == PlayerFishEvent.State.CAUGHT_ENTITY) {
            
            MobilityItems.GrappleHook.onUse(plugin, player, item);
        }
    }
    
    /**
     * 消費される矢を探す（特殊矢を優先）
     * Minecraft 1.8.8では矢はインベントリの若い番号から消費される
     */
    private ItemStack findConsumedArrow(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack firstNormalArrow = null;
        
        // インベントリを順番に検索（若い番号から）
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.ARROW) {
                // 特殊矢かどうか判定
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    // 特殊矢が見つかったら優先的に返す
                    return item;
                }
                // 普通の矢は記録しておく
                if (firstNormalArrow == null) {
                    firstNormalArrow = item;
                }
            }
        }
        
        // 特殊矢がなければ普通の矢を返す
        return firstNormalArrow;
    }
    
    /**
     * 特殊矢かどうか判定
     */
    private boolean isSpecialArrow(ItemStack item, String name) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        return ChatColor.stripColor(meta.getDisplayName()).contains(name);
    }
    
    /**
     * キング投票アイテムかどうか
     */
    private boolean isKingVoteItem(ItemStack item) {
        if (item == null || item.getType() != GameManager.KING_VOTE_MATERIAL) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().contains("キング投票");
    }
    
    /**
     * エレメント選択アイテムかどうか
     */
    private boolean isElementSelector(ItemStack item) {
        if (item == null || item.getType() != GameManager.ELEMENT_SELECT_MATERIAL) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().contains("エレメント選択");
    }
    
    /**
     * SP技アイテムかどうか
     */
    private boolean isSPItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().contains("SP技");
    }
    
    /**
     * 剣かどうか
     */
    private boolean isSword(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        return type == Material.WOOD_SWORD ||
               type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD ||
               type == Material.GOLD_SWORD ||
               type == Material.DIAMOND_SWORD;
    }
}
