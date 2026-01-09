package tensaimc.kingsline.database;

import tensaimc.kingsline.KingsLine;

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
    
    public StatsDatabase(KingsLine plugin) {
        this.plugin = plugin;
        connect();
        if (connection != null) {
            createTables();
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
        
        String sql = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT," +
                "wins INTEGER DEFAULT 0," +
                "kills INTEGER DEFAULT 0," +
                "deaths INTEGER DEFAULT 0," +
                "games_played INTEGER DEFAULT 0" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("テーブル作成に失敗: " + e.getMessage());
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
    
    /**
     * 勝利を追加
     */
    public void addWin(UUID uuid) {
        addStat(uuid, "wins", 1);
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
            plugin.getLogger().warning("統計更新に失敗: " + e.getMessage());
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
                        rs.getInt("kills"),
                        rs.getInt("deaths"),
                        rs.getInt("games_played")
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
        public final int wins;
        public final int kills;
        public final int deaths;
        public final int gamesPlayed;
        
        public PlayerStats(String name, int wins, int kills, int deaths, int gamesPlayed) {
            this.name = name;
            this.wins = wins;
            this.kills = kills;
            this.deaths = deaths;
            this.gamesPlayed = gamesPlayed;
        }
        
        public double getKDRatio() {
            if (deaths == 0) {
                return kills;
            }
            return (double) kills / deaths;
        }
    }
}
