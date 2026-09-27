package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class SunlightAntiDupe implements Listener {
    private final NamespacedKey pickaxeKey;
    private final NamespacedKey uniqueKey;

    public SunlightAntiDupe(SunlightPlayerSettings plugin) {
        pickaxeKey = new NamespacedKey(plugin, "sunlight_pickaxe");
        uniqueKey = new NamespacedKey(plugin, "sunlight_pickaxe_uuid");
    }

    private boolean isProtectedPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(pickaxeKey, PersistentDataType.BYTE);
        String uuid = meta.getPersistentDataContainer().get(uniqueKey, PersistentDataType.STRING);
        return marker != null && marker == 1 && uuid != null && !uuid.isBlank();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockCreativeClone(InventoryCreativeEvent e) {
        if (isProtectedPickaxe(e.getCursor()) || isProtectedPickaxe(e.getCurrentItem())) {
            e.setCancelled(true);
            e.setCursor(null);
            e.getWhoClicked().sendMessage(ChatColor.RED + "☀ Sunlight Pickaxes cannot be duplicated.");
        }
    }
}
