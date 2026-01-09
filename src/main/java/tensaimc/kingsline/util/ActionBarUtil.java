package tensaimc.kingsline.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 1.8.8対応 ActionBar送信ユーティリティ
 */
public class ActionBarUtil {
    
    private static final String VERSION;
    
    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
    }
    
    /**
     * アクションバーにメッセージを送信
     * @param player プレイヤー
     * @param message メッセージ
     */
    public static void sendActionBar(Player player, String message) {
        try {
            Object chatComponent = serializeText(message);
            
            Class<?> packetClass = getNMSClass("PacketPlayOutChat");
            Constructor<?> constructor = packetClass.getConstructor(getNMSClass("IChatBaseComponent"), byte.class);
            
            // byte 2 = ActionBar
            Object packet = constructor.newInstance(chatComponent, (byte) 2);
            
            sendPacket(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 複数プレイヤーにアクションバーを送信
     */
    public static void broadcastActionBar(Iterable<Player> players, String message) {
        for (Player player : players) {
            sendActionBar(player, message);
        }
    }
    
    /**
     * テキストをIChatBaseComponentに変換
     */
    private static Object serializeText(String text) throws Exception {
        if (text == null) {
            text = "";
        }
        
        Class<?> chatSerializerClass = getNMSClass("IChatBaseComponent$ChatSerializer");
        Method aMethod = chatSerializerClass.getMethod("a", String.class);
        return aMethod.invoke(null, "{\"text\":\"" + escapeJson(text) + "\"}");
    }
    
    /**
     * JSON用にエスケープ
     */
    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
    
    /**
     * パケットを送信
     */
    private static void sendPacket(Player player, Object packet) throws Exception {
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
