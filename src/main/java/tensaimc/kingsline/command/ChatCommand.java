package tensaimc.kingsline.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.chat.ChatManager;
import tensaimc.kingsline.chat.ChatManager.ChatMode;

/**
 * チャットモード切り替えコマンド
 * /chat [p|party|a|all|t|team]
 * /ch [p|party|a|all|t|team]
 */
public class ChatCommand implements CommandExecutor {
    
    private final KingsLine plugin;
    
    public ChatCommand(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ使用できます。");
            return true;
        }
        
        Player player = (Player) sender;
        ChatManager chatManager = plugin.getChatManager();
        
        // スペクテイターはチャットモード変更不可
        if (chatManager.isSpectator(player)) {
            player.sendMessage(ChatColor.RED + "観戦者はチャットモードを変更できません。");
            return true;
        }
        
        if (args.length == 0) {
            // 引数なし：現在のモードを表示
            ChatMode currentMode = chatManager.getChatMode(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "現在のチャットモード: " + currentMode.getPrefixColor() + currentMode.getDisplayName());
            player.sendMessage(ChatColor.GRAY + "使い方: /chat [p|a|t]");
            player.sendMessage(ChatColor.GRAY + "  p, party - パーティーチャット");
            player.sendMessage(ChatColor.GRAY + "  a, all - 全体チャット");
            player.sendMessage(ChatColor.GRAY + "  t, team - チームチャット");
            return true;
        }
        
        String arg = args[0].toLowerCase();
        ChatMode newMode;
        
        switch (arg) {
            case "p":
            case "party":
                newMode = ChatMode.PARTY;
                break;
            case "a":
            case "all":
                newMode = ChatMode.ALL;
                break;
            case "t":
            case "team":
                newMode = ChatMode.TEAM;
                break;
            default:
                player.sendMessage(ChatColor.RED + "不明なチャットモード: " + arg);
                player.sendMessage(ChatColor.GRAY + "使い方: /chat [p|a|t]");
                return true;
        }
        
        chatManager.setChatMode(player.getUniqueId(), newMode);
        player.sendMessage(ChatColor.GREEN + "チャットモードを " + newMode.getPrefixColor() + newMode.getDisplayName() + ChatColor.GREEN + " に変更しました。");
        
        return true;
    }
}
