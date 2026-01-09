package tensaimc.kingsline.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import tensaimc.kingsline.KingsLine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 1.8.8対応 BossBar ユーティリティ（Wither方式）
 * Witherの見えない位置にスポーンしてHPバーを表示
 */
public class BossBarManager {
    
    private static final String VERSION;
    
    private final KingsLine plugin;
    private final Map<UUID, Object> witherEntities;
    private final Map<UUID, String> playerMessages;
    private final Map<UUID, Float> playerProgress;
    
    private BukkitTask updateTask;
    
    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
    }
    
    public BossBarManager(KingsLine plugin) {
        this.plugin = plugin;
        this.witherEntities = new HashMap<>();
        this.playerMessages = new HashMap<>();
        this.playerProgress = new HashMap<>();
    }
    
    /**
     * ボスバーを開始
     */
    public void start() {
        stop();
        
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllBossBars();
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
    
    /**
     * ボスバーを停止
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        // すべてのWitherを削除
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBossBar(player);
        }
        
        witherEntities.clear();
        playerMessages.clear();
        playerProgress.clear();
    }
    
    /**
     * プレイヤーにボスバーを設定
     * @param player プレイヤー
     * @param message メッセージ
     * @param progress 進行度（0.0-1.0）
     */
    public void setBossBar(Player player, String message, float progress) {
        UUID uuid = player.getUniqueId();
        playerMessages.put(uuid, message);
        playerProgress.put(uuid, Math.max(0, Math.min(1, progress)));
    }
    
    /**
     * すべてのプレイヤーに同じボスバーを設定
     */
    public void setGlobalBossBar(String message, float progress) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setBossBar(player, message, progress);
        }
    }
    
    /**
     * プレイヤーからボスバーを削除
     */
    public void removeBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        Object wither = witherEntities.remove(uuid);
        playerMessages.remove(uuid);
        playerProgress.remove(uuid);
        
        if (wither != null) {
            try {
                sendDestroyPacket(player, wither);
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * すべてのボスバーを更新
     */
    private void updateAllBossBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateBossBar(player);
        }
    }
    
    /**
     * プレイヤーのボスバーを更新
     */
    private void updateBossBar(Player player) {
        UUID uuid = player.getUniqueId();
        String message = playerMessages.get(uuid);
        Float progress = playerProgress.get(uuid);
        
        if (message == null || progress == null) {
            // ボスバーがない場合は削除
            Object wither = witherEntities.remove(uuid);
            if (wither != null) {
                try {
                    sendDestroyPacket(player, wither);
                } catch (Exception ignored) {}
            }
            return;
        }
        
        try {
            Object wither = witherEntities.get(uuid);
            Location loc = getWitherLocation(player);
            
            if (wither == null) {
                // 新規作成
                wither = createWither(loc, message, progress);
                witherEntities.put(uuid, wither);
                sendSpawnPacket(player, wither);
            } else {
                // 更新
                updateWither(wither, loc, message, progress);
                sendMetadataPacket(player, wither);
                sendTeleportPacket(player, wither);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Witherの表示位置を計算（プレイヤーの視線の先、地下）
     */
    private Location getWitherLocation(Player player) {
        Location loc = player.getLocation().clone();
        loc.add(loc.getDirection().multiply(40));
        loc.setY(-50);
        return loc;
    }
    
    /**
     * Witherエンティティを作成
     */
    private Object createWither(Location loc, String name, float progress) throws Exception {
        Class<?> worldClass = getNMSClass("World");
        Class<?> witherClass = getNMSClass("EntityWither");
        
        Object nmsWorld = loc.getWorld().getClass().getMethod("getHandle").invoke(loc.getWorld());
        Constructor<?> constructor = witherClass.getConstructor(worldClass);
        Object wither = constructor.newInstance(nmsWorld);
        
        // 位置設定
        Method setLocation = witherClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
        setLocation.invoke(wither, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        
        // 名前設定
        Method setCustomName = witherClass.getMethod("setCustomName", String.class);
        setCustomName.invoke(wither, name);
        
        // 可視性設定
        Method setInvisible = witherClass.getMethod("setInvisible", boolean.class);
        setInvisible.invoke(wither, true);
        
        // HP設定（最大300）
        Method setHealth = witherClass.getMethod("setHealth", float.class);
        float health = progress * 300.0f;
        setHealth.invoke(wither, Math.max(1, health));
        
        return wither;
    }
    
    /**
     * Witherを更新
     */
    private void updateWither(Object wither, Location loc, String name, float progress) throws Exception {
        Class<?> witherClass = getNMSClass("EntityWither");
        
        // 位置更新
        Method setLocation = witherClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
        setLocation.invoke(wither, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        
        // 名前更新
        Method setCustomName = witherClass.getMethod("setCustomName", String.class);
        setCustomName.invoke(wither, name);
        
        // HP更新
        Method setHealth = witherClass.getMethod("setHealth", float.class);
        float health = progress * 300.0f;
        setHealth.invoke(wither, Math.max(1, health));
    }
    
    /**
     * スポーンパケットを送信
     */
    private void sendSpawnPacket(Player player, Object wither) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutSpawnEntityLiving");
        Class<?> entityClass = getNMSClass("EntityLiving");
        
        Constructor<?> constructor = packetClass.getConstructor(entityClass);
        Object packet = constructor.newInstance(wither);
        
        sendPacket(player, packet);
    }
    
    /**
     * メタデータパケットを送信
     */
    private void sendMetadataPacket(Player player, Object wither) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutEntityMetadata");
        Class<?> entityClass = getNMSClass("Entity");
        Class<?> dataWatcherClass = getNMSClass("DataWatcher");
        
        Method getIdMethod = entityClass.getMethod("getId");
        int entityId = (int) getIdMethod.invoke(wither);
        
        Method getDataWatcherMethod = entityClass.getMethod("getDataWatcher");
        Object dataWatcher = getDataWatcherMethod.invoke(wither);
        
        Method cMethod = dataWatcherClass.getMethod("c");
        Object watchableObjects = cMethod.invoke(dataWatcher);
        
        Constructor<?> constructor = packetClass.getConstructor(int.class, List.class, boolean.class);
        Object packet = constructor.newInstance(entityId, watchableObjects, true);
        
        sendPacket(player, packet);
    }
    
    /**
     * テレポートパケットを送信
     */
    private void sendTeleportPacket(Player player, Object wither) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutEntityTeleport");
        Class<?> entityClass = getNMSClass("Entity");
        
        Constructor<?> constructor = packetClass.getConstructor(entityClass);
        Object packet = constructor.newInstance(wither);
        
        sendPacket(player, packet);
    }
    
    /**
     * 削除パケットを送信
     */
    private void sendDestroyPacket(Player player, Object wither) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutEntityDestroy");
        Class<?> entityClass = getNMSClass("Entity");
        
        Method getIdMethod = entityClass.getMethod("getId");
        int entityId = (int) getIdMethod.invoke(wither);
        
        Constructor<?> constructor = packetClass.getConstructor(int[].class);
        Object packet = constructor.newInstance(new int[]{entityId});
        
        sendPacket(player, packet);
    }
    
    /**
     * パケットを送信
     */
    private void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
        playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet"))
                .invoke(playerConnection, packet);
    }
    
    /**
     * NMSクラスを取得
     */
    private static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + VERSION + "." + name);
    }
}
