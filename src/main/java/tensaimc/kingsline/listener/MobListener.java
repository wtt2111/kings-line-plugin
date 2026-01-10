package tensaimc.kingsline.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.arena.Arena;

/**
 * モブスポーン制御リスナー
 * ゲーム中はバニラモブの自然スポーンを無効化
 */
public class MobListener implements Listener {
    
    private final KingsLine plugin;
    
    public MobListener(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        SpawnReason reason = event.getSpawnReason();
        
        // プレイヤーは常に許可
        if (entity instanceof Player) {
            return;
        }
        
        // プラグインからのスポーン（NPC村人など）は許可
        if (reason == SpawnReason.CUSTOM || reason == SpawnReason.SPAWNER_EGG) {
            return;
        }
        
        Arena arena = plugin.getArenaConfig().getCurrentArena();
        
        // アリーナが設定されていない場合は何もしない
        if (arena == null || arena.getWorldName() == null) {
            return;
        }
        
        // ゲームワールドでのモブスポーンを制御
        String worldName = event.getLocation().getWorld().getName();
        if (!worldName.equals(arena.getWorldName())) {
            return;
        }
        
        // ゲーム中（または待機中）はバニラモブのスポーンをキャンセル
        // 自然スポーン、スポナー、チャンク生成などをブロック
        switch (reason) {
            case NATURAL:
            case SPAWNER:
            case CHUNK_GEN:
            case DEFAULT:
            case JOCKEY:
            case MOUNT:
            case VILLAGE_DEFENSE:
            case VILLAGE_INVASION:
            case LIGHTNING:
            case SLIME_SPLIT:
            case REINFORCEMENTS:
            case NETHER_PORTAL:
            case BREEDING:
            case BUILD_IRONGOLEM:
            case BUILD_SNOWMAN:
            case BUILD_WITHER:
            case INFECTION:
            case CURED:
            case OCELOT_BABY:
            case EGG:
                event.setCancelled(true);
                break;
            default:
                // その他の理由は許可
                break;
        }
    }
    
    /**
     * NPCへのダメージをキャンセル（無敵化）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        
        // ゲームNPCへのダメージをキャンセル
        if (plugin.getNPCManager().isGameNPC(entity)) {
            event.setCancelled(true);
        }
    }
}
