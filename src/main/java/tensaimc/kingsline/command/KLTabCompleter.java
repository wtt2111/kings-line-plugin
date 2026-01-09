package tensaimc.kingsline.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /kl コマンドのタブ補完
 */
public class KLTabCompleter implements TabCompleter {
    
    private final KingsLine plugin;
    
    private static final List<String> MAIN_COMMANDS = Arrays.asList(
            "start", "stop", "reload", "party", "info"
    );
    
    private static final List<String> ADMIN_COMMANDS = Arrays.asList(
            "setspawn", "setcore", "setnpc", "setarea", "setlobby", 
            "save", "createarena", "setarena"
    );
    
    private static final List<String> PARTY_ACTIONS = Arrays.asList(
            "invite", "accept", "deny", "leave", "list", "disband"
    );
    
    private static final List<String> TEAMS = Arrays.asList("blue", "red");
    private static final List<String> AREAS = Arrays.asList("A", "B", "C");
    private static final List<String> POSITIONS = Arrays.asList("pos1", "pos2");
    
    public KLTabCompleter(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(MAIN_COMMANDS);
            if (sender.hasPermission("kingsline.admin")) {
                completions.addAll(ADMIN_COMMANDS);
            }
            return filter(completions, args[0]);
        }
        
        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "party":
                if (args.length == 2) {
                    return filter(PARTY_ACTIONS, args[1]);
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("invite")) {
                    return getOnlinePlayerNames(args[2]);
                }
                break;
                
            case "setspawn":
            case "setcore":
            case "setnpc":
                if (args.length == 2) {
                    return filter(TEAMS, args[1]);
                }
                break;
                
            case "setarea":
                if (args.length == 2) {
                    return filter(AREAS, args[1]);
                }
                if (args.length == 3) {
                    return filter(POSITIONS, args[2]);
                }
                break;
                
            case "setarena":
                if (args.length == 2) {
                    return filter(new ArrayList<>(plugin.getArenaConfig().getArenaNames()), args[1]);
                }
                break;
        }
        
        return completions;
    }
    
    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    private List<String> getOnlinePlayerNames(String prefix) {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
