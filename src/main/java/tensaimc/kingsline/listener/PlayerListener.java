package tensaimc.kingsline.listener;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.util.ActionBarUtil;

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
        
        // ゲーム中に参加した場合
        if (gm.getState() == GameState.RUNNING) {
            player.sendMessage(ChatColor.YELLOW + "ゲームが進行中です。観戦モードになります。");
            player.setGameMode(GameMode.SPECTATOR);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        
        KLPlayer klPlayer = gm.getPlayer(player);
        if (klPlayer != null) {
            // ゲーム中に抜けた場合
            if (gm.isState(GameState.RUNNING, GameState.STARTING)) {
                klPlayer.setAlive(false);
                
                // 所持Shard/Luminaドロップ
                plugin.getShardManager().dropPlayerShards(klPlayer, player.getLocation());
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
        
        klPlayer.setAlive(false);
        klPlayer.addDeath();
        
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
        
        // 所持Shard/Luminaドロップ
        Location deathLoc = player.getLocation();
        plugin.getShardManager().dropPlayerShards(klPlayer, deathLoc);
        plugin.getLuminaManager().dropPlayerLumina(klPlayer, deathLoc);
        
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
        
        // リスポーン地点をチームスポーンに
        if (gm.getCurrentArena() != null) {
            Location spawn = gm.getCurrentArena().getSpawn(klPlayer.getTeam());
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
        }
        
        // 遅延でリスポーン処理
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    klPlayer.setAlive(true);
                    
                    // 装備を再付与
                    giveRespawnGear(player, klPlayer.getTeam());
                    
                    // アップグレード効果を再適用
                    plugin.getUpgradeManager().applyUpgradeToPlayer(klPlayer);
                }
            }
        }.runTaskLater(plugin, 1L);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        GameManager gm = plugin.getGameManager();
        
        // !king コマンド
        if (message.equalsIgnoreCase("!king")) {
            event.setCancelled(true);
            
            if (!gm.isVotingPhase()) {
                player.sendMessage(ChatColor.RED + "投票フェーズ中のみ使用できます。");
                return;
            }
            
            KLPlayer klPlayer = gm.getPlayer(player);
            if (klPlayer == null) {
                player.sendMessage(ChatColor.RED + "ゲームに参加していません。");
                return;
            }
            
            // 立候補
            gm.addKingCandidate(klPlayer);
            player.sendMessage(ChatColor.GOLD + "キングに立候補しました！");
        }
    }
    
    /**
     * リスポーン時の装備付与
     */
    private void giveRespawnGear(Player player, Team team) {
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
}
