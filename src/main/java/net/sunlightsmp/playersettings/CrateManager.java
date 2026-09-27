package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CrateManager {
    private final SunlightPlayerSettings plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<String, CrateType> locations = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> openings = new ConcurrentHashMap<>();
    private final NamespacedKey keyTag;

    public CrateManager(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
        if (!file.exists()) try { plugin.getDataFolder().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        this.keyTag = new NamespacedKey(plugin, "crate_key");
        loadLocations();
    }

    public void loadLocations() {
        data = YamlConfiguration.loadConfiguration(file);
        locations.clear();
        ConfigurationSection section = data.getConfigurationSection("locations");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            String world = section.getString(id + ".world");
            CrateType type = parse(section.getString(id + ".type"));
            if (world != null && type != null)
                locations.put(locationKey(world, data.getInt(id + ".x"), data.getInt(id + ".y"), data.getInt(id + ".z")), type);
        }
    }

    public void saveLocations() {
        data.set("locations", null);
        int i = 0;
        for (Map.Entry<String, CrateType> e : locations.entrySet()) {
            String[] p = e.getKey().split("\\|", -1);
            String id = "crate_" + (++i);
            data.set("locations." + id + ".world", p[0]);
            data.set("locations." + id + ".x", Integer.parseInt(p[1]));
            data.set("locations." + id + ".y", Integer.parseInt(p[2]));
            data.set("locations." + id + ".z", Integer.parseInt(p[3]));
            data.set("locations." + id + ".type", e.getValue().name());
        }
        try { data.save(file); } catch (IOException ex) { plugin.getLogger().warning("Could not save crates.yml: " + ex.getMessage()); }
    }

    private String locationKey(String world, int x, int y, int z) { return world + "|" + x + "|" + y + "|" + z; }

    public CrateType getCrate(Location l) {
        if (l == null || l.getWorld() == null) return null;
        return locations.get(locationKey(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ()));
    }

    public void setCrate(Location l, CrateType type) {
        locations.put(locationKey(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ()), type);
        saveLocations();
    }

    public void removeCrate(Location l) {
        if (l != null && l.getWorld() != null) {
            locations.remove(locationKey(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ()));
            saveLocations();
        }
    }

    public ItemStack createKey(CrateType type, int amount) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "✦ " + type.displayName() + " Key");
        meta.setLore(List.of(ChatColor.GRAY + "Use this key at a " + type.displayName() + " Crate."));
        meta.getPersistentDataContainer().set(keyTag, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public CrateType getKeyType(ItemStack item) {
        if (item == null || item.getType() != Material.TRIPWIRE_HOOK || !item.hasItemMeta()) return null;
        return parse(item.getItemMeta().getPersistentDataContainer().get(keyTag, PersistentDataType.STRING));
    }

    public ItemStack createCrateItem(CrateType type, int amount) {
        ItemStack item = new ItemStack(type.icon(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "✦ " + type.displayName() + " Crate");
        meta.setLore(List.of(ChatColor.GRAY + "Place this block, then use /crate set " + type.name().toLowerCase(Locale.ROOT) + "."));
        item.setItemMeta(meta);
        return item;
    }

    public void openCrate(Player player, CrateType type) {
        if (openings.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already opening a crate.");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (getKeyType(hand) != type) {
            player.sendMessage(ChatColor.RED + "You need a " + type.displayName() + " Key.");
            return;
        }

        List<CrateReward> rewards = rewards(type);
        if (rewards.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This crate has no rewards configured.");
            return;
        }

        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);

        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "✦ " + type.displayName() + " Crate");
        player.openInventory(inv);
        Random random = new Random();
        final int[] ticks = {0};

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopOpening(player);
                return;
            }
            ticks[0]++;
            if (ticks[0] <= 50) {
                fillSpin(inv, rewards, random);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.45f, 1.0f + ticks[0] / 90f);
                return;
            }
            finishOpening(player, type, inv, chooseReward(rewards, random));
        }, 0L, 2L);

        openings.put(player.getUniqueId(), task);
    }

    private void fillSpin(Inventory inv, List<CrateReward> rewards, Random random) {
        for (int i = 0; i < 9; i++) {
            inv.setItem(9 + i, displayItem(rewards.get(random.nextInt(rewards.size())), false));
        }
    }

    private void finishOpening(Player player, CrateType type, Inventory inv, CrateReward reward) {
        BukkitTask task = openings.remove(player.getUniqueId());
        if (task != null) task.cancel();

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, null);
        inv.setItem(13, displayItem(reward, true));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f,
                type == CrateType.SUNLIGHT ? 0.7f : 1.1f);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0),
                type == CrateType.SUNLIGHT ? 100 : 35, 0.5, 0.8, 0.5, 0.05);

        giveReward(player, reward);

        player.sendMessage(ChatColor.GOLD + "✦ " + ChatColor.YELLOW + "You won "
                + ChatColor.WHITE + reward.name() + ChatColor.YELLOW + " from the "
                + type.displayName() + " Crate!");

        if (type == CrateType.SUNLIGHT || reward.chance() <= 2.0) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "✦ " + ChatColor.YELLOW + player.getName()
                    + " won " + ChatColor.WHITE + reward.name() + ChatColor.YELLOW
                    + " from the " + type.displayName() + " Crate!");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.closeInventory();
        }, 40L);
    }

    private ItemStack displayItem(CrateReward reward, boolean winner) {
        Material material = reward.material() == Material.AIR ? Material.PAPER : reward.material();
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, reward.amount())));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((winner ? ChatColor.GOLD + "✦ " : ChatColor.YELLOW + "") + reward.name());
        meta.setLore(List.of(ChatColor.GRAY + "Chance: " + reward.chance() + "%"));
        item.setItemMeta(meta);
        return item;
    }

    private void giveReward(Player player, CrateReward reward) {
        if (reward.material() != Material.AIR && reward.amount() > 0) {
            HashMap<Integer, ItemStack> left = player.getInventory().addItem(
                    new ItemStack(reward.material(), reward.amount()));
            left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }

        for (String command : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    command.replace("%player%", player.getName()));
        }
    }

    public List<CrateReward> rewards(CrateType type) {
        List<CrateReward> result = new ArrayList<>();
        String base = "crates." + type.name().toLowerCase(Locale.ROOT) + ".rewards";
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(base);
        if (section == null) return result;

        for (String id : section.getKeys(false)) {
            String path = base + "." + id;
            Material mat = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "STONE"));
            if (mat == null) continue;

            result.add(new CrateReward(
                    plugin.getConfig().getString(path + ".name", mat.name()),
                    mat,
                    plugin.getConfig().getInt(path + ".amount", 1),
                    plugin.getConfig().getDouble(path + ".chance", 1),
                    plugin.getConfig().getStringList(path + ".commands")
            ));
        }
        return result;
    }

    public void preview(Player player, CrateType type) {
        List<CrateReward> rewards = rewards(type);
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_AQUA + "✦ " + type.displayName() + " Rewards");
        for (int i = 0; i < rewards.size() && i < 54; i++) {
            inv.setItem(i, displayItem(rewards.get(i), false));
        }
        player.openInventory(inv);
    }

    public void stopOpening(Player player) {
        BukkitTask task = openings.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public boolean isOpening(Player player) {
        return openings.containsKey(player.getUniqueId());
    }

    public void shutdown() {
        openings.values().forEach(BukkitTask::cancel);
        openings.clear();
    }

    private CrateReward chooseReward(List<CrateReward> rewards, Random random) {
        double total = rewards.stream()
                .mapToDouble(CrateReward::chance)
                .filter(v -> v > 0)
                .sum();

        if (total <= 0) return rewards.get(random.nextInt(rewards.size()));

        double roll = random.nextDouble() * total;
        double cursor = 0;

        for (CrateReward reward : rewards) {
            if (reward.chance() <= 0) continue;
            cursor += reward.chance();
            if (roll < cursor) return reward;
        }

        return rewards.get(rewards.size() - 1);
    }

    private CrateType parse(String value) {
        if (value == null) return null;
        try {
            return CrateType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
