package tensaimc.kingsline.config;

import org.bukkit.configuration.file.FileConfiguration;
import tensaimc.kingsline.KingsLine;

/**
 * config.yml の管理クラス
 */
public class ConfigManager {
    
    private final KingsLine plugin;
    private FileConfiguration config;
    
    // ゲーム設定
    private int minPlayers;
    private int startingPhaseDuration;
    private int respawnTime;
    private int pointsToWin;
    private int smallScaleThreshold;
    
    // スコア設定
    private int scoreKill;
    private int scoreKingKill;
    private int scoreKingAssist;
    private int scoreKingDeathPenalty;
    private int scoreCoreDestroy;
    private int scoreAreaCapture;
    private int areaTickInterval;
    
    // Shard設定
    private int shardSpawnInterval;
    private int shardSpawnAmount;
    private int shardCoreDestroyDrop;
    private int shardKingDeathDrop;
    private int shardScaleBasePlayers;
    private double shardScaleMin;
    private double shardScaleMax;
    
    // Lumina設定
    private int luminaPerKill;
    
    // キング設定
    private int kingCandidacyDuration;
    private int kingVoteDuration;
    private int kingAuraRadius;
    private int kingDeathBuffDuration;
    
    // エレメント設定
    private int elementSelectionDuration;
    private int spCooldown;
    private int spRequiredHits;
    
    public ConfigManager(KingsLine plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // ゲーム設定
        minPlayers = config.getInt("game.min-players", 6);
        startingPhaseDuration = config.getInt("game.starting-phase-duration", 45);
        respawnTime = config.getInt("game.respawn-time", 5);
        pointsToWin = config.getInt("game.points-to-win", 500);
        smallScaleThreshold = config.getInt("game.small-scale-threshold", 4);
        
        // スコア設定
        scoreKill = config.getInt("score.kill", 1);
        scoreKingKill = config.getInt("score.king-kill", 5);
        scoreKingAssist = config.getInt("score.king-assist", 0);
        scoreKingDeathPenalty = config.getInt("score.king-death-penalty", -50);
        scoreCoreDestroy = config.getInt("score.core-destroy", 100);
        scoreAreaCapture = config.getInt("score.area-capture", 2);
        areaTickInterval = config.getInt("score.area-tick-interval", 100); // 5秒ごと
        
        // Shard設定
        shardSpawnInterval = config.getInt("shard.spawn-interval", 10);
        shardSpawnAmount = config.getInt("shard.spawn-amount", 1);
        shardCoreDestroyDrop = config.getInt("shard.core-destroy-drop", 20);
        shardKingDeathDrop = config.getInt("shard.king-death-drop", 5);
        shardScaleBasePlayers = config.getInt("shard.scale-base-players", 10);
        shardScaleMin = config.getDouble("shard.scale-min", 1.0);
        shardScaleMax = config.getDouble("shard.scale-max", 3.0);
        
        // Lumina設定
        luminaPerKill = config.getInt("lumina.per-kill", 2);
        
        // キング設定
        kingCandidacyDuration = config.getInt("king.candidacy-duration", 15);
        kingVoteDuration = config.getInt("king.vote-duration", 15);
        kingAuraRadius = config.getInt("king.aura-radius", 8);
        kingDeathBuffDuration = config.getInt("king.death-buff-duration", 15);
        
        // エレメント設定
        elementSelectionDuration = config.getInt("element.selection-duration", 20);
        spCooldown = config.getInt("element.sp-cooldown", 15);
        spRequiredHits = config.getInt("element.sp-required-hits", 10);
    }
    
    public void reload() {
        loadConfig();
    }
    
    // ========== Getters ==========
    
    // ゲーム設定
    public int getMinPlayers() { return minPlayers; }
    public int getStartingPhaseDuration() { return startingPhaseDuration; }
    public int getRespawnTime() { return respawnTime; }
    public int getPointsToWin() { return pointsToWin; }
    public int getSmallScaleThreshold() { return smallScaleThreshold; }
    
    // スコア設定
    public int getScoreKill() { return scoreKill; }
    public int getScoreKingKill() { return scoreKingKill; }
    public int getScoreKingAssist() { return scoreKingAssist; }
    public int getScoreKingDeathPenalty() { return scoreKingDeathPenalty; }
    public int getScoreCoreDestroy() { return scoreCoreDestroy; }
    public int getScoreAreaCapture() { return scoreAreaCapture; }
    public int getAreaTickInterval() { return areaTickInterval; }
    
    // Shard設定
    public int getShardSpawnInterval() { return shardSpawnInterval; }
    public int getShardSpawnAmount() { return shardSpawnAmount; }
    public int getShardCoreDestroyDrop() { return shardCoreDestroyDrop; }
    public int getShardKingDeathDrop() { return shardKingDeathDrop; }
    public int getShardScaleBasePlayers() { return shardScaleBasePlayers; }
    public double getShardScaleMin() { return shardScaleMin; }
    public double getShardScaleMax() { return shardScaleMax; }
    
    // Lumina設定
    public int getLuminaPerKill() { return luminaPerKill; }
    
    // キング設定
    public int getKingCandidacyDuration() { return kingCandidacyDuration; }
    public int getKingVoteDuration() { return kingVoteDuration; }
    public int getKingAuraRadius() { return kingAuraRadius; }
    public int getKingDeathBuffDuration() { return kingDeathBuffDuration; }
    
    // エレメント設定
    public int getElementSelectionDuration() { return elementSelectionDuration; }
    public int getSpCooldown() { return spCooldown; }
    public int getSpCooldownTicks() { return spCooldown * 20; }
    public long getSpCooldownMillis() { return spCooldown * 1000L; }
    public int getSpRequiredHits() { return spRequiredHits; }
    
    // Raw config access
    public FileConfiguration getConfig() { return config; }
}
