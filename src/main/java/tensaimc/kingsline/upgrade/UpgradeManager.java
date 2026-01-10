package tensaimc.kingsline.upgrade;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.player.Team;
import tensaimc.kingsline.player.TeamManager;
import tensaimc.kingsline.util.TitleUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * チームアップグレード管理クラス
 * 複数人が協力してShardを投資し、目標に達したらアップグレード完了
 */
public class UpgradeManager {
    
    private final KingsLine plugin;
    
    // チームごとのアップグレードレベル
    private final Map<TeamUpgrade, Integer> blueLevels;
    private final Map<TeamUpgrade, Integer> redLevels;
    
    // チームごとのアップグレード進捗（現在の投資額）
    private final Map<TeamUpgrade, Integer> blueProgress;
    private final Map<TeamUpgrade, Integer> redProgress;
    
    public UpgradeManager(KingsLine plugin) {
        this.plugin = plugin;
        this.blueLevels = new HashMap<>();
        this.redLevels = new HashMap<>();
        this.blueProgress = new HashMap<>();
        this.redProgress = new HashMap<>();
        reset();
    }
    
    /**
     * リセット
     */
    public void reset() {
        blueLevels.clear();
        redLevels.clear();
        blueProgress.clear();
        redProgress.clear();
        
        for (TeamUpgrade upgrade : TeamUpgrade.values()) {
            blueLevels.put(upgrade, 0);
            redLevels.put(upgrade, 0);
            blueProgress.put(upgrade, 0);
            redProgress.put(upgrade, 0);
        }
    }
    
    /**
     * 現在のレベルを取得
     */
    public int getLevel(Team team, TeamUpgrade upgrade) {
        Map<TeamUpgrade, Integer> levels = team == Team.BLUE ? blueLevels : redLevels;
        return levels.getOrDefault(upgrade, 0);
    }
    
    /**
     * 現在の投資進捗を取得
     */
    public int getProgress(Team team, TeamUpgrade upgrade) {
        Map<TeamUpgrade, Integer> progress = team == Team.BLUE ? blueProgress : redProgress;
        return progress.getOrDefault(upgrade, 0);
    }
    
    /**
     * 次のレベルに必要なコストを取得
     */
    public int getRequiredCost(Team team, TeamUpgrade upgrade) {
        int currentLevel = getLevel(team, upgrade);
        return upgrade.getNextTierCost(currentLevel);
    }
    
