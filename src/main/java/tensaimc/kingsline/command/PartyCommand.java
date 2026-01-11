package tensaimc.kingsline.command;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.player.PartyManager;

import java.util.List;

/**
 * /p, /party, /pl コマンド
 */
public class PartyCommand implements CommandExecutor {
    
    private final KingsLine plugin;
    
    public PartyCommand(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ使用可能です。");
            return true;
        }
        
        Player player = (Player) sender;
        
        // /pl コマンドはlistのショートカット
        if (command.getName().equalsIgnoreCase("pl")) {
            return handleList(player);
        }
        
        // LOBBYフェーズチェック
        GameManager gm = plugin.getGameManager();
        if (gm.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "パーティー操作はロビー待機中のみ可能です。");
            return true;
        }
        
        // 引数なしの場合はヘルプを表示
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "list":
                return handleList(player);
            case "leave":
                return handleLeave(player);
            case "disband":
                return handleDisband(player);
            case "accept":
                return handleAccept(player);
            case "deny":
                return handleDeny(player);
            default:
                // プレイヤー名として招待を試みる
                return handleInvite(player, args[0]);
        }
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "===== パーティーコマンド =====");
        player.sendMessage(ChatColor.YELLOW + "/p <プレイヤー名>" + ChatColor.GRAY + " - プレイヤーを招待");
        player.sendMessage(ChatColor.YELLOW + "/p list" + ChatColor.GRAY + " - メンバー一覧");
        player.sendMessage(ChatColor.YELLOW + "/p leave" + ChatColor.GRAY + " - パーティーを離脱");
        player.sendMessage(ChatColor.YELLOW + "/p disband" + ChatColor.GRAY + " - パーティーを解散（リーダーのみ）");
        player.sendMessage(ChatColor.GRAY + "パーティーメンバーは同じチームになります（最大5人）");
    }
    
    /**
     * プレイヤーを招待
     */
    private boolean handleInvite(Player inviter, String targetName) {
        PartyManager pm = plugin.getPartyManager();
        
        // ターゲットプレイヤーを検索
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            inviter.sendMessage(ChatColor.RED + "プレイヤー「" + targetName + "」が見つかりません。");
            return true;
        }
        
        // 自分自身への招待を防止
        if (target.getUniqueId().equals(inviter.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "自分自身を招待することはできません。");
            return true;
        }
        
        // 招待を送信
        if (pm.invite(inviter.getUniqueId(), target.getUniqueId())) {
            // 招待成功 - クリック可能なメッセージは PartyManager で送信される
        }
        
        return true;
    }
    
    /**
     * パーティーメンバー一覧
     */
    private boolean handleList(Player player) {
        PartyManager pm = plugin.getPartyManager();
        
        if (!pm.isInParty(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "パーティーに所属していません。");
            return true;
        }
        
        List<String> members = pm.getPartyMemberNames(player.getUniqueId());
        int maxSize = plugin.getConfigManager().getPartyMaxSize();
        
        player.sendMessage(ChatColor.GOLD + "===== パーティーメンバー (" + members.size() + "/" + maxSize + ") =====");
        for (String name : members) {
            player.sendMessage(ChatColor.GRAY + "- " + name);
        }
        
        return true;
    }
    
    /**
     * パーティーを離脱
     */
    private boolean handleLeave(Player player) {
        PartyManager pm = plugin.getPartyManager();
        pm.leave(player.getUniqueId());
        return true;
    }
    
    /**
     * パーティーを解散
     */
    private boolean handleDisband(Player player) {
        PartyManager pm = plugin.getPartyManager();
        pm.disband(player.getUniqueId());
        return true;
    }
    
    /**
     * 招待を承諾
     */
    private boolean handleAccept(Player player) {
        PartyManager pm = plugin.getPartyManager();
        pm.acceptInvite(player.getUniqueId());
        return true;
    }
    
    /**
     * 招待を拒否
     */
    private boolean handleDeny(Player player) {
        PartyManager pm = plugin.getPartyManager();
        pm.denyInvite(player.getUniqueId());
        return true;
    }
    
    /**
     * クリック可能な招待メッセージを送信（PartyManagerから呼び出される）
     */
    public static void sendClickableInvite(Player invitee, String inviterName) {
        // ヘッダーメッセージ
        TextComponent header = new TextComponent(ChatColor.GOLD + "[Party] " + ChatColor.YELLOW + 
                inviterName + ChatColor.WHITE + " があなたをパーティーに招待しました！");
        invitee.spigot().sendMessage(header);
        
        // 承諾ボタン
        TextComponent accept = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "[承諾]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/p accept"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatColor.GREEN + "クリックして招待を承諾").create()));
        
        // スペース
        TextComponent space = new TextComponent("  ");
        
        // 拒否ボタン
        TextComponent deny = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "[拒否]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/p deny"));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatColor.RED + "クリックして招待を拒否").create()));
        
        // ボタンを結合して送信
        invitee.spigot().sendMessage(accept, space, deny);
    }
}
