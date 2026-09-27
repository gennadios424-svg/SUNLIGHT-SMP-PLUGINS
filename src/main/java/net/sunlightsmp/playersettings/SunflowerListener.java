package net.sunlightsmp.playersettings;

import org.bukkit.event.*;
import org.bukkit.event.player.*;

public final class SunflowerListener implements Listener {
    private final SunflowerManager manager;
    public SunflowerListener(SunflowerManager manager) { this.manager = manager; }

    @EventHandler public void join(PlayerJoinEvent e) { manager.join(e.getPlayer()); }
    @EventHandler public void move(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            manager.markActive(e.getPlayer());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent e) { manager.markActive(e.getPlayer()); }
}