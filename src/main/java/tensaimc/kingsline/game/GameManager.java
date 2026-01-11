package tensaimc.kingsline.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Area;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.config.ConfigManager;
import tensaimc.kingsline.element.Element;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.player.TeamManager;
import tensaimc.kingsline.util.ActionBarUtil;
import tensaimc.kingsline.util.TitleUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ゲーム全体を管理するクラス
 */
public class GameManager {
    
    private final KingsLine plugin;
    private GameState state;
    private Arena currentArena;
    
    private final Map<UUID, KLPlayer> players;
    
    // タスク
    private BukkitTask startingTask;
    private BukkitTask gameLoopTask;
    private BukkitTask lobbyTask;
    
    // ロビー関連
    private boolean autoLoopEnabled;
    private int lobbyCountdown;
    private boolean lobbyShortcutTriggered;
    
    // ゲームデータ
    private int blueScore;
    private int redScore;
    private boolean blueCanRespawn;
    private boolean redCanRespawn;
    
    // キング投票関連
    private final Set<UUID> kingCandidatesBlue;
    private final Set<UUID> kingCandidatesRed;
    private boolean votingPhase;
    
    // ゲーム制限時間
    private int gameTimeRemaining;
    
    // エレメント選択アイテム
    public static final Material ELEMENT_SELECT_MATERIAL = Material.NETHER_STAR;
    public static final String ELEMENT_SELECT_NAME = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "エレメント選択";
    
    // ロビーヘルプアイテム
    public static final Material HELP_ITEM_MATERIAL = Material.BOOK;
    public static final String HELP_ITEM_NAME = ChatColor.YELLOW + "" + ChatColor.BOLD + "ゲームガイド";
    
    public GameManager(KingsLine plugin) {
        this.plugin = plugin;
        this.state = GameState.WAITING;
        this.players = new HashMap<>();
        this.kingCandidatesBlue = new HashSet<>();
        this.kingCandidatesRed = new HashSet<>();
        reset();
    }
    
    /**
     * ゲームデータをリセット
     */
    public void reset() {
        blueScore = 0;
        redScore = 0;
        blueCanRespawn = true;
        redCanRespawn = true;
        votingPhase = false;
        kingCandidatesBlue.clear();
        kingCandidatesRed.clear();
        lobbyShortcutTriggered = false;
        gameTimeRemaining = 0;
        
        if (startingTask != null) {
            startingTask.cancel();
            startingTask = null;
        }
        if (gameLoopTask != null) {
            gameLoopTask.cancel();
            gameLoopTask = null;
        }
        if (lobbyTask != null) {
            lobbyTask.cancel();
            lobbyTask = null;
        }
        
        if (plugin.getTeamManager() != null) {
            plugin.getTeamManager().reset();
        }
        if (plugin.getCoreListener() != null) {
            plugin.getCoreListener().reset();
        }
        if (plugin.getChatManager() != null) {
            plugin.getChatManager().reset();
        }
    }
    
    // ========== Game State ==========
    
    public GameState getState() {
        return state;
    }
    
    public boolean isState(GameState... states) {
        for (GameState s : states) {
            if (state == s) return true;
        }
        return false;
    }
    
    // ========== Player Management ==========
    
    public Map<UUID, KLPlayer> getPlayers() {
        return players;
    }
    
