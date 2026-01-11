package tensaimc.kingsline.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * チャットコマンドのタブ補完
 */
public class ChatTabCompleter implements TabCompleter {
    
    private static final List<String> CHAT_MODES = Arrays.asList("p", "party", "a", "all", "t", "team");
    
    public ChatTabCompleter() {
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return CHAT_MODES.stream()
                    .filter(mode -> mode.startsWith(input))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
