package tensaimc.kingsline.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Area;
import tensaimc.kingsline.arena.Arena;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * arenas.yml の管理クラス
 */
public class ArenaConfig {
    
    private final KingsLine plugin;
    private File file;
    private FileConfiguration config;
    
    private final Map<String, Arena> arenas;
    private String currentArenaName;
    
    public ArenaConfig(KingsLine plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        loadConfig();
    }
    
    private void loadConfig() {
        file = new File(plugin.getDataFolder(), "arenas.yml");
        
        if (!file.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(file);
        
        // 現在のアリーナ名を読み込み
        currentArenaName = config.getString("current-arena", "default");
        
        // アリーナを読み込み
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String arenaName : arenasSection.getKeys(false)) {
                Arena arena = loadArena(arenaName, arenasSection.getConfigurationSection(arenaName));
                if (arena != null) {
                    arenas.put(arenaName, arena);
                }
            }
        }
        
        // デフォルトアリーナがなければ作成
        if (arenas.isEmpty()) {
            arenas.put("default", new Arena("default"));
        }
    }
    
    private Arena loadArena(String name, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        
        Arena arena = new Arena(name);
        
        // ワールド名
        arena.setWorldName(section.getString("world"));
        
        // スポーン
        arena.setBlueSpawn(loadLocation(section.getConfigurationSection("spawns.blue"), arena.getWorldName()));
        arena.setRedSpawn(loadLocation(section.getConfigurationSection("spawns.red"), arena.getWorldName()));
        
        // コア
        arena.setBlueCore(loadLocation(section.getConfigurationSection("cores.blue"), arena.getWorldName()));
        arena.setRedCore(loadLocation(section.getConfigurationSection("cores.red"), arena.getWorldName()));
        
        // NPC
        arena.setBlueNPC(loadLocation(section.getConfigurationSection("npcs.blue"), arena.getWorldName()));
        arena.setRedNPC(loadLocation(section.getConfigurationSection("npcs.red"), arena.getWorldName()));
        
        // エリア
        loadArea(arena.getAreaA(), section.getConfigurationSection("areas.A"), arena.getWorldName());
        loadArea(arena.getAreaB(), section.getConfigurationSection("areas.B"), arena.getWorldName());
        loadArea(arena.getAreaC(), section.getConfigurationSection("areas.C"), arena.getWorldName());
        
        // ロビー
        arena.setLobby(loadLocation(section.getConfigurationSection("lobby"), arena.getWorldName()));
        
        return arena;
    }
    
    private Location loadLocation(ConfigurationSection section, String defaultWorld) {
        if (section == null) {
            return null;
        }
        
        String worldName = section.getString("world", defaultWorld);
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    private void loadArea(Area area, ConfigurationSection section, String worldName) {
        if (section == null) {
            return;
        }
        
        area.setPos1(loadLocation(section.getConfigurationSection("pos1"), worldName));
        area.setPos2(loadLocation(section.getConfigurationSection("pos2"), worldName));
        area.setShardSpawn(loadLocation(section.getConfigurationSection("shard-spawn"), worldName));
    }
    
    public void saveConfig() {
        config.set("current-arena", currentArenaName);
        
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            saveArena(entry.getKey(), entry.getValue());
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("arenas.yml の保存に失敗しました: " + e.getMessage());
        }
    }
    
    private void saveArena(String name, Arena arena) {
        String path = "arenas." + name;
        
        config.set(path + ".world", arena.getWorldName());
        
        // スポーン
        saveLocation(path + ".spawns.blue", arena.getBlueSpawn());
        saveLocation(path + ".spawns.red", arena.getRedSpawn());
        
        // コア
        saveLocation(path + ".cores.blue", arena.getBlueCore());
        saveLocation(path + ".cores.red", arena.getRedCore());
        
        // NPC
        saveLocation(path + ".npcs.blue", arena.getBlueNPC());
        saveLocation(path + ".npcs.red", arena.getRedNPC());
        
        // エリア
        saveArea(path + ".areas.A", arena.getAreaA());
        saveArea(path + ".areas.B", arena.getAreaB());
        saveArea(path + ".areas.C", arena.getAreaC());
        
        // ロビー
        saveLocation(path + ".lobby", arena.getLobby());
    }
    
    private void saveLocation(String path, Location loc) {
        if (loc == null) {
            return;
        }
        
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
        
        if (loc.getWorld() != null) {
            config.set(path + ".world", loc.getWorld().getName());
        }
    }
    
    private void saveArea(String path, Area area) {
        if (area == null) {
            return;
        }
        
        saveLocation(path + ".pos1", area.getPos1());
        saveLocation(path + ".pos2", area.getPos2());
        saveLocation(path + ".shard-spawn", area.getShardSpawn());
    }
    
    public void reload() {
        arenas.clear();
        loadConfig();
    }
    
    // ========== Arena Management ==========
    
    public Arena getCurrentArena() {
        return arenas.get(currentArenaName);
    }
    
    public void setCurrentArena(String name) {
        if (arenas.containsKey(name)) {
            currentArenaName = name;
        }
    }
    
    public Arena getArena(String name) {
        return arenas.get(name);
    }
    
    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        return arena;
    }
    
    public Set<String> getArenaNames() {
        return arenas.keySet();
    }
    
    public boolean arenaExists(String name) {
        return arenas.containsKey(name);
    }
}
