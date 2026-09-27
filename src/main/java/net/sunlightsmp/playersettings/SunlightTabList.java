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

    public SunlightTabList(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        refresh();
        new BukkitRunnable() {
            @Override public void run() { refresh(); }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { refresh(); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { Bukkit.getScheduler().runTask(plugin, this::refresh); }

    private Economy economy() {
        var registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }

    private String money(Player p) {
        Economy eco = economy();
        return eco == null ? "0" : NumberFormat.getNumberInstance(Locale.US).format(eco.getBalance(p));
    }

    private String playtime(Player p) {
        long minutes = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 1200L;
        long days = minutes / 1440L;
        long hours = (minutes % 1440L) / 60L;
        long mins = minutes % 60L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    private String team(Player p) {
        var team = p.getScoreboard().getEntryTeam(p.getName());
        if (team == null || team.getName().isBlank()) return "No Team";
        String prefix = ChatColor.stripColor(team.getPrefix());
        if (prefix != null && !prefix.isBlank()) return prefix.trim();
        return team.getName();
    }

    private void refresh() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player viewer : players) {
            StringBuilder header = new StringBuilder();
            header.append("\n").append(ChatColor.GOLD).append("☀ ").append(ChatColor.YELLOW).append("SUNLIGHT SMP").append("\n")
                    .append(ChatColor.DARK_GRAY).append("━━━━━━━━━━━━━━━━━━━━━━━━").append("\n")
                    .append(ChatColor.GRAY).append("Online ").append(ChatColor.YELLOW).append(players.size())
                    .append(ChatColor.DARK_GRAY).append("  •  ").append(ChatColor.GRAY).append("Ping ")
                    .append(ChatColor.GREEN).append(viewer.getPing()).append("ms").append("\n")
                    .append(ChatColor.DARK_GRAY).append("━━━━━━━━━━━━━━━━━━━━━━━━").append("\n")
                    .append(ChatColor.GOLD).append("💰 $").append(ChatColor.WHITE).append(money(viewer))
                    .append(ChatColor.DARK_GRAY).append("  •  ").append(ChatColor.YELLOW).append("🌻 ")
                    .append(ChatColor.WHITE).append(plugin.getSunflowerManager().get(viewer)).append("\n")
                    .append(ChatColor.AQUA).append("⚔ Team: ").append(ChatColor.WHITE).append(team(viewer))
                    .append(ChatColor.DARK_GRAY).append("  •  ").append(ChatColor.LIGHT_PURPLE).append("⏱ ")
                    .append(ChatColor.WHITE).append(playtime(viewer)).append("\n");

            StringBuilder footer = new StringBuilder();
            footer.append("\n").append(ChatColor.GOLD).append("☀ ").append(ChatColor.YELLOW).append("SUNLIGHT SMP")
                    .append(ChatColor.GRAY).append("  •  ").append(ChatColor.YELLOW).append("play.sunlight-smp.net").append("\n")
                    .append(ChatColor.DARK_GRAY).append("━━━━━━━━━━━━━━━━━━━━━━━━").append("\n")
                    .append(ChatColor.GRAY).append("Your Ping: ").append(ChatColor.GREEN).append(viewer.getPing()).append(" ms")
                    .append(ChatColor.DARK_GRAY).append("  •  ").append(ChatColor.GRAY).append("Stay bright ☀").append("\n");

            viewer.setPlayerListHeaderFooter(header.toString(), footer.toString());
            for (Player target : players) {
                target.setPlayerListName(ChatColor.YELLOW + target.getName()
                        + ChatColor.DARK_GRAY + "  •  " + ChatColor.AQUA + team(target)
                        + ChatColor.DARK_GRAY + "  •  " + ChatColor.GREEN + target.getPing() + "ms");
            }
        }
    }
}
