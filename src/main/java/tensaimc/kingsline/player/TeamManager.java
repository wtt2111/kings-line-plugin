package tensaimc.kingsline.player;

import tensaimc.kingsline.KingsLine;

import java.util.*;
import java.util.stream.Collectors;

/**
 * チーム管理クラス
 */
public class TeamManager {
    
    private final KingsLine plugin;
    
    // チームのShardプール
    private int blueTeamShard;
    private int redTeamShard;
    
    public TeamManager(KingsLine plugin) {
        this.plugin = plugin;
        reset();
    }
    
    /**
     * リセット
     */
    public void reset() {
        blueTeamShard = 0;
        redTeamShard = 0;
    }
    
    /**
     * プレイヤーをチームにランダム振り分け（パーティー考慮）
     */
    public void assignTeams(Map<UUID, KLPlayer> players) {
        PartyManager partyManager = plugin.getPartyManager();
        
        List<KLPlayer> playerList = new ArrayList<>(players.values());
        
        // パーティーをグループ化
        Map<UUID, List<KLPlayer>> partyGroups = new HashMap<>();
        List<KLPlayer> soloPlayers = new ArrayList<>();
        
        for (KLPlayer player : playerList) {
            UUID partyId = partyManager.getPartyId(player.getUuid());
            if (partyId != null) {
                partyGroups.computeIfAbsent(partyId, k -> new ArrayList<>()).add(player);
            } else {
                soloPlayers.add(player);
            }
        }
        
        // パーティーを交互にチームに割り当て
        List<List<KLPlayer>> parties = new ArrayList<>(partyGroups.values());
        Collections.shuffle(parties);
        
        int blueCount = 0;
        int redCount = 0;
        
        for (List<KLPlayer> party : parties) {
            Team team = blueCount <= redCount ? Team.BLUE : Team.RED;
            for (KLPlayer player : party) {
                player.setTeam(team);
            }
            if (team == Team.BLUE) {
                blueCount += party.size();
            } else {
                redCount += party.size();
            }
        }
        
        // ソロプレイヤーをバランス調整しながら割り当て
        Collections.shuffle(soloPlayers);
        for (KLPlayer player : soloPlayers) {
            if (blueCount <= redCount) {
                player.setTeam(Team.BLUE);
                blueCount++;
            } else {
                player.setTeam(Team.RED);
                redCount++;
            }
        }
    }
    
    /**
     * 指定チームのプレイヤーを取得
     */
    public List<KLPlayer> getTeamPlayers(Map<UUID, KLPlayer> players, Team team) {
        return players.values().stream()
                .filter(p -> p.getTeam() == team)
                .collect(Collectors.toList());
    }
    
    /**
     * 指定チームの生存プレイヤーを取得
     */
    public List<KLPlayer> getAliveTeamPlayers(Map<UUID, KLPlayer> players, Team team) {
        return players.values().stream()
                .filter(p -> p.getTeam() == team && p.isAlive())
                .collect(Collectors.toList());
    }
    
    /**
     * 指定チームの人数を取得
     */
    public int getTeamSize(Map<UUID, KLPlayer> players, Team team) {
        return (int) players.values().stream()
                .filter(p -> p.getTeam() == team)
                .count();
    }
    
    /**
     * 指定チームの生存人数を取得
     */
    public int getAliveCount(Map<UUID, KLPlayer> players, Team team) {
        return (int) players.values().stream()
                .filter(p -> p.getTeam() == team && p.isAlive())
                .count();
    }
    
    // ========== Team Shard ==========
    
    public int getTeamShard(Team team) {
        switch (team) {
            case BLUE:
                return blueTeamShard;
            case RED:
                return redTeamShard;
            default:
                return 0;
        }
    }
    
    public void addTeamShard(Team team, int amount) {
        switch (team) {
            case BLUE:
                blueTeamShard += amount;
                break;
            case RED:
                redTeamShard += amount;
                break;
        }
    }
    
    public boolean spendTeamShard(Team team, int amount) {
        int current = getTeamShard(team);
        if (current >= amount) {
            switch (team) {
                case BLUE:
                    blueTeamShard -= amount;
                    return true;
                case RED:
                    redTeamShard -= amount;
                    return true;
            }
        }
        return false;
    }
}
