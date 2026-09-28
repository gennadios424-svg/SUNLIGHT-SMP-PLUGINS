package net.sunlightsmp.playersettings;

import org.bukkit.World;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class MobSpawnListener implements Listener {
    private static final int HOPPER_ITEMS_PER_TICK = 32;
    private final SunlightPlayerSettings plugin;

    public MobSpawnListener(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        // 100,000 x 100,000 blocks = +/- 50,000 from the center.
        for (World world : plugin.getServer().getWorlds()) {
            world.getWorldBorder().setCenter(0.0, 0.0);
            world.getWorldBorder().setSize(100000.0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.DEFAULT) {
            return;
        }

        for (Player player : event.getEntity().getWorld().getPlayers()) {
            if (!plugin.getSettings().get(player, Setting.MOB_SPAWNS)
                    && player.getLocation().distanceSquared(event.getLocation()) <= 24 * 24) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperTransfer(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        ItemStack wanted = event.getItem();
        if (wanted == null || wanted.getType().isAir()) return;

        int sourceSlot = findMatchingSlot(source, wanted);
        if (sourceSlot < 0) return;

        ItemStack existing = source.getItem(sourceSlot);
        int amount = Math.min(HOPPER_ITEMS_PER_TICK, existing.getAmount());
        amount = Math.min(amount, availableCapacity(destination, wanted));
        if (amount <= 0) {
            event.setCancelled(true);
            setFastCooldown(source);
            setFastCooldown(destination);
            return;
        }

        ItemStack moved = existing.clone();
        moved.setAmount(amount);
        existing.setAmount(existing.getAmount() - amount);
        source.setItem(sourceSlot, existing.getAmount() <= 0 ? null : existing);

        Map<Integer, ItemStack> leftovers = destination.addItem(moved);
        if (!leftovers.isEmpty()) {
            int returned = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (returned > 0) {
                ItemStack restore = moved.clone();
                restore.setAmount(returned);
                source.addItem(restore);
            }
        }

        event.setCancelled(true);
        setFastCooldown(source);
        setFastCooldown(destination);
    }

    private int findMatchingSlot(Inventory inventory, ItemStack wanted) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.isSimilar(wanted) && item.getAmount() > 0) return i;
        }
        return -1;
    }

    private int availableCapacity(Inventory inventory, ItemStack item) {
        int capacity = 0;
        int max = Math.min(inventory.getMaxStackSize(), item.getMaxStackSize());
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) capacity += max;
            else if (slot.isSimilar(item)) capacity += Math.max(0, max - slot.getAmount());
            if (capacity >= HOPPER_ITEMS_PER_TICK) return HOPPER_ITEMS_PER_TICK;
        }
        return capacity;
    }

    private void setFastCooldown(Inventory inventory) {
        Object holder = inventory.getHolder();
        if (holder instanceof Hopper hopper) hopper.setTransferCooldown(1);
    }
}
