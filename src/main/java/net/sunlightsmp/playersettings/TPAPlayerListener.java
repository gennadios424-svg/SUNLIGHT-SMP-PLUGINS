package net.sunlightsmp.playersettings;

import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TPAPlayerListener implements Listener {
    private final TPAManager manager;
    public TPAPlayerListener(TPAManager manager) { this.manager = manager; }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        manager.cleanup(event.getPlayer());
    }
}
