package tensaimc.kingsline.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;

import java.util.*;

/**
 * キング投票GUI
 * 立候補者のプレイヤーヘッドを表示し、クリックで投票
 */
public class KingVoteGUI {
    
    private static final String TITLE_BLUE = ChatColor.BLUE + "👑 キング投票 (BLUE)";
    private static final String TITLE_RED = ChatColor.RED + "👑 キング投票 (RED)";
    
    private final KingsLine plugin;
    
    // 投票データ
    private final Map<UUID, UUID> blueVotes;  // 投票者 -> 立候補者
    private final Map<UUID, UUID> redVotes;
    private final Set<UUID> blueCandidates;
    private final Set<UUID> redCandidates;
    
    public KingVoteGUI(KingsLine plugin) {
        this.plugin = plugin;
        this.blueVotes = new HashMap<>();
        this.redVotes = new HashMap<>();
        this.blueCandidates = new HashSet<>();
        this.redCandidates = new HashSet<>();
    }
    
    /**
     * 投票データをリセット
     */
    public void reset() {
        blueVotes.clear();
        redVotes.clear();
        blueCandidates.clear();
        redCandidates.clear();
    }
    
    /**
     * 立候補者を追加
     */
    public void addCandidate(UUID uuid, Team team) {
        if (team == Team.BLUE) {
            blueCandidates.add(uuid);
        } else if (team == Team.RED) {
            redCandidates.add(uuid);
        }
    }
    
    /**
     * 立候補者一覧を取得
     */
    public Set<UUID> getCandidates(Team team) {
        return team == Team.BLUE ? blueCandidates : redCandidates;
    }
    
    /**
     * GUIを開く
     */
    public void open(Player player) {
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        Team team = klPlayer.getTeam();
        Set<UUID> candidates = getCandidates(team);
        Map<UUID, UUID> votes = team == Team.BLUE ? blueVotes : redVotes;
        
        String title = team == Team.BLUE ? TITLE_BLUE : TITLE_RED;
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        // 背景
        ItemStack gray = createFillerItem(Material.STAINED_GLASS_PANE, (short) 7);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, gray);
        }
        
        if (candidates.isEmpty()) {
            // 立候補者なし
            ItemStack noCandidate = new ItemStack(Material.BARRIER);
            ItemMeta meta = noCandidate.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "立候補者がいません");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "!king とチャットで発言して");
            lore.add(ChatColor.GRAY + "キングに立候補しましょう！");
            meta.setLore(lore);
            noCandidate.setItemMeta(meta);
            inv.setItem(13, noCandidate);
        } else {
            // 立候補者のヘッドを配置
            int slot = 10;
            for (UUID candidateId : candidates) {
                if (slot > 16) break; // 最大7人まで
                
                KLPlayer candidate = plugin.getGameManager().getPlayer(candidateId);
                if (candidate == null || !candidate.isOnline()) continue;
                
                int voteCount = countVotes(candidateId, votes);
                boolean hasVoted = votes.containsKey(player.getUniqueId()) && 
                                   votes.get(player.getUniqueId()).equals(candidateId);
                
                inv.setItem(slot, createCandidateHead(candidate, voteCount, hasVoted));
                slot++;
            }
        }
        
        // 自分の投票状態を表示
        UUID myVote = votes.get(player.getUniqueId());
        ItemStack voteInfo = new ItemStack(Material.PAPER);
        ItemMeta voteInfoMeta = voteInfo.getItemMeta();
        if (myVote != null) {
            KLPlayer votedFor = plugin.getGameManager().getPlayer(myVote);
            String name = votedFor != null ? votedFor.getName() : "Unknown";
            voteInfoMeta.setDisplayName(ChatColor.GREEN + "あなたの投票: " + name);
        } else {
            voteInfoMeta.setDisplayName(ChatColor.YELLOW + "まだ投票していません");
        }
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "立候補者のヘッドをクリックで投票");
        voteInfoMeta.setLore(infoLore);
        voteInfo.setItemMeta(voteInfoMeta);
        inv.setItem(22, voteInfo);
        
        player.openInventory(inv);
        plugin.getGUIManager().setOpenGUI(player.getUniqueId(), GUIManager.GUIType.KING_VOTE);
    }
    
    /**
     * 立候補者のヘッドを作成
     */
    private ItemStack createCandidateHead(KLPlayer candidate, int voteCount, boolean hasVoted) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        Player player = candidate.getPlayer();
        if (player != null) {
            meta.setOwner(player.getName());
        }
        
        String prefix = hasVoted ? ChatColor.GREEN + "✓ " : "";
        meta.setDisplayName(prefix + ChatColor.GOLD + candidate.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "キング立候補者");
        lore.add("");
        lore.add(ChatColor.YELLOW + "現在の得票数: " + ChatColor.WHITE + voteCount + "票");
        lore.add("");
        if (hasVoted) {
            lore.add(ChatColor.GREEN + "あなたはこの人に投票済み");
        } else {
            lore.add(ChatColor.AQUA + "クリックで投票");
        }
        meta.setLore(lore);
        
        head.setItemMeta(meta);
        return head;
    }
    
    /**
     * 投票数をカウント
     */
    private int countVotes(UUID candidateId, Map<UUID, UUID> votes) {
        int count = 0;
        for (UUID votedFor : votes.values()) {
            if (votedFor.equals(candidateId)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * クリック処理
     */
    public void handleClick(Player player, int slot) {
        KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
        if (klPlayer == null) {
            return;
        }
        
        Team team = klPlayer.getTeam();
        Set<UUID> candidates = getCandidates(team);
        Map<UUID, UUID> votes = team == Team.BLUE ? blueVotes : redVotes;
        
        // 立候補者のスロット（10-16）
        if (slot < 10 || slot > 16) {
            return;
        }
        
        // スロットから立候補者を特定
        int index = slot - 10;
        List<UUID> candidateList = new ArrayList<>(candidates);
        if (index >= candidateList.size()) {
            return;
        }
        
        UUID candidateId = candidateList.get(index);
        
        // 投票
        votes.put(player.getUniqueId(), candidateId);
        
        KLPlayer candidate = plugin.getGameManager().getPlayer(candidateId);
        String name = candidate != null ? candidate.getName() : "Unknown";
        
        player.sendMessage(ChatColor.GREEN + name + " に投票しました！");
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.2f);
        
        // GUIを更新
        open(player);
    }
    
    /**
     * 最多得票者を取得
     */
    public UUID getWinner(Team team) {
        Map<UUID, UUID> votes = team == Team.BLUE ? blueVotes : redVotes;
        Set<UUID> candidates = getCandidates(team);
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        UUID winner = null;
        int maxVotes = -1;
        
        for (UUID candidateId : candidates) {
            int count = countVotes(candidateId, votes);
            if (count > maxVotes) {
                maxVotes = count;
                winner = candidateId;
            }
        }
        
        // 投票が0の場合でも立候補者がいればその中からランダム
        if (winner == null && !candidates.isEmpty()) {
            List<UUID> list = new ArrayList<>(candidates);
            winner = list.get(new Random().nextInt(list.size()));
        }
        
        return winner;
    }
    
    /**
     * フィラーアイテム作成
     */
    private ItemStack createFillerItem(Material material, short data) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * このGUIのタイトルかどうか
     */
    public static boolean isThisGUI(String title) {
        return TITLE_BLUE.equals(title) || TITLE_RED.equals(title);
    }
}
