package tensaimc.kingsline.resource;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.player.KLPlayer;
import tensaimc.kingsline.util.ActionBarUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Lumina（個人通貨）管理クラス
 * Luminaは個人ショップで使用する通貨
 * - キル時に入手
 * - 死亡時にドロップ（敵に奪われる）
 * - 拠点に戻ると自動貯金
 */
public class LuminaManager {
    
    private final KingsLine plugin;
    
    // Luminaのアイテム定義（ドロップ時）
    public static final Material LUMINA_MATERIAL = Material.GLOWSTONE_DUST;
    public static final String LUMINA_DISPLAY_NAME = ChatColor.LIGHT_PURPLE + "✦ Lumina";
    
    public LuminaManager(KingsLine plugin) {
        this.plugin = plugin;
    }
    
    /**
     * キル時にLuminaを付与
     */
    public void awardKillLumina(KLPlayer killer, int points) {
        int amount = plugin.getConfigManager().getLuminaPerKill();
        killer.addLuminaCarrying(amount);
        
        // 統計: ルミナ獲得を記録
        plugin.getStatsDatabase().addLumina(killer.getUuid(), amount);
        
        Player player = killer.getPlayer();
        if (player != null) {
            String message = ChatColor.GREEN + "+" + points + "pt " + 
                    ChatColor.LIGHT_PURPLE + "✦+" + amount;
            ActionBarUtil.sendActionBar(player, message);
        }
    }
    
    /**
     * 死亡時にLuminaをドロップ
     */
    public void dropPlayerLumina(KLPlayer klPlayer, Location location) {
        int carrying = klPlayer.takeAllCarryingLumina();
        if (carrying <= 0) {
            return;
        }
        
        // ドロップアイテムとして生成
        ItemStack luminaItem = createLuminaItem();
        luminaItem.setAmount(carrying);
        
        if (location != null && location.getWorld() != null) {
            location.getWorld().dropItemNaturally(location, luminaItem);
        }
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.RED + "所持していた " + carrying + " Luminaをドロップしました！");
        }
    }
    
    /**
     * Luminaを拾った時の処理
     */
    public void onPickupLumina(KLPlayer klPlayer, int amount) {
        klPlayer.addLuminaCarrying(amount);
        
        // 統計: ルミナ獲得を記録
        plugin.getStatsDatabase().addLumina(klPlayer.getUuid(), amount);
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            String message = ChatColor.LIGHT_PURPLE + "✦+" + amount + 
                    ChatColor.GRAY + " (所持: " + klPlayer.getLuminaCarrying() + ")";
            ActionBarUtil.sendActionBar(player, message);
        }
    }
    
    /**
     * 拠点に帰還した時の自動貯金
     */
    public void onReturnToBase(KLPlayer klPlayer) {
        int deposited = klPlayer.depositLumina();
        if (deposited > 0) {
            Player player = klPlayer.getPlayer();
            if (player != null) {
                String message = ChatColor.GREEN + "✓ " + ChatColor.LIGHT_PURPLE + "✦" + deposited + 
                        ChatColor.GREEN + " を貯金しました！" + 
                        ChatColor.GRAY + " (合計: " + klPlayer.getLuminaSaved() + ")";
                ActionBarUtil.sendActionBar(player, message);
                player.sendMessage(ChatColor.GREEN + "✦ " + deposited + " Lumina を貯金しました！");
            }
        }
    }
    
    /**
     * Luminaを消費してアイテムを購入（貯金から使用）
     */
    public boolean purchase(KLPlayer klPlayer, int cost, String itemName) {
        if (!klPlayer.hasLumina(cost)) {
            Player player = klPlayer.getPlayer();
            if (player != null) {
                player.sendMessage(ChatColor.RED + "Luminaが足りません。(必要: " + cost + 
                        ", 所持: " + klPlayer.getTotalLumina() + ")");
            }
            return false;
        }
        
        klPlayer.spendLumina(cost);
        
        Player player = klPlayer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.GREEN + itemName + " を購入しました！");
            player.sendMessage(ChatColor.GRAY + "残りLumina: " + klPlayer.getTotalLumina());
        }
        
        return true;
    }
    
    /**
     * 現在のLuminaを表示
     */
    public void showLumina(KLPlayer klPlayer) {
        Player player = klPlayer.getPlayer();
        if (player != null) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "=== Lumina ===");
            player.sendMessage(ChatColor.GRAY + "所持: " + ChatColor.WHITE + klPlayer.getLuminaCarrying());
            player.sendMessage(ChatColor.GRAY + "貯金: " + ChatColor.WHITE + klPlayer.getLuminaSaved());
            player.sendMessage(ChatColor.GRAY + "合計: " + ChatColor.YELLOW + klPlayer.getTotalLumina());
        }
    }
    
    /**
     * Luminaアイテムを作成（ドロップ用）
     */
    public ItemStack createLuminaItem() {
        ItemStack item = new ItemStack(LUMINA_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(LUMINA_DISPLAY_NAME);
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "個人ショップ用の通貨");
        lore.add("");
        lore.add(ChatColor.YELLOW + "拠点に持ち帰ると貯金されます");
        lore.add(ChatColor.RED + "死亡すると所持分をドロップ！");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * アイテムがLuminaかどうか判定
     */
    public boolean isLumina(ItemStack item) {
        if (item == null || item.getType() != LUMINA_MATERIAL) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && LUMINA_DISPLAY_NAME.equals(meta.getDisplayName());
    }
}
