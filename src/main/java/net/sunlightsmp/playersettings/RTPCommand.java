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

        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage(ChatColor.RED + "☀ RTP is only available in the Overworld.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "☀ " + ChatColor.YELLOW + "Finding a safe random location...");

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                attempts++;
                World world = player.getWorld();

                // Random location within a 5,000 block radius.
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = 1_000 + random.nextDouble() * 4_000;
                int x = (int) Math.round(Math.cos(angle) * distance);
                int z = (int) Math.round(Math.sin(angle) * distance);

                int y = world.getHighestBlockYAt(x, z);
                Location target = new Location(world, x + 0.5, y + 1.0, z + 0.5);

                if (isSafe(target)) {
                    player.teleport(target);
                    player.sendMessage(ChatColor.GREEN + "☀ RTP successful! Teleported to a random safe location.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    cancel();
                    return;
                }

                if (attempts >= 20) {
                    player.sendMessage(ChatColor.RED + "☀ Couldn't find a safe location. Try /rtp again.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);

        return true;
    }

    private boolean isSafe(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

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
}
