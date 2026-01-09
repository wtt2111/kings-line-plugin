package tensaimc.kingsline;

import org.bukkit.plugin.java.JavaPlugin;
import tensaimc.kingsline.arena.AreaManager;
import tensaimc.kingsline.command.KLCommand;
import tensaimc.kingsline.command.KLTabCompleter;
import tensaimc.kingsline.config.ArenaConfig;
import tensaimc.kingsline.config.ConfigManager;
import tensaimc.kingsline.database.StatsDatabase;
import tensaimc.kingsline.element.ElementManager;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.gui.ElementSelectGUI;
import tensaimc.kingsline.gui.GUIManager;
import tensaimc.kingsline.gui.ShopGUI;
import tensaimc.kingsline.gui.UpgradeGUI;
import tensaimc.kingsline.king.KingManager;
import tensaimc.kingsline.listener.*;
import tensaimc.kingsline.npc.NPCManager;
import tensaimc.kingsline.player.PartyManager;
import tensaimc.kingsline.player.TeamManager;
import tensaimc.kingsline.resource.LuminaManager;
import tensaimc.kingsline.resource.ShardManager;
import tensaimc.kingsline.score.ScoreManager;
import tensaimc.kingsline.scoreboard.ScoreboardManager;
import tensaimc.kingsline.upgrade.UpgradeManager;

/**
 * King's Line - 2チーム対戦型 PvP ミニゲーム
 */
public final class KingsLine extends JavaPlugin {
    
    private static KingsLine instance;
    
    // Config
    private ConfigManager configManager;
    private ArenaConfig arenaConfig;
    
    // Core Managers
    private GameManager gameManager;
    private TeamManager teamManager;
    private PartyManager partyManager;
    private ScoreManager scoreManager;
    private AreaManager areaManager;
    private ScoreboardManager scoreboardManager;
    
    // Resource Managers
    private ShardManager shardManager;
    private LuminaManager luminaManager;
    
    // Feature Managers
    private ElementManager elementManager;
    private UpgradeManager upgradeManager;
    private NPCManager npcManager;
    private GUIManager guiManager;
    private KingManager kingManager;
    
    // Database
    private StatsDatabase statsDatabase;
    
    // GUIs
    private ElementSelectGUI elementSelectGUI;
    private ShopGUI shopGUI;
    private UpgradeGUI upgradeGUI;
    
    // Listeners（参照を保持）
    private CoreListener coreListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 設定ファイル読み込み
        configManager = new ConfigManager(this);
        arenaConfig = new ArenaConfig(this);
        
        // マネージャー初期化
        partyManager = new PartyManager(this);
        teamManager = new TeamManager(this);
        scoreManager = new ScoreManager(this);
        areaManager = new AreaManager(this);
        shardManager = new ShardManager(this);
        luminaManager = new LuminaManager(this);
        elementManager = new ElementManager(this);
        upgradeManager = new UpgradeManager(this);
        npcManager = new NPCManager(this);
        guiManager = new GUIManager(this);
        kingManager = new KingManager(this);
        statsDatabase = new StatsDatabase(this);
        scoreboardManager = new ScoreboardManager(this);
        gameManager = new GameManager(this);
        
        // GUI初期化
        elementSelectGUI = new ElementSelectGUI(this);
        shopGUI = new ShopGUI(this);
        upgradeGUI = new UpgradeGUI(this);
        
        // コマンド登録
        getCommand("kl").setExecutor(new KLCommand(this));
        getCommand("kl").setTabCompleter(new KLTabCompleter(this));
        
        // リスナー登録
        coreListener = new CoreListener(this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(coreListener, this);
        
        getLogger().info("King's Line が有効になりました！");
    }
    
    @Override
    public void onDisable() {
        // スコアボード停止
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }
        
        // ゲーム中なら強制終了
        if (gameManager != null) {
            gameManager.forceStop();
        }
        
        // NPC削除
        if (npcManager != null) {
            npcManager.removeAllNPCs();
        }
        
        // データベース閉じる
        if (statsDatabase != null) {
            statsDatabase.close();
        }
        
        // 設定保存
        if (arenaConfig != null) {
            arenaConfig.saveConfig();
        }
        
        getLogger().info("King's Line が無効になりました。");
    }
    
    // ========== Static Instance ==========
    
    public static KingsLine getInstance() {
        return instance;
    }
    
    // ========== Config Getters ==========
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ArenaConfig getArenaConfig() {
        return arenaConfig;
    }
    
    // ========== Core Manager Getters ==========
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public TeamManager getTeamManager() {
        return teamManager;
    }
    
    public PartyManager getPartyManager() {
        return partyManager;
    }
    
    public ScoreManager getScoreManager() {
        return scoreManager;
    }
    
    public AreaManager getAreaManager() {
        return areaManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    // ========== Resource Manager Getters ==========
    
    public ShardManager getShardManager() {
        return shardManager;
    }
    
    public LuminaManager getLuminaManager() {
        return luminaManager;
    }
    
    // ========== Feature Manager Getters ==========
    
    public ElementManager getElementManager() {
        return elementManager;
    }
    
    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }
    
    public NPCManager getNPCManager() {
        return npcManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public KingManager getKingManager() {
        return kingManager;
    }
    
    // ========== Database ==========
    
    public StatsDatabase getStatsDatabase() {
        return statsDatabase;
    }
    
    // ========== GUI Getters ==========
    
    public ElementSelectGUI getElementSelectGUI() {
        return elementSelectGUI;
    }
    
    public ShopGUI getShopGUI() {
        return shopGUI;
    }
    
    public UpgradeGUI getUpgradeGUI() {
        return upgradeGUI;
    }
    
    // ========== Listener Getters ==========
    
    public CoreListener getCoreListener() {
        return coreListener;
    }
}
