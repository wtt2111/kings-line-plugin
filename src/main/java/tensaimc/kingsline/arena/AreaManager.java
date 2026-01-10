package tensaimc.kingsline.arena;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.util.ActionBarUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * エリア管理クラス
 */
public class AreaManager {
    
    private final KingsLine plugin;
    private BukkitTask captureTask;
    
    // 直前の占領チーム（連続通知防止用）
    private Team lastCapturingTeam = Team.NONE;
    
    // プレイヤーがエリア内にいるかどうかを追跡
    private final Map<UUID, Boolean> playersInBArea;
    
    public AreaManager(KingsLine plugin) {
        this.plugin = plugin;
        this.playersInBArea = new HashMap<>();
    }
    
    /**
     * エリア占領判定ループを開始
     */
    public void startCaptureLoop() {
        stopCaptureLoop();
        
        int tickInterval = plugin.getConfigManager().getAreaTickInterval();
        
        plugin.getLogger().info("[AreaManager] エリア占領ループを開始します（間隔: " + tickInterval + " ticks）");
        
        captureTask = new BukkitRunnable() {
            @Override
            public void run() {
                processBAreaCapture();
            }
        }.runTaskTimer(plugin, tickInterval, tickInterval);
    }
    
    /**
     * エリア占領判定ループを停止
     */
    public void stopCaptureLoop() {
        if (captureTask != null) {
            captureTask.cancel();
            captureTask = null;
        }
        lastCapturingTeam = Team.NONE;
        playersInBArea.clear();
    }
    
    // デバッグ用カウンター（ログスパム防止）
    private int debugCounter = 0;
    
    /**
     * Bエリアの占領判定
     */
    private void processBAreaCapture() {
        GameManager gm = plugin.getGameManager();
        Arena arena = gm.getCurrentArena();
        
        debugCounter++;
        boolean shouldLog = (debugCounter % 20 == 1); // 20回に1回だけログ
        
        if (arena == null) {
            if (shouldLog) {
                plugin.getLogger().warning("[AreaManager] arena is null!");
            }
            return;
        }
        
        Area areaB = arena.getAreaB();
        if (areaB == null) {
            if (shouldLog) {
                plugin.getLogger().warning("[AreaManager] areaB is null!");
            }
            return;
        }
        
        if (!areaB.isEnabled()) {
            if (shouldLog) {
                plugin.getLogger().warning("[AreaManager] areaB is disabled!");
            }
            return;
        }
        
        if (!areaB.isValid()) {
            if (shouldLog) {
                plugin.getLogger().warning("[AreaManager] areaB is not valid! pos1=" + areaB.getPos1() + ", pos2=" + areaB.getPos2());
            }
            return;
        }
        
        Map<UUID, KLPlayer> players = gm.getPlayers();
        
        int blueCount = areaB.getTeamCount(players, Team.BLUE);
        int redCount = areaB.getTeamCount(players, Team.RED);
        
        int points = plugin.getConfigManager().getScoreAreaCapture();
        
        // エリア内のプレイヤーにアクションバー通知
        for (KLPlayer klPlayer : gm.getOnlinePlayers()) {
            if (!klPlayer.isAlive()) continue;
            
            Player player = klPlayer.getPlayer();
            if (player == null) continue;
            
            boolean inArea = areaB.contains(player.getLocation());
            boolean wasInArea = playersInBArea.getOrDefault(klPlayer.getUuid(), false);
            
            if (inArea && !wasInArea) {
                // エリアに入った
                player.sendMessage(ChatColor.YELLOW + "【Bエリアに入りました】人数優位でポイント獲得！");
            }
            
            if (inArea) {
                // Bエリア内にいる間、シャードを自動付与（スケール適用）
                int baseShardAmount = plugin.getConfigManager().getShardSpawnAmount();
                int shardAmount = gm.getScaledShardAmount(baseShardAmount);
                klPlayer.addShardCarrying(shardAmount);
                
                // Bエリア内にいる間、再生1を付与（白熱したPVP用）
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 0), true); // 7秒間、レベル1
                
                // エリア内にいる場合、アクションバーで通知
                String status;
                String shardNotice = ChatColor.AQUA + " [+" + shardAmount + " Shard]";
                if (blueCount > redCount) {
                    if (klPlayer.getTeam() == Team.BLUE) {
                        status = ChatColor.GREEN + "Bエリア制圧中！ +" + points + "pt/tick" + shardNotice;
                    } else {
                        status = ChatColor.RED + "Bエリアを奪われています！ (" + blueCount + " vs " + redCount + ")" + shardNotice;
                    }
                } else if (redCount > blueCount) {
                    if (klPlayer.getTeam() == Team.RED) {
                        status = ChatColor.GREEN + "Bエリア制圧中！ +" + points + "pt/tick" + shardNotice;
                    } else {
                        status = ChatColor.RED + "Bエリアを奪われています！ (" + blueCount + " vs " + redCount + ")" + shardNotice;
                    }
                } else {
                    status = ChatColor.YELLOW + "Bエリア: 拮抗中 (" + blueCount + " vs " + redCount + ")" + shardNotice;
                }
                
                ActionBarUtil.sendActionBar(player, status);
            } else if (wasInArea && !inArea) {
                // エリアから出た場合、再生効果を削除
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }
            
            playersInBArea.put(klPlayer.getUuid(), inArea);
        }
        
        if (blueCount > redCount) {
            gm.addScore(Team.BLUE, points);
            
            // 占領開始通知（チームが変わった時のみ）
            if (lastCapturingTeam != Team.BLUE) {
                notifyAreaCapture(Team.BLUE, blueCount, redCount);
                lastCapturingTeam = Team.BLUE;
            }
        } else if (redCount > blueCount) {
            gm.addScore(Team.RED, points);
            
            if (lastCapturingTeam != Team.RED) {
                notifyAreaCapture(Team.RED, redCount, blueCount);
                lastCapturingTeam = Team.RED;
            }
        } else {
            // 同数または0人
            if (lastCapturingTeam != Team.NONE && blueCount == 0 && redCount == 0) {
                gm.broadcast(ChatColor.GRAY + "Bエリアが中立状態になりました。");
            }
            lastCapturingTeam = Team.NONE;
        }
    }
    
    /**
     * エリア占領通知
     */
    private void notifyAreaCapture(Team team, int capturingCount, int defendingCount) {
        GameManager gm = plugin.getGameManager();
        gm.broadcast(team.getChatColor() + "" + ChatColor.BOLD + team.getDisplayName() + 
                ChatColor.YELLOW + " がBエリアを制圧中！ (" + capturingCount + " vs " + defendingCount + ")");
    }
    
    /**
     * プレイヤーが指定エリア内にいるかチェック
     */
    public boolean isInArea(KLPlayer player, String areaId) {
        GameManager gm = plugin.getGameManager();
        Arena arena = gm.getCurrentArena();
        
        if (arena == null || player == null || !player.isOnline()) {
            return false;
        }
        
        Area area = arena.getArea(areaId);
        if (area == null || !area.isEnabled() || !area.isValid()) {
            return false;
        }
        
        return area.contains(player.getPlayer().getLocation());
    }
    
    /**
     * Bエリア内かチェック
     */
    public boolean isInBArea(KLPlayer player) {
        return isInArea(player, "B");
    }
}
