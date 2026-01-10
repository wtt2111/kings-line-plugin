package tensaimc.kingsline.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tensaimc.kingsline.KingsLine;
import tensaimc.kingsline.element.Element;
import tensaimc.kingsline.player.KLPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * エレメントオーブ系アイテム
 * 他のエレメントのSP技を1回使用可能
 */
public class ElementOrbs {
    
    public static void registerAll(ShopItemRegistry registry) {
        registry.register(new FireOrb());
        registry.register(new IceOrb());
        registry.register(new WindOrb());
        registry.register(new EarthOrb());
    }
    
    /**
     * エレメントオーブの基底クラス
     */
    private static abstract class ElementOrb extends AbstractShopItem {
        
        protected final Element element;
        
        public ElementOrb(String id, String displayName, String description, 
                         Material material, Element element) {
            super(
                id,
                displayName,
                description,
                material,
                6,  // 全オーブ共通価格
                1,
                ShopItemCategory.ELEMENT_ORB
            );
            this.element = element;
        }
        
        @Override
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName(element.getColor() + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add("");
            lore.add(element.getColor() + "▶ " + getSpName() + " を発動");
            lore.add(ChatColor.YELLOW + "右クリックで使用");
            lore.add(SHOP_ITEM_IDENTIFIER + id);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
            return item;
        }
        
        protected abstract String getSpName();
        
        @Override
        public boolean use(KingsLine plugin, Player player, ItemStack item) {
            KLPlayer klPlayer = plugin.getGameManager().getPlayer(player);
            if (klPlayer == null) {
                return false;
            }
            
            // オーブからSP技を発動（クールダウン・ゲージ無視）
            boolean success = activateOrbAbility(plugin, klPlayer, player);
            
            if (success) {
                player.sendMessage(element.getColor() + "✦ " + displayName + " を使用！");
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.5f);
                // ItemListenerで消費されるのでここでは消費しない
                return true;
            }
            
            return false;
        }
        
        protected abstract boolean activateOrbAbility(KingsLine plugin, KLPlayer klPlayer, Player player);
    }
    
    // ========== ファイアオーブ ==========
    public static class FireOrb extends ElementOrb {
        
        public FireOrb() {
            super(
                "fire_orb",
                "ファイアオーブ",
                "Overheatを1回使用可能",
                Material.BLAZE_POWDER,
                Element.FIRE
            );
        }
        
        @Override
        protected String getSpName() {
            return "Overheat (与ダメ+40%, 確定炎上)";
        }
        
        @Override
        protected boolean activateOrbAbility(KingsLine plugin, KLPlayer klPlayer, Player player) {
            plugin.getElementManager().activateOrbOverheat(klPlayer);
            return true;
        }
    }
    
    // ========== アイスオーブ ==========
    public static class IceOrb extends ElementOrb {
        
        public IceOrb() {
            super(
                "ice_orb",
                "アイスオーブ",
                "Ice Ageを1回使用可能",
                Material.PACKED_ICE,
                Element.ICE
            );
        }
        
        @Override
        protected String getSpName() {
            return "Ice Age (周囲の敵を凍結)";
        }
        
        @Override
        protected boolean activateOrbAbility(KingsLine plugin, KLPlayer klPlayer, Player player) {
            int frozenCount = plugin.getElementManager().activateOrbIceAge(klPlayer);
            if (frozenCount == 0) {
                player.sendMessage(ChatColor.RED + "周囲に敵がいません。");
                return false;
            }
            return true;
        }
    }
    
    // ========== ウィンドオーブ ==========
    public static class WindOrb extends ElementOrb {
        
        public WindOrb() {
            super(
                "wind_orb",
                "ウィンドオーブ",
                "Gale Stepを1回使用可能",
                Material.FEATHER,
                Element.WIND
            );
        }
        
        @Override
        protected String getSpName() {
            return "Gale Step (敵の背後にテレポート)";
        }
        
        @Override
        protected boolean activateOrbAbility(KingsLine plugin, KLPlayer klPlayer, Player player) {
            return plugin.getElementManager().activateOrbGaleStep(klPlayer);
        }
    }
    
    // ========== アースオーブ ==========
    public static class EarthOrb extends ElementOrb {
        
        public EarthOrb() {
            super(
                "earth_orb",
                "アースオーブ",
                "Bulwarkを1回使用可能",
                Material.CLAY_BALL,
                Element.EARTH
            );
        }
        
        @Override
        protected String getSpName() {
            return "Bulwark (5秒間、被ダメ-80%)";
        }
        
        @Override
        protected boolean activateOrbAbility(KingsLine plugin, KLPlayer klPlayer, Player player) {
            plugin.getElementManager().activateOrbBulwark(klPlayer);
            return true;
        }
    }
}
