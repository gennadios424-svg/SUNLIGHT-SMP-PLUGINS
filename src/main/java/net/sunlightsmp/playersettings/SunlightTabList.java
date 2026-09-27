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

    public SunlightTabList(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        refresh();
        new BukkitRunnable() {
            @Override public void run() { refresh(); }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { refresh(); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { Bukkit.getScheduler().runTask(plugin, this::refresh); }

    private void refresh() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player viewer : players) {
            StringBuilder header = new StringBuilder();
            header.append("\n").append(ChatColor.GOLD).append("☀ SUNLIGHT SMP").append("\n");
            header.append(ChatColor.GRAY).append("Online: ").append(ChatColor.YELLOW).append(players.size()).append("\n");
            header.append(ChatColor.DARK_GRAY).append("────────────────────────").append("\n");

            StringBuilder footer = new StringBuilder();
            footer.append("\n").append(ChatColor.GOLD).append("☀ ").append(ChatColor.YELLOW).append("SUNLIGHT SMP");
            footer.append(ChatColor.GRAY).append("  •  ");
            footer.append(ChatColor.YELLOW).append("play.sunlight-smp.net");
            footer.append("\n").append(ChatColor.GRAY).append("Your Ping: ").append(ChatColor.GREEN).append(viewer.getPing()).append(" ms");
            footer.append(ChatColor.DARK_GRAY).append("  •  ");
            footer.append(ChatColor.GRAY).append("IP addresses are private");
            footer.append("\n");

            viewer.setPlayerListHeaderFooter(header.toString(), footer.toString());
            for (Player target : players) {
                target.setPlayerListName(ChatColor.YELLOW + target.getName() + ChatColor.DARK_GRAY + "  •  " + ChatColor.GREEN + target.getPing() + "ms");
            }
        }
    }
}
