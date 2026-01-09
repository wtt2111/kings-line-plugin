package tensaimc.kingsline.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 1.8.8対応 Title送信ユーティリティ
 */
public class TitleUtil {
    
    private static final String VERSION;
    
    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
    }
    
    /**
     * タイトルを送信
     * @param player プレイヤー
     * @param title タイトル（上の大きい文字）
     * @param subtitle サブタイトル（下の小さい文字）
     * @param fadeIn フェードイン時間（tick）
     * @param stay 表示時間（tick）
     * @param fadeOut フェードアウト時間（tick）
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Object chatTitle = serializeText(title);
            Object chatSubtitle = serializeText(subtitle);
            
            // TIMESパケット
            sendTitlePacket(player, "TIMES", null, fadeIn, stay, fadeOut);
            
            // TITLEパケット
            if (title != null && !title.isEmpty()) {
                sendTitlePacket(player, "TITLE", chatTitle, 0, 0, 0);
            }
            
            // SUBTITLEパケット
            if (subtitle != null && !subtitle.isEmpty()) {
                sendTitlePacket(player, "SUBTITLE", chatSubtitle, 0, 0, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * タイトルを送信（デフォルト時間）
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }
    
    /**
     * タイトルのみ送信
     */
    public static void sendTitle(Player player, String title) {
        sendTitle(player, title, "", 10, 70, 20);
    }
    
    /**
     * タイトルをクリア
     */
    public static void clearTitle(Player player) {
        try {
            sendTitlePacket(player, "CLEAR", null, 0, 0, 0);
        } catch (Exception ignored) {}
    }
    
    /**
     * タイトルパケットを送信
     */
    private static void sendTitlePacket(Player player, String action, Object text, int fadeIn, int stay, int fadeOut) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutTitle");
        Class<?> enumClass = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
        
        Object enumAction = Enum.valueOf((Class<Enum>) enumClass, action);
        
        Constructor<?> constructor;
        Object packet;
        
        if (action.equals("TIMES")) {
            constructor = packetClass.getConstructor(int.class, int.class, int.class);
            packet = constructor.newInstance(fadeIn, stay, fadeOut);
        } else {
            Class<?> chatComponentClass = getNMSClass("IChatBaseComponent");
            constructor = packetClass.getConstructor(enumClass, chatComponentClass);
            packet = constructor.newInstance(enumAction, text);
        }
        
        sendPacket(player, packet);
    }
    
    /**
     * テキストをIChatBaseComponentに変換
     */
    private static Object serializeText(String text) throws Exception {
        if (text == null || text.isEmpty()) {
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
