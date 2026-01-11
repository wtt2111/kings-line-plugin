package tensaimc.kingsline.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.listener.CoreListener;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.util.BossBarManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * スコアボード管理クラス
 * サイドバー、ボスバーの表示を制御
 */
public class ScoreboardManager {
    
    private static final String SERVER_IP = "mc.miyabimc.net";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    
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
        
        // LOBBY状態：全オンラインプレイヤーにロビースコアボードを表示
        if (gm.isState(GameState.LOBBY)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateLobbyScoreboard(player);
            }
            return;
        }
        
        // RUNNING/STARTING状態：ゲーム参加者にゲームスコアボードを表示
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
     * ロビースコアボードを更新（カウントダウン中）
     */
    private void updateLobbyScoreboard(Player player) {
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
        objective.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ KING'S LINE ⚔");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        int line = 12;
        
        // ヘッダー区切り線
        setLine(objective, line--, ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        
        // 日付
        setLine(objective, line--, ChatColor.GRAY + " " + DATE_FORMAT.format(new Date()));
        setLine(objective, line--, "");
        
        // プレイヤー数
        int playerCount = gm.getPlayerCount();
        int maxPlayers = plugin.getConfigManager().getLobbyMaxPlayers();
        setLine(objective, line--, ChatColor.WHITE + " プレイヤー: " + ChatColor.GREEN + playerCount + ChatColor.GRAY + "/" + maxPlayers);
        setLine(objective, line--, " ");
        
        // カウントダウン
        int countdown = gm.getLobbyCountdown();
        String timeStr = formatTime(countdown);
        String countdownColor = countdown <= 10 ? ChatColor.RED.toString() : 
                               countdown <= 30 ? ChatColor.YELLOW.toString() : 
                               ChatColor.GREEN.toString();
        setLine(objective, line--, ChatColor.WHITE + " 開始まで: " + countdownColor + timeStr);
        
        // 開始条件
        int minPlayers = plugin.getConfigManager().getLobbyMinPlayers();
        if (playerCount < minPlayers) {
            int needed = minPlayers - playerCount;
            setLine(objective, line--, ChatColor.YELLOW + " あと" + ChatColor.WHITE + needed + "人" + ChatColor.YELLOW + "で開始");
        } else {
            setLine(objective, line--, ChatColor.GREEN + " 開始準備完了！");
        }
        setLine(objective, line--, "  ");
        
        // アリーナ名
        Arena arena = gm.getCurrentArena();
        String arenaName = arena != null ? arena.getName() : "未設定";
        setLine(objective, line--, ChatColor.WHITE + " マップ: " + ChatColor.AQUA + arenaName);
        
        // フッター
        setLine(objective, line--, ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        setLine(objective, line--, ChatColor.YELLOW + " " + SERVER_IP);
    }
    
    /**
     * 時間をフォーマット（MM:SS）
     */
    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }
    
    /**
     * プレイヤーのスコアボードを更新（ゲーム中）
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
        objective.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ KING'S LINE ⚔");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        int line = 14;
        
        // ヘッダー区切り線
        setLine(objective, line--, ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        
        // 残り時間
        int timeRemaining = gm.getGameTimeRemaining();
        String timeColor = timeRemaining <= 300 ? ChatColor.RED.toString() : 
                          timeRemaining <= 600 ? ChatColor.YELLOW.toString() : 
                          ChatColor.GREEN.toString();
        setLine(objective, line--, ChatColor.WHITE + " 残り: " + timeColor + formatTime(timeRemaining));
        setLine(objective, line--, "");
        
        // スコア（リスポーン不可チームは残り人数を表示）
        int blueScore = gm.getScore(Team.BLUE);
        int redScore = gm.getScore(Team.RED);
        boolean blueCanRespawn = gm.canTeamRespawn(Team.BLUE);
        boolean redCanRespawn = gm.canTeamRespawn(Team.RED);
        
        // BLUEチーム - スコアは常に表示、リスポーン不可時は残り人数も追加
        if (!blueCanRespawn) {
            int blueAlive = plugin.getTeamManager().getAliveCount(gm.getPlayers(), Team.BLUE);
            setLine(objective, line--, ChatColor.BLUE + " ◆ BLUE: " + ChatColor.WHITE + blueScore + "pt " + ChatColor.YELLOW + "(残" + blueAlive + "人)");
        } else {
            setLine(objective, line--, ChatColor.BLUE + " ◆ BLUE: " + ChatColor.WHITE + blueScore + "pt");
        }
        
        // REDチーム - スコアは常に表示、リスポーン不可時は残り人数も追加
        if (!redCanRespawn) {
            int redAlive = plugin.getTeamManager().getAliveCount(gm.getPlayers(), Team.RED);
            setLine(objective, line--, ChatColor.RED + " ◆ RED: " + ChatColor.WHITE + redScore + "pt " + ChatColor.YELLOW + "(残" + redAlive + "人)");
        } else {
            setLine(objective, line--, ChatColor.RED + " ◆ RED: " + ChatColor.WHITE + redScore + "pt");
        }
        
        // コア状態
        CoreListener coreListener = getCoreListener();
        String blueCore = coreListener != null && coreListener.isBlueCoreDestroyed() ? 
                ChatColor.DARK_RED + "✗" : ChatColor.GREEN + "✓";
        String redCore = coreListener != null && coreListener.isRedCoreDestroyed() ? 
                ChatColor.DARK_RED + "✗" : ChatColor.GREEN + "✓";
        
        setLine(objective, line--, ChatColor.GRAY + " コア: " + ChatColor.BLUE + "B" + blueCore + 
                ChatColor.GRAY + " | " + ChatColor.RED + "R" + redCore);
        setLine(objective, line--, " ");
        
        // リソース（所持/貯金）
        setLine(objective, line--, ChatColor.AQUA + " Shard: " + ChatColor.WHITE + 
                klPlayer.getShardCarrying() + ChatColor.GRAY + "/" + 
                ChatColor.GREEN + klPlayer.getShardSaved());
        
        setLine(objective, line--, ChatColor.LIGHT_PURPLE + " Lumina: " + ChatColor.WHITE + 
                klPlayer.getLuminaCarrying() + ChatColor.GRAY + "/" + 
                ChatColor.GREEN + klPlayer.getLuminaSaved());
        setLine(objective, line--, "  ");
        
        // エレメント・K/D
        String elementDisplay = klPlayer.getElement() != null ? 
                klPlayer.getElement().getColor() + klPlayer.getElement().getDisplayName() : 
                ChatColor.GRAY + "未選択";
        setLine(objective, line--, ChatColor.YELLOW + " 属性: " + elementDisplay);
        
        setLine(objective, line--, ChatColor.GREEN + " K/D: " + ChatColor.WHITE + 
                klPlayer.getKillsThisGame() + ChatColor.GRAY + "/" + ChatColor.WHITE + klPlayer.getDeathsThisGame());
        
        // フッター
        setLine(objective, line--, ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━");
        setLine(objective, line--, ChatColor.YELLOW + " " + SERVER_IP);
    }
    
    /**
     * スコアボードにラインを設定
     */
    private void setLine(Objective objective, int score, String text) {
        // スコアが負の場合は無視（範囲外エラー防止）
        if (score < 0) {
            return;
        }
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
