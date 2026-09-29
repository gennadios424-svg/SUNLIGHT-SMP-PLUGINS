package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

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
            viewer.setPlayerListHeaderFooter(
                "\n"+ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP\n"
                +ChatColor.DARK_GRAY+"━━━━━━━━━━━━━━━━━━━━━━━━\n"
                +ChatColor.GRAY+"Players: "+ChatColor.YELLOW+players.size()+"\n"
                +ChatColor.DARK_GRAY+"━━━━━━━━━━━━━━━━━━━━━━━━\n",
                "\n"+ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP"
                +ChatColor.DARK_GRAY+"\n━━━━━━━━━━━━━━━━━━━━━━━━\n"
            );
        }

        Map<UUID,Integer> teamOrder=new HashMap<>();
        List<TeamManager.TeamData> teams=new ArrayList<>(plugin.getTeamManager().getTeams());
        teams.sort(Comparator.comparing(t->t.name.toLowerCase(Locale.ROOT)));
        int group=0;
        for(TeamManager.TeamData team:teams){
            List<UUID> members=new ArrayList<>(team.members.keySet());
            members.sort(Comparator.comparing(u->{
                Player p=Bukkit.getPlayer(u);
                return p==null?u.toString():p.getName().toLowerCase(Locale.ROOT);
            }));
            int memberIndex=0;
            for(UUID u:members)teamOrder.put(u,++memberIndex+group*100);
            group++;
        }
        int soloBase=(group+1)*100;
        players.sort(Comparator.comparing(Player::getName,String.CASE_INSENSITIVE_ORDER));
        int solo=0;
        for(Player target:players){
            TeamManager.TeamData team=plugin.getTeamManager().getTeam(target.getUniqueId());
            String identity=team==null?"":plugin.getTeamManager().prefix(team);
            target.setPlayerListName(identity+ChatColor.WHITE+target.getName());
            target.setPlayerListOrder(teamOrder.getOrDefault(target.getUniqueId(),soloBase+(++solo)));
        }
    }
}