    public KLPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }
    
    public KLPlayer getPlayer(Player player) {
        return players.get(player.getUniqueId());
    }
    
    public void addPlayer(Player player) {
        if (!players.containsKey(player.getUniqueId())) {
            players.put(player.getUniqueId(), new KLPlayer(player.getUniqueId()));
            // データベースにプレイヤーを登録
            plugin.getStatsDatabase().ensurePlayer(player.getUniqueId(), player.getName());
        }
    }
    
    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }
    
    public int getPlayerCount() {
        return players.size();
    }
    
    /**
     * ロビーカウントダウンの残り秒数を取得
     */
    public int getLobbyCountdown() {
        return lobbyCountdown;
    }
    
    public List<KLPlayer> getOnlinePlayers() {
        List<KLPlayer> online = new ArrayList<>();
        for (KLPlayer klp : players.values()) {
            if (klp.isOnline()) {
                online.add(klp);
            }
        }
        return online;
    }
    
    // ========== Arena ==========
    
    public Arena getCurrentArena() {
        return currentArena;
    }
    
    public void setCurrentArena(Arena arena) {
        this.currentArena = arena;
    }
    
    // ========== Score ==========
    
    public int getScore(Team team) {
        switch (team) {
            case BLUE: return blueScore;
            case RED: return redScore;
            default: return 0;
        }
    }
    
    public void addScore(Team team, int amount) {
        int maxPoints = plugin.getConfigManager().getPointsToWin();
        
        switch (team) {
            case BLUE:
                // 既に500pt以上なら加算しない
                if (blueScore >= maxPoints) return;
                blueScore = Math.min(blueScore + amount, maxPoints);
                break;
            case RED:
                if (redScore >= maxPoints) return;
                redScore = Math.min(redScore + amount, maxPoints);
                break;
        }
        
        // 500点到達チェック
        checkPointsThreshold();
    }
    
    /**
     * スコアを直接設定（デバッグ用）
     */
    public void setScore(Team team, int amount) {
        switch (team) {
            case BLUE:
                blueScore = Math.max(0, amount);
                break;
            case RED:
                redScore = Math.max(0, amount);
                break;
        }
        
        // 500点到達チェック
        checkPointsThreshold();
    }
    
    private void checkPointsThreshold() {
        ConfigManager config = plugin.getConfigManager();
        int threshold = config.getPointsToWin();
        
        if (blueScore >= threshold && redCanRespawn) {
            redCanRespawn = false;
            disableRespawn(Team.RED);
        }
        
        if (redScore >= threshold && blueCanRespawn) {
            blueCanRespawn = false;
            disableRespawn(Team.BLUE);
        }
    }
    
    private void disableRespawn(Team team) {
        TeamManager tm = plugin.getTeamManager();
        for (KLPlayer klp : tm.getTeamPlayers(players, team)) {
            klp.setCanRespawn(false);
        }
        
        // Titleで通知
        String title = ChatColor.RED + "" + ChatColor.BOLD + "⚠ リスポーン無効化！";
        String subtitle = team.getChatColor() + team.getDisplayName() + 
                ChatColor.RED + " チームはリスポーンできなくなりました！";
        
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, title, subtitle, 10, 60, 20);
                player.playSound(player.getLocation(), Sound.WITHER_DEATH, 0.5f, 1.5f);
            }
        }
        
        broadcast(ChatColor.RED + "" + ChatColor.BOLD + team.getColoredName() + 
                " チームのリスポーンが無効化されました！");
    }
    
    public boolean canTeamRespawn(Team team) {
        switch (team) {
            case BLUE: return blueCanRespawn;
            case RED: return redCanRespawn;
            default: return false;
        }
    }
    
    // ========== King Voting ==========
    
    /**
     * キング立候補を追加
     */
    public void addKingCandidate(KLPlayer klPlayer) {
        if (klPlayer.getTeam() == Team.BLUE) {
            kingCandidatesBlue.add(klPlayer.getUuid());
        } else if (klPlayer.getTeam() == Team.RED) {
            kingCandidatesRed.add(klPlayer.getUuid());
        }
        
        broadcast(klPlayer.getTeam().getChatColor() + klPlayer.getName() + 
                ChatColor.GOLD + " がキングに立候補しました！");
    }
    
    public boolean isVotingPhase() {
        return votingPhase;
    }
    
    /**
     * ゲーム残り時間を取得（秒）
     */
    public int getGameTimeRemaining() {
        return gameTimeRemaining;
    }
    
    // ========== Lobby System ==========
    
    /**
     * 自動ループを開始
     * /kl start で呼ばれる
     */
    public boolean startAutoLoop() {
        if (autoLoopEnabled) {
            return false;
        }
        
        // アリーナをセット
        currentArena = plugin.getArenaConfig().getCurrentArena();
        if (currentArena == null || !currentArena.isValid()) {
            broadcast(ChatColor.RED + "アリーナが正しく設定されていません。");
            return false;
        }
        
        autoLoopEnabled = true;
        startLobbyCountdown();
        return true;
    }
    
    /**
     * デバッグ用: 人数を無視してゲームを強制開始
     */
    public boolean forceStartGame() {
        if (state == GameState.RUNNING || state == GameState.STARTING) {
            return false;
        }
        
        // アリーナをセット
        currentArena = plugin.getArenaConfig().getCurrentArena();
        if (currentArena == null || !currentArena.isValid()) {
            broadcast(ChatColor.RED + "アリーナが正しく設定されていません。");
            return false;
        }
        
        // ロビータスクが動いていたら停止
        if (lobbyTask != null) {
            lobbyTask.cancel();
            lobbyTask = null;
        }
        
        autoLoopEnabled = true; // ゲーム終了後の自動ループを有効化
        
        // オンラインプレイヤーを追加
        players.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayer(player);
        }
        
        if (players.isEmpty()) {
            broadcast(ChatColor.RED + "プレイヤーがいません。");
            return false;
        }
        
        broadcast(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "[DEBUG] " + 
                ChatColor.YELLOW + "ゲームを強制開始します！（プレイヤー: " + players.size() + "人）");
        
        // 直接ゲーム開始（通常のstartGameを呼ぶ）
        return startGame();
    }
    
    /**
     * 自動ループを停止
     * /kl stop で呼ばれる
     */
    public void stopAutoLoop() {
        autoLoopEnabled = false;
        
        if (lobbyTask != null) {
            lobbyTask.cancel();
            lobbyTask = null;
        }
        
        // ゲーム中なら強制終了（統計保存なし）
        if (state == GameState.STARTING || state == GameState.RUNNING) {
            forceStopWithoutStats();
        } else if (state == GameState.LOBBY) {
            state = GameState.WAITING;
            broadcast(ChatColor.YELLOW + "自動ゲームループが停止されました。");
            
            // スコアボード停止
            plugin.getScoreboardManager().stop();
            
            // プレイヤーをロビーへ
            Location lobby = currentArena != null ? currentArena.getLobby() : null;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (lobby != null) {
                    player.teleport(lobby);
                }
            }
            players.clear();
        }
    }
    
    /**
     * ロビーカウントダウンを開始
     */
    public void startLobbyCountdown() {
        if (!autoLoopEnabled) {
            return;
        }
        
        state = GameState.LOBBY;
        lobbyShortcutTriggered = false;
        lobbyCountdown = plugin.getConfigManager().getLobbyCountdownTime();
        
        // 既存プレイヤーをロビーに追加
        players.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayer(player);
            
            // ロビー位置へテレポート
            Location lobby = currentArena != null ? currentArena.getLobby() : null;
            if (lobby != null) {
                player.teleport(lobby);
            }
            
            // ヘルプアイテムを配布
            giveHelpItem(player);
        }
        
        broadcast(ChatColor.GREEN + "========================================");
        broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "  ⚔ KING'S LINE ⚔");
        broadcast(ChatColor.YELLOW + "  次のゲームまで " + formatTime(lobbyCountdown));
        broadcast(ChatColor.GRAY + "  最低 " + plugin.getConfigManager().getLobbyMinPlayers() + " 人で開始");
        broadcast(ChatColor.GREEN + "========================================");
        
        // スコアボード更新開始
        plugin.getScoreboardManager().start();
        
        // カウントダウンタスク
        lobbyTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.LOBBY) {
                    cancel();
                    return;
                }
                
                lobbyCountdown--;
                
                // 人数チェック
                int playerCount = getPlayerCount();
                int minPlayers = plugin.getConfigManager().getLobbyMinPlayers();
                int shortcutPlayers = plugin.getConfigManager().getLobbyShortcutPlayers();
                int shortcutTime = plugin.getConfigManager().getLobbyShortcutTime();
                
                // 14人で短縮（一度だけ）
                if (!lobbyShortcutTriggered && playerCount >= shortcutPlayers && lobbyCountdown > shortcutTime) {
                    lobbyShortcutTriggered = true;
                    lobbyCountdown = shortcutTime;
                    
                    broadcast(ChatColor.GOLD + "========================================");
                    broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "  ⚡ " + shortcutPlayers + "人到達！");
                    broadcast(ChatColor.YELLOW + "  カウントダウンが " + formatTime(shortcutTime) + " に短縮されました！");
                    broadcast(ChatColor.GOLD + "========================================");
                    
                    // 全員にサウンド
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.5f);
                    }
                }
                
                // アクションバーで残り時間表示
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String countdownColor = lobbyCountdown <= 10 ? ChatColor.RED.toString() : 
                                           lobbyCountdown <= 30 ? ChatColor.YELLOW.toString() : 
                                           ChatColor.GREEN.toString();
                    
                    String statusText;
                    if (playerCount < minPlayers) {
                        int needed = minPlayers - playerCount;
                        statusText = ChatColor.RED + "あと " + needed + " 人必要！";
                    } else {
                        statusText = ChatColor.GREEN + "開始準備完了！";
                    }
                    
                    ActionBarUtil.sendActionBar(p, 
                            ChatColor.GOLD + "⚔ KING'S LINE ⚔ " + 
                            ChatColor.WHITE + "| " + countdownColor + formatTime(lobbyCountdown) + 
                            ChatColor.WHITE + " | " + 
                            ChatColor.AQUA + playerCount + "/" + plugin.getConfigManager().getLobbyMaxPlayers() + "人" +
                            ChatColor.WHITE + " | " + statusText);
                }
                
                // カウントダウン通知
                if (lobbyCountdown == 300 || lobbyCountdown == 120 || lobbyCountdown == 60 || 
                    lobbyCountdown == 30 || lobbyCountdown == 10 || lobbyCountdown == 5 || 
                    lobbyCountdown == 4 || lobbyCountdown == 3 || lobbyCountdown == 2 || lobbyCountdown == 1) {
                    
                    broadcast(ChatColor.YELLOW + "ゲーム開始まで " + ChatColor.WHITE + formatTime(lobbyCountdown));
                    
                    // サウンド
                    float pitch = lobbyCountdown <= 5 ? 1.5f : 1.0f;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, pitch);
                    }
                }
                
                // カウント終了
                if (lobbyCountdown <= 0) {
                    cancel();
                    lobbyTask = null;
                    
                    if (playerCount >= minPlayers) {
                        // ゲーム開始
                        startGame();
                    } else {
                        // 人数不足、リスタート
                        broadcast(ChatColor.RED + "========================================");
                        broadcast(ChatColor.RED + "" + ChatColor.BOLD + "  ⚠ 人数不足！");
                        broadcast(ChatColor.YELLOW + "  " + minPlayers + "人以上でゲーム開始できます");
                        broadcast(ChatColor.GRAY + "  カウントダウンをリスタートします...");
                        broadcast(ChatColor.RED + "========================================");
                        
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                        }
                        
                        // 少し待ってからリスタート
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (autoLoopEnabled && state == GameState.LOBBY) {
                                    startLobbyCountdown();
                                }
                            }
                        }.runTaskLater(plugin, 60L); // 3秒後
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // 1秒ごと
    }
    
    /**
     * ロビーにプレイヤーを追加（参加時）
     */
    public void onPlayerJoinLobby(Player player) {
        if (state != GameState.LOBBY) {
            return;
        }
        
        int maxPlayers = plugin.getConfigManager().getLobbyMaxPlayers();
        if (getPlayerCount() >= maxPlayers) {
            player.sendMessage(ChatColor.RED + "ゲームは満員です！（" + maxPlayers + "/" + maxPlayers + "）");
            return;
        }
        
        addPlayer(player);
        
        // ロビー位置へテレポート
        Location lobby = currentArena != null ? currentArena.getLobby() : null;
        if (lobby != null) {
            player.teleport(lobby);
        }
        
        // ヘルプアイテムを配布
        giveHelpItem(player);
        
        broadcast(ChatColor.GREEN + player.getName() + ChatColor.YELLOW + " がロビーに参加しました！ " +
                ChatColor.GRAY + "(" + getPlayerCount() + "/" + maxPlayers + ")");
    }
    
    /**
     * ロビーからプレイヤーを削除（退出時）
     */
    public void onPlayerLeaveLobby(Player player) {
        if (state != GameState.LOBBY) {
            return;
        }
        
        if (players.containsKey(player.getUniqueId())) {
            removePlayer(player.getUniqueId());
            broadcast(ChatColor.RED + player.getName() + ChatColor.YELLOW + " がロビーから退出しました。 " +
                    ChatColor.GRAY + "(" + getPlayerCount() + "/" + plugin.getConfigManager().getLobbyMaxPlayers() + ")");
        }
    }
    
    /**
     * 自動ループが有効かどうか
     */
    public boolean isAutoLoopEnabled() {
        return autoLoopEnabled;
    }
    
    /**
     * 時間をフォーマット（MM:SS）
     */
    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }
    
    // ========== Game Flow ==========
    
    /**
     * ゲーム開始
     */
    public boolean startGame() {
        if (state != GameState.WAITING && state != GameState.LOBBY) {
            return false;
        }
        
        // アリーナをセット
        currentArena = plugin.getArenaConfig().getCurrentArena();
        if (currentArena == null || !currentArena.isValid()) {
            broadcast(ChatColor.RED + "アリーナが正しく設定されていません。");
            return false;
        }
        
        // オンラインプレイヤーを追加
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayer(player);
        }
        
        if (players.isEmpty()) {
            broadcast(ChatColor.RED + "プレイヤーがいません。");
            return false;
        }
        
        // 新規ゲーム用にリセット（エレメント選択前）
        for (KLPlayer klp : players.values()) {
            Player player = klp.getPlayer();
            if (player != null) {
                // 体力の最大値を正常値に戻す（前ゲームのキングの体力が残っている場合）
                player.setMaxHealth(20.0);
                player.setHealth(20.0);
                
                // ポーション効果をクリア
                player.getActivePotionEffects().forEach(effect -> 
                        player.removePotionEffect(effect.getType()));
                
                // walkSpeedをデフォルトに戻す（Wind対策）
                player.setWalkSpeed(0.2f);
            }
            klp.resetForNewGame();
        }
        
        state = GameState.STARTING;
        
        // チーム振り分け
        plugin.getTeamManager().assignTeams(players);
        
        // 各プレイヤーにチーム通知
        for (KLPlayer klp : players.values()) {
            Player player = klp.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, 
                        klp.getTeam().getChatColor() + "" + ChatColor.BOLD + klp.getTeam().getDisplayName() + " TEAM",
                        ChatColor.WHITE + "あなたのチームです", 
                        10, 60, 20);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }
        
        // スケール判定
        int perTeam = players.size() / 2;
        if (perTeam <= plugin.getConfigManager().getSmallScaleThreshold()) {
            currentArena.applySmallScaleMode();
            broadcast(ChatColor.GRAY + "小規模モード: Bエリアのみ有効");
        } else {
            currentArena.applyLargeScaleMode();
            broadcast(ChatColor.GRAY + "大規模モード: A/B/Cエリア有効");
        }
        
        // 準備フェーズ開始（エレメント選択、キング投票）
        startPreparationPhase();
        
        return true;
    }
    
    /**
     * 準備フェーズ開始
     */
    private void startPreparationPhase() {
        votingPhase = false; // エレメント選択フェーズではまだfalse
        
        // 全員にエレメント選択アイテムを配布
        for (KLPlayer klp : players.values()) {
            Player player = klp.getPlayer();
            if (player != null) {
                giveElementSelectItem(player);
                
                // スポーンへテレポート（待機用）
                Location spawn = currentArena.getSpawn(klp.getTeam());
                if (spawn != null) {
                    player.teleport(spawn);
                }
            }
        }
        
        // 開始タイトル
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, 
                        ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ KING'S LINE ⚔",
                        ChatColor.WHITE + "準備フェーズ開始！", 
                        10, 60, 20);
            }
        }
        
        broadcast(ChatColor.GREEN + "========================================");
        broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "  準備フェーズ開始！");
        broadcast(ChatColor.YELLOW + "  ・エレメントを選択してください（ネザースターを右クリック）");
        broadcast(ChatColor.GREEN + "========================================");
        
        startingTask = new BukkitRunnable() {
            int countdown = 60; // エレメント選択フェーズ60秒
            int phase = 0; // 0=エレメント選択, 1=キング投票
            
            @Override
            public void run() {
                if (countdown <= 0) {
                    if (phase == 0) {
                        // エレメント選択終了、キング投票開始
                        phase = 1;
                        countdown = 60; // キング投票60秒
                        votingPhase = true; // キング投票フェーズ開始
                        startKingVotingPhase();
                        return;
                    } else {
                        // キング投票終了、ゲーム開始
                        cancel();
                        finishVotingAndStart();
                        return;
                    }
                }
                
                // 全員にアクションバーで残り時間を表示
                for (KLPlayer klp : getOnlinePlayers()) {
                    Player player = klp.getPlayer();
                    if (player != null) {
                        String phaseText = phase == 0 ? "エレメント選択" : "キング投票";
                        String statusText = "";
                        
                        if (phase == 0 && !klp.hasSelectedElement()) {
                            statusText = ChatColor.RED + " ⚠未選択！";
                        }
                        
                        ActionBarUtil.sendActionBar(player, 
                                ChatColor.YELLOW + "【" + phaseText + "】" + 
                                ChatColor.WHITE + "残り " + ChatColor.GREEN + countdown + ChatColor.WHITE + " 秒" +
                                statusText);
                    }
                }
                
                // カウントダウン通知
                if (phase == 0) {
                    // エレメント選択フェーズ - チャット通知
                    if (countdown == 30 || countdown == 20 || countdown == 10 || countdown == 5 || countdown == 3) {
                        broadcast(ChatColor.YELLOW + "エレメント選択終了まで " + countdown + " 秒...");
                    }
                } else {
                    // キング投票フェーズ - チャット通知
                    if (countdown == 30 || countdown == 20 || countdown == 10 || countdown == 5 || countdown == 3) {
                        broadcast(ChatColor.GOLD + "キング投票終了まで " + countdown + " 秒...");
                    }
                }
                
                // サウンド通知
                if (countdown <= 5 || countdown == 10) {
                    float pitch = countdown <= 3 ? 1.5f : 1.0f;
                    for (KLPlayer klp : getOnlinePlayers()) {
                        Player player = klp.getPlayer();
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, pitch);
                        }
                    }
                }
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    // 投票GUIアイテム
    public static final Material KING_VOTE_MATERIAL = Material.JUKEBOX;
    public static final String KING_VOTE_ITEM_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "👑 キング投票";
    
    /**
     * キング投票フェーズ開始
     */
    private void startKingVotingPhase() {
        // 投票GUIをリセット（既存の立候補者は保持されない）
        plugin.getKingVoteGUI().reset();
        for (UUID candidateId : kingCandidatesBlue) {
            plugin.getKingVoteGUI().addCandidate(candidateId, Team.BLUE);
        }
        for (UUID candidateId : kingCandidatesRed) {
            plugin.getKingVoteGUI().addCandidate(candidateId, Team.RED);
        }
        
        // 全員にTitle通知と投票アイテム配布
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, 
                        ChatColor.GOLD + "" + ChatColor.BOLD + "👑 キング投票タイム 👑",
                        ChatColor.WHITE + "!king で立候補 / ジュークボックスで投票", 
                        10, 60, 20);
                
                // 投票用アイテムを配布
                giveKingVoteItem(player);
            }
        }
        
        broadcast(ChatColor.GOLD + "========================================");
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "  👑 キング投票フェーズ！");
        broadcast(ChatColor.YELLOW + "  ・!king とチャットで立候補できます");
        broadcast(ChatColor.YELLOW + "  ・ジュークボックスを右クリックで投票GUIを開けます");
        if (kingCandidatesBlue.isEmpty() && kingCandidatesRed.isEmpty()) {
            broadcast(ChatColor.GRAY + "  （立候補者がいない場合はランダムで決定）");
        }
        broadcast(ChatColor.GOLD + "========================================");
    }
    
    /**
     * キング投票アイテムを付与
     */
    private void giveKingVoteItem(Player player) {
        ItemStack item = new ItemStack(KING_VOTE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(KING_VOTE_ITEM_NAME);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "右クリックでキング投票GUIを開く");
        lore.add("");
        lore.add(ChatColor.YELLOW + "チームのキングを選ぼう！");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        
        player.getInventory().setItem(5, item); // エレメント選択アイテムの隣
    }
    
    /**
     * 投票終了とゲーム開始
     */
    private void finishVotingAndStart() {
        votingPhase = false;
        
        // 立候補者からキングを選出（いなければランダム）
        selectKings();
        
        // 本戦開始
        beginGame();
    }
    
    /**
     * キングを選出（投票結果から）
     */
    private void selectKings() {
        TeamManager tm = plugin.getTeamManager();
        
        // Blue - 投票結果から最多得票者を選出
        UUID blueWinner = plugin.getKingVoteGUI().getWinner(Team.BLUE);
        if (blueWinner != null) {
            KLPlayer king = getPlayer(blueWinner);
            if (king != null) {
                plugin.getKingManager().setKing(Team.BLUE, king);
            }
        } else {
            // 立候補者もいない場合はランダム選出
            List<KLPlayer> bluePlayers = tm.getTeamPlayers(players, Team.BLUE);
            if (!bluePlayers.isEmpty()) {
                KLPlayer king = bluePlayers.get(new Random().nextInt(bluePlayers.size()));
                plugin.getKingManager().setKing(Team.BLUE, king);
            }
        }
        
        // Red - 投票結果から最多得票者を選出
        UUID redWinner = plugin.getKingVoteGUI().getWinner(Team.RED);
        if (redWinner != null) {
            KLPlayer king = getPlayer(redWinner);
            if (king != null) {
                plugin.getKingManager().setKing(Team.RED, king);
            }
        } else {
            List<KLPlayer> redPlayers = tm.getTeamPlayers(players, Team.RED);
            if (!redPlayers.isEmpty()) {
                KLPlayer king = redPlayers.get(new Random().nextInt(redPlayers.size()));
                plugin.getKingManager().setKing(Team.RED, king);
            }
        }
    }
    
    /**
     * 本戦開始
     */
    private void beginGame() {
        state = GameState.RUNNING;
        
        // 制限時間をセット
        gameTimeRemaining = plugin.getConfigManager().getGameTimeLimit();
        
        // コアを強制設置（バグ防止）
        placeCores();
        
        // エレメント未選択のプレイヤーにランダムで割り当て
        Element[] elements = Element.values();
        for (KLPlayer klp : players.values()) {
            if (klp.getElement() == null) {
                Element randomElement = elements[ThreadLocalRandom.current().nextInt(elements.length)];
                klp.setElement(randomElement);
                
                Player player = klp.getPlayer();
                if (player != null) {
                    player.sendMessage(ChatColor.YELLOW + "エレメントが自動選択されました: " + 
                            randomElement.getColor() + randomElement.getName());
                }
            }
            
            // パッシブ効果を適用（全員に）
            plugin.getElementManager().applyPassiveEffects(klp);
        }
        
        // 天候を常に晴れに固定
        World world = currentArena.getWorld();
        if (world != null) {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
        }
        
        // 開始Title
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, 
                        ChatColor.GREEN + "" + ChatColor.BOLD + "⚔ ゲーム開始！ ⚔",
                        ChatColor.WHITE + "敵のコアを破壊してポイントを稼げ！", 
                        10, 60, 20);
                player.playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5f, 1.0f);
            }
        }
        
        broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "ゲーム開始！");
        
        // プレイヤーを各チームスポーンへテレポート & 装備
        for (KLPlayer klp : players.values()) {
            Player player = klp.getPlayer();
            if (player != null) {
                // スポーンへテレポート
                Location spawn = currentArena.getSpawn(klp.getTeam());
                if (spawn != null) {
                    player.teleport(spawn);
                }
                
                // 初期装備
                giveGear(player, klp.getTeam());
                
                // アップグレード効果を適用（キングのダイヤチェストプレート等）
                plugin.getUpgradeManager().applyUpgradeToPlayer(klp);
                
                // ゲームモードをサバイバルに
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
            }
            
            // ゲーム開始時の状態設定
            klp.setAlive(true);
            klp.setCanRespawn(true);
        }
        
        // ワールド内の全村人をクリーンアップしてからNPCをスポーン
        plugin.getNPCManager().cleanupVillagers(currentArena.getWorld());
        plugin.getNPCManager().removeAllNPCs();
        plugin.getNPCManager().spawnNPCs(currentArena);
        
        // スコアボード開始
        plugin.getScoreboardManager().start();
        
        // コア監視開始
        plugin.getCoreListener().startMonitor();
        
        // Shardスポーン開始
        plugin.getShardManager().startSpawnLoop();
        
        // キングオーラ開始
        plugin.getKingManager().startAuraLoop();
        
        // エリア占領ループ開始
        plugin.getAreaManager().startCaptureLoop();
        
        // デバッグ: エリアB状態を表示
        Area areaB = currentArena.getAreaB();
        if (areaB != null) {
            plugin.getLogger().info("[Debug] AreaB status - enabled: " + areaB.isEnabled() + ", valid: " + areaB.isValid());
            if (areaB.getPos1() != null) {
                plugin.getLogger().info("[Debug] AreaB pos1: " + areaB.getPos1().getBlockX() + "," + areaB.getPos1().getBlockY() + "," + areaB.getPos1().getBlockZ());
            }
            if (areaB.getPos2() != null) {
                plugin.getLogger().info("[Debug] AreaB pos2: " + areaB.getPos2().getBlockX() + "," + areaB.getPos2().getBlockY() + "," + areaB.getPos2().getBlockZ());
            }
        } else {
            plugin.getLogger().warning("[Debug] AreaB is NULL!");
        }
        
        // ゲームループ開始
        startGameLoop();
    }
    
    /**
     * エレメント選択アイテムを付与
     */
    private void giveElementSelectItem(Player player) {
        ItemStack item = new ItemStack(ELEMENT_SELECT_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ELEMENT_SELECT_NAME);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "右クリックでエレメントを選択");
        lore.add("");
        lore.add(ChatColor.YELLOW + "エレメントを選んで戦え！");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        
        player.getInventory().setItem(4, item); // 中央スロット
    }
    
    /**
     * ロビーヘルプアイテムを付与
     */
    public void giveHelpItem(Player player) {
        ItemStack item = new ItemStack(HELP_ITEM_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(HELP_ITEM_NAME);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "右クリックでガイドを開く");
        lore.add("");
        lore.add(ChatColor.YELLOW + "• ゲームルール");
        lore.add(ChatColor.YELLOW + "• コマンド一覧");
        lore.add(ChatColor.YELLOW + "• エレメント情報");
        lore.add(ChatColor.YELLOW + "• ショップアイテム");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        
        player.getInventory().setItem(4, item); // 中央スロット
    }
    
    /**
     * ゲームループ
     */
    private void startGameLoop() {
        gameLoopTask = new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (state != GameState.RUNNING) {
                    cancel();
                    return;
                }
                
                // 毎秒の処理
                if (tick % 20 == 0) {
                    // 拠点帰還チェック
                    checkBaseReturn();
                    
                    // 残り時間カウントダウン
                    if (gameTimeRemaining > 0) {
                        gameTimeRemaining--;
                        
                        // 時間警告通知
                        checkTimeWarnings();
                        
                        // 時間切れ判定
                        if (gameTimeRemaining <= 0) {
                            handleTimeUp();
                            return;
                        }
                    }
                }
                
                // 勝利判定
                checkWinCondition();
                
                tick++;
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }
    
    /**
     * 残り時間の警告通知
     */
    private void checkTimeWarnings() {
        // 特定の残り時間で通知
        if (gameTimeRemaining == 300) { // 5分
            broadcastTimeWarning(5, "分");
        } else if (gameTimeRemaining == 180) { // 3分
            broadcastTimeWarning(3, "分");
        } else if (gameTimeRemaining == 60) { // 1分
            broadcastTimeWarning(1, "分");
        } else if (gameTimeRemaining == 30) { // 30秒
            broadcastTimeWarning(30, "秒");
        } else if (gameTimeRemaining == 10) { // 10秒
            broadcastTimeWarning(10, "秒");
        } else if (gameTimeRemaining <= 5 && gameTimeRemaining > 0) { // 5, 4, 3, 2, 1秒
            broadcastTimeWarning(gameTimeRemaining, "秒");
        }
    }
    
    /**
     * 残り時間警告をブロードキャスト
     */
    private void broadcastTimeWarning(int time, String unit) {
        String color = time <= 30 && unit.equals("秒") ? ChatColor.RED.toString() : ChatColor.YELLOW.toString();
        
        broadcast(color + "⏰ 残り時間: " + ChatColor.WHITE + ChatColor.BOLD + time + unit);
        
        // サウンド
        float pitch = time <= 5 && unit.equals("秒") ? 1.5f : 1.0f;
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, pitch);
            }
        }
    }
    
    /**
     * 時間切れ時の処理
     * 勝敗を判定してゲームを終了する
     */
    private void handleTimeUp() {
        broadcast(ChatColor.RED + "" + ChatColor.BOLD + "⏰ 時間切れ！");
        
        // 全員にTitle通知
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, 
                        ChatColor.RED + "" + ChatColor.BOLD + "⏰ TIME UP!",
                        ChatColor.WHITE + "勝敗を判定中...", 
                        10, 60, 20);
                player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0f, 1.0f);
            }
        }
        
        // 勝者を決定
        Team winner = determineWinner();
        
        if (winner != null) {
            endGame(winner);
        } else {
            // 完全引き分け（ランダム）
            Team randomWinner = Math.random() < 0.5 ? Team.BLUE : Team.RED;
            broadcast(ChatColor.GOLD + "完全な引き分け！ランダムで勝者を決定...");
            endGame(randomWinner);
        }
    }
    
    /**
     * 時間切れ時の勝者を決定
     * @return 勝者チーム、完全引き分けの場合はnull
     */
    private Team determineWinner() {
        TeamManager tm = plugin.getTeamManager();
        
        // 1. スコア比較
        if (blueScore > redScore) {
            broadcast(ChatColor.BLUE + "BLUE" + ChatColor.YELLOW + " がスコアで勝利！ (" + blueScore + " vs " + redScore + ")");
            return Team.BLUE;
        } else if (redScore > blueScore) {
            broadcast(ChatColor.RED + "RED" + ChatColor.YELLOW + " がスコアで勝利！ (" + redScore + " vs " + blueScore + ")");
            return Team.RED;
        }
        
        // 2. キング生存比較
        boolean blueKingAlive = isKingAlive(Team.BLUE);
        boolean redKingAlive = isKingAlive(Team.RED);
        
        if (blueKingAlive && !redKingAlive) {
            broadcast(ChatColor.BLUE + "BLUE" + ChatColor.YELLOW + " のキングが生存しているため勝利！");
            return Team.BLUE;
        } else if (redKingAlive && !blueKingAlive) {
            broadcast(ChatColor.RED + "RED" + ChatColor.YELLOW + " のキングが生存しているため勝利！");
            return Team.RED;
        }
        
        // 3. 生存者数比較
        int blueAlive = tm.getAliveCount(players, Team.BLUE);
        int redAlive = tm.getAliveCount(players, Team.RED);
        
        if (blueAlive > redAlive) {
            broadcast(ChatColor.BLUE + "BLUE" + ChatColor.YELLOW + " が生存者数で勝利！ (" + blueAlive + " vs " + redAlive + ")");
            return Team.BLUE;
        } else if (redAlive > blueAlive) {
            broadcast(ChatColor.RED + "RED" + ChatColor.YELLOW + " が生存者数で勝利！ (" + redAlive + " vs " + blueAlive + ")");
            return Team.RED;
        }
        
        // 4. 完全引き分け
        return null;
    }
    
    /**
     * 指定チームのキングが生存しているかチェック
     */
    private boolean isKingAlive(Team team) {
        KLPlayer king = plugin.getKingManager().getKing(team);
        return king != null && king.isAlive();
    }
    
    /**
     * 拠点帰還チェック（自動貯金）
     */
    private void checkBaseReturn() {
        if (currentArena == null) return;
        
        for (KLPlayer klp : getOnlinePlayers()) {
            if (!klp.isAlive()) continue;
            
            Player player = klp.getPlayer();
            if (player == null) continue;
            
            Location playerLoc = player.getLocation();
            Location spawn = currentArena.getSpawn(klp.getTeam());
            
            if (spawn != null && playerLoc.getWorld().equals(spawn.getWorld())) {
                double distance = playerLoc.distance(spawn);
                
                // スポーン地点から10ブロック以内で自動貯金
                if (distance <= 10) {
                    // Shard貯金
                    if (klp.getShardCarrying() > 0) {
                        plugin.getShardManager().onReturnToBase(klp);
                    }
                    
                    // Lumina貯金
                    if (klp.getLuminaCarrying() > 0) {
                        plugin.getLuminaManager().onReturnToBase(klp);
                    }
                }
            }
        }
    }
    
    /**
     * 勝利判定
     */
    private void checkWinCondition() {
        TeamManager tm = plugin.getTeamManager();
        
        // リスポーン無効かつ全滅
        if (!blueCanRespawn) {
            int aliveBlue = tm.getAliveCount(players, Team.BLUE);
            if (aliveBlue == 0) {
                endGame(Team.RED);
                return;
            }
        }
        
        if (!redCanRespawn) {
            int aliveRed = tm.getAliveCount(players, Team.RED);
            if (aliveRed == 0) {
                endGame(Team.BLUE);
                return;
            }
        }
    }
    
    /**
     * ゲーム終了
     */
    public void endGame(Team winner) {
        if (state == GameState.ENDING || state == GameState.WAITING) {
            return;
        }
        
        state = GameState.ENDING;
        
        if (gameLoopTask != null) {
            gameLoopTask.cancel();
            gameLoopTask = null;
        }
        
        // スコアボード停止
        plugin.getScoreboardManager().stop();
        
        // コア監視停止
        plugin.getCoreListener().stopMonitor();
        
        // Shardスポーン停止
        plugin.getShardManager().stopSpawnLoop();
        
        // キングオーラ停止
        plugin.getKingManager().stopAuraLoop();
        
        // エリア占領ループ停止
        plugin.getAreaManager().stopCaptureLoop();
        
        // 統計保存
        saveStats(winner);
        
        // 勝利演出フェーズを開始
        startCelebrationPhase(winner);
    }
    
    /**
     * 勝利演出フェーズを開始
     */
    private void startCelebrationPhase(Team winner) {
        int celebrationDuration = plugin.getConfigManager().getCelebrationDuration();
        
        // 勝利/敗北タイトルを表示
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                if (klp.getTeam() == winner) {
                    TitleUtil.sendTitle(player, 
                            ChatColor.GOLD + "" + ChatColor.BOLD + "🎉 勝利！ 🎉",
                            ChatColor.WHITE + "やったぜ！", 
                            10, celebrationDuration * 20, 20);
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                } else {
                    TitleUtil.sendTitle(player, 
                            ChatColor.RED + "" + ChatColor.BOLD + "敗北...",
                            ChatColor.WHITE + "また次がんばろう！", 
                            10, celebrationDuration * 20, 20);
                    player.playSound(player.getLocation(), Sound.WITHER_DEATH, 0.5f, 1.5f);
                }
            }
        }
        
        // 詳細な戦績をチャットに表示
        broadcastGameSummary(winner);
        
        // 花火演出（有効な場合）
        if (plugin.getConfigManager().isCelebrationFireworksEnabled()) {
            startFireworkShow(winner, celebrationDuration);
        }
        
        // 演出終了後にクリーンアップ
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskLater(plugin, celebrationDuration * 20L);
    }
    
    /**
     * 詳細な戦績をチャットに表示
     */
    private void broadcastGameSummary(Team winner) {
        // MVP算出
        KLPlayer mvp = findMVP();
        
        // キルランキング取得
        int topCount = plugin.getConfigManager().getCelebrationTopKillersCount();
        List<KLPlayer> topKillers = getTopKillers(topCount);
        
        // チーム合計キル数
        int blueKills = getTotalTeamKills(Team.BLUE);
        int redKills = getTotalTeamKills(Team.RED);
        
        // ヘッダー
        broadcast("");
        broadcast(ChatColor.GOLD + "=========================================");
        broadcast(ChatColor.WHITE + "  🎉 " + winner.getChatColor() + ChatColor.BOLD + 
                winner.getDisplayName() + " チームの勝利！ " + ChatColor.WHITE + "🎉");
        broadcast(ChatColor.GOLD + "=========================================");
        
        // スコア
        broadcast(ChatColor.YELLOW + "📊 スコア: " + 
                Team.BLUE.getChatColor() + "BLUE " + blueScore + "pt" + 
                ChatColor.WHITE + " vs " + 
                Team.RED.getChatColor() + "RED " + redScore + "pt");
        
        // MVP
        if (mvp != null) {
            broadcast(ChatColor.GOLD + "👑 MVP: " + 
                    mvp.getTeam().getChatColor() + mvp.getName() + 
                    ChatColor.WHITE + " (" + ChatColor.GREEN + mvp.getKillsThisGame() + "キル" + 
                    ChatColor.WHITE + "/" + ChatColor.RED + mvp.getDeathsThisGame() + "デス" + 
                    ChatColor.WHITE + ")");
        }
        
        // キルランキング（キルがあるプレイヤーがいる場合のみ）
        if (!topKillers.isEmpty()) {
            broadcast("");
            broadcast(ChatColor.AQUA + "🏆 キルランキング");
            for (int i = 0; i < topKillers.size(); i++) {
                KLPlayer klp = topKillers.get(i);
                String rankColor;
                String rankMark;
                switch (i) {
                    case 0: rankColor = ChatColor.GOLD.toString(); rankMark = "🥇"; break;
                    case 1: rankColor = ChatColor.GRAY.toString(); rankMark = "🥈"; break;
                    case 2: rankColor = ChatColor.RED.toString(); rankMark = "🥉"; break;
                    default: rankColor = ChatColor.WHITE.toString(); rankMark = "  ";
                }
                broadcast(ChatColor.WHITE + "  " + rankMark + " " + rankColor + 
                        klp.getTeam().getChatColor() + klp.getName() + 
                        ChatColor.GRAY + " - " + ChatColor.GREEN + klp.getKillsThisGame() + "キル");
            }
        }
        
        // フッター
        broadcast("");
        broadcast(ChatColor.GRAY + "チーム戦績: " + 
                Team.BLUE.getChatColor() + "BLUE " + blueKills + "キル" + 
                ChatColor.GRAY + " | " + 
                Team.RED.getChatColor() + "RED " + redKills + "キル");
        broadcast(ChatColor.GOLD + "=========================================");
        broadcast(ChatColor.GREEN + "お疲れ様でした！");
        broadcast("");
    }
    
    /**
     * MVP（最多キル）を算出
     */
    private KLPlayer findMVP() {
        KLPlayer mvp = null;
        int maxKills = 0;
        
        for (KLPlayer klp : players.values()) {
            if (klp.getKillsThisGame() > maxKills) {
                maxKills = klp.getKillsThisGame();
                mvp = klp;
            }
        }
        
        return mvp;
    }
    
    /**
     * キルランキング上位を取得
     */
    private List<KLPlayer> getTopKillers(int count) {
        List<KLPlayer> sorted = new ArrayList<>(players.values());
        sorted.sort((a, b) -> b.getKillsThisGame() - a.getKillsThisGame());
        
        // キル0のプレイヤーは除外
        sorted.removeIf(klp -> klp.getKillsThisGame() == 0);
        
        if (sorted.size() > count) {
            return sorted.subList(0, count);
        }
        return sorted;
    }
    
    /**
     * チームの合計キル数を取得
     */
    private int getTotalTeamKills(Team team) {
        int total = 0;
        for (KLPlayer klp : players.values()) {
            if (klp.getTeam() == team) {
                total += klp.getKillsThisGame();
            }
        }
        return total;
    }
    
    /**
     * 花火演出を開始
     */
    private void startFireworkShow(Team winner, int duration) {
        // 花火は最初の5秒だけ
        int fireworkDuration = Math.min(5, duration);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= fireworkDuration * 20) {
                    cancel();
                    return;
                }
                
                // 1秒ごとに花火を打ち上げ
                if (ticks % 20 == 0) {
                    for (KLPlayer klp : getOnlinePlayers()) {
                        if (klp.getTeam() == winner && klp.isOnline()) {
                            Player player = klp.getPlayer();
                            if (player != null) {
                                spawnFirework(player.getLocation().add(0, 1, 0), winner);
                            }
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * 花火を打ち上げ
     */
    private void spawnFirework(Location location, Team team) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        
        // チームカラーで花火を作成
        Color primary = team == Team.BLUE ? Color.BLUE : Color.RED;
        Color secondary = team == Team.BLUE ? Color.AQUA : Color.ORANGE;
        
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(primary)
                .withFade(secondary)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build();
        
        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }
    
    /**
     * 統計を保存
     */
    private void saveStats(Team winner) {
        for (KLPlayer klp : players.values()) {
            boolean won = klp.getTeam() == winner;
            plugin.getStatsDatabase().recordGame(
                    klp.getUuid(),
                    won,
                    klp.getKillsThisGame(),
                    klp.getDeathsThisGame()
            );
        }
    }
    
    /**
     * 強制終了（統計は保存する）
     */
    public void forceStop() {
        if (state == GameState.WAITING || state == GameState.LOBBY) {
            return;
        }
        
        broadcast(ChatColor.RED + "ゲームが強制終了されました。");
        cleanup();
    }
    
    /**
     * 強制終了（統計保存なし - /kl stop用）
     */
    public void forceStopWithoutStats() {
        if (state == GameState.WAITING || state == GameState.LOBBY) {
            return;
        }
        
        state = GameState.ENDING;
        
        if (gameLoopTask != null) {
            gameLoopTask.cancel();
            gameLoopTask = null;
        }
        
        broadcast(ChatColor.RED + "========================================");
        broadcast(ChatColor.RED + "" + ChatColor.BOLD + "  ゲームが強制終了されました");
        broadcast(ChatColor.GRAY + "  統計は記録されません");
        broadcast(ChatColor.RED + "========================================");
        
        // 統計を保存せずにクリーンアップ
        cleanupWithoutLoop();
    }
    
    /**
     * クリーンアップ（ループなし - 強制終了用）
     */
    private void cleanupWithoutLoop() {
        state = GameState.WAITING;
        autoLoopEnabled = false;
        
        // スコアボード停止
        plugin.getScoreboardManager().stop();
        
        // NPC削除
        plugin.getNPCManager().removeAllNPCs();
        
        // Shard停止
        plugin.getShardManager().stopSpawnLoop();
        
        // アップグレードリセット
        plugin.getUpgradeManager().reset();
        
        // プレイヤーをロビーへ
        Location lobby = currentArena != null ? currentArena.getLobby() : null;
        for (KLPlayer klp : players.values()) {
            Player player = klp.getPlayer();
            if (player != null) {
                resetPlayerState(player);
                if (lobby != null) {
                    player.teleport(lobby);
                }
            }
        }
        
        players.clear();
        reset();
    }
    
    /**
     * クリーンアップ（自動ループ対応）
     */
    private void cleanup() {
        // スコアボード停止
        plugin.getScoreboardManager().stop();
        
        // NPC削除
        plugin.getNPCManager().removeAllNPCs();
        
        // Shard停止
        plugin.getShardManager().stopSpawnLoop();
        
        // アップグレードリセット
        plugin.getUpgradeManager().reset();
        
        // プレイヤーをロビーへ
        Location lobby = currentArena != null ? currentArena.getLobby() : null;
        for (KLPlayer klp : players.values()) {
            Player player = klp.getPlayer();
            if (player != null) {
                resetPlayerState(player);
                if (lobby != null) {
                    player.teleport(lobby);
                }
            }
        }
        
        players.clear();
        reset();
        
        // 自動ループが有効なら、少し待ってからロビーカウントダウン再開
        if (autoLoopEnabled) {
            state = GameState.LOBBY;
            broadcast(ChatColor.YELLOW + "次のゲームの準備中...");
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (autoLoopEnabled) {
                        startLobbyCountdown();
                    }
                }
            }.runTaskLater(plugin, 100L); // 5秒後
        } else {
            state = GameState.WAITING;
        }
    }
    
    /**
     * プレイヤーの状態をリセット（共通処理）
     */
    private void resetPlayerState(Player player) {
        // インベントリと防具を完全にクリア
        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        
        // キングの体力を元に戻す
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);
        
        // ポーション効果をクリア
        player.getActivePotionEffects().forEach(effect -> 
                player.removePotionEffect(effect.getType()));
        
        // walkSpeedをデフォルトに戻す（Wind対策）
        player.setWalkSpeed(0.2f);
    }
    
    /**
     * 装備を付与（ゲーム開始時・リスポーン時共通）
     */
    public void giveGear(Player player, Team team) {
        player.getInventory().clear();
        
        // 皮装備（チームカラー）
        ItemStack helmet = createColoredArmor(Material.LEATHER_HELMET, team);
        ItemStack chestplate = createColoredArmor(Material.LEATHER_CHESTPLATE, team);
        ItemStack leggings = createColoredArmor(Material.LEATHER_LEGGINGS, team);
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, team);
        
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        // 木の剣（初期）
        player.getInventory().addItem(new ItemStack(Material.WOOD_SWORD));
        
        // 釣り竿
        player.getInventory().addItem(new ItemStack(Material.FISHING_ROD));
        
        // ダイヤピッケル
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_PICKAXE));
        
        // 金リンゴ x3
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
        
        // 食料
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
    }
    
    private ItemStack createColoredArmor(Material material, Team team) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team.getArmorColor());
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 全員にメッセージを送信
     */
    public void broadcast(String message) {
        String prefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "KingsLine" + ChatColor.GRAY + "] " + ChatColor.RESET;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(prefix + message);
        }
        plugin.getLogger().info(ChatColor.stripColor(message));
    }
    
    /**
     * 特定チームにメッセージを送信
     */
    public void broadcastToTeam(Team team, String message) {
        String prefix = ChatColor.GRAY + "[" + team.getChatColor() + team.getDisplayName() + ChatColor.GRAY + "] " + ChatColor.RESET;
        for (KLPlayer klp : players.values()) {
            if (klp.getTeam() == team && klp.isOnline()) {
                klp.getPlayer().sendMessage(prefix + message);
            }
        }
    }
    
    // ========== シャードスケーリング ==========
    
    /**
     * シャードスケール係数を取得
     * 少人数ゲームではシャード獲得量が増加する
     */
    public double getShardScaleMultiplier() {
        int playerCount = players.size();
        if (playerCount <= 0) {
            return 1.0;
        }
        
        ConfigManager config = plugin.getConfigManager();
        int basePlayers = config.getShardScaleBasePlayers();
        double minScale = config.getShardScaleMin();
        double maxScale = config.getShardScaleMax();
        
        double scale = (double) basePlayers / playerCount;
        
        // 上限・下限を適用
        return Math.max(minScale, Math.min(maxScale, scale));
    }
    
    /**
     * スケールを適用したシャード量を取得
     * 端数は確率で繰り上げ（例: 1.67 → 67%の確率で2、33%の確率で1）
     */
    public int getScaledShardAmount(int baseAmount) {
        double scaled = baseAmount * getShardScaleMultiplier();
        int base = (int) scaled;
        double fraction = scaled - base;
        
        // 端数を確率で繰り上げ
        if (Math.random() < fraction) {
            base++;
        }
        
        return Math.max(1, base);
    }
    
    /**
     * コア（黒曜石）を設置
     * ゲーム開始時に呼び出してバグを防止
     */
    private void placeCores() {
        if (currentArena == null) {
            return;
        }
        
        Location blueCore = currentArena.getBlueCore();
        Location redCore = currentArena.getRedCore();
        
        if (blueCore != null && blueCore.getWorld() != null) {
            blueCore.getBlock().setType(Material.OBSIDIAN);
            plugin.getLogger().info("[GameManager] Blueコアを設置: " + 
                    blueCore.getBlockX() + ", " + blueCore.getBlockY() + ", " + blueCore.getBlockZ());
        }
        
        if (redCore != null && redCore.getWorld() != null) {
            redCore.getBlock().setType(Material.OBSIDIAN);
            plugin.getLogger().info("[GameManager] Redコアを設置: " + 
                    redCore.getBlockX() + ", " + redCore.getBlockY() + ", " + redCore.getBlockZ());
        }
    }
}
