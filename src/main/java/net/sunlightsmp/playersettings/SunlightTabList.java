package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;

public final class SunlightTabList implements Listener {
    private final SunlightPlayerSettings plugin;
    public SunlightTabList(SunlightPlayerSettings plugin){
        this.plugin=plugin;
        refreshNow();
        new BukkitRunnable(){@Override public void run(){refreshNow();}}.runTaskTimer(plugin,20L,20L);
    }
    @EventHandler public void onJoin(PlayerJoinEvent e){refreshNow();}
    @EventHandler public void onQuit(PlayerQuitEvent e){Bukkit.getScheduler().runTask(plugin,this::refreshNow);}

    public void refreshNow(){
        List<Player> players=new ArrayList<>(Bukkit.getOnlinePlayers());
        for(Player viewer:players){
            String header="\n"+ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP\n"
                    +ChatColor.DARK_GRAY+"━━━━━━━━━━━━━━━━━━━━━━━━\n"
                    +ChatColor.GRAY+"Players: "+ChatColor.YELLOW+players.size()+ChatColor.GRAY+"\n"
                    +ChatColor.DARK_GRAY+"━━━━━━━━━━━━━━━━━━━━━━━━\n";
            String footer="\n"+ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP"
                    +ChatColor.DARK_GRAY+"\n━━━━━━━━━━━━━━━━━━━━━━━━\n";
            viewer.setPlayerListHeaderFooter(header,footer);
        }
        for(Player target:players){
            TeamManager.TeamData team=plugin.getTeamManager().getTeam(target.getUniqueId());
            String identity=team==null?"":plugin.getTeamManager().prefix(team);
            target.setPlayerListName(identity+ChatColor.WHITE+target.getName());
        }
    }
}
