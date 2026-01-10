package tensaimc.kingsline.npc;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.World;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;
import tensaimc.kingsline.player.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NPC（Villager）管理クラス
 */
public class NPCManager {
    
    private final KingsLine plugin;
    private final List<UUID> spawnedNPCs;
    
    public NPCManager(KingsLine plugin) {
        this.plugin = plugin;
        this.spawnedNPCs = new ArrayList<>();
    }
    
    /**
     * ゲーム開始時にNPCをスポーン
     */
    public void spawnNPCs(Arena arena) {
        if (arena == null) {
            return;
        }
        
        // Blue NPC
        Location blueNpcLoc = arena.getBlueNPC();
        if (blueNpcLoc != null && blueNpcLoc.getWorld() != null) {
            spawnNPC(blueNpcLoc, Team.BLUE);
        }
        
        // Red NPC
        Location redNpcLoc = arena.getRedNPC();
        if (redNpcLoc != null && redNpcLoc.getWorld() != null) {
            spawnNPC(redNpcLoc, Team.RED);
        }
    }
    
    /**
     * NPCをスポーン
     */
    private void spawnNPC(Location location, Team team) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        
        // 設定
        villager.setCustomName(team.getChatColor() + "" + ChatColor.BOLD + "ショップ / 銀行");
        villager.setCustomNameVisible(true);
        
        // 職業設定
        try {
            villager.setProfession(Villager.Profession.LIBRARIAN);
        } catch (Exception ignored) {
            // バージョンによっては異なる
        }
        
        // 1.8.8対応：NMSでNoAIを設定
        setNoAI(villager);
        
        spawnedNPCs.add(villager.getUniqueId());
        
        plugin.getLogger().info(team.getDisplayName() + " NPC をスポーンしました");
    }
    
    /**
     * NMSを使用してNoAIを設定（1.8.8対応）
     */
    private void setNoAI(Entity entity) {
        try {
            Object nmsEntity = entity.getClass().getMethod("getHandle").invoke(entity);
            
            // NBTTagCompoundを取得または作成
            Class<?> nbtClass = Class.forName("net.minecraft.server.v1_8_R3.NBTTagCompound");
            Object nbtTag = nbtClass.newInstance();
            
            // エンティティのNBTを書き込み
            nmsEntity.getClass().getMethod("c", nbtClass).invoke(nmsEntity, nbtTag);
            
            // NoAI=1を設定
            nbtClass.getMethod("setInt", String.class, int.class).invoke(nbtTag, "NoAI", 1);
            
            // NBTをエンティティに適用
            nmsEntity.getClass().getMethod("f", nbtClass).invoke(nmsEntity, nbtTag);
            
        } catch (Exception e) {
            plugin.getLogger().warning("NoAI設定に失敗: " + e.getMessage());
        }
    }
    
    /**
     * 全NPCを削除
     */
    public void removeAllNPCs() {
        for (UUID npcId : spawnedNPCs) {
            // 全ワールドから検索
            for (World world : plugin.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getUniqueId().equals(npcId)) {
                        entity.remove();
                        break;
                    }
                }
            }
        }
        spawnedNPCs.clear();
    }
    
    /**
     * アリーナワールド内の全ての村人を削除（ゲーム開始前のクリーンアップ）
     */
    public void cleanupVillagers(World world) {
        if (world == null) {
            return;
        }
        
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Villager) {
                entity.remove();
            }
        }
        plugin.getLogger().info("ワールド内の村人をクリーンアップしました");
    }
    
    /**
     * EntityがゲームNPCかどうか判定
     */
    public boolean isGameNPC(Entity entity) {
        return entity != null && spawnedNPCs.contains(entity.getUniqueId());
    }
    
    /**
     * NPCのチームを取得
     */
    public Team getNPCTeam(Entity entity) {
        if (entity == null || !isGameNPC(entity)) {
            return Team.NONE;
        }
        
        Arena arena = plugin.getGameManager().getCurrentArena();
        if (arena == null) {
            return Team.NONE;
        }
        
        Location npcLoc = entity.getLocation();
        Location blueNpcLoc = arena.getBlueNPC();
        Location redNpcLoc = arena.getRedNPC();
        
        if (blueNpcLoc != null && npcLoc.distance(blueNpcLoc) < 5) {
            return Team.BLUE;
        }
        if (redNpcLoc != null && npcLoc.distance(redNpcLoc) < 5) {
            return Team.RED;
        }
        
        return Team.NONE;
    }
}
