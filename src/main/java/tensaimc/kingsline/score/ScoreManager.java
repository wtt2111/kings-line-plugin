package tensaimc.kingsline.score;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;

/**
 * スコア管理クラス
 */
public class ScoreManager {
    
    private final KingsLine plugin;
    
    public ScoreManager(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * キルポイントを加算
     */
    public void addKillPoints(KLPlayer killer, KLPlayer victim) {
        GameManager gm = plugin.getGameManager();
        int points = plugin.getConfigManager().getScoreKill();
        
        // 通常キル
        gm.addScore(killer.getTeam(), points);
        
        // Lumina加算
        int lumina = plugin.getConfigManager().getLuminaPerKill();
        killer.addLumina(lumina);
        
        // 通知
        Player killerPlayer = killer.getPlayer();
        if (killerPlayer != null) {
            killerPlayer.sendMessage(ChatColor.GREEN + "+" + points + "pt " + 
                    ChatColor.YELLOW + "+" + lumina + " Lumina");
        }
    }
    
    /**
     * キングキルポイントを加算
     */
    public void addKingKillPoints(KLPlayer killer, KLPlayer kingVictim) {
        GameManager gm = plugin.getGameManager();
        
        // キングキルポイント
        int kingKillPoints = plugin.getConfigManager().getScoreKingKill();
        gm.addScore(killer.getTeam(), kingKillPoints);
        
        // キング死亡ペナルティ
        int penalty = plugin.getConfigManager().getScoreKingDeathPenalty();
        gm.addScore(kingVictim.getTeam(), penalty);
        
        // 通知
        gm.broadcast(ChatColor.RED + "" + ChatColor.BOLD + 
                kingVictim.getTeam().getColoredName() + "のキングが倒されました！");
        
        Player killerPlayer = killer.getPlayer();
        if (killerPlayer != null) {
            killerPlayer.sendMessage(ChatColor.GOLD + "+" + kingKillPoints + "pt (キングキル)");
        }
    }
    
    /**
     * エリア占領ポイントを加算
     */
    public void addAreaCapturePoints(Team team) {
        GameManager gm = plugin.getGameManager();
        int points = plugin.getConfigManager().getScoreAreaCapture();
        gm.addScore(team, points);
    }
    
    /**
     * コア破壊ポイントを加算
     */
    public void addCoreDestroyPoints(Team destroyerTeam) {
        GameManager gm = plugin.getGameManager();
        int points = plugin.getConfigManager().getScoreCoreDestroy();
        gm.addScore(destroyerTeam, points);
        
        gm.broadcast(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + 
                destroyerTeam.getColoredName() + "チームが敵のコアを破壊しました！ +" + points + "pt");
    }
    
    /**
     * スコアボードを更新（将来的にサイドバー表示用）
     */
    public void updateScoreboard() {
        // TODO: サイドバースコアボード実装
    }
}
