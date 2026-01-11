package tensaimc.kingsline.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /p, /party コマンドのタブ補完
 */
public class PartyTabCompleter implements TabCompleter {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "leave", "disband", "accept", "deny");
    
    public PartyTabCompleter(KingsLine plugin) {
        // plugin は将来的な拡張用に保持（現在は未使用）
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        // /pl コマンドはタブ補完なし
        if (command.getName().equalsIgnoreCase("pl")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            
            // サブコマンドを追加
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            
            // オンラインプレイヤー名を追加（招待用）
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input) && !player.equals(sender)) {
                    completions.add(player.getName());
                }
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}
