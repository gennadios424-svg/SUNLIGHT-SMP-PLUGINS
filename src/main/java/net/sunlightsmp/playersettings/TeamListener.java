package net.sunlightsmp.playersettings;

import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.*;
import org.bukkit.entity.Player;

public final class TeamListener implements Listener {
    private final SunlightPlayerSettings plugin;
    public TeamListener(SunlightPlayerSettings plugin){this.plugin=plugin;}
    @EventHandler public void join(PlayerJoinEvent e){plugin.getTabList().refreshNow();}
    @EventHandler public void quit(PlayerQuitEvent e){plugin.getTeamChat().clear(e.getPlayer());}
    @EventHandler public void chat(AsyncPlayerChatEvent e){
        if(plugin.getTeamChat().isToggled(e.getPlayer())){
            e.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin,()->plugin.getTeamChat().send(e.getPlayer(),e.getMessage()));
        }
    }
}
