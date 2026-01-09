package tensaimc.kingsline.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ゲームエリア (A/B/C) を表すクラス
 */
public class Area {
    
    private final String id;  // "A", "B", "C"
    private Location pos1;
    private Location pos2;
    private Location shardSpawn;
    private boolean enabled;
    
    public Area(String id) {
        this.id = id;
        this.enabled = true;
    }
    
    public Area(String id, Location pos1, Location pos2, Location shardSpawn) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.shardSpawn = shardSpawn;
        this.enabled = true;
    }
    
    public String getId() {
        return id;
    }
    
    public Location getPos1() {
        return pos1;
    }
    
    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }
    
    public Location getPos2() {
        return pos2;
    }
    
    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }
    
    public Location getShardSpawn() {
        return shardSpawn;
    }
    
    public void setShardSpawn(Location shardSpawn) {
        this.shardSpawn = shardSpawn;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * エリアが正しく設定されているか
     */
    public boolean isValid() {
        return pos1 != null && pos2 != null;
    }
    
    /**
     * 指定された座標がエリア内かどうかを判定
     */
    public boolean contains(Location loc) {
        if (!isValid() || loc == null) {
            return false;
        }
        
        World world = pos1.getWorld();
        if (world == null || !world.equals(loc.getWorld())) {
            return false;
        }
        
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
    
    /**
     * エリア内のプレイヤーを取得
     */
    public List<Player> getPlayersInside() {
        List<Player> players = new ArrayList<>();
        
        if (!isValid() || pos1.getWorld() == null) {
            return players;
        }
        
        for (Player player : pos1.getWorld().getPlayers()) {
            if (contains(player.getLocation())) {
                players.add(player);
            }
        }
        
        return players;
    }
    
    /**
     * エリア内の指定チームのプレイヤーを取得
     */
    public List<KLPlayer> getPlayersInside(Map<UUID, KLPlayer> playerMap, Team team) {
        List<KLPlayer> players = new ArrayList<>();
        
        for (Player player : getPlayersInside()) {
            KLPlayer klPlayer = playerMap.get(player.getUniqueId());
            if (klPlayer != null && klPlayer.getTeam() == team) {
                players.add(klPlayer);
            }
        }
        
        return players;
    }
    
    /**
     * 各チームのエリア内人数を取得
     */
    public int getTeamCount(Map<UUID, KLPlayer> playerMap, Team team) {
        return getPlayersInside(playerMap, team).size();
    }
    
    /**
     * エリアの中心座標を取得
     */
    public Location getCenter() {
        if (!isValid()) {
            return null;
        }
        
        double centerX = (pos1.getX() + pos2.getX()) / 2;
        double centerY = (pos1.getY() + pos2.getY()) / 2;
        double centerZ = (pos1.getZ() + pos2.getZ()) / 2;
        
        return new Location(pos1.getWorld(), centerX, centerY, centerZ);
    }
}
