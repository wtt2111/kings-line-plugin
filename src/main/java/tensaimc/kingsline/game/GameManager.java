package tensaimc.kingsline.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.config.ConfigManager;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.player.TeamManager;
import tensaimc.kingsline.util.ActionBarUtil;
import tensaimc.kingsline.util.TitleUtil;

import java.util.*;

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
        
        plugin.getTeamManager().reset();
        plugin.getCoreListener().reset();
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
        switch (team) {
            case BLUE:
                blueScore += amount;
                break;
            case RED:
                redScore += amount;
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
        votingPhase = true;
        int duration = plugin.getConfigManager().getStartingPhaseDuration();
        
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
        broadcast(ChatColor.YELLOW + "  ・キングに立候補するには !king とチャットで発言");
        broadcast(ChatColor.GREEN + "========================================");
        
        startingTask = new BukkitRunnable() {
            int countdown = duration;
            int phase = 0; // 0=エレメント選択, 1=キング投票
            
            @Override
            public void run() {
                if (countdown <= 0) {
                    if (phase == 0) {
                        // エレメント選択終了、キング投票開始
                        phase = 1;
                        countdown = 15; // 投票時間15秒
                        startKingVotingPhase();
                        return;
                    } else {
                        // キング投票終了、ゲーム開始
                        cancel();
                        finishVotingAndStart();
                        return;
                    }
                }
                
                // カウントダウン通知
                if (phase == 0) {
                    // エレメント選択フェーズ
                    if (countdown <= 10 || countdown == 20 || countdown == 30) {
                        for (KLPlayer klp : getOnlinePlayers()) {
                            Player player = klp.getPlayer();
                            if (player != null && !klp.hasSelectedElement()) {
                                ActionBarUtil.sendActionBar(player, 
                                        ChatColor.RED + "⚠ エレメントを選択してください！ あと " + countdown + " 秒");
                            }
                        }
                    }
                } else {
                    // キング投票フェーズ
                    if (countdown <= 5) {
                        broadcast(ChatColor.YELLOW + "キング投票終了まで " + countdown + " 秒...");
                    }
                }
                
                if (countdown <= 5 || countdown == 10) {
                    for (KLPlayer klp : getOnlinePlayers()) {
                        Player player = klp.getPlayer();
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                        }
                    }
                }
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * キング投票フェーズ開始
     */
    private void startKingVotingPhase() {
        // 全員にTitle通知
        for (KLPlayer klp : getOnlinePlayers()) {
            Player player = klp.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, 
                        ChatColor.GOLD + "" + ChatColor.BOLD + "👑 キング投票タイム 👑",
                        ChatColor.WHITE + "!king でキングに立候補！", 
                        10, 60, 20);
            }
        }
        
        broadcast(ChatColor.GOLD + "========================================");
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "  👑 キング投票フェーズ！");
        broadcast(ChatColor.YELLOW + "  キングに立候補するには !king とチャットで発言");
        broadcast(ChatColor.YELLOW + "  立候補者がいない場合はランダムで決定されます");
        broadcast(ChatColor.GOLD + "========================================");
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
     * キングを選出
     */
    private void selectKings() {
        TeamManager tm = plugin.getTeamManager();
        
        // Blue
        if (!kingCandidatesBlue.isEmpty()) {
            UUID candidateId = kingCandidatesBlue.iterator().next();
            KLPlayer king = getPlayer(candidateId);
            if (king != null) {
                plugin.getKingManager().setKing(Team.BLUE, king);
            }
        } else {
            // ランダム選出
            List<KLPlayer> bluePlayers = tm.getTeamPlayers(players, Team.BLUE);
            if (!bluePlayers.isEmpty()) {
                KLPlayer king = bluePlayers.get(new Random().nextInt(bluePlayers.size()));
                plugin.getKingManager().setKing(Team.BLUE, king);
            }
        }
        
        // Red
        if (!kingCandidatesRed.isEmpty()) {
            UUID candidateId = kingCandidatesRed.iterator().next();
            KLPlayer king = getPlayer(candidateId);
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
                giveStartingGear(player, klp.getTeam());
                
                // ゲームモードをサバイバルに
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
            }
            
            // リセット
            klp.resetForNewGame();
            klp.setAlive(true);
            klp.setCanRespawn(true);
        }
        
        // NPCをスポーン
        plugin.getNPCManager().spawnNPCs(currentArena);
        
        // スコアボード開始
        plugin.getScoreboardManager().start();
        
        // コア監視開始
        plugin.getCoreListener().startMonitor();
        
        // Shardスポーン開始
        plugin.getShardManager().startSpawnLoop();
        
        // キングオーラ開始
        plugin.getKingManager().startAuraLoop();
        
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
        ConfigManager config = plugin.getConfigManager();
        int areaTick = config.getAreaTickInterval();
        
        gameLoopTask = new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (state != GameState.RUNNING) {
                    cancel();
                    return;
                }
                
                // エリア占領判定 (設定された間隔で)
                if (tick % areaTick == 0) {
                    processAreaCapture();
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
     * Bエリア占領処理
     */
    private void processAreaCapture() {
        if (currentArena == null || currentArena.getAreaB() == null) {
            return;
        }
        
        if (!currentArena.getAreaB().isEnabled() || !currentArena.getAreaB().isValid()) {
            return;
        }
        
        int blueCount = currentArena.getAreaB().getTeamCount(players, Team.BLUE);
        int redCount = currentArena.getAreaB().getTeamCount(players, Team.RED);
        
        int points = plugin.getConfigManager().getScoreAreaCapture();
        
        if (blueCount > redCount) {
            addScore(Team.BLUE, points);
            notifyAreaCapture(Team.BLUE, points);
        } else if (redCount > blueCount) {
            addScore(Team.RED, points);
            notifyAreaCapture(Team.RED, points);
        }
    }
    
    /**
     * エリア占領通知
     */
    private void notifyAreaCapture(Team team, int points) {
        for (KLPlayer klp : getOnlinePlayers()) {
            if (klp.getTeam() == team && klp.isOnline()) {
                Player player = klp.getPlayer();
                if (player != null && currentArena.getAreaB().contains(player.getLocation())) {
                    ActionBarUtil.sendActionBar(player, 
                            ChatColor.GREEN + "Bエリア制圧中！ +" + points + "pt");
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
        
        // プレイヤーをロビーへ
        Location lobby = currentArena != null ? currentArena.getLobby() : null;
        for (KLPlayer klp : players.values()) {
            Player player = klp.getPlayer();
            if (player != null) {
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setGameMode(GameMode.SURVIVAL);
                
                // ポーション効果をクリア
                player.getActivePotionEffects().forEach(effect -> 
                        player.removePotionEffect(effect.getType()));
                
                if (lobby != null) {
                    player.teleport(lobby);
                }
            }
        }
        
        players.clear();
        reset();
    }
    
    /**
     * 初期装備を付与
     */
    private void giveStartingGear(Player player, Team team) {
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
}
