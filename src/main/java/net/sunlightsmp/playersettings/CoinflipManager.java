package net.sunlightsmp.playersettings;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CoinflipManager {
    public static final class Stats {
        long flips;
        long heads;
        long tails;
    }

    private final SunlightPlayerSettings plugin;
    private final Map<UUID, Stats> stats = new ConcurrentHashMap<>();
    private final File file;

    public CoinflipManager(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "coinflip.yml");
        load();
    }

    public Stats get(UUID id) {
        return stats.computeIfAbsent(id, k -> new Stats());
    }

    public void record(UUID id, boolean heads) {
        Stats s = get(id);
        s.flips++;
        if (heads) s.heads++;
        else s.tails++;
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Stats> e : stats.entrySet()) {
            String path = "players." + e.getKey();
            Stats s = e.getValue();
            y.set(path + ".flips", s.flips);
            y.set(path + ".heads", s.heads);
            y.set(path + ".tails", s.tails);
        }
        try { y.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("Could not save coinflip.yml: " + ex.getMessage()); }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        var section = y.getConfigurationSection("players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String p = "players." + key;
                Stats s = new Stats();
                s.flips = y.getLong(p + ".flips");
                s.heads = y.getLong(p + ".heads");
                s.tails = y.getLong(p + ".tails");
                stats.put(id, s);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}