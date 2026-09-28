package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class SunlightExpiryManager implements Listener {
    private final SunlightPlayerSettings plugin;
    private final NamespacedKey[] expiryKeys;

    public SunlightExpiryManager(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        expiryKeys = new NamespacedKey[]{
                new NamespacedKey(plugin, "sunlight_pickaxe_expiry"),
                new NamespacedKey(plugin, "sunlight_sellaxe_expiry"),
                new NamespacedKey(plugin, "sunlight_tree_chopper_expiry"),
                new NamespacedKey(plugin, "sunlight_bucket_expiry"),
                new NamespacedKey(plugin, "sunset_elytra_expiry")
        };
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) update(p);
        }, 20L, 20L);
    }

    @EventHandler public void open(InventoryOpenEvent e) { if (e.getPlayer() instanceof Player p) update(p); }
    @EventHandler public void click(InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player p) plugin.getServer().getScheduler().runTask(plugin, () -> update(p)); }

    private void update(Player p) {
        for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
            ItemStack item = p.getInventory().getItem(slot);
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            Long expiry = null;
            boolean marked = false;
            for (NamespacedKey key : expiryKeys) {
                Long value = data.get(key, PersistentDataType.LONG);
                if (value != null) { expiry = value; marked = true; break; }
            }
            if (!marked || expiry == null) continue;
            long left = expiry - System.currentTimeMillis();
            if (left <= 0) {
                p.getInventory().setItem(slot, null);
                p.sendMessage(ChatColor.RED + "☀ A Sunlight item in your inventory has expired.");
                continue;
            }
            long total = left / 1000;
            long days = total / 86400;
            long hours = (total % 86400) / 3600;
            long minutes = (total % 3600) / 60;
            long seconds = total % 60;
            String time = days + "d " + String.format("%02dh %02dm %02ds", hours, minutes, seconds);
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.removeIf(line -> ChatColor.stripColor(line).toLowerCase().startsWith("expires in") || ChatColor.stripColor(line).toLowerCase().startsWith("time left:"));
            lore.add(ChatColor.LIGHT_PURPLE + "☀ Time left: " + time);
            meta.setLore(lore);
            item.setItemMeta(meta);
            p.getInventory().setItem(slot, item);
        }
    }
}
