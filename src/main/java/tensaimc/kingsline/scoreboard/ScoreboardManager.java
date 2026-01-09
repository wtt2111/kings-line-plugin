package tensaimc.kingsline.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.listener.CoreListener;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.util.BossBarManager;

/**
 * スコアボード管理クラス
 * サイドバー、ボスバーの表示を制御
 */
public class ScoreboardManager {
    
    private final KingsLine plugin;
    private final BossBarManager bossBarManager;
    private BukkitTask updateTask;
    
    public ScoreboardManager(KingsLine plugin) {
        this.plugin = plugin;
        this.bossBarManager = new BossBarManager(plugin);
    }
    
    /**
     * スコアボード更新ループを開始
     */
    public void start() {
        stop();
        
        bossBarManager.start();
        
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllScoreboards();
                updateAllBossBars();
            }
        }.runTaskTimer(plugin, 0L, 10L); // 0.5秒ごとに更新
    }
    
    /**
     * スコアボード更新ループを停止
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        bossBarManager.stop();
        
        // スコアボードをクリア
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }
    
    /**
     * ボスバーマネージャーを取得
     */
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
    
    /**
     * 全プレイヤーのスコアボードを更新
     */
    private void updateAllScoreboards() {
        GameManager gm = plugin.getGameManager();
        
        if (!gm.isState(GameState.RUNNING, GameState.STARTING)) {
            return;
        }
        
        for (KLPlayer klPlayer : gm.getOnlinePlayers()) {
            Player player = klPlayer.getPlayer();
            if (player != null) {
                updatePlayerScoreboard(player, klPlayer);
            }
        }
    }
    
    /**
     * プレイヤーのスコアボードを更新
     */
    private void updatePlayerScoreboard(Player player, KLPlayer klPlayer) {
        GameManager gm = plugin.getGameManager();
        
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null || scoreboard.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }
        
        // 既存のObjectiveを削除
        Objective oldObjective = scoreboard.getObjective("kingsline");
        if (oldObjective != null) {
            oldObjective.unregister();
        }
        
        // 新規Objective作成
        Objective objective = scoreboard.registerNewObjective("kingsline", "dummy");
        objective.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ King's Line ⚔");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        int line = 15;
        
        // スコア
        int blueScore = gm.getScore(Team.BLUE);
        int redScore = gm.getScore(Team.RED);
        
        setLine(objective, line--, ChatColor.DARK_GRAY + "═════════════════");
        setLine(objective, line--, "");
        setLine(objective, line--, ChatColor.BLUE + "◆ BLUE: " + ChatColor.WHITE + blueScore + "pt");
        setLine(objective, line--, ChatColor.RED + "◆ RED: " + ChatColor.WHITE + redScore + "pt");
        setLine(objective, line--, " ");
        
        // コア状態
        CoreListener coreListener = getCoreListener();
        String blueCore = coreListener != null && coreListener.isBlueCoreDestroyed() ? 
                ChatColor.DARK_RED + "✗" : ChatColor.GREEN + "✓";
        String redCore = coreListener != null && coreListener.isRedCoreDestroyed() ? 
                ChatColor.DARK_RED + "✗" : ChatColor.GREEN + "✓";
        
        setLine(objective, line--, ChatColor.GRAY + "コア: " + ChatColor.BLUE + blueCore + 
                ChatColor.GRAY + " | " + ChatColor.RED + redCore);
        setLine(objective, line--, "  ");
        setLine(objective, line--, ChatColor.DARK_GRAY + "─────────────────");
        
        // 個人情報
        setLine(objective, line--, ChatColor.YELLOW + "あなたの状態:");
        setLine(objective, line--, ChatColor.AQUA + "◈ Shard: " + ChatColor.WHITE + 
                klPlayer.getShardCarrying() + ChatColor.GRAY + " (所持)");
        setLine(objective, line--, ChatColor.AQUA + "  貯金: " + ChatColor.WHITE + 
                klPlayer.getShardSaved());
        setLine(objective, line--, ChatColor.LIGHT_PURPLE + "✦ Lumina: " + ChatColor.WHITE + 
                klPlayer.getLuminaCarrying() + ChatColor.GRAY + " / " + 
                ChatColor.WHITE + klPlayer.getLuminaSaved());
        setLine(objective, line--, "   ");
        setLine(objective, line--, ChatColor.GREEN + "K/D: " + ChatColor.WHITE + 
                klPlayer.getKillsThisGame() + "/" + klPlayer.getDeathsThisGame());
        setLine(objective, line--, ChatColor.DARK_GRAY + "═════════════════");
    }
    
    /**
     * スコアボードにラインを設定
     */
    private void setLine(Objective objective, int score, String text) {
        // スコアボードの制限（16文字）回避のためにユニークな接頭辞を追加
        String unique = ChatColor.values()[score % ChatColor.values().length].toString();
        String displayText = text;
        if (displayText.length() > 40) {
            displayText = displayText.substring(0, 40);
        }
        
        Score line = objective.getScore(unique + displayText);
        line.setScore(score);
    }
    
    /**
     * 全プレイヤーのボスバーを更新
     */
    private void updateAllBossBars() {
        GameManager gm = plugin.getGameManager();
        
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        int blueScore = gm.getScore(Team.BLUE);
        int redScore = gm.getScore(Team.RED);
        int total = blueScore + redScore;
        
        float progress = total > 0 ? (float) blueScore / total : 0.5f;
        
        // コア破壊状態をチェック
        CoreListener coreListener = getCoreListener();
        boolean blueCoreDestroyed = coreListener != null && coreListener.isBlueCoreDestroyed();
        boolean redCoreDestroyed = coreListener != null && coreListener.isRedCoreDestroyed();
        
        String message;
        if (blueCoreDestroyed || redCoreDestroyed) {
            if (blueCoreDestroyed && redCoreDestroyed) {
                message = ChatColor.DARK_RED + "⚠ 両チームのコアが破壊されています！";
            } else if (blueCoreDestroyed) {
                message = ChatColor.BLUE + "BLUE" + ChatColor.DARK_RED + " のコアが破壊されています！";
            } else {
                message = ChatColor.RED + "RED" + ChatColor.DARK_RED + " のコアが破壊されています！";
            }
        } else {
            message = ChatColor.BLUE + "BLUE " + blueScore + "pt" + 
                    ChatColor.WHITE + " ═══════ " + 
                    ChatColor.RED + redScore + "pt RED";
        }
        
        bossBarManager.setGlobalBossBar(message, progress);
    }
    
    /**
     * CoreListenerを取得
     */
    private CoreListener getCoreListener() {
        // リスナーは直接アクセスできないため、プラグインに参照を追加する必要がある
        // 一旦nullを返す（後で修正）
        return plugin.getCoreListener();
    }
    
    /**
     * コア警告を表示
     */
    public void showCoreWarning(Team team) {
        String message = ChatColor.RED + "" + ChatColor.BOLD + "⚠ " + 
                team.getColoredName() + ChatColor.RED + "" + ChatColor.BOLD + 
                " コアに敵が接近中！";
        
        // 該当チームにのみ警告
        GameManager gm = plugin.getGameManager();
        for (KLPlayer klPlayer : gm.getOnlinePlayers()) {
            if (klPlayer.getTeam() == team && klPlayer.isOnline()) {
                bossBarManager.setBossBar(klPlayer.getPlayer(), message, 1.0f);
            }
        }
    }
}
