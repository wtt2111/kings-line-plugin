package tensaimc.kingsline.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.command.PartyCommand;

import java.util.*;

/**
 * パーティー管理クラス
 */
public class PartyManager {
    
    private final KingsLine plugin;
    
    // パーティーID -> メンバーUUID
    private final Map<UUID, Set<UUID>> parties;
    
    // プレイヤーUUID -> パーティーID
    private final Map<UUID, UUID> playerParty;
    
    // パーティーID -> リーダーUUID
    private final Map<UUID, UUID> partyLeaders;
    
    // 招待: 招待されたプレイヤー -> 招待元パーティーID
    private final Map<UUID, UUID> pendingInvites;
    
    public PartyManager(KingsLine plugin) {
        this.plugin = plugin;
        this.parties = new HashMap<>();
        this.playerParty = new HashMap<>();
        this.partyLeaders = new HashMap<>();
        this.pendingInvites = new HashMap<>();
    }
    
    /**
     * プレイヤーのパーティーIDを取得
     */
    public UUID getPartyId(UUID playerUuid) {
        return playerParty.get(playerUuid);
    }
    
    /**
     * パーティーに所属しているか
     */
    public boolean isInParty(UUID playerUuid) {
        return playerParty.containsKey(playerUuid);
    }
    
    /**
     * パーティーリーダーかどうか
     */
    public boolean isPartyLeader(UUID playerUuid) {
        UUID partyId = getPartyId(playerUuid);
        return partyId != null && playerUuid.equals(partyLeaders.get(partyId));
    }
    
    /**
     * パーティーメンバーを取得
     */
    public Set<UUID> getPartyMembers(UUID partyId) {
        return parties.getOrDefault(partyId, Collections.emptySet());
    }
    
    /**
     * パーティーの現在の人数を取得
     */
    public int getPartySize(UUID playerUuid) {
        UUID partyId = getPartyId(playerUuid);
        if (partyId == null) {
            return 0;
        }
        Set<UUID> members = parties.get(partyId);
        return members != null ? members.size() : 0;
    }
    
    /**
     * プレイヤーを招待
     */
    public boolean invite(UUID inviterUuid, UUID inviteeUuid) {
        Player inviter = Bukkit.getPlayer(inviterUuid);
        Player invitee = Bukkit.getPlayer(inviteeUuid);
        
        if (inviter == null || invitee == null) {
            return false;
        }
        
        // 招待された側が既にパーティーにいる
        if (isInParty(inviteeUuid)) {
            inviter.sendMessage(ChatColor.RED + invitee.getName() + " は既にパーティーに所属しています。");
            return false;
        }
        
        // 招待者がパーティーにいなければ新規作成
        UUID partyId = getPartyId(inviterUuid);
        if (partyId == null) {
            partyId = createParty(inviterUuid);
        }
        
        // リーダーでなければ招待不可
        if (!inviterUuid.equals(partyLeaders.get(partyId))) {
            inviter.sendMessage(ChatColor.RED + "パーティーリーダーのみが招待できます。");
            return false;
        }
        
        // パーティー人数上限チェック
        int maxSize = plugin.getConfigManager().getPartyMaxSize();
        Set<UUID> members = parties.get(partyId);
        if (members != null && members.size() >= maxSize) {
            inviter.sendMessage(ChatColor.RED + "パーティーが満員です（最大" + maxSize + "人）");
            return false;
        }
        
        pendingInvites.put(inviteeUuid, partyId);
        
        inviter.sendMessage(ChatColor.GREEN + invitee.getName() + " をパーティーに招待しました。");
        
        // クリック可能な招待メッセージを送信
        PartyCommand.sendClickableInvite(invitee, inviter.getName());
        
        return true;
    }
    
    /**
     * 招待を承諾
     */
    public boolean acceptInvite(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return false;
        }
        
        UUID partyId = pendingInvites.remove(playerUuid);
        if (partyId == null) {
            player.sendMessage(ChatColor.RED + "有効な招待がありません。");
            return false;
        }
        
        if (!parties.containsKey(partyId)) {
            player.sendMessage(ChatColor.RED + "そのパーティーは既に存在しません。");
            return false;
        }
        
        // パーティー人数上限チェック（招待を受けている間に満員になった場合）
        int maxSize = plugin.getConfigManager().getPartyMaxSize();
        Set<UUID> members = parties.get(partyId);
        if (members != null && members.size() >= maxSize) {
            player.sendMessage(ChatColor.RED + "パーティーが満員になりました。");
            return false;
        }
        
