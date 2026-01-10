package tensaimc.kingsline.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.config.ArenaConfig;
import tensaimc.kingsline.game.GameManager;
import tensaimc.kingsline.game.GameState;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.PartyManager;
import tensaimc.kingsline.player.Team;

import java.util.List;

/**
 * /kl コマンド
 */
public class KLCommand implements CommandExecutor {
    
    private final KingsLine plugin;
    
    public KLCommand(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "start":
                return handleStart(sender);
            case "stop":
                return handleStop(sender);
            case "reload":
                return handleReload(sender);
            case "party":
                return handleParty(sender, args);
            case "setspawn":
                return handleSetSpawn(sender, args);
            case "setcore":
                return handleSetCore(sender, args);
            case "setnpc":
                return handleSetNPC(sender, args);
            case "setarea":
                return handleSetArea(sender, args);
            case "setlobby":
                return handleSetLobby(sender);
            case "save":
                return handleSave(sender);
            case "createarena":
                return handleCreateArena(sender, args);
            case "setarena":
                return handleSetArena(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "debug":
                return handleDebug(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== King's Line Commands =====");
        sender.sendMessage(ChatColor.YELLOW + "/kl start" + ChatColor.GRAY + " - ゲームを開始");
        sender.sendMessage(ChatColor.YELLOW + "/kl stop" + ChatColor.GRAY + " - ゲームを強制終了");
        sender.sendMessage(ChatColor.YELLOW + "/kl party <invite|accept|deny|leave|list|disband>" + ChatColor.GRAY + " - パーティー");
        
        if (sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/kl reload" + ChatColor.GRAY + " - 設定をリロード");
            sender.sendMessage(ChatColor.YELLOW + "/kl setspawn <blue|red>" + ChatColor.GRAY + " - スポーン設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl setcore <blue|red>" + ChatColor.GRAY + " - コア設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl setnpc <blue|red>" + ChatColor.GRAY + " - NPC位置設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl setarea <A|B|C> <pos1|pos2>" + ChatColor.GRAY + " - エリア設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl setlobby" + ChatColor.GRAY + " - ロビー設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl save" + ChatColor.GRAY + " - 設定を保存");
            sender.sendMessage(ChatColor.YELLOW + "/kl createarena <name>" + ChatColor.GRAY + " - アリーナ作成");
            sender.sendMessage(ChatColor.YELLOW + "/kl setarena <name>" + ChatColor.GRAY + " - アリーナ切替");
            sender.sendMessage(ChatColor.YELLOW + "/kl info" + ChatColor.GRAY + " - ゲーム情報");
            sender.sendMessage(ChatColor.YELLOW + "/kl info arena" + ChatColor.GRAY + " - アリーナ詳細設定");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "--- デバッグコマンド (OP専用) ---");
            sender.sendMessage(ChatColor.YELLOW + "/kl debug shard <amount>" + ChatColor.GRAY + " - 所持シャード設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl debug lumina <amount>" + ChatColor.GRAY + " - 所持ルミナ設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl debug score <blue|red> <amount>" + ChatColor.GRAY + " - チームスコア設定");
        }
    }
    
    // ========== Game Commands ==========
    
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }
        
        GameManager gm = plugin.getGameManager();
        if (gm.getState() != GameState.WAITING) {
            sender.sendMessage(ChatColor.RED + "ゲームは既に進行中です。");
            return true;
        }
        
