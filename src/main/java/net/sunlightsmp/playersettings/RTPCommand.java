package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Random;

public final class RTPCommand implements org.bukkit.command.CommandExecutor, Listener {
    private static final String TITLE = ChatColor.GOLD + "☀ Sunlight RTP";
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

        open(player);
        return true;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        for (int slot : new int[]{0,1,2,3,4,5,6,7,8,18,19,20,21,22,23,24,25,26}) {
            inv.setItem(slot, item(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + " "));
        }

        inv.setItem(10, item(Material.GRASS_BLOCK,
                ChatColor.GREEN + "☀ Overworld",
                ChatColor.GRAY + "Random safe location",
                ChatColor.YELLOW + "15,000 blocks from 0,0",
                "",
                ChatColor.GREEN + "Click to teleport"));

        inv.setItem(13, item(Material.NETHERRACK,
                ChatColor.RED + "🔥 Nether",
                ChatColor.GRAY + "Random safe location",
                ChatColor.YELLOW + "15,000 blocks from 0,0",
                "",
                ChatColor.GREEN + "Click to teleport"));

        inv.setItem(16, item(Material.END_STONE,
                ChatColor.LIGHT_PURPLE + "✦ The End",
                ChatColor.GRAY + "Random safe location",
                ChatColor.YELLOW + "15,000 blocks from 0,0",
                "",
                ChatColor.GREEN + "Click to teleport"));

        inv.setItem(22, item(Material.BARRIER,
                ChatColor.RED + "Close",
                ChatColor.GRAY + "Close the RTP menu"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.15f);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(TITLE)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 22) {
            player.closeInventory();
            return;
        }

        if (slot == 10) {
            startRtp(player, World.Environment.NORMAL);
        } else if (slot == 13) {
            startRtp(player, World.Environment.NETHER);
        } else if (slot == 16) {
            startRtp(player, World.Environment.THE_END);
        }
    }

    private void startRtp(Player player, World.Environment environment) {
        World world = findWorld(environment);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "☀ That dimension is not available on this server.");
            return;
        }

        player.closeInventory();
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
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    cancel();
                    return;
                }

                if (attempts >= 30) {
                    player.sendMessage(ChatColor.RED + "☀ Couldn't find a safe location. Try /rtp again.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private World findWorld(World.Environment environment) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == environment) return world;
        }
        return null;
    }

    private Location randomLocation(World world) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        int x = (int) Math.round(Math.cos(angle) * 15_000);
        int z = (int) Math.round(Math.sin(angle) * 15_000);
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
            case THE_END -> "The End";
            default -> "world";
        };
    }
}
