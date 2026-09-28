package net.sunlightsmp.playersettings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SunlightTabList implements Listener {
    private final SunlightPlayerSettings plugin;
    public SunlightTabList(SunlightPlayerSettings plugin){this.plugin=plugin;refresh();new BukkitRunnable(){@Override public void run(){refresh();}}.runTaskTimer(plugin,20L,20L);}
    @EventHandler public void onJoin(PlayerJoinEvent e){refresh();}
    @EventHandler public void onQuit(PlayerQuitEvent e){Bukkit.getScheduler().runTask(plugin,this::refresh);}
    private Economy economy(){var r=plugin.getServer().getServicesManager().getRegistration(Economy.class);return r==null?null:r.getProvider();}
    private String money(Player p){Economy e=economy();return e==null?"0":NumberFormat.getNumberInstance(Locale.US).format(e.getBalance(p));}
    private String playtime(Player p){long m=p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE)/1200L,d=m/1440L,h=(m%1440L)/60L,min=m%60L;return d>0?d+"d "+h+"h":h>0?h+"h "+min+"m":min+"m";}
    private String team(Player p){var t=p.getScoreboard().getEntryTeam(p.getName());if(t==null||t.getName().isBlank())return "No Team";String s=ChatColor.stripColor(t.getPrefix());return s==null||s.isBlank()?t.getName():s.trim();}
    private void refresh(){List<Player> players=new ArrayList<>(Bukkit.getOnlinePlayers());for(Player viewer:players){
        String header="\n"+ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP\n"+ChatColor.DARK_GRAY+"━━━━━━━━━━━━━━━━━━━━━━━━\n"+ChatColor.GRAY+"Online "+ChatColor.YELLOW+players.size()+ChatColor.DARK_GRAY+"  •  "+ChatColor.GRAY+"Ping "+ChatColor.GREEN+viewer.getPing()+"ms\n"+ChatColor.DARK_GRAY+"━━━━━━━━━━━━━━━━━━━━━━━━\n"+ChatColor.GOLD+"💰 $"+ChatColor.WHITE+money(viewer)+ChatColor.DARK_GRAY+"  •  "+ChatColor.YELLOW+"🌻 "+ChatColor.WHITE+plugin.getSunflowerManager().get(viewer)+"\n"+ChatColor.AQUA+"⚔ Team: "+ChatColor.WHITE+team(viewer)+ChatColor.DARK_GRAY+"  •  "+ChatColor.LIGHT_PURPLE+"⏱ "+ChatColor.WHITE+playtime(viewer)+"\n";
        String footer="\n"+ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP"+ChatColor.GRAY+"  •  "+ChatColor.YELLOW+"s1.seranodes.com:25638\n"+ChatColor.DARK_GRAY+"━━━━━━━━━━━━━━━━━━━━━━━━\n"+ChatColor.GRAY+"Online: "+ChatColor.GREEN+players.size()+ChatColor.DARK_GRAY+"  •  "+ChatColor.GRAY+"Sunlight SMP\n";
        viewer.setPlayerListHeaderFooter(header,footer);
        for(Player target:players)target.setPlayerListName(ChatColor.YELLOW+target.getName()+ChatColor.DARK_GRAY+"  •  "+ChatColor.AQUA+team(target)+ChatColor.DARK_GRAY+"  •  "+ChatColor.GREEN+target.getPing()+"ms");
    }}
}