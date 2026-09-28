package net.sunlightsmp.playersettings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.text.NumberFormat;
import java.util.*;

public final class SunlightSidebar {
    private final SunlightPlayerSettings plugin;
    private final Map<UUID,Set<String>> ownedLines=new HashMap<>();
    public SunlightSidebar(SunlightPlayerSettings plugin){this.plugin=plugin;setWorldBorders();plugin.getServer().getPluginManager().registerEvents(new HopperSpeedListener(),plugin);new BukkitRunnable(){public void run(){refresh();}}.runTaskTimer(plugin,20L,20L);}
    private void setWorldBorders(){for(World w:Bukkit.getWorlds())if(w.getEnvironment()==World.Environment.NORMAL){w.getWorldBorder().setCenter(0,0);w.getWorldBorder().setSize(100000.0);w.getWorldBorder().setWarningDistance(16);}}
    private final class HopperSpeedListener implements Listener{@EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void move(InventoryMoveItemEvent e){if(e.getInitiator()!=null&&e.getInitiator().getType().name().contains("HOPPER")){ItemStack item=e.getItem();int available=0;for(ItemStack x:e.getSource().getContents())if(x!=null&&x.isSimilar(item))available=Math.max(available,x.getAmount());if(available>1)item.setAmount(Math.min(32,available));}}}
    private String money(Player p){var r=plugin.getServer().getServicesManager().getRegistration(Economy.class);return r==null?"0":NumberFormat.getNumberInstance(Locale.US).format(r.getProvider().getBalance(p));}
    private String playtime(Player p){long min=p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE)/1200L;long d=min/1440L,h=(min%1440L)/60L,m=min%60L;return d>0?d+"d "+h+"h":h>0?h+"h "+m+"m":m+"m";}
    private void refresh(){for(Player p:Bukkit.getOnlinePlayers()){
        Scoreboard board=p.getScoreboard();Objective o=board.getObjective("sunlight");if(o==null)o=board.registerNewObjective("sunlight","dummy",ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP");o.setDisplaySlot(DisplaySlot.SIDEBAR);
        Set<String> old=ownedLines.computeIfAbsent(p.getUniqueId(),k->new HashSet<>());for(String x:old)board.resetScores(x);old.clear();
        List<String> lines=new ArrayList<>();
        lines.add(ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP");
        lines.add(ChatColor.DARK_GRAY+"━━━━━━━━━━━━");
        lines.add(ChatColor.GRAY+"Money: "+ChatColor.GOLD+"$"+ChatColor.WHITE+money(p));
        lines.add(ChatColor.GRAY+"Sunflowers: "+ChatColor.YELLOW+"🌻 "+ChatColor.WHITE+plugin.getSunflowerManager().get(p));
        lines.add(ChatColor.GRAY+"Playtime: "+ChatColor.LIGHT_PURPLE+"⏱ "+ChatColor.WHITE+playtime(p));
        lines.add(ChatColor.DARK_GRAY+" ");
        lines.add(ChatColor.GRAY+"IP: "+ChatColor.YELLOW+"s1.seranodes.com:25638");
        int score=lines.size();for(String line:lines){String entry=line;while(old.contains(entry)||board.getEntries().contains(entry)){entry=entry+ChatColor.RESET;}o.getScore(entry).setScore(score--);old.add(entry);}
    }}
}