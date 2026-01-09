package tensaimc.kingsline.resource;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Area;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.util.ActionBarUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Shard管理クラス
 * Shardはチームアップグレード用の通貨
 * - フィールドで拾う
 * - 死亡時にドロップ（敵に奪われる）
 * - 拠点に戻ると自動貯金
 */
public class ShardManager {
    
    private final KingsLine plugin;
    private BukkitTask spawnTask;
    private final List<Item> spawnedShards;
    
    // Shardのアイテム定義
    public static final Material SHARD_MATERIAL = Material.PRISMARINE_SHARD;
    public static final String SHARD_DISPLAY_NAME = ChatColor.AQUA + "◈ Shard";
    
    public ShardManager(KingsLine plugin) {
        this.plugin = plugin;
        this.spawnedShards = new ArrayList<>();
    }
    
    /**
     * Shardスポーンループを開始
     */
    public void startSpawnLoop() {
        stopSpawnLoop();
        
        int intervalSeconds = plugin.getConfigManager().getShardSpawnInterval();
        int intervalTicks = intervalSeconds * 20;
        
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnShardsAtAreas();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }
    
    /**
     * Shardスポーンループを停止
     */
    public void stopSpawnLoop() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        
        // スポーンしたShardを削除
        for (Item item : spawnedShards) {
            if (item != null && item.isValid()) {
                item.remove();
            }
        }
        spawnedShards.clear();
    }
    
    /**
     * 各エリアにShardをスポーン
     */
    private void spawnShardsAtAreas() {
        GameManager gm = plugin.getGameManager();
        Arena arena = gm.getCurrentArena();
        
        if (arena == null) {
            return;
        }
        
        int amount = plugin.getConfigManager().getShardSpawnAmount();
        
        // Bエリア
        Area areaB = arena.getAreaB();
        if (areaB != null && areaB.isEnabled() && areaB.getShardSpawn() != null) {
            spawnShard(areaB.getShardSpawn(), amount);
        }
        
        // A/Cエリア (大規模モードのみ)
        Area areaA = arena.getAreaA();
        if (areaA != null && areaA.isEnabled() && areaA.getShardSpawn() != null) {
            spawnShard(areaA.getShardSpawn(), amount);
        }
        
        Area areaC = arena.getAreaC();
        if (areaC != null && areaC.isEnabled() && areaC.getShardSpawn() != null) {
            spawnShard(areaC.getShardSpawn(), amount);
        }
    }
    
    /**
     * 指定位置にShardをスポーン
     */
    public void spawnShard(Location location, int amount) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        for (int i = 0; i < amount; i++) {
            ItemStack shardItem = createShardItem();
            Item item = location.getWorld().dropItem(location, shardItem);
            item.setPickupDelay(20); // 1秒のピックアップ遅延
            spawnedShards.add(item);
        }
    }
    
    /**
     * プレイヤーがShardを拾った時の処理
     */
    public void onPickupShard(KLPlayer klPlayer, int amount) {
        klPlayer.addShardCarrying(amount);
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            String message = ChatColor.AQUA + "◈+" + amount + 
                    ChatColor.GRAY + " (所持: " + klPlayer.getShardCarrying() + ")";
            ActionBarUtil.sendActionBar(player, message);
        }
    }
    
    /**
     * プレイヤーの死亡時にShardをドロップ
     */
    public void dropPlayerShards(KLPlayer klPlayer, Location location) {
        int carrying = klPlayer.takeAllCarryingShards();
        if (carrying <= 0) {
            return;
        }
        
        // 1つのスタックとしてドロップ
        ItemStack shardItem = createShardItem();
        shardItem.setAmount(carrying);
        
        if (location != null && location.getWorld() != null) {
            location.getWorld().dropItemNaturally(location, shardItem);
        }
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.RED + "所持していた " + carrying + " Shardをドロップしました！");
        }
    }
    
    /**
     * 拠点に帰還した時の自動貯金
     */
    public void onReturnToBase(KLPlayer klPlayer) {
        int deposited = klPlayer.depositShards();
        if (deposited > 0) {
            klPlayer.addShardDeposited(deposited);
            
            Player player = klPlayer.getPlayer();
            if (player != null) {
                String message = ChatColor.GREEN + "✓ " + ChatColor.AQUA + "◈" + deposited + 
                        ChatColor.GREEN + " を貯金しました！" + 
                        ChatColor.GRAY + " (合計: " + klPlayer.getShardSaved() + ")";
                ActionBarUtil.sendActionBar(player, message);
                player.sendMessage(ChatColor.GREEN + "◈ " + deposited + " Shard を貯金しました！");
            }
        }
    }
    
    /**
     * コア破壊時のShardドロップ
     */
    public void dropCoreShards(Location location) {
        int amount = plugin.getConfigManager().getShardCoreDestroyDrop();
        spawnShard(location, amount);
    }
    
    /**
     * キング死亡時のShardドロップ
     */
    public void dropKingDeathShards(Location location) {
        int amount = plugin.getConfigManager().getShardKingDeathDrop();
        spawnShard(location, amount);
    }
    
    /**
     * Shardアイテムを作成
     */
    public ItemStack createShardItem() {
        ItemStack item = new ItemStack(SHARD_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(SHARD_DISPLAY_NAME);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "チームアップグレード用の通貨");
        lore.add("");
        lore.add(ChatColor.YELLOW + "拠点に持ち帰ると貯金されます");
        lore.add(ChatColor.RED + "死亡すると所持分をドロップ！");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * アイテムがShardかどうか判定
     */
    public boolean isShard(ItemStack item) {
        if (item == null || item.getType() != SHARD_MATERIAL) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && SHARD_DISPLAY_NAME.equals(meta.getDisplayName());
    }
}
