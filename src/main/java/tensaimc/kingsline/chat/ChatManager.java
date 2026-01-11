package tensaimc.kingsline.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.PartyManager;
import tensaimc.kingsline.player.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * チャット管理クラス
 * チームチャット、全体チャット、パーティーチャット、スペクテイターチャットを管理
 */
public class ChatManager {
    
    private final KingsLine plugin;
    
    // プレイヤーのチャットモード
    private final Map<UUID, ChatMode> playerChatModes;
    
    public enum ChatMode {
        TEAM("Team", ChatColor.GREEN),
        ALL("All", ChatColor.WHITE),
        PARTY("Party", ChatColor.LIGHT_PURPLE);
        
        private final String displayName;
        private final ChatColor prefixColor;
        
        ChatMode(String displayName, ChatColor prefixColor) {
            this.displayName = displayName;
            this.prefixColor = prefixColor;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public ChatColor getPrefixColor() {
            return prefixColor;
        }
    }
    
    public ChatManager(KingsLine plugin) {
        this.plugin = plugin;
        this.playerChatModes = new HashMap<>();
    }
    
    /**
     * プレイヤーのチャットモードを取得（デフォルト: TEAM）
     */
    public ChatMode getChatMode(UUID playerUuid) {
        return playerChatModes.getOrDefault(playerUuid, ChatMode.TEAM);
    }
    
    /**
     * プレイヤーのチャットモードを設定
     */
    public void setChatMode(UUID playerUuid, ChatMode mode) {
        playerChatModes.put(playerUuid, mode);
    }
    
    /**
     * プレイヤーがスペクテイター（途中参加の観戦者）かどうか
     */
    public boolean isSpectator(Player player) {
        if (player.getGameMode() != GameMode.SPECTATOR) {
            return false;
        }
        GameManager gm = plugin.getGameManager();
        // ゲーム参加者でない（途中参加の観戦者）
        return gm.getPlayer(player) == null;
    }
    
    /**
     * チャットメッセージを送信
     * @return true: 処理完了（イベントをキャンセルする）、false: 通常処理
     */
    public boolean handleChat(Player sender, String message) {
        GameManager gm = plugin.getGameManager();
        
        // ゲーム中でなければ通常のチャット
        if (!gm.isState(GameState.RUNNING, GameState.STARTING)) {
            return false;
        }
        
        // スペクテイターのチャット処理
        if (isSpectator(sender)) {
            sendSpectatorChat(sender, message);
            return true;
        }
        
        // ゲーム参加者のチャット処理
        KLPlayer klPlayer = gm.getPlayer(sender);
        if (klPlayer == null) {
            return false;
        }
        
        ChatMode mode = getChatMode(sender.getUniqueId());
        
        switch (mode) {
            case TEAM:
                sendTeamChat(sender, klPlayer, message);
                break;
            case ALL:
                sendAllChat(sender, klPlayer, message);
                break;
            case PARTY:
                sendPartyChat(sender, message);
                break;
        }
        
        return true;
    }
    
    /**
     * チームチャットを送信（味方チームのみ）
     */
    private void sendTeamChat(Player sender, KLPlayer klPlayer, String message) {
        Team team = klPlayer.getTeam();
        String formattedMessage = formatMessage("Team", team.getChatColor(), sender, message);
        
        GameManager gm = plugin.getGameManager();
        
        // 同じチームのプレイヤーにのみ送信
        for (KLPlayer target : gm.getPlayers().values()) {
            if (target.getTeam() == team && target.isOnline()) {
                Player targetPlayer = Bukkit.getPlayer(target.getUuid());
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(formattedMessage);
                }
            }
        }
        
        // コンソールにも出力
        plugin.getLogger().info("[TeamChat/" + team.getDisplayName() + "] " + sender.getName() + ": " + message);
    }
    
    /**
     * 全体チャットを送信（青チーム・赤チーム＋スペクテイター）
     */
    private void sendAllChat(Player sender, KLPlayer klPlayer, String message) {
        Team team = klPlayer.getTeam();
        String formattedMessage = formatMessage("All", team.getChatColor(), sender, message);
        
        GameManager gm = plugin.getGameManager();
        
        // 全ゲーム参加者に送信
        for (KLPlayer target : gm.getPlayers().values()) {
            if (target.isOnline()) {
                Player targetPlayer = Bukkit.getPlayer(target.getUuid());
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(formattedMessage);
                }
            }
        }
        
        // スペクテイターにも送信
        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (isSpectator(spectator)) {
                spectator.sendMessage(formattedMessage);
            }
        }
        
        // コンソールにも出力
        plugin.getLogger().info("[AllChat] " + sender.getName() + ": " + message);
    }
    
    /**
     * パーティーチャットを送信（パーティーメンバーのみ）
     */
    private void sendPartyChat(Player sender, String message) {
        PartyManager pm = plugin.getPartyManager();
        UUID partyId = pm.getPartyId(sender.getUniqueId());
        
        if (partyId == null) {
            sender.sendMessage(ChatColor.RED + "パーティーに所属していません。/p <player> で招待できます。");
            return;
        }
        
        String formattedMessage = formatMessage("Party", ChatColor.LIGHT_PURPLE, sender, message);
        
        // パーティーメンバーに送信
        Set<UUID> members = pm.getPartyMembers(partyId);
        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
        
        // コンソールにも出力
        plugin.getLogger().info("[PartyChat] " + sender.getName() + ": " + message);
    }
    
    /**
     * スペクテイターチャットを送信（スペクテイター同士のみ）
     */
    private void sendSpectatorChat(Player sender, String message) {
        String formattedMessage = formatMessage("Spec", ChatColor.GRAY, sender, message);
        
        // 全スペクテイターに送信
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSpectator(player)) {
                player.sendMessage(formattedMessage);
            }
        }
        
        // コンソールにも出力
        plugin.getLogger().info("[SpectatorChat] " + sender.getName() + ": " + message);
    }
    
    /**
     * チャットメッセージをフォーマット
     */
    private String formatMessage(String prefix, ChatColor nameColor, Player sender, String message) {
        return ChatColor.GRAY + "[" + prefix + "] " + nameColor + sender.getName() + ChatColor.WHITE + ": " + message;
    }
    
    /**
     * プレイヤーデータをリセット（ゲーム終了時）
     */
    public void reset() {
        playerChatModes.clear();
    }
    
    /**
     * プレイヤーのチャットモード設定を削除
     */
    public void removePlayer(UUID playerUuid) {
        playerChatModes.remove(playerUuid);
    }
}