    /**
     * Shardを投資する
     * @param klPlayer 投資するプレイヤー
     * @param upgrade アップグレード種類
     * @param amount 投資額
     * @return 実際に投資した額
     */
    public int invest(KLPlayer klPlayer, TeamUpgrade upgrade, int amount) {
        Team team = klPlayer.getTeam();
        if (team == Team.NONE) {
            return 0;
        }
        
        int currentLevel = getLevel(team, upgrade);
        if (currentLevel >= upgrade.getMaxTier()) {
            klPlayer.sendMessage(ChatColor.YELLOW + "このアップグレードは最大レベルです。");
            return 0;
        }
        
        // 投資可能額を計算
        int requiredCost = getRequiredCost(team, upgrade);
        int currentProgress = getProgress(team, upgrade);
        int remaining = requiredCost - currentProgress;
        
        int toInvest = Math.min(amount, remaining);
        toInvest = Math.min(toInvest, klPlayer.getShardSaved());
        
        if (toInvest <= 0) {
            klPlayer.sendMessage(ChatColor.RED + "投資するShardがありません。");
            return 0;
        }
        
        // 投資を実行
        klPlayer.spendSavedShard(toInvest);
        
        Map<TeamUpgrade, Integer> progress = team == Team.BLUE ? blueProgress : redProgress;
        int newProgress = currentProgress + toInvest;
        progress.put(upgrade, newProgress);
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.GREEN + upgrade.getDisplayName() + " に " + 
                    ChatColor.AQUA + "◈" + toInvest + ChatColor.GREEN + " 投資しました！");
            player.sendMessage(ChatColor.GRAY + "進捗: " + newProgress + "/" + requiredCost);
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
        }
        
        // チームに通知
        plugin.getGameManager().broadcastToTeam(team, 
                ChatColor.AQUA + klPlayer.getName() + " が " + 
                ChatColor.GOLD + upgrade.getDisplayName() + ChatColor.AQUA + " に投資！ (" + 
                newProgress + "/" + requiredCost + ")");
        
        // アップグレード完了チェック
        if (newProgress >= requiredCost) {
            completeUpgrade(team, upgrade);
        }
        
        return toInvest;
    }
    
    /**
     * アップグレード完了処理
     */
    private void completeUpgrade(Team team, TeamUpgrade upgrade) {
        Map<TeamUpgrade, Integer> levels = team == Team.BLUE ? blueLevels : redLevels;
        Map<TeamUpgrade, Integer> progress = team == Team.BLUE ? blueProgress : redProgress;
        
        int newLevel = levels.get(upgrade) + 1;
        levels.put(upgrade, newLevel);
        progress.put(upgrade, 0); // 進捗リセット
        
        // チーム全員に通知 & 効果適用
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "⬆ アップグレード完了！";
        String subtitle = ChatColor.WHITE + upgrade.getDisplayName() + " → " + 
                upgrade.getTierDescription(newLevel);
        
        TeamManager tm = plugin.getTeamManager();
        for (KLPlayer klPlayer : tm.getTeamPlayers(plugin.getGameManager().getPlayers(), team)) {
            Player player = klPlayer.getPlayer();
            if (player != null) {
                TitleUtil.sendTitle(player, title, subtitle, 10, 60, 20);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            }
            
            // 効果を即座に適用
            applyUpgradeToPlayer(klPlayer);
        }
        
        plugin.getGameManager().broadcast(team.getChatColor() + team.getDisplayName() + 
                ChatColor.GOLD + " の " + upgrade.getDisplayName() + " が " + 
                ChatColor.GREEN + upgrade.getTierDescription(newLevel) + ChatColor.GOLD + " にアップグレード！");
    }
    
    /**
     * プレイヤーにすべてのアップグレード効果を適用
     */
    public void applyUpgradeToPlayer(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player == null) {
            return;
        }
        
        Team team = klPlayer.getTeam();
        if (team == Team.NONE) {
            return;
        }
        
        // 防具アップグレード
        int armorLevel = getLevel(team, TeamUpgrade.ARMOR);
        applyArmorUpgrade(player, team, armorLevel, klPlayer.isKing());
        
        // 武器アップグレード
        int weaponLevel = getLevel(team, TeamUpgrade.WEAPON);
        applyWeaponUpgrade(player, weaponLevel);
        
        // プロテクション
        int protLevel = getLevel(team, TeamUpgrade.PROTECTION);
        applyProtectionUpgrade(player, protLevel);
        
        // シャープネス
        int sharpLevel = getLevel(team, TeamUpgrade.SHARPNESS);
        applySharpnessUpgrade(player, sharpLevel);
        
        // HP強化
        int healthLevel = getLevel(team, TeamUpgrade.HEALTH);
        applyHealthUpgrade(player, healthLevel);
        
        // スピード強化
        int speedLevel = getLevel(team, TeamUpgrade.SPEED);
        applySpeedUpgrade(player, speedLevel);
    }
    
    /**
     * 防具アップグレードを適用
     * ※ヘルメットとチェストプレートは常に皮装備、レギンスとブーツのみアップグレード
     * ※キングの場合はチェストプレートがダイヤ
     */
    private void applyArmorUpgrade(Player player, Team team, int level, boolean isKing) {
        // ヘルメットは常に皮装備（チームカラー）
        ItemStack leatherHelmet = new ItemStack(Material.LEATHER_HELMET);
        setLeatherColor(leatherHelmet, team);
        player.getInventory().setHelmet(leatherHelmet);
        
        // チェストプレート: キングはダイヤ、それ以外は皮
        if (isKing) {
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        } else {
            ItemStack leatherChest = new ItemStack(Material.LEATHER_CHESTPLATE);
            setLeatherColor(leatherChest, team);
            player.getInventory().setChestplate(leatherChest);
        }
        
        // レギンスとブーツのみアップグレード対象
        Material leggings, boots;
        
        switch (level) {
            case 1: // 鉄装備
                leggings = Material.IRON_LEGGINGS;
                boots = Material.IRON_BOOTS;
                break;
            case 2: // ダイヤ装備
                leggings = Material.DIAMOND_LEGGINGS;
                boots = Material.DIAMOND_BOOTS;
                break;
            default: // 皮装備
                ItemStack leatherLegs = new ItemStack(Material.LEATHER_LEGGINGS);
                ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
                setLeatherColor(leatherLegs, team);
                setLeatherColor(leatherBoots, team);
                player.getInventory().setLeggings(leatherLegs);
                player.getInventory().setBoots(leatherBoots);
                return;
        }
        
        player.getInventory().setLeggings(new ItemStack(leggings));
        player.getInventory().setBoots(new ItemStack(boots));
    }
    
    private void setLeatherColor(ItemStack item, Team team) {
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team.getArmorColor());
        item.setItemMeta(meta);
    }
    
    /**
     * 武器アップグレードを適用
     */
    private void applyWeaponUpgrade(Player player, int level) {
        Material swordType;
        switch (level) {
            case 1: swordType = Material.STONE_SWORD; break;
            case 2: swordType = Material.IRON_SWORD; break;
            case 3: swordType = Material.DIAMOND_SWORD; break;
            default: swordType = Material.WOOD_SWORD; break;
        }
        
        // 既存の剣を探して置き換え
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType().name().contains("SWORD")) {
                ItemStack newSword = new ItemStack(swordType);
                // 既存のエンチャントを維持
                if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                    for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                        newSword.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                    }
                }
                player.getInventory().setItem(i, newSword);
                return;
            }
        }
        
        // 剣がなければ追加
        player.getInventory().addItem(new ItemStack(swordType));
    }
    
    /**
     * プロテクションエンチャントを適用
     */
    private void applyProtectionUpgrade(Player player, int level) {
        if (level <= 0) return;
        
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                armor.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, level);
            }
        }
    }
    
    /**
     * シャープネスエンチャントを適用
     */
    private void applySharpnessUpgrade(Player player, int level) {
        if (level <= 0) return;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType().name().contains("SWORD")) {
                item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, level);
            }
        }
    }
    
    /**
     * HP強化を適用
     */
    private void applyHealthUpgrade(Player player, int level) {
        if (level <= 0) {
            player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
            return;
        }
        
        int amplifier = level - 1; // 0 = +2ハート, 1 = +4ハート
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HEALTH_BOOST,
                Integer.MAX_VALUE,
                amplifier,
                false, false
        ), true);
    }
    
    /**
     * スピード強化を適用
     */
    private void applySpeedUpgrade(Player player, int level) {
        if (level <= 0) {
            player.removePotionEffect(PotionEffectType.SPEED);
            return;
        }
        
        int amplifier = level - 1; // 0 = Speed I, 1 = Speed II
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                Integer.MAX_VALUE,
                amplifier,
                false, false
        ), true);
    }
}
