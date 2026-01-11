package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.item.SpecialItems;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.util.TitleUtil;

/**
 * プレイヤー関連のイベントリスナー
 */
public class PlayerListener implements Listener {
    
    private final KingsLine plugin;
    
    public PlayerListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        
        // ロビー待機中の場合、自動的に参加
        if (gm.getState() == GameState.LOBBY) {
            gm.onPlayerJoinLobby(player);
        }
        // ゲーム中に参加した場合
        else if (gm.getState() == GameState.RUNNING || gm.getState() == GameState.STARTING) {
            player.sendMessage(ChatColor.YELLOW + "ゲームが進行中です。観戦モードになります。");
            player.setGameMode(GameMode.SPECTATOR);
        }
    }
    
    /**
     * スペクテイター（途中参加者）の移動制限
     * 座標0,0から500ブロック以上離れたら0,0にテレポート
     */
    @EventHandler
    public void onSpectatorMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // スペクテイターモードでない場合はスキップ
        if (player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中でない場合はスキップ
        if (!gm.isState(GameState.RUNNING, GameState.STARTING)) {
            return;
        }
        
        // ゲーム参加者（死亡中のリスポーン待ち）はスキップ
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer != null) {
            return;
        }
        
        // 座標0,0からの水平距離をチェック
        Location loc = player.getLocation();
        double distanceFromOrigin = Math.sqrt(loc.getX() * loc.getX() + loc.getZ() * loc.getZ());
        
        if (distanceFromOrigin > 500) {
            // 0,0にテレポート（Y座標は現在の高さを維持）
            Location teleportLoc = new Location(loc.getWorld(), 0, loc.getY(), 0, loc.getYaw(), loc.getPitch());
            player.teleport(teleportLoc);
            player.sendMessage(ChatColor.YELLOW + "観戦エリアの外に出たため、中心に戻されました。");
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        
        // ロビー待機中の場合
        if (gm.getState() == GameState.LOBBY) {
            gm.onPlayerLeaveLobby(player);
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer != null) {
            // ゲーム中に抜けた場合
            if (gm.isState(GameState.RUNNING, GameState.STARTING)) {
                klPlayer.setAlive(false);
                
                // 所持Shard/Luminaは消失（キラーがいないため）
                plugin.getShardManager().transferShardsOnDeath(klPlayer, null);
                plugin.getLuminaManager().dropPlayerLumina(klPlayer, player.getLocation());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameManager gm = plugin.getGameManager();
        
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        // リバイバルチャーム: 死亡を免れる
        if (SpecialItems.RevivalCharm.tryRevive(plugin, player, klPlayer)) {
            // インベントリはSpecialItemsで保存済み、ドロップをクリア
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setDeathMessage(null);
            
            // 死亡位置を保存（リスポーン時にテレポートするため）
            klPlayer.setLastDeathLocation(player.getLocation());
            
            return; // 通常の死亡処理をスキップ
        }
        
        klPlayer.setAlive(false);
        klPlayer.addDeath();
        klPlayer.setLastDeathLocation(player.getLocation());
        
        // キラー処理
        Player killer = player.getKiller();
        if (killer != null) {
            KLPlayer klKiller = gm.getPlayer(killer);
            if (klKiller != null && klKiller.getTeam() != klPlayer.getTeam()) {
                klKiller.addKill();
                
                // ポイント加算
                int points = plugin.getConfigManager().getScoreKill();
                gm.addScore(klKiller.getTeam(), points);
                
                // Lumina加算
                plugin.getLuminaManager().awardKillLumina(klKiller, points);
                
                // キング死亡チェック
                if (klPlayer.isKing()) {
                    plugin.getKingManager().onKingDeath(klPlayer, klKiller);
                }
            }
        }
        
        // 所持Shard/Luminaをキラーに移動
        KLPlayer klKillerForShard = (killer != null) ? gm.getPlayer(killer) : null;
        plugin.getShardManager().transferShardsOnDeath(klPlayer, klKillerForShard);
        plugin.getLuminaManager().dropPlayerLumina(klPlayer, player.getLocation());
        
        // アイテムドロップ禁止（死んだ人はアイテムを失うが、地面にはドロップしない）
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // デスメッセージをカスタマイズ
        String deathMessage = klPlayer.getTeam().getChatColor() + player.getName();
        if (killer != null) {
            KLPlayer klKiller = gm.getPlayer(killer);
            if (klKiller != null) {
                deathMessage += ChatColor.GRAY + " was killed by " + 
                        klKiller.getTeam().getChatColor() + killer.getName();
            }
        }
        event.setDeathMessage(deathMessage);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        
        if (!gm.isState(GameState.RUNNING)) {
            return;
        }
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        // リバイバルチャームで復活した場合
        if (SpecialItems.RevivalCharm.isReviving(player.getUniqueId())) {
            // 死亡位置でその場復活
            Location deathLoc = klPlayer.getLastDeathLocation();
            if (deathLoc != null) {
                event.setRespawnLocation(deathLoc);
            }
            
            // 遅延でHP40%復活とインベントリ復元
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // インベントリ復元
                        SpecialItems.RevivalCharm.restoreInventory(player);
                        
                        // HP40%で復活
                        player.setHealth(player.getMaxHealth() * 0.4);
                        player.setFoodLevel(20);
                        
                        // 復活タイトル
                        TitleUtil.sendTitle(player, 
                                ChatColor.GOLD + "✟ REVIVED!",
                                ChatColor.YELLOW + "リバイバルチャームで死を免れた",
                                5, 30, 10);
                    }
                }
            }.runTaskLater(plugin, 1L);
            
            return; // 通常のリスポーン処理をスキップ
        }
        
        // リスポーン可能かチェック
        if (!klPlayer.canRespawn()) {
            player.sendMessage(ChatColor.RED + "リスポーンが無効化されています。観戦モードになります。");
            
            // 遅延でスペクテイターモードに
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setGameMode(GameMode.SPECTATOR);
                    }
                }
            }.runTaskLater(plugin, 1L);
            return;
        }
        
        // リスポーン待機時間を計算（コア周辺10ブロック以内なら15秒、それ以外は5秒）
        int respawnDelay = calculateRespawnDelay(klPlayer, gm);
        
        // リスポーン地点をチームスポーンに
        Location respawnLoc = null;
        if (gm.getCurrentArena() != null) {
            respawnLoc = gm.getCurrentArena().getSpawn(klPlayer.getTeam());
            if (respawnLoc != null) {
                event.setRespawnLocation(respawnLoc);
            }
        }
        
        final Location finalRespawnLoc = respawnLoc;
        
        // スペクテイターモードにして待機
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    
                    // カウントダウン開始
                    startRespawnCountdown(player, klPlayer, respawnDelay, finalRespawnLoc);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * リスポーン待機時間を計算
     */
    private int calculateRespawnDelay(KLPlayer klPlayer, GameManager gm) {
        Arena arena = gm.getCurrentArena();
        if (arena == null) {
            return 5; // デフォルト5秒
        }
        
        // 死亡場所の記録（直前のLocation）
        Location deathLoc = klPlayer.getLastDeathLocation();
        if (deathLoc == null) {
            return 5;
        }
        
        // 自チームのコア位置を取得
        Location coreLoc = (klPlayer.getTeam() == Team.BLUE) ? 
                arena.getBlueCore() : arena.getRedCore();
        
        if (coreLoc != null && deathLoc.getWorld().equals(coreLoc.getWorld())) {
            double distance = deathLoc.distance(coreLoc);
            if (distance <= 10) {
                return 15; // コア周辺10ブロック以内なら15秒
            }
        }
        
        return 5; // 通常は5秒
    }
    
    /**
     * リスポーンカウントダウン
     */
    private void startRespawnCountdown(Player player, KLPlayer klPlayer, int delay, Location respawnLoc) {
        new BukkitRunnable() {
            int countdown = delay;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                GameManager gm = plugin.getGameManager();
                if (!gm.isState(GameState.RUNNING)) {
                    cancel();
                    return;
                }
                
                if (countdown <= 0) {
                    // リスポーン実行
                    executeRespawn(player, klPlayer, respawnLoc);
                    cancel();
                    return;
                }
                
                // カウントダウン表示
                TitleUtil.sendTitle(player, 
                        ChatColor.RED + "復活まで " + countdown + " 秒",
                        delay >= 15 ? ChatColor.YELLOW + "（コア防衛エリア内で死亡）" : "",
                        0, 25, 0);
                
                if (countdown <= 3) {
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
                }
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * リスポーン実行
     */
    private void executeRespawn(Player player, KLPlayer klPlayer, Location respawnLoc) {
        if (!player.isOnline()) return;
        
        // サバイバルモードに
        player.setGameMode(GameMode.SURVIVAL);
        
        // テレポート
        if (respawnLoc != null) {
            player.teleport(respawnLoc);
        }
        
        // 状態回復
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        
        klPlayer.setAlive(true);
        
        // 装備を再付与（GameManagerの共通メソッドを使用）
        plugin.getGameManager().giveGear(player, klPlayer.getTeam());
        
        // アップグレード効果を再適用
        plugin.getUpgradeManager().applyUpgradeToPlayer(klPlayer);
        
        // エレメントのパッシブ効果を再適用
        plugin.getElementManager().applyPassiveEffects(klPlayer);
        
        // 通知
        TitleUtil.sendTitle(player, 
                ChatColor.GREEN + "復活！",
                "",
                5, 20, 5);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        String message = event.getMessage();
        GameManager gm = plugin.getGameManager();
        
        // !king コマンド
        if (message.equalsIgnoreCase("!king")) {
            event.setCancelled(true);
            
            if (!gm.isState(GameState.STARTING)) {
                player.sendMessage(ChatColor.RED + "準備フェーズ中のみ使用できます。");
                return;
            }
            
            KLPlayer klPlayer = gm.getPlayer(player);
            if (klPlayer == null) {
                player.sendMessage(ChatColor.RED + "ゲームに参加していません。");
                return;
            }
            
            // キング投票フェーズ中のみ立候補可能
            if (gm.isVotingPhase()) {
                // 立候補
                gm.addKingCandidate(klPlayer);
                // 立候補後、投票GUIに登録
                plugin.getKingVoteGUI().addCandidate(klPlayer.getUuid(), klPlayer.getTeam());
                player.sendMessage(ChatColor.GOLD + "キングに立候補しました！");
                player.sendMessage(ChatColor.GRAY + "ジュークボックスを右クリックで投票GUIを開けます。");
            } else {
                // エレメント選択フェーズ中は使用不可
                player.sendMessage(ChatColor.RED + "キング投票フェーズになったら !king で立候補できます。");
            }
            return;
        }
        
        // チャットシステムで処理
        if (plugin.getChatManager().handleChat(player, message)) {
            event.setCancelled(true);
        }
    }
}
