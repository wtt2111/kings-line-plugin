package tensaimc.kingsline.database;

import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.element.Element;

import java.io.File;
import java.sql.*;
import java.util.UUID;

/**
 * SQLiteでプレイヤー統計を管理
 */
public class StatsDatabase {
    
    private final KingsLine plugin;
    private Connection connection;
    private boolean enabled = false;
    
    // 現在のスキーマバージョン
    private static final int SCHEMA_VERSION = 3;
    
    public StatsDatabase(KingsLine plugin) {
        this.plugin = plugin;
        connect();
        if (connection != null) {
            createTables();
            migrateSchema();
            enabled = true;
        }
    }
    
    /**
     * データベースに接続
     */
    private void connect() {
        try {
            // JDBCドライバーを明示的にロード
            Class.forName("org.sqlite.JDBC");
            
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            File dbFile = new File(dataFolder, "stats.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("SQLiteデータベースに接続しました");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("SQLiteドライバーが見つかりません。統計機能は無効です。");
        } catch (SQLException e) {
            plugin.getLogger().warning("データベース接続に失敗: " + e.getMessage());
            plugin.getLogger().warning("統計機能は無効です。");
        }
    }
    
    /**
     * テーブルを作成
     */
    private void createTables() {
        if (connection == null) {
            return;
        }
        
        // player_stats テーブル（全統計を含む）
        String sql = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT," +
                // 基本統計
                "wins INTEGER DEFAULT 0," +
                "losses INTEGER DEFAULT 0," +
                "kills INTEGER DEFAULT 0," +
                "deaths INTEGER DEFAULT 0," +
                "games_played INTEGER DEFAULT 0," +
                // エレメント別選択回数
                "fire_picks INTEGER DEFAULT 0," +
                "ice_picks INTEGER DEFAULT 0," +
                "wind_picks INTEGER DEFAULT 0," +
                "earth_picks INTEGER DEFAULT 0," +
                // 戦闘統計
                "hits INTEGER DEFAULT 0," +
                "total_damage INTEGER DEFAULT 0," +
                "fishing_rod_uses INTEGER DEFAULT 0," +
                "skill_uses INTEGER DEFAULT 0," +
                // キング統計
                "king_times INTEGER DEFAULT 0," +
                // リソース統計
                "total_lumina INTEGER DEFAULT 0," +
                "total_shard INTEGER DEFAULT 0," +
                "shard_deposited INTEGER DEFAULT 0," +
                // コア統計
                "cores_destroyed INTEGER DEFAULT 0" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("テーブル作成に失敗: " + e.getMessage());
        }
        
        // スキーマバージョン管理テーブル
        String versionSql = "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(versionSql);
        } catch (SQLException e) {
            plugin.getLogger().severe("バージョンテーブル作成に失敗: " + e.getMessage());
        }
    }
    
    /**
     * スキーママイグレーション
     */
    private void migrateSchema() {
        if (connection == null) return;
        
        int currentVersion = getSchemaVersion();
        
        if (currentVersion < 2) {
            // v1 -> v2: 新カラムを追加
            String[] newColumns = {
                "losses INTEGER DEFAULT 0",
                "fire_picks INTEGER DEFAULT 0",
                "ice_picks INTEGER DEFAULT 0",
                "wind_picks INTEGER DEFAULT 0",
                "earth_picks INTEGER DEFAULT 0",
                "hits INTEGER DEFAULT 0",
                "total_damage INTEGER DEFAULT 0",
                "fishing_rod_uses INTEGER DEFAULT 0",
                "skill_uses INTEGER DEFAULT 0",
                "king_times INTEGER DEFAULT 0",
                "total_lumina INTEGER DEFAULT 0",
                "total_shard INTEGER DEFAULT 0",
                "shard_deposited INTEGER DEFAULT 0"
            };
            
            for (String columnDef : newColumns) {
                String columnName = columnDef.split(" ")[0];
                if (!columnExists("player_stats", columnName)) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("ALTER TABLE player_stats ADD COLUMN " + columnDef);
                        plugin.getLogger().info("カラム追加: " + columnName);
                    } catch (SQLException e) {
                        // カラムが既に存在する場合は無視
                    }
                }
            }
            
            setSchemaVersion(2);
            plugin.getLogger().info("データベーススキーマをv2にマイグレーションしました");
        }
        
        if (currentVersion < 3) {
            // v2 -> v3: コア破壊カラムを追加
            String[] newColumns = {
                "cores_destroyed INTEGER DEFAULT 0"
            };
            
            for (String columnDef : newColumns) {
                String columnName = columnDef.split(" ")[0];
                if (!columnExists("player_stats", columnName)) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("ALTER TABLE player_stats ADD COLUMN " + columnDef);
                        plugin.getLogger().info("カラム追加: " + columnName);
                    } catch (SQLException e) {
                        // カラムが既に存在する場合は無視
                    }
                }
            }
            
            setSchemaVersion(3);
            plugin.getLogger().info("データベーススキーマをv3にマイグレーションしました");
        }
    }
    
    /**
     * カラムが存在するかチェック
     */
    private boolean columnExists(String table, String column) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // 無視
        }
        return false;
    }
    
    /**
     * 現在のスキーマバージョンを取得
     */
    private int getSchemaVersion() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version LIMIT 1")) {
            if (rs.next()) {
                return rs.getInt("version");
            }
        } catch (SQLException e) {
            // 無視
        }
        return 1; // デフォルトはv1
    }
    
    /**
     * スキーマバージョンを設定
     */
    private void setSchemaVersion(int version) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM schema_version");
            stmt.execute("INSERT INTO schema_version (version) VALUES (" + version + ")");
        } catch (SQLException e) {
            plugin.getLogger().warning("バージョン設定に失敗: " + e.getMessage());
        }
    }
    
    /**
     * データベースが有効かどうか
     */
    public boolean isEnabled() {
        return enabled && connection != null;
    }
    
    /**
     * プレイヤーが存在するか確認、なければ作成
     */
    public void ensurePlayer(UUID uuid, String name) {
        if (!isEnabled()) return;
        
        String sql = "INSERT OR IGNORE INTO player_stats (uuid, name) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("プレイヤー作成に失敗: " + e.getMessage());
        }
        
        // 名前を更新
        String updateSql = "UPDATE player_stats SET name = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("名前更新に失敗: " + e.getMessage());
        }
    }
    
    // ========== 基本統計 ==========
    
    /**
     * 勝利を追加
     */
    public void addWin(UUID uuid) {
        addStat(uuid, "wins", 1);
    }
    
    /**
     * 敗北を追加
     */
    public void addLoss(UUID uuid) {
        addStat(uuid, "losses", 1);
    }
    
    /**
     * キル数を追加
     */
    public void addKills(UUID uuid, int amount) {
        addStat(uuid, "kills", amount);
    }
    
    /**
     * デス数を追加
     */
    public void addDeaths(UUID uuid, int amount) {
        addStat(uuid, "deaths", amount);
    }
    
    /**
     * ゲーム参加を追加
     */
    public void addGamePlayed(UUID uuid) {
        addStat(uuid, "games_played", 1);
    }
    
    // ========== エレメント統計 ==========
    
    /**
     * エレメント選択を記録
     */
    public void addElementPick(UUID uuid, Element element) {
        if (element == null) return;
        
        String column;
        switch (element) {
            case FIRE:
                column = "fire_picks";
                break;
            case ICE:
                column = "ice_picks";
                break;
            case WIND:
                column = "wind_picks";
                break;
            case EARTH:
                column = "earth_picks";
                break;
            default:
                return;
        }
        addStat(uuid, column, 1);
    }
    
    // ========== 戦闘統計 ==========
    
    /**
     * 敵にHITした回数を追加
     */
    public void addHit(UUID uuid) {
        addStat(uuid, "hits", 1);
    }
    
    /**
     * 与えたダメージを追加
     */
    public void addDamage(UUID uuid, int damage) {
        addStat(uuid, "total_damage", damage);
    }
    
    /**
     * 釣り竿使用を記録
     */
    public void addFishingRodUse(UUID uuid) {
        addStat(uuid, "fishing_rod_uses", 1);
    }
    
    /**
     * SP技使用を記録
     */
    public void addSkillUse(UUID uuid) {
        addStat(uuid, "skill_uses", 1);
    }
    
    // ========== キング統計 ==========
    
    /**
     * キングになった回数を追加
     */
    public void addKingTime(UUID uuid) {
        addStat(uuid, "king_times", 1);
    }
    
    // ========== コア統計 ==========
    
    /**
     * コア破壊を記録
     */
    public void addCoreDestroyed(UUID uuid) {
        addStat(uuid, "cores_destroyed", 1);
    }
    
    // ========== リソース統計 ==========
    
    /**
     * ルミナ獲得を記録
     */
    public void addLumina(UUID uuid, int amount) {
        if (amount > 0) {
            addStat(uuid, "total_lumina", amount);
        }
    }
    
    /**
     * シャード獲得を記録
     */
    public void addShard(UUID uuid, int amount) {
        if (amount > 0) {
            addStat(uuid, "total_shard", amount);
        }
    }
    
    /**
     * シャード納品を記録
     */
    public void addShardDeposited(UUID uuid, int amount) {
        if (amount > 0) {
            addStat(uuid, "shard_deposited", amount);
        }
    }
    
    // ========== 内部メソッド ==========
    
    /**
     * 統計を追加
     */
    private void addStat(UUID uuid, String column, int amount) {
        if (!isEnabled()) return;
        
        String sql = "UPDATE player_stats SET " + column + " = " + column + " + ? WHERE uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("統計更新に失敗 (" + column + "): " + e.getMessage());
        }
    }
    
    /**
     * プレイヤーの統計を取得
     */
    public PlayerStats getStats(UUID uuid) {
        if (!isEnabled()) return null;
        
        String sql = "SELECT * FROM player_stats WHERE uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new PlayerStats(
                        rs.getString("name"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("games_played"),
                        rs.getInt("fire_picks"),
                        rs.getInt("ice_picks"),
                        rs.getInt("wind_picks"),
                        rs.getInt("earth_picks"),
                        rs.getInt("hits"),
                        rs.getInt("total_damage"),
                        rs.getInt("fishing_rod_uses"),
                        rs.getInt("skill_uses"),
                        rs.getInt("king_times"),
                        rs.getInt("cores_destroyed"),
                        rs.getInt("total_lumina"),
                        rs.getInt("total_shard"),
                        rs.getInt("shard_deposited")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("統計取得に失敗: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * ゲーム結果を記録
     */
    public void recordGame(UUID uuid, boolean won, int kills, int deaths) {
        if (!isEnabled()) return;
        
        addGamePlayed(uuid);
        addKills(uuid, kills);
        addDeaths(uuid, deaths);
        
        if (won) {
            addWin(uuid);
        } else {
            addLoss(uuid);
        }
    }
    
    /**
     * 接続を閉じる
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("データベース接続を閉じました");
            } catch (SQLException e) {
                plugin.getLogger().warning("データベース切断に失敗: " + e.getMessage());
            }
        }
    }
    
    /**
     * プレイヤー統計データクラス
     */
    public static class PlayerStats {
        public final String name;
        // 基本統計
        public final int wins;
        public final int losses;
        public final int kills;
        public final int deaths;
        public final int gamesPlayed;
        // エレメント別選択回数
        public final int firePicks;
        public final int icePicks;
        public final int windPicks;
        public final int earthPicks;
        // 戦闘統計
        public final int hits;
        public final int totalDamage;
        public final int fishingRodUses;
        public final int skillUses;
        // キング統計
        public final int kingTimes;
        // コア統計
        public final int coresDestroyed;
        // リソース統計
        public final int totalLumina;
        public final int totalShard;
        public final int shardDeposited;
        
        public PlayerStats(String name, int wins, int losses, int kills, int deaths, int gamesPlayed,
                          int firePicks, int icePicks, int windPicks, int earthPicks,
                          int hits, int totalDamage, int fishingRodUses, int skillUses,
                          int kingTimes, int coresDestroyed, int totalLumina, int totalShard, int shardDeposited) {
            this.name = name;
            this.wins = wins;
            this.losses = losses;
            this.kills = kills;
            this.deaths = deaths;
            this.gamesPlayed = gamesPlayed;
            this.firePicks = firePicks;
            this.icePicks = icePicks;
            this.windPicks = windPicks;
            this.earthPicks = earthPicks;
            this.hits = hits;
            this.totalDamage = totalDamage;
            this.fishingRodUses = fishingRodUses;
            this.skillUses = skillUses;
            this.kingTimes = kingTimes;
            this.coresDestroyed = coresDestroyed;
            this.totalLumina = totalLumina;
            this.totalShard = totalShard;
            this.shardDeposited = shardDeposited;
        }
        
        public double getKDRatio() {
            if (deaths == 0) {
                return kills;
            }
            return (double) kills / deaths;
        }
        
        public double getWinRate() {
            if (gamesPlayed == 0) {
                return 0;
            }
            return (double) wins / gamesPlayed * 100;
        }
        
        public int getTotalElementPicks() {
            return firePicks + icePicks + windPicks + earthPicks;
        }
        
        public String getFavoriteElement() {
            int max = Math.max(Math.max(firePicks, icePicks), Math.max(windPicks, earthPicks));
            if (max == 0) return "None";
            if (max == firePicks) return "Fire";
            if (max == icePicks) return "Ice";
            if (max == windPicks) return "Wind";
            return "Earth";
        }
    }
}
