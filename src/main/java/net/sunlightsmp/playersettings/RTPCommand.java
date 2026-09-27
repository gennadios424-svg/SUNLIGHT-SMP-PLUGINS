package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public final class RTPCommand implements org.bukkit.command.CommandExecutor {
    private final JavaPlugin plugin;
    private final Random random = new Random();

    public RTPCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /rtp.");
            return true;
        }

        World world = player.getWorld();
        player.sendMessage(ChatColor.GOLD + "☀ " + ChatColor.YELLOW + "Finding a safe random location in the " + dimensionName(world) + "...");

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                attempts++;
                Location target = randomLocation(world);

                if (target != null && isSafe(target, world)) {
                    player.teleport(target);
                    player.sendMessage(ChatColor.GREEN + "☀ RTP successful! Teleported " + ChatColor.YELLOW + "15,000 blocks" + ChatColor.GREEN + " away.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    cancel();
                    return;
                }

                if (attempts >= 30) {
                    player.sendMessage(ChatColor.RED + "☀ Couldn't find a safe location. Try /rtp again.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);

        return true;
    }

    private Location randomLocation(World world) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 15_000;

        int x = (int) Math.round(Math.cos(angle) * distance);
        int z = (int) Math.round(Math.sin(angle) * distance);

        if (world.getEnvironment() == World.Environment.NETHER) {
            // Nether RTP is 15,000 Nether blocks from 0,0.
            // It does not use the Overworld coordinate conversion.
            int y = world.getHighestBlockYAt(x, z);
            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }

        if (world.getEnvironment() == World.Environment.THE_END) {
            int y = world.getHighestBlockYAt(x, z);
            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }

        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }

    private boolean isSafe(Location location, World world) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 2) return false;

        Material floor = world.getBlockAt(x, y - 1, z).getType();
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();

        if (!floor.isSolid()) return false;
        if (!feet.isAir() || !head.isAir()) return false;

        return !isDangerous(floor);
    }

    private boolean isDangerous(Material material) {
        return switch (material) {
            case LAVA, MAGMA_BLOCK, FIRE, SOUL_FIRE, CACTUS,
                 CAMPFIRE, SOUL_CAMPFIRE, POWDER_SNOW -> true;
            default -> false;
        };
    }

    private String dimensionName(World world) {
        return switch (world.getEnvironment()) {
            case NORMAL -> "Overworld";
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "world";
        };
    }
}