        // パーティーに追加
        parties.get(partyId).add(playerUuid);
        playerParty.put(playerUuid, partyId);
        
        // パーティーメンバーに通知
        broadcastToParty(partyId, ChatColor.GREEN + player.getName() + " がパーティーに参加しました！");
        
        return true;
    }
    
    /**
     * 招待を拒否
     */
    public boolean denyInvite(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return false;
        }
        
        UUID partyId = pendingInvites.remove(playerUuid);
        if (partyId == null) {
            player.sendMessage(ChatColor.RED + "有効な招待がありません。");
            return false;
        }
        
        player.sendMessage(ChatColor.YELLOW + "パーティー招待を拒否しました。");
        
        // リーダーに通知
        UUID leaderId = partyLeaders.get(partyId);
        if (leaderId != null) {
            Player leader = Bukkit.getPlayer(leaderId);
            if (leader != null) {
                leader.sendMessage(ChatColor.RED + player.getName() + " がパーティー招待を拒否しました。");
            }
        }
        
        return true;
    }
    
    /**
     * パーティーを離脱
     */
    public boolean leave(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return false;
        }
        
        UUID partyId = playerParty.remove(playerUuid);
        if (partyId == null) {
            player.sendMessage(ChatColor.RED + "パーティーに所属していません。");
            return false;
        }
        
        Set<UUID> members = parties.get(partyId);
        if (members != null) {
            members.remove(playerUuid);
            
            // パーティーが空になったら削除
            if (members.isEmpty()) {
                parties.remove(partyId);
                partyLeaders.remove(partyId);
            }
            // リーダーが離脱したら次のメンバーをリーダーに
            else if (playerUuid.equals(partyLeaders.get(partyId))) {
                UUID newLeader = members.iterator().next();
                partyLeaders.put(partyId, newLeader);
                Player newLeaderPlayer = Bukkit.getPlayer(newLeader);
                if (newLeaderPlayer != null) {
                    broadcastToParty(partyId, ChatColor.YELLOW + newLeaderPlayer.getName() + " が新しいリーダーになりました。");
                }
            }
        }
        
        broadcastToParty(partyId, ChatColor.RED + player.getName() + " がパーティーを離脱しました。");
        player.sendMessage(ChatColor.YELLOW + "パーティーを離脱しました。");
        
        return true;
    }
    
    /**
     * パーティーを解散
     */
    public boolean disband(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return false;
        }
        
        UUID partyId = getPartyId(playerUuid);
        if (partyId == null) {
            player.sendMessage(ChatColor.RED + "パーティーに所属していません。");
            return false;
        }
        
        if (!isPartyLeader(playerUuid)) {
            player.sendMessage(ChatColor.RED + "パーティーリーダーのみが解散できます。");
            return false;
        }
        
        broadcastToParty(partyId, ChatColor.RED + "パーティーが解散されました。");
        
        // 全メンバーをパーティーから除去
        Set<UUID> members = parties.remove(partyId);
        if (members != null) {
            for (UUID memberId : members) {
                playerParty.remove(memberId);
            }
        }
        partyLeaders.remove(partyId);
        
        return true;
    }
    
    /**
     * パーティーメンバー一覧
     */
    public List<String> getPartyMemberNames(UUID playerUuid) {
        UUID partyId = getPartyId(playerUuid);
        if (partyId == null) {
            return Collections.emptyList();
        }
        
        List<String> names = new ArrayList<>();
        UUID leaderId = partyLeaders.get(partyId);
        
        for (UUID memberId : getPartyMembers(partyId)) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                String name = member.getName();
                if (memberId.equals(leaderId)) {
                    name = ChatColor.GOLD + "★ " + name + ChatColor.RESET;
                }
                names.add(name);
            }
        }
        
        return names;
    }
    
    /**
     * 新しいパーティーを作成
     */
    private UUID createParty(UUID leaderUuid) {
        UUID partyId = UUID.randomUUID();
        Set<UUID> members = new HashSet<>();
        members.add(leaderUuid);
        
        parties.put(partyId, members);
        playerParty.put(leaderUuid, partyId);
        partyLeaders.put(partyId, leaderUuid);
        
        return partyId;
    }
    
    /**
     * パーティーメンバー全員にメッセージを送信
     */
    private void broadcastToParty(UUID partyId, String message) {
        Set<UUID> members = parties.get(partyId);
        if (members == null) {
            return;
        }
        
        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(message);
            }
        }
    }
    
    /**
     * ゲーム終了時にリセット
     */
    public void reset() {
        parties.clear();
        playerParty.clear();
        partyLeaders.clear();
        pendingInvites.clear();
    }
}
