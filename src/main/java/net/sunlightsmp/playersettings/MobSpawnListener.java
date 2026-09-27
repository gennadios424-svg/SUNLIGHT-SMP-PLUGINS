package net.sunlightsmp.playersettings;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class MobSpawnListener implements Listener {
    private final SunlightPlayerSettings plugin;

    public MobSpawnListener(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
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
}
