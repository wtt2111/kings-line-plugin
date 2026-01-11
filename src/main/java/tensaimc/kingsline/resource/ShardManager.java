package tensaimc.kingsline.resource;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.util.ActionBarUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Shard管理クラス
 * Shardはチームアップグレード用の通貨
 * - Bエリアにいると自動獲得
 * - キル時に敵のシャードを奪取
 * - 拠点に戻ると自動貯金
 * 
 * ※物体としてのドロップは廃止、内部的な数値管理のみ
 */
public class ShardManager {
    
    private final KingsLine plugin;
    
    // Shardのアイテム定義（レガシー互換用）
    public static final Material SHARD_MATERIAL = Material.PRISMARINE_SHARD;
    public static final String SHARD_DISPLAY_NAME = ChatColor.AQUA + "◈ Shard";
    
    public ShardManager(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Shardスポーンループを開始（現在は無効化）
     * Bエリアでの自動獲得はAreaManagerで処理
     */
    public void startSpawnLoop() {
        // 物体スポーンは廃止 - 何もしない
    }
    
    /**
     * Shardスポーンループを停止
     */
    public void stopSpawnLoop() {
        // 物体スポーンは廃止 - 何もしない
    }
    
    /**
     * プレイヤーにシャードを付与（内部的）
     */
    public void awardShard(KLPlayer klPlayer, int amount) {
        if (amount <= 0) return;
        
        klPlayer.addShardCarrying(amount);
        
        // 統計: シャード獲得を記録
        plugin.getStatsDatabase().addShard(klPlayer.getUuid(), amount);
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            String message = ChatColor.AQUA + "◈+" + amount + 
                    ChatColor.GRAY + " (所持: " + klPlayer.getShardCarrying() + ")";
            ActionBarUtil.sendActionBar(player, message);
        }
    }
    
    /**
     * 死亡時にシャードをキラーに移動
     * @param victim 死亡したプレイヤー
     * @param killer キラー（nullの場合シャードは消失）
     */
    public void transferShardsOnDeath(KLPlayer victim, KLPlayer killer) {
        int carrying = victim.takeAllCarryingShards();
        if (carrying <= 0) {
            return;
        }
        
        Player victimPlayer = victim.getPlayer();
        
        if (killer != null) {
            // キラーにシャードを付与
            killer.addShardCarrying(carrying);
            plugin.getStatsDatabase().addShard(killer.getUuid(), carrying);
            
            Player killerPlayer = killer.getPlayer();
            if (killerPlayer != null) {
                String message = ChatColor.AQUA + "◈+" + carrying + 
                        ChatColor.GOLD + " (奪取!) " +
                        ChatColor.GRAY + "(所持: " + killer.getShardCarrying() + ")";
                ActionBarUtil.sendActionBar(killerPlayer, message);
                killerPlayer.sendMessage(ChatColor.AQUA + "◈ " + carrying + " Shard を奪取しました！");
            }
            
            if (victimPlayer != null) {
                victimPlayer.sendMessage(ChatColor.RED + "所持していた " + carrying + " Shardを奪われました！");
            }
        } else {
            // キラーがいない場合はシャード消失
            if (victimPlayer != null) {
                victimPlayer.sendMessage(ChatColor.RED + "所持していた " + carrying + " Shardを失いました！");
            }
        }
    }
    
    /**
     * プレイヤーの死亡時にShardをドロップ（後方互換）
     * → transferShardsOnDeathを使用推奨
     */
    public void dropPlayerShards(KLPlayer klPlayer, Location location) {
        // 物体ドロップは廃止 - シャードは消失扱い
        int carrying = klPlayer.takeAllCarryingShards();
        if (carrying > 0) {
            Player player = klPlayer.getPlayer();
            if (player != null) {
                player.sendMessage(ChatColor.RED + "所持していた " + carrying + " Shardを失いました！");
            }
        }
    }
    
    /**
     * 拠点に帰還した時の自動貯金
     */
    public void onReturnToBase(KLPlayer klPlayer) {
        int deposited = klPlayer.depositShards();
        if (deposited > 0) {
            klPlayer.addShardDeposited(deposited);
            
            // 統計: シャード納品を記録
            plugin.getStatsDatabase().addShardDeposited(klPlayer.getUuid(), deposited);
            
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
     * コア破壊時のシャード付与（破壊者に直接付与）
     */
    public void awardCoreDestroyShards(KLPlayer destroyer) {
        int baseAmount = plugin.getConfigManager().getShardCoreDestroyDrop();
        int amount = plugin.getGameManager().getScaledShardAmount(baseAmount);
        
        awardShard(destroyer, amount);
        
        Player player = destroyer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.GOLD + "★ コア破壊ボーナス: " + ChatColor.AQUA + "◈" + amount);
        }
    }
    
    /**
     * キング撃破時のシャード付与（キラーに直接付与）
     */
    public void awardKingKillShards(KLPlayer killer) {
        int baseAmount = plugin.getConfigManager().getShardKingDeathDrop();
        int amount = plugin.getGameManager().getScaledShardAmount(baseAmount);
        
        awardShard(killer, amount);
        
        Player player = killer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.GOLD + "★ キング撃破ボーナス: " + ChatColor.AQUA + "◈" + amount);
        }
    }
    
    /**
     * プレイヤーがShardを拾った時の処理（レガシー互換）
     */
    public void onPickupShard(KLPlayer klPlayer, int amount) {
        awardShard(klPlayer, amount);
    }
    
    /**
     * Shardアイテムを作成（レガシー互換用）
     */
    public ItemStack createShardItem() {
        ItemStack item = new ItemStack(SHARD_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(SHARD_DISPLAY_NAME);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "チームアップグレード用の通貨");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * アイテムがShardかどうか判定（レガシー互換用）
     */
    public boolean isShard(ItemStack item) {
        if (item == null || item.getType() != SHARD_MATERIAL) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && SHARD_DISPLAY_NAME.equals(meta.getDisplayName());
    }
}
