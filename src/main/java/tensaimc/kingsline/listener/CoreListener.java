package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.player.TeamManager;
import tensaimc.kingsline.util.ActionBarUtil;
import tensaimc.kingsline.util.TitleUtil;

/**
 * コア破壊リスナー
 * - コア破壊処理
 * - コア再生成（5秒後）
 * - コア接近警告システム
 */
public class CoreListener implements Listener {
    
    private final KingsLine plugin;
    
    // コアが破壊されたかどうか
    private boolean blueCoreDestroyed = false;
    private boolean redCoreDestroyed = false;
    
    // コア監視タスク
    private BukkitTask monitorTask;
    
    // 警告クールダウン（連続警告防止）
    private long lastBlueWarning = 0;
    private long lastRedWarning = 0;
    private static final long WARNING_COOLDOWN = 5000; // 5秒
    
    public CoreListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * リセット
     */
    public void reset() {
        blueCoreDestroyed = false;
        redCoreDestroyed = false;
        lastBlueWarning = 0;
        lastRedWarning = 0;
        stopMonitor();
    }
    
    /**
     * コア監視を開始
     */
    public void startMonitor() {
        stopMonitor();
        
        monitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkCoreProximity();
            }
        }.runTaskTimer(plugin, 40L, 40L); // 2秒ごと
    }
    
    /**
     * コア監視を停止
     */
    public void stopMonitor() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }
    
    /**
     * コア接近チェック
     */
    private void checkCoreProximity() {
        GameManager gm = plugin.getGameManager();
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        Arena arena = gm.getCurrentArena();
        if (arena == null) {
            return;
        }
        
        Location blueCore = arena.getBlueCore();
        Location redCore = arena.getRedCore();
        
        int warningRadius = 10;
        long now = System.currentTimeMillis();
        
        for (KLPlayer klPlayer : gm.getOnlinePlayers()) {
            if (!klPlayer.isAlive()) continue;
            
            Player player = klPlayer.getPlayer();
            if (player == null) continue;
            
            Location playerLoc = player.getLocation();
            
            // Blueコアへの接近（Redチームがチェック対象）
            if (klPlayer.getTeam() == Team.RED && blueCore != null && !blueCoreDestroyed) {
                if (playerLoc.getWorld().equals(blueCore.getWorld()) && 
                    playerLoc.distance(blueCore) <= warningRadius) {
                    if (now - lastBlueWarning > WARNING_COOLDOWN) {
                        warnTeam(Team.BLUE, player.getName());
                        lastBlueWarning = now;
                    }
                }
            }
            
            // Redコアへの接近（Blueチームがチェック対象）
            if (klPlayer.getTeam() == Team.BLUE && redCore != null && !redCoreDestroyed) {
                if (playerLoc.getWorld().equals(redCore.getWorld()) && 
                    playerLoc.distance(redCore) <= warningRadius) {
                    if (now - lastRedWarning > WARNING_COOLDOWN) {
                        warnTeam(Team.RED, player.getName());
                        lastRedWarning = now;
                    }
                }
            }
        }
    }
    
    /**
     * チームに警告を送信
     */
    private void warnTeam(Team team, String enemyName) {
        GameManager gm = plugin.getGameManager();
        TeamManager tm = plugin.getTeamManager();
        
        String warningMessage = ChatColor.RED + "" + ChatColor.BOLD + "⚠ コアに敵が接近中！ (" + enemyName + ")";
        
        for (KLPlayer klPlayer : tm.getTeamPlayers(gm.getPlayers(), team)) {
            if (!klPlayer.isOnline()) continue;
            
            Player player = klPlayer.getPlayer();
            if (player != null) {
                // サウンド
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 0.5f);
                
                // アクションバー
                ActionBarUtil.sendActionBar(player, warningMessage);
                
                // チャット
                player.sendMessage(warningMessage);
            }
        }
        
        // ボスバーでも警告
        plugin.getScoreboardManager().showCoreWarning(team);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中でなければ無視
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        // 黒曜石でなければ無視
        if (block.getType() != Material.OBSIDIAN) {
            return;
        }
        
        Arena arena = gm.getCurrentArena();
        if (arena == null) {
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        Location blockLoc = block.getLocation();
        
        // Blueコアをチェック
        Location blueCoreLoc = arena.getBlueCore();
        if (blueCoreLoc != null && isNearLocation(blockLoc, blueCoreLoc)) {
            // Redチームのみ破壊可能
            if (klPlayer.getTeam() == Team.RED && !blueCoreDestroyed) {
                destroyCore(Team.BLUE, klPlayer, blockLoc);
            } else {
                event.setCancelled(true);
                if (klPlayer.getTeam() == Team.BLUE) {
                    player.sendMessage(ChatColor.RED + "自チームのコアは破壊できません！");
                } else if (blueCoreDestroyed) {
                    player.sendMessage(ChatColor.YELLOW + "このコアは既に破壊されています。");
                }
            }
            return;
        }
        
        // Redコアをチェック
        Location redCoreLoc = arena.getRedCore();
        if (redCoreLoc != null && isNearLocation(blockLoc, redCoreLoc)) {
            // Blueチームのみ破壊可能
            if (klPlayer.getTeam() == Team.BLUE && !redCoreDestroyed) {
                destroyCore(Team.RED, klPlayer, blockLoc);
            } else {
                event.setCancelled(true);
                if (klPlayer.getTeam() == Team.RED) {
                    player.sendMessage(ChatColor.RED + "自チームのコアは破壊できません！");
                } else if (redCoreDestroyed) {
                    player.sendMessage(ChatColor.YELLOW + "このコアは既に破壊されています。");
                }
            }
            return;
        }
    }
    
    /**
     * コア破壊処理
     */
    private void destroyCore(Team destroyedTeam, KLPlayer destroyer, Location coreLoc) {
        GameManager gm = plugin.getGameManager();
        
        // コア破壊フラグを設定
        if (destroyedTeam == Team.BLUE) {
            blueCoreDestroyed = true;
        } else {
            redCoreDestroyed = true;
        }
        
        // ポイント加算
        int points = plugin.getConfigManager().getScoreCoreDestroy();
        gm.addScore(destroyer.getTeam(), points);
        
        // Shardドロップ
        plugin.getShardManager().dropCoreShards(coreLoc);
        
        // 全員にTitle通知
        String title = ChatColor.RED + "" + ChatColor.BOLD + "💥 コア破壊！";
        String subtitle = destroyedTeam.getChatColor() + destroyedTeam.getDisplayName() + 
                ChatColor.WHITE + " のコアが破壊されました！";
        
        for (KLPlayer klp : gm.getOnlinePlayers()) {
            Player p = klp.getPlayer();
            if (p != null) {
                TitleUtil.sendTitle(p, title, subtitle, 5, 40, 10);
                p.playSound(p.getLocation(), Sound.EXPLODE, 1.0f, 0.8f);
            }
        }
        
        // 通知
        gm.broadcast(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + 
                destroyer.getTeam().getColoredName() + " チームが " +
                destroyedTeam.getColoredName() + " のコアを破壊！ +" + points + "pt");
        
        Player player = destroyer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + 
                    "コアを破壊しました！ +" + points + "pt");
        }
        
        // 5秒後にコア再生成
        scheduleRegeneration(coreLoc, destroyedTeam);
    }
    
    /**
     * コア再生成をスケジュール
     */
    private void scheduleRegeneration(Location coreLoc, Team team) {
        new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                GameManager gm = plugin.getGameManager();
                
                if (!gm.isState(GameState.RUNNING)) {
                    cancel();
                    return;
                }
                
                if (countdown <= 0) {
                    // コアを再生成
                    regenerateCore(coreLoc, team);
                    cancel();
                    return;
                }
                
                // カウントダウン通知
                if (countdown <= 3) {
                    gm.broadcast(ChatColor.YELLOW + team.getColoredName() + 
                            " のコアが " + countdown + " 秒後に再生成されます...");
                }
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * コアを再生成
     */
    private void regenerateCore(Location coreLoc, Team team) {
        GameManager gm = plugin.getGameManager();
        
        if (coreLoc != null && coreLoc.getWorld() != null) {
            coreLoc.getBlock().setType(Material.OBSIDIAN);
        }
        
        // フラグをリセット
        if (team == Team.BLUE) {
            blueCoreDestroyed = false;
        } else {
            redCoreDestroyed = false;
        }
        
        // 通知
        String title = ChatColor.GREEN + "" + ChatColor.BOLD + "コア再生成！";
        String subtitle = team.getChatColor() + team.getDisplayName() + 
                ChatColor.WHITE + " のコアが復活しました！";
        
        for (KLPlayer klp : gm.getOnlinePlayers()) {
            Player p = klp.getPlayer();
            if (p != null) {
                TitleUtil.sendTitle(p, title, subtitle, 5, 40, 10);
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }
        
        gm.broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + 
                team.getColoredName() + " のコアが再生成されました！");
    }
    
    /**
     * 座標が近いかチェック
     */
    private boolean isNearLocation(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) {
            return false;
        }
        return loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
    
    public boolean isBlueCoreDestroyed() {
        return blueCoreDestroyed;
    }
    
    public boolean isRedCoreDestroyed() {
        return redCoreDestroyed;
    }
}
