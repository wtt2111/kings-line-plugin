package tensaimc.kingsline.arena;

import org.bukkit.Location;
import org.bukkit.World;
import tensaimc.kingsline.player.Team;

/**
 * アリーナ（マップ）を表すクラス
 */
public class Arena {
    
    private final String name;
    private String worldName;
    
    // チームスポーン
    private Location blueSpawn;
    private Location redSpawn;
    
    // コア位置
    private Location blueCore;
    private Location redCore;
    
    // NPC位置 (ショップ/Shard銀行)
    private Location blueNPC;
    private Location redNPC;
    
    // エリア
    private Area areaA;  // 自陣寄り (5v5以上)
    private Area areaB;  // 中央 (常に有効)
    private Area areaC;  // 敵陣寄り (5v5以上)
    
    // ロビー位置
    private Location lobby;
    
    public Arena(String name) {
        this.name = name;
        this.areaA = new Area("A");
        this.areaB = new Area("B");
        this.areaC = new Area("C");
    }
    
    // ========== Basic Info ==========
    
    public String getName() {
        return name;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    // ========== Spawns ==========
    
    public Location getBlueSpawn() {
        return blueSpawn;
    }
    
    public void setBlueSpawn(Location blueSpawn) {
        this.blueSpawn = blueSpawn;
    }
    
    public Location getRedSpawn() {
        return redSpawn;
    }
    
    public void setRedSpawn(Location redSpawn) {
        this.redSpawn = redSpawn;
    }
    
    public Location getSpawn(Team team) {
        switch (team) {
            case BLUE:
                return blueSpawn;
            case RED:
                return redSpawn;
            default:
                return lobby;
        }
    }
    
    // ========== Cores ==========
    
    public Location getBlueCore() {
        return blueCore;
    }
    
    public void setBlueCore(Location blueCore) {
        this.blueCore = blueCore;
    }
    
    public Location getRedCore() {
        return redCore;
    }
    
    public void setRedCore(Location redCore) {
        this.redCore = redCore;
    }
    
    public Location getCore(Team team) {
        switch (team) {
            case BLUE:
                return blueCore;
            case RED:
                return redCore;
            default:
                return null;
        }
    }
    
    // ========== NPCs ==========
    
    public Location getBlueNPC() {
        return blueNPC;
    }
    
    public void setBlueNPC(Location blueNPC) {
        this.blueNPC = blueNPC;
    }
    
    public Location getRedNPC() {
        return redNPC;
    }
    
    public void setRedNPC(Location redNPC) {
        this.redNPC = redNPC;
    }
    
    public Location getNPC(Team team) {
        switch (team) {
            case BLUE:
                return blueNPC;
            case RED:
                return redNPC;
            default:
                return null;
        }
    }
    
    // ========== Areas ==========
    
    public Area getAreaA() {
        return areaA;
    }
    
    public void setAreaA(Area areaA) {
        this.areaA = areaA;
    }
    
    public Area getAreaB() {
        return areaB;
    }
    
    public void setAreaB(Area areaB) {
        this.areaB = areaB;
    }
    
    public Area getAreaC() {
        return areaC;
    }
    
    public void setAreaC(Area areaC) {
        this.areaC = areaC;
    }
    
    public Area getArea(String id) {
        switch (id.toUpperCase()) {
            case "A":
                return areaA;
            case "B":
                return areaB;
            case "C":
                return areaC;
            default:
                return null;
        }
    }
    
    // ========== Lobby ==========
    
    public Location getLobby() {
        return lobby;
    }
    
    public void setLobby(Location lobby) {
        this.lobby = lobby;
    }
    
    // ========== Validation ==========
    
    /**
     * アリーナが使用可能かどうか
     */
    public boolean isValid() {
        return worldName != null
                && blueSpawn != null
                && redSpawn != null
                && areaB.isValid();
    }
    
    /**
     * 完全に設定されているかどうか（コア、NPCも含む）
     */
    public boolean isFullyConfigured() {
        return isValid()
                && blueCore != null
                && redCore != null
                && blueNPC != null
                && redNPC != null;
    }
    
    /**
     * 小規模モードを適用（A/Cエリアを無効化）
     */
    public void applySmallScaleMode() {
        areaA.setEnabled(false);
        areaC.setEnabled(false);
    }
    
    /**
     * 大規模モードを適用（全エリア有効化）
     */
    public void applyLargeScaleMode() {
        areaA.setEnabled(true);
        areaC.setEnabled(true);
    }
}
