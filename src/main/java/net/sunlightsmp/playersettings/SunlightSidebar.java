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
    

    public SunlightSidebar(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        setWorldBorders();
        plugin.getServer().getPluginManager().registerEvents(new HopperSpeedListener(), plugin);
        new BukkitRunnable() {
            public void run() { refresh(); }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void setWorldBorders() {
        for (World w : Bukkit.getWorlds()) if (w.getEnvironment() == World.Environment.NORMAL) {
            w.getWorldBorder().setCenter(0, 0);
            w.getWorldBorder().setSize(100000.0);
            w.getWorldBorder().setWarningDistance(16);
        }
    }

    private final class HopperSpeedListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void move(InventoryMoveItemEvent e) {
            if (e.getInitiator() != null && e.getInitiator().getType().name().contains("HOPPER")) {
                ItemStack item = e.getItem();
                int available = 0;
                for (ItemStack x : e.getSource().getContents())
                    if (x != null && x.isSimilar(item)) available = Math.max(available, x.getAmount());
                if (available > 1) item.setAmount(Math.min(32, available));
            }
        }
    }

    private String money(Player p) {
        var r = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return r == null ? "0" : NumberFormat.getNumberInstance(Locale.US).format(r.getProvider().getBalance(p));
    }

    private String playtime(Player p) {
        long min = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 1200L;
        long d = min / 1440L, h = (min % 1440L) / 60L, m = min % 60L;
        return d > 0 ? d + "d " + h + "h" : h > 0 ? h + "h " + m + "m" : m + "m";
    }

    private void refresh() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            Objective o = board.getObjective("sunlight");
            if (o == null) o = board.registerNewObjective("sunlight", "dummy",
                    ChatColor.GOLD + "☀ " + ChatColor.YELLOW + "SUNLIGHT SMP");
            o.setDisplaySlot(DisplaySlot.SIDEBAR);

            // Remove every old line from our dedicated objective so stale lines from previous plugin versions cannot duplicate.
            for (String x : new HashSet<>(o.getScoreboard().getEntries())) o.getScoreboard().resetScores(x);

            List<String> lines = List.of(
                    ChatColor.DARK_GRAY + "━━━━━━━━━━━━",
                    ChatColor.GRAY + "Money: " + ChatColor.GOLD + "$" + ChatColor.WHITE + money(p),
                    ChatColor.GRAY + "Sunflowers: " + ChatColor.YELLOW + plugin.getSunflowerManager().get(p),
                    ChatColor.GRAY + "Playtime: " + ChatColor.LIGHT_PURPLE + playtime(p),
                    ChatColor.DARK_GRAY + " ",
                    ChatColor.YELLOW + "sunlightsmp.play-mc.fun"
            );

            int score = lines.size();
            int index = 0;
            for (String line : lines) {
                String entry = line + uniqueCode(index++);
                o.getScore(entry).setScore(score--);

            }
        }
    }

    private String uniqueCode(int index) {
        return switch (index % 8) {
            case 0 -> ChatColor.BLACK.toString();
            case 1 -> ChatColor.DARK_BLUE.toString();
            case 2 -> ChatColor.DARK_GREEN.toString();
            case 3 -> ChatColor.DARK_AQUA.toString();
            case 4 -> ChatColor.DARK_RED.toString();
            case 5 -> ChatColor.DARK_PURPLE.toString();
            case 6 -> ChatColor.GOLD.toString();
            default -> ChatColor.DARK_GRAY.toString();
        };
    }
}