package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SunflowerManager {
    private final SunlightPlayerSettings plugin;
    private final Map<UUID, Long> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClaim = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAfkAward = new ConcurrentHashMap<>();
    private final File file;

    public SunflowerManager(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "sunflowers.yml");
        load();
        for (Player p : plugin.getServer().getOnlinePlayers()) markActive(p);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public long get(Player p) { return balances.getOrDefault(p.getUniqueId(), 0L); }
    public long get(UUID id) { return balances.getOrDefault(id, 0L); }

    public Map<UUID, Long> topBalances(int limit) {
        List<Map.Entry<UUID, Long>> entries = new ArrayList<>(balances.entrySet());
        entries.sort(Map.Entry.<UUID, Long>comparingByValue().reversed());
        Map<UUID, Long> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Long> e : entries) {
            if (out.size() >= Math.max(1, limit)) break;
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    public void add(Player p, long amount) {
        if (amount <= 0) return;
        balances.merge(p.getUniqueId(), amount, Long::sum);
    }

    public boolean remove(Player p, long amount) {
        if (amount <= 0 || get(p) < amount) return false;
        balances.put(p.getUniqueId(), get(p) - amount);
        return true;
    }

    public void markActive(Player p) {
        lastActivity.put(p.getUniqueId(), System.currentTimeMillis());
        lastAfkAward.putIfAbsent(p.getUniqueId(), System.currentTimeMillis());
    }

    public void join(Player p) {
        markActive(p);
        lastClaim.putIfAbsent(p.getUniqueId(), 0L);
        lastAfkAward.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public boolean claim(Player p) {
        long now = System.currentTimeMillis();
        long previous = lastClaim.getOrDefault(p.getUniqueId(), 0L);
        if (now - previous < 5 * 60 * 1000L) return false;
        long amount = hasBooster(p) ? 4 : 1;
        add(p, amount);
        lastClaim.put(p.getUniqueId(), now);
        return true;
    }

    private boolean hasBooster(Player p) {
        for (org.bukkit.inventory.ItemStack i : p.getInventory().getContents()) {
            if (i == null || !i.hasItemMeta()) continue;
            Byte b = i.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "sunlight_shard_booster"), org.bukkit.persistence.PersistentDataType.BYTE);
            Long expiry = i.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "sunlight_shard_booster_expiry"), org.bukkit.persistence.PersistentDataType.LONG);
            if (b != null && b == 1 && expiry != null && System.currentTimeMillis() < expiry) return true;
        }
        return false;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            lastActivity.putIfAbsent(id, now);
            lastAfkAward.putIfAbsent(id, now);
            if (now - lastActivity.get(id) < 5 * 60 * 1000L) continue;
            if (now - lastAfkAward.get(id) < 5 * 60 * 1000L) continue;

            long amount = hasBooster(p) ? 4 : 1;
            add(p, amount);
            lastAfkAward.put(id, now);
            p.sendMessage(ChatColor.GOLD + "☀ AFK Sunflowers: " + ChatColor.YELLOW + "+" + amount
                    + ChatColor.GRAY + " (Balance: " + ChatColor.YELLOW + get(p) + ChatColor.GRAY + ")");
        }
    }

    public void save() {
        org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) y.set("players." + e.getKey(), e.getValue());
        try { y.save(file); } catch (IOException ex) { plugin.getLogger().warning("Could not save sunflowers.yml: " + ex.getMessage()); }
    }

    private void load() {
        if (!file.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration y = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        org.bukkit.configuration.ConfigurationSection s = y.getConfigurationSection("players");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            try { balances.put(UUID.fromString(key), s.getLong(key)); } catch (IllegalArgumentException ignored) {}
        }
    }
}