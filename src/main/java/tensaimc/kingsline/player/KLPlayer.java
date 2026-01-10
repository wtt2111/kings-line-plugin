package tensaimc.kingsline.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tensaimc.kingsline.element.Element;

import java.util.UUID;

/**
 * King's Lineのプレイヤーデータ
 */
public class KLPlayer {
    
    private final UUID uuid;
    private Team team;
    private Element element;
    
    // リソース - Shard（チームアップグレード用）
    private int shardCarrying;  // 所持中のShard（未保護、死亡時ドロップ）
    private int shardSaved;     // 貯金済みShard（保護済み）
    
    // リソース - Lumina（個人ショップ用）
    private int luminaCarrying; // 所持中のLumina（未保護、死亡時ドロップ）
    private int luminaSaved;    // 貯金済みLumina（保護済み）
    
    // SPシステム
    private int spGauge;        // SP技ゲージ (0-10)
    private long spCooldownEnd; // SP技クールダウン終了時刻
    private long silenceEnd;    // 沈黙状態終了時刻（SP使用不可）
    
    // キング
    private boolean isKing;
    
    // ゲーム状態
    private boolean canRespawn;
    private boolean isAlive;
    
    // 今回のゲームの統計
    private int killsThisGame;
    private int deathsThisGame;
    private int shardDepositedThisGame;
    
    // 死亡場所（リスポーン待機時間計算用）
    private Location lastDeathLocation;
    
    public KLPlayer(UUID uuid) {
        this.uuid = uuid;
        this.team = Team.NONE;
        this.element = null;
        this.shardCarrying = 0;
        this.shardSaved = 0;
        this.luminaCarrying = 0;
        this.luminaSaved = 0;
        this.spGauge = 0;
        this.spCooldownEnd = 0;
        this.silenceEnd = 0;
        this.isKing = false;
        this.canRespawn = true;
        this.isAlive = true;
        this.killsThisGame = 0;
        this.deathsThisGame = 0;
        this.shardDepositedThisGame = 0;
    }
    
    // ========== Bukkit Player ==========
    
    public UUID getUuid() {
        return uuid;
    }
    
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
    
    public boolean isOnline() {
        return getPlayer() != null && getPlayer().isOnline();
    }
    
    public String getName() {
        Player player = getPlayer();
        return player != null ? player.getName() : "Unknown";
    }
    
    // ========== Team ==========
    
    public Team getTeam() {
        return team;
    }
    
    public void setTeam(Team team) {
        this.team = team;
    }
    
    // ========== Element ==========
    
    public Element getElement() {
        return element;
    }
    
    public void setElement(Element element) {
        this.element = element;
    }
    
    public boolean hasSelectedElement() {
        return element != null;
    }
    
    // ========== Shard（所持中） ==========
    
    public int getShardCarrying() {
        return shardCarrying;
    }
    
    public void setShardCarrying(int amount) {
        this.shardCarrying = Math.max(0, amount);
    }
    
    public void addShardCarrying(int amount) {
        this.shardCarrying += amount;
    }
    
    /**
     * 所持中のShardをすべて取り出す（ドロップ用）
     */
    public int takeAllCarryingShards() {
        int shards = shardCarrying;
        shardCarrying = 0;
        return shards;
    }
    
    // ========== Shard（貯金済み） ==========
    
    public int getShardSaved() {
        return shardSaved;
    }
    
    public void setShardSaved(int amount) {
        this.shardSaved = Math.max(0, amount);
    }
    
    public void addShardSaved(int amount) {
        this.shardSaved += amount;
    }
    
    /**
     * 所持中のShardを貯金に移動
     * @return 貯金した量
     */
    public int depositShards() {
        int amount = shardCarrying;
        shardSaved += shardCarrying;
        shardCarrying = 0;
        return amount;
    }
    
    /**
     * 貯金Shardを使用
     */
    public boolean spendSavedShard(int amount) {
        if (shardSaved >= amount) {
            shardSaved -= amount;
            return true;
        }
        return false;
    }
    
    // ========== Lumina（所持中） ==========
    
    public int getLuminaCarrying() {
        return luminaCarrying;
    }
    
    public void setLuminaCarrying(int amount) {
        this.luminaCarrying = Math.max(0, amount);
    }
    
    public void addLuminaCarrying(int amount) {
        this.luminaCarrying += amount;
    }
    
    /**
     * 所持中のLuminaをすべて取り出す（ドロップ用）
     */
    public int takeAllCarryingLumina() {
        int amount = luminaCarrying;
        luminaCarrying = 0;
        return amount;
    }
    
    // ========== Lumina（貯金済み） ==========
    
    public int getLuminaSaved() {
        return luminaSaved;
    }
    
    public void setLuminaSaved(int amount) {
        this.luminaSaved = Math.max(0, amount);
    }
    
    public void addLuminaSaved(int amount) {
        this.luminaSaved += amount;
    }
    
    /**
     * 所持中のLuminaを貯金に移動
     * @return 貯金した量
     */
    public int depositLumina() {
        int amount = luminaCarrying;
        luminaSaved += luminaCarrying;
        luminaCarrying = 0;
        return amount;
    }
    
    /**
     * 合計Luminaを取得（所持 + 貯金）
     */
    public int getTotalLumina() {
        return luminaCarrying + luminaSaved;
    }
    
    /**
     * Luminaを持っているか（貯金含む）
     */
    public boolean hasLumina(int amount) {
        return getTotalLumina() >= amount;
    }
    
    /**
     * Luminaを使用（貯金から優先）
     */
    public boolean spendLumina(int amount) {
        if (!hasLumina(amount)) {
            return false;
        }
        
        // 貯金から優先的に使用
        if (luminaSaved >= amount) {
            luminaSaved -= amount;
        } else {
            int remaining = amount - luminaSaved;
            luminaSaved = 0;
            luminaCarrying -= remaining;
        }
        return true;
    }
    
    // ========== 旧Lumina互換メソッド ==========
    
    @Deprecated
    public int getLumina() {
        return getTotalLumina();
    }
    
    @Deprecated
    public void setLumina(int amount) {
        this.luminaSaved = Math.max(0, amount);
        this.luminaCarrying = 0;
    }
    
    @Deprecated
    public void addLumina(int amount) {
        // キル時などはcarryingに加算
        this.luminaCarrying += amount;
    }
    
    // ========== SP System ==========
    
    public int getSpGauge() {
        return spGauge;
    }
    
    public void setSpGauge(int gauge) {
        this.spGauge = Math.min(10, Math.max(0, gauge));
    }
    
    public void addSpGauge(int amount) {
        setSpGauge(spGauge + amount);
    }
    
    public boolean isSpReady() {
        return spGauge >= 10 && !isSpOnCooldown();
    }
    
    public boolean isSpOnCooldown() {
        return System.currentTimeMillis() < spCooldownEnd;
    }
    
    public long getSpCooldownRemaining() {
        long remaining = spCooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    public void useSpAbility(long cooldownMillis) {
        this.spGauge = 0;
        this.spCooldownEnd = System.currentTimeMillis() + cooldownMillis;
    }
    
    // ========== Silence（沈黙状態） ==========
    
    /**
     * 沈黙状態を適用（SP技使用不可）
     */
    public void applySilence(long durationMillis) {
        this.silenceEnd = System.currentTimeMillis() + durationMillis;
    }
    
    /**
     * 沈黙状態かどうか
     */
    public boolean isSilenced() {
        return System.currentTimeMillis() < silenceEnd;
    }
    
    /**
     * 沈黙状態の残り時間
     */
    public long getSilenceRemaining() {
        long remaining = silenceEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    // ========== King ==========
    
    public boolean isKing() {
        return isKing;
    }
    
    public void setKing(boolean king) {
        isKing = king;
    }
    
    // ========== Game State ==========
    
    public boolean canRespawn() {
        return canRespawn;
    }
    
    public void setCanRespawn(boolean canRespawn) {
        this.canRespawn = canRespawn;
    }
    
    public boolean isAlive() {
        return isAlive;
    }
    
    public void setAlive(boolean alive) {
        isAlive = alive;
    }
    
    public Location getLastDeathLocation() {
        return lastDeathLocation;
    }
    
    public void setLastDeathLocation(Location loc) {
        this.lastDeathLocation = loc;
    }
    
    // ========== Statistics ==========
    
    public int getKillsThisGame() {
        return killsThisGame;
    }
    
    public void addKill() {
        killsThisGame++;
    }
    
    public int getDeathsThisGame() {
        return deathsThisGame;
    }
    
    public void addDeath() {
        deathsThisGame++;
    }
    
    public int getShardDepositedThisGame() {
        return shardDepositedThisGame;
    }
    
    public void addShardDeposited(int amount) {
        shardDepositedThisGame += amount;
    }
    
    // ========== Utility ==========
    
    public void sendMessage(String message) {
        Player player = getPlayer();
        if (player != null) {
            player.sendMessage(message);
        }
    }
    
    /**
     * ゲーム開始時にデータをリセット
     */
    public void resetForNewGame() {
        this.element = null;
        this.shardCarrying = 0;
        this.shardSaved = 0;
        this.luminaCarrying = 0;
        this.luminaSaved = 0;
        this.spGauge = 0;
        this.spCooldownEnd = 0;
        this.silenceEnd = 0;
        this.isKing = false;
        this.canRespawn = true;
        this.isAlive = true;
        this.killsThisGame = 0;
        this.deathsThisGame = 0;
        this.shardDepositedThisGame = 0;
    }
}
