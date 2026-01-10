package tensaimc.kingsline.king;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.player.TeamManager;

import java.util.UUID;

/**
 * キング管理クラス
 */
public class KingManager {
    
    private final KingsLine plugin;
    
    private UUID blueKing;
    private UUID redKing;
    
    private BukkitTask auraTask;
    
    public KingManager(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * リセット
     */
    public void reset() {
        blueKing = null;
        redKing = null;
        
        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
    }
    
    /**
     * キングを設定
     */
    public void setKing(Team team, KLPlayer klPlayer) {
        if (klPlayer == null) {
            return;
        }
        
        switch (team) {
            case BLUE:
                blueKing = klPlayer.getUuid();
                break;
            case RED:
                redKing = klPlayer.getUuid();
                break;
            default:
                return;
        }
        
        klPlayer.setKing(true);
        
        // キングバフを適用
        applyKingBuffs(klPlayer);
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "あなたがキングに選ばれました！");
        }
        
        plugin.getGameManager().broadcast(team.getChatColor() + klPlayer.getName() + 
                ChatColor.GOLD + " が " + team.getColoredName() + " のキングになりました！");
    }
    
    /**
     * チームのキングを取得
     */
    public KLPlayer getKing(Team team) {
        UUID kingId = null;
        switch (team) {
            case BLUE:
                kingId = blueKing;
                break;
            case RED:
                kingId = redKing;
                break;
        }
        
        if (kingId == null) {
            return null;
        }
        
        return plugin.getGameManager().getPlayer(kingId);
    }
    
    /**
     * キングバフを適用
     */
    private void applyKingBuffs(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) {
            return;
        }
        
        // HP15ハート (30HP)
        player.setMaxHealth(30.0);
        player.setHealth(30.0);
        
        // キング専用装備: プロテクション1のダイヤチェストプレート
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = chestplate.getItemMeta();
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "王のチェストプレート");
        chestplate.setItemMeta(meta);
        player.getInventory().setChestplate(chestplate);
    }
    
    /**
     * キングオーラループを開始
     */
    public void startAuraLoop() {
        if (auraTask != null) {
            auraTask.cancel();
        }
        
        int radius = plugin.getConfigManager().getKingAuraRadius();
        
        auraTask = new BukkitRunnable() {
            @Override
            public void run() {
                applyKingAura(Team.BLUE, radius);
                applyKingAura(Team.RED, radius);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    /**
     * キングオーラを適用
     * キングの周囲8マスにいるチームメイトにRegeneration IIを付与
     */
    private void applyKingAura(Team team, int radius) {
        KLPlayer king = getKing(team);
        if (king == null || !king.isOnline() || !king.isAlive()) {
            return;
        }
        
        Player kingPlayer = king.getPlayer();
        if (kingPlayer == null) {
            return;
        }
        
        TeamManager tm = plugin.getTeamManager();
        
        // 半径8マス固定
        int auraRadius = 8;
        
        for (KLPlayer klPlayer : tm.getTeamPlayers(plugin.getGameManager().getPlayers(), team)) {
            if (!klPlayer.isOnline() || !klPlayer.isAlive()) {
                continue;
            }
            
            Player player = klPlayer.getPlayer();
            if (player == null) {
                continue;
            }
            
            // キング本人は除外（オーラは味方のみ）
            if (klPlayer.getUuid().equals(king.getUuid())) {
                continue;
            }
            
            // 同じワールドかチェック
            if (!player.getWorld().equals(kingPlayer.getWorld())) {
                continue;
            }
            
            double distance = player.getLocation().distance(kingPlayer.getLocation());
            
            if (distance <= auraRadius) {
                // Regeneration II (40 ticks = 2秒、ループで維持)
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.REGENERATION, 40, 1, false, false), true);
            }
        }
    }
    
    /**
     * キング死亡処理
     */
    public void onKingDeath(KLPlayer king, KLPlayer killer) {
        GameManager gm = plugin.getGameManager();
        Team kingTeam = king.getTeam();
        
        // スコア処理
        if (killer != null) {
            int kingKillPoints = plugin.getConfigManager().getScoreKingKill();
            gm.addScore(killer.getTeam(), kingKillPoints);
            
            Player killerPlayer = killer.getPlayer();
            if (killerPlayer != null) {
                killerPlayer.sendMessage(ChatColor.GOLD + "+" + kingKillPoints + "pt (キングキル)");
            }
        }
        
        // 敵全員にStrengthバフ
        Team enemyTeam = kingTeam.getOpposite();
        int buffDuration = plugin.getConfigManager().getKingDeathBuffDuration() * 20;
        
        TeamManager tm = plugin.getTeamManager();
        for (KLPlayer klPlayer : tm.getTeamPlayers(gm.getPlayers(), enemyTeam)) {
            Player player = klPlayer.getPlayer();
            if (player != null) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.INCREASE_DAMAGE, buffDuration, 0, false, false), true);
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + 
                        "敵のキングが倒された！15秒間攻撃力UP！");
            }
        }
        
        // Shardドロップ
        plugin.getShardManager().dropKingDeathShards(king.getPlayer().getLocation());
        
        // 通知
        gm.broadcast(ChatColor.RED + "" + ChatColor.BOLD + 
                kingTeam.getColoredName() + " のキングが倒されました！");
        
        // キングはゲーム中変わらない（死んでもキングのまま）
    }
    
    /**
     * 新しいキングを選出
     */
    private void selectNewKing(Team team) {
        TeamManager tm = plugin.getTeamManager();
        
        for (KLPlayer klPlayer : tm.getAliveTeamPlayers(plugin.getGameManager().getPlayers(), team)) {
            if (!klPlayer.isKing()) {
                setKing(team, klPlayer);
                return;
            }
        }
        
        // 生存者がいなければキングなし
        switch (team) {
            case BLUE:
                blueKing = null;
                break;
            case RED:
                redKing = null;
                break;
        }
    }
    
    /**
     * 最高スタッツのプレイヤーをキングに選出
     */
    public void selectKingByStats(Team team) {
        TeamManager tm = plugin.getTeamManager();
        
        KLPlayer bestPlayer = null;
        int bestKills = -1;
        
        for (KLPlayer klPlayer : tm.getTeamPlayers(plugin.getGameManager().getPlayers(), team)) {
            // TODO: データベースから累計キル数を取得
            // 今は仮にランダム
            if (bestPlayer == null) {
                bestPlayer = klPlayer;
            }
        }
        
        if (bestPlayer != null) {
            setKing(team, bestPlayer);
        }
    }
}