        if (gm.startGame()) {
            sender.sendMessage(ChatColor.GREEN + "ゲームを開始しました。");
        } else {
            sender.sendMessage(ChatColor.RED + "ゲームの開始に失敗しました。");
        }
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }
        
        GameManager gm = plugin.getGameManager();
        if (gm.getState() == GameState.WAITING) {
            sender.sendMessage(ChatColor.RED + "ゲームは開始されていません。");
            return true;
        }
        
        gm.forceStop();
        sender.sendMessage(ChatColor.YELLOW + "ゲームを強制終了しました。");
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }
        
        plugin.getConfigManager().reload();
        plugin.getArenaConfig().reload();
        sender.sendMessage(ChatColor.GREEN + "設定をリロードしました。");
        
        return true;
    }
    
    // ========== Party Commands ==========
    
    private boolean handleParty(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ使用可能です。");
            return true;
        }
        
        Player player = (Player) sender;
        PartyManager pm = plugin.getPartyManager();
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "/kl party <invite|accept|deny|leave|list|disband>");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "invite":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "/kl party invite <player>");
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
                    return true;
                }
                pm.invite(player.getUniqueId(), target.getUniqueId());
                break;
                
            case "accept":
                pm.acceptInvite(player.getUniqueId());
                break;
                
            case "deny":
                pm.denyInvite(player.getUniqueId());
                break;
                
            case "leave":
                pm.leave(player.getUniqueId());
                break;
                
            case "list":
                if (!pm.isInParty(player.getUniqueId())) {
                    sender.sendMessage(ChatColor.YELLOW + "パーティーに所属していません。");
                } else {
                    List<String> members = pm.getPartyMemberNames(player.getUniqueId());
                    sender.sendMessage(ChatColor.GOLD + "===== パーティーメンバー =====");
                    for (String name : members) {
                        sender.sendMessage(ChatColor.GRAY + "- " + name);
                    }
                }
                break;
                
            case "disband":
                pm.disband(player.getUniqueId());
                break;
                
            default:
                sender.sendMessage(ChatColor.YELLOW + "/kl party <invite|accept|deny|leave|list|disband>");
        }
        
        return true;
    }
    
    // ========== Setup Commands ==========
    
    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (!checkAdminPlayer(sender)) return true;
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/kl setspawn <blue|red>");
            return true;
        }
        
        Player player = (Player) sender;
        Arena arena = getOrCreateArena(player);
        String team = args[1].toLowerCase();
        
        if (team.equals("blue")) {
            arena.setBlueSpawn(player.getLocation());
            arena.setWorldName(player.getWorld().getName());
            sender.sendMessage(ChatColor.GREEN + "Blueチームのスポーンを設定しました。");
        } else if (team.equals("red")) {
            arena.setRedSpawn(player.getLocation());
            arena.setWorldName(player.getWorld().getName());
            sender.sendMessage(ChatColor.GREEN + "Redチームのスポーンを設定しました。");
        } else {
            sender.sendMessage(ChatColor.RED + "blue または red を指定してください。");
        }
        
        return true;
    }
    
    private boolean handleSetCore(CommandSender sender, String[] args) {
        if (!checkAdminPlayer(sender)) return true;
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/kl setcore <blue|red>");
            return true;
        }
        
        Player player = (Player) sender;
        Arena arena = getOrCreateArena(player);
        String team = args[1].toLowerCase();
        
        // 見ている先のブロックを取得（最大50ブロック先まで）
        org.bukkit.block.Block targetBlock = player.getTargetBlock((java.util.Set<org.bukkit.Material>) null, 50);
        if (targetBlock == null || targetBlock.getType() == org.bukkit.Material.AIR) {
            sender.sendMessage(ChatColor.RED + "ブロックを見てください。");
            return true;
        }
        
        Location coreLoc = targetBlock.getLocation();
        
        if (team.equals("blue")) {
            arena.setBlueCore(coreLoc);
            sender.sendMessage(ChatColor.GREEN + "Blueチームのコアを設定しました。(" + 
                    coreLoc.getBlockX() + ", " + coreLoc.getBlockY() + ", " + coreLoc.getBlockZ() + ")");
        } else if (team.equals("red")) {
            arena.setRedCore(coreLoc);
            sender.sendMessage(ChatColor.GREEN + "Redチームのコアを設定しました。(" + 
                    coreLoc.getBlockX() + ", " + coreLoc.getBlockY() + ", " + coreLoc.getBlockZ() + ")");
        } else {
            sender.sendMessage(ChatColor.RED + "blue または red を指定してください。");
        }
        
        return true;
    }
    
    private boolean handleSetNPC(CommandSender sender, String[] args) {
        if (!checkAdminPlayer(sender)) return true;
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/kl setnpc <blue|red>");
            return true;
        }
        
        Player player = (Player) sender;
        Arena arena = getOrCreateArena(player);
        String team = args[1].toLowerCase();
        
        if (team.equals("blue")) {
            arena.setBlueNPC(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "BlueチームのNPC位置を設定しました。");
        } else if (team.equals("red")) {
            arena.setRedNPC(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "RedチームのNPC位置を設定しました。");
        } else {
            sender.sendMessage(ChatColor.RED + "blue または red を指定してください。");
        }
        
        return true;
    }
    
    private boolean handleSetArea(CommandSender sender, String[] args) {
        if (!checkAdminPlayer(sender)) return true;
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "/kl setarea <A|B|C> <pos1|pos2>");
            return true;
        }
        
        Player player = (Player) sender;
        Arena arena = getOrCreateArena(player);
        String areaId = args[1].toUpperCase();
        String pos = args[2].toLowerCase();
        
        tensaimc.kingsline.arena.Area area = arena.getArea(areaId);
        if (area == null) {
            sender.sendMessage(ChatColor.RED + "A, B, C のいずれかを指定してください。");
            return true;
        }
        
        Location loc = player.getLocation();
        
        if (pos.equals("pos1")) {
            area.setPos1(loc);
            sender.sendMessage(ChatColor.GREEN + areaId + "エリアのpos1を設定しました。");
        } else if (pos.equals("pos2")) {
            area.setPos2(loc);
            sender.sendMessage(ChatColor.GREEN + areaId + "エリアのpos2を設定しました。");
        } else {
            sender.sendMessage(ChatColor.RED + "pos1 または pos2 を指定してください。");
        }
        
        return true;
    }
    
    private boolean handleSetLobby(CommandSender sender) {
        if (!checkAdminPlayer(sender)) return true;
        
        Player player = (Player) sender;
        Arena arena = getOrCreateArena(player);
        
        arena.setLobby(player.getLocation());
        sender.sendMessage(ChatColor.GREEN + "ロビーを設定しました。");
        
        return true;
    }
    
    private boolean handleSave(CommandSender sender) {
        if (!sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }
        
        plugin.getArenaConfig().saveConfig();
        sender.sendMessage(ChatColor.GREEN + "設定を保存しました。");
        
        return true;
    }
    
    private boolean handleCreateArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/kl createarena <name>");
            return true;
        }
        
        String name = args[1];
        ArenaConfig ac = plugin.getArenaConfig();
        
        if (ac.arenaExists(name)) {
            sender.sendMessage(ChatColor.RED + "そのアリーナは既に存在します。");
            return true;
        }
        
        ac.createArena(name);
        ac.setCurrentArena(name);
        sender.sendMessage(ChatColor.GREEN + "アリーナ '" + name + "' を作成し、選択しました。");
        
        return true;
    }
    
    private boolean handleSetArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/kl setarena <name>");
            sender.sendMessage(ChatColor.GRAY + "利用可能: " + plugin.getArenaConfig().getArenaNames());
            return true;
        }
        
        String name = args[1];
        ArenaConfig ac = plugin.getArenaConfig();
        
        if (!ac.arenaExists(name)) {
            sender.sendMessage(ChatColor.RED + "そのアリーナは存在しません。");
            return true;
        }
        
        ac.setCurrentArena(name);
        sender.sendMessage(ChatColor.GREEN + "アリーナを '" + name + "' に切り替えました。");
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        // /kl info arena - アリーナ詳細情報
        if (args.length >= 2 && args[1].equalsIgnoreCase("arena")) {
            return handleInfoArena(sender);
        }
        
        // 通常のゲーム情報
        GameManager gm = plugin.getGameManager();
        Arena arena = plugin.getArenaConfig().getCurrentArena();
        
        sender.sendMessage(ChatColor.GOLD + "===== King's Line Info =====");
        sender.sendMessage(ChatColor.YELLOW + "状態: " + ChatColor.WHITE + gm.getState().getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "プレイヤー数: " + ChatColor.WHITE + gm.getPlayerCount());
        sender.sendMessage(ChatColor.YELLOW + "スコア: " + 
                ChatColor.BLUE + "BLUE " + gm.getScore(tensaimc.kingsline.player.Team.BLUE) + 
                ChatColor.WHITE + " - " + 
                ChatColor.RED + gm.getScore(tensaimc.kingsline.player.Team.RED) + " RED");
        
        if (arena != null) {
            sender.sendMessage(ChatColor.YELLOW + "アリーナ: " + ChatColor.WHITE + arena.getName());
            sender.sendMessage(ChatColor.YELLOW + "有効: " + ChatColor.WHITE + arena.isValid());
        } else {
            sender.sendMessage(ChatColor.YELLOW + "アリーナ: " + ChatColor.RED + "未設定");
        }
        
        sender.sendMessage(ChatColor.GRAY + "詳細情報: /kl info arena");
        
        return true;
    }
    
    private boolean handleInfoArena(CommandSender sender) {
        Arena arena = plugin.getArenaConfig().getCurrentArena();
        
        sender.sendMessage(ChatColor.GOLD + "===== アリーナ設定情報 =====");
        
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "アリーナが設定されていません。");
            sender.sendMessage(ChatColor.GRAY + "/kl createarena <name> で作成してください。");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "アリーナ名: " + ChatColor.WHITE + arena.getName());
        sender.sendMessage(ChatColor.YELLOW + "ワールド: " + ChatColor.WHITE + 
                (arena.getWorldName() != null ? arena.getWorldName() : ChatColor.RED + "未設定"));
        sender.sendMessage(ChatColor.YELLOW + "状態: " + 
                (arena.isValid() ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効（設定不足）"));
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.BLUE + "【BLUEチーム】");
        sender.sendMessage(ChatColor.GRAY + "  スポーン: " + formatLocation(arena.getBlueSpawn()));
        sender.sendMessage(ChatColor.GRAY + "  コア: " + formatLocation(arena.getBlueCore()));
        sender.sendMessage(ChatColor.GRAY + "  NPC: " + formatLocation(arena.getBlueNPC()));
        
        sender.sendMessage(ChatColor.RED + "【REDチーム】");
        sender.sendMessage(ChatColor.GRAY + "  スポーン: " + formatLocation(arena.getRedSpawn()));
        sender.sendMessage(ChatColor.GRAY + "  コア: " + formatLocation(arena.getRedCore()));
        sender.sendMessage(ChatColor.GRAY + "  NPC: " + formatLocation(arena.getRedNPC()));
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "【エリア設定】");
        
        // エリアA
        tensaimc.kingsline.arena.Area areaA = arena.getAreaA();
        if (areaA != null && areaA.isValid()) {
            sender.sendMessage(ChatColor.GRAY + "  エリアA: " + ChatColor.GREEN + "設定済み" + 
                    ChatColor.GRAY + " (" + formatAreaRange(areaA) + ")");
        } else {
            sender.sendMessage(ChatColor.GRAY + "  エリアA: " + ChatColor.RED + "未設定");
        }
        
        // エリアB（必須）
        tensaimc.kingsline.arena.Area areaB = arena.getAreaB();
        if (areaB != null && areaB.isValid()) {
            sender.sendMessage(ChatColor.GRAY + "  エリアB: " + ChatColor.GREEN + "設定済み" + 
                    ChatColor.GRAY + " (" + formatAreaRange(areaB) + ")");
        } else {
            sender.sendMessage(ChatColor.GRAY + "  エリアB: " + ChatColor.RED + "未設定 " + 
                    ChatColor.DARK_RED + "（必須！）");
        }
        
        // エリアC
        tensaimc.kingsline.arena.Area areaC = arena.getAreaC();
        if (areaC != null && areaC.isValid()) {
            sender.sendMessage(ChatColor.GRAY + "  エリアC: " + ChatColor.GREEN + "設定済み" + 
                    ChatColor.GRAY + " (" + formatAreaRange(areaC) + ")");
        } else {
            sender.sendMessage(ChatColor.GRAY + "  エリアC: " + ChatColor.RED + "未設定");
        }
        
        // ロビー
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "【その他】");
        sender.sendMessage(ChatColor.GRAY + "  ロビー: " + formatLocation(arena.getLobby()));
        
        return true;
    }
    
    private String formatLocation(Location loc) {
        if (loc == null) {
            return ChatColor.RED + "未設定";
        }
        return ChatColor.WHITE + String.format("(%.1f, %.1f, %.1f)", 
                loc.getX(), loc.getY(), loc.getZ());
    }
    
    private String formatAreaRange(tensaimc.kingsline.arena.Area area) {
        if (area == null || area.getPos1() == null || area.getPos2() == null) {
            return "不明";
        }
        Location p1 = area.getPos1();
        Location p2 = area.getPos2();
        return String.format("%d,%d,%d ~ %d,%d,%d",
                p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
    }
    
    // ========== Helpers ==========
    
    private boolean checkAdminPlayer(CommandSender sender) {
        if (!sender.hasPermission("kingsline.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ使用可能です。");
            return false;
        }
        return true;
    }
    
    private Arena getOrCreateArena(Player player) {
        ArenaConfig ac = plugin.getArenaConfig();
        Arena arena = ac.getCurrentArena();
        if (arena == null) {
            arena = ac.createArena("default");
            ac.setCurrentArena("default");
        }
        return arena;
    }
    
    // ========== Debug Commands (OP Only) ==========
    
    private boolean handleDebug(CommandSender sender, String[] args) {
        // OP権限チェック
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ使用可能です。");
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.isOp()) {
            sender.sendMessage(ChatColor.RED + "OP権限が必要です。");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "===== デバッグコマンド =====");
            sender.sendMessage(ChatColor.YELLOW + "/kl debug shard <amount>" + ChatColor.GRAY + " - 所持シャードを設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl debug lumina <amount>" + ChatColor.GRAY + " - 所持ルミナを設定");
            sender.sendMessage(ChatColor.YELLOW + "/kl debug score <blue|red> <amount>" + ChatColor.GRAY + " - チームスコアを設定");
            return true;
        }
        
        String type = args[1].toLowerCase();
        
        switch (type) {
            case "shard":
                return handleDebugShard(player, args);
            case "lumina":
                return handleDebugLumina(player, args);
            case "score":
                return handleDebugScore(player, args);
            default:
                sender.sendMessage(ChatColor.RED + "不明なサブコマンド: " + type);
                sender.sendMessage(ChatColor.GRAY + "使用可能: shard, lumina, score");
                return true;
        }
    }
    
    private boolean handleDebugShard(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "使用法: /kl debug shard <amount>");
            return true;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "無効な数値です。");
            return true;
        }
        
        GameManager gm = plugin.getGameManager();
        KLPlayer klPlayer = gm.getPlayer(player);
        
        if (klPlayer == null) {
            player.sendMessage(ChatColor.RED + "ゲームに参加していません。");
            return true;
        }
        
        klPlayer.setShardCarrying(amount);
        player.sendMessage(ChatColor.GREEN + "所持シャードを " + ChatColor.AQUA + amount + ChatColor.GREEN + " に設定しました。");
        
        return true;
    }
    
    private boolean handleDebugLumina(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "使用法: /kl debug lumina <amount>");
            return true;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "無効な数値です。");
            return true;
        }
        
        GameManager gm = plugin.getGameManager();
        KLPlayer klPlayer = gm.getPlayer(player);
        
        if (klPlayer == null) {
            player.sendMessage(ChatColor.RED + "ゲームに参加していません。");
            return true;
        }
        
        klPlayer.setLuminaCarrying(amount);
        player.sendMessage(ChatColor.GREEN + "所持ルミナを " + ChatColor.LIGHT_PURPLE + amount + ChatColor.GREEN + " に設定しました。");
        
        return true;
    }
    
    private boolean handleDebugScore(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "使用法: /kl debug score <blue|red> <amount>");
            return true;
        }
        
        String teamName = args[2].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "無効な数値です。");
            return true;
        }
        
        GameManager gm = plugin.getGameManager();
        
        Team team;
        if (teamName.equals("blue")) {
            team = Team.BLUE;
        } else if (teamName.equals("red")) {
            team = Team.RED;
        } else {
            player.sendMessage(ChatColor.RED + "blue または red を指定してください。");
            return true;
        }
        
        // スコアを直接設定するメソッドを呼ぶ
        gm.setScore(team, amount);
        player.sendMessage(ChatColor.GREEN + team.getColoredName() + ChatColor.GREEN + " のスコアを " + 
                ChatColor.WHITE + amount + ChatColor.GREEN + " に設定しました。");
        
        return true;
    }
}
