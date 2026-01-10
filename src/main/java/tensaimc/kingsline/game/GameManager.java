package tensaimc.kingsline.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    
    // ゲームデータ
    private int blueScore;
    private int redScore;
    private boolean blueCanRespawn;
    private boolean redCanRespawn;
    
    // キング投票関連
    private final Set<UUID> kingCandidatesBlue;
    private final Set<UUID> kingCandidatesRed;
    private boolean votingPhase;
    
    // エレメント選択アイテム
    public static final Material ELEMENT_SELECT_MATERIAL = Material.NETHER_STAR;
    public static final String ELEMENT_SELECT_NAME = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "エレメント選択";
    
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
        
        if (startingTask != null) {
            startingTask.cancel();
            startingTask = null;
        }
        if (gameLoopTask != null) {
            gameLoopTask.cancel();
            gameLoopTask = null;
        }
        
        if (plugin.getTeamManager() != null) {
            plugin.getTeamManager().reset();
        }
        if (plugin.getCoreListener() != null) {
            plugin.getCoreListener().reset();
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
        }
    }
    
    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }
    
    public int getPlayerCount() {
        return players.size();
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
    
    // ========== Game Flow ==========
    
    /**
     * ゲーム開始
     */
    public boolean startGame() {
        if (state != GameState.WAITING) {
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
            int countdown = 30; // エレメント選択フェーズ30秒
            int phase = 0; // 0=エレメント選択, 1=キング投票
            
            @Override
            public void run() {
                if (countdown <= 0) {
                    if (phase == 0) {
                        // エレメント選択終了、キング投票開始
                        phase = 1;
                        countdown = 30; // キング投票30秒
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
                    if (countdown == 10 || countdown == 5 || countdown == 3) {
                        broadcast(ChatColor.YELLOW + "エレメント選択終了まで " + countdown + " 秒...");
                    }
                } else {
                    // キング投票フェーズ - チャット通知
                    if (countdown == 10 || countdown == 5 || countdown == 3) {
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
                
                // 拠点帰還チェック（毎秒）
                if (tick % 20 == 0) {
                    checkBaseReturn();
                }
                
                // 勝利判定
                checkWinCondition();
                
                tick++;
            }
        }.runTaskTimer(plugin, 20L, 1L);
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
        
        // 勝利Title
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                String title;
                String subtitle;
                
                if (klp.getTeam() == winner) {
                    title = ChatColor.GOLD + "" + ChatColor.BOLD + "🎉 勝利！ 🎉";
                    subtitle = ChatColor.WHITE + "おめでとうございます！";
                    player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                } else {
                    title = ChatColor.RED + "" + ChatColor.BOLD + "敗北...";
                    subtitle = ChatColor.WHITE + "また次回頑張りましょう";
                    player.playSound(player.getLocation(), Sound.WITHER_DEATH, 0.5f, 1.5f);
                }
                
                TitleUtil.sendTitle(player, title, subtitle, 10, 100, 20);
            }
        }
        
        // 結果発表
        broadcast("");
        broadcast(ChatColor.GOLD + "=====================================");
        broadcast(ChatColor.GOLD + "        " + winner.getChatColor() + ChatColor.BOLD + 
                winner.getDisplayName() + " TEAM WINS!");
        broadcast(ChatColor.GOLD + "=====================================");
        broadcast(ChatColor.WHITE + "  Final Score: " + 
                Team.BLUE.getChatColor() + "BLUE " + blueScore + 
                ChatColor.WHITE + " - " + 
                Team.RED.getChatColor() + redScore + " RED");
        broadcast(ChatColor.GOLD + "=====================================");
        broadcast("");
        
        // 統計保存
        saveStats(winner);
        
        // クリーンアップ
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskLater(plugin, 100L); // 5秒後
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
     * 強制終了
     */
    public void forceStop() {
        if (state == GameState.WAITING) {
            return;
        }
        
        broadcast(ChatColor.RED + "ゲームが強制終了されました。");
        cleanup();
    }
    
    /**
     * クリーンアップ
     */
    private void cleanup() {
        state = GameState.WAITING;
        
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
                
                if (lobby != null) {
                    player.teleport(lobby);
                }
            }
        }
        
        players.clear();
        reset();
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
