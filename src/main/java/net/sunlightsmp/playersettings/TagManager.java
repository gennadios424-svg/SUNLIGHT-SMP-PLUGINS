package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class TagManager {
    public static final long TAG_COST = 1000L;
    private final SunlightPlayerSettings plugin;
    private final SunflowerManager sunflower;
    private final File file;
    private final Map<UUID, Set<String>> owned = new HashMap<>();
    private final Map<UUID, String> equipped = new HashMap<>();
    private static final List<Tag> TAGS = List.of(
        new Tag("Sunlight", "☀", ChatColor.YELLOW),
        new Tag("Sun", "☀", ChatColor.GOLD),
        new Tag("Cool", "😎", ChatColor.YELLOW),
        new Tag("Genzo", "🌻", ChatColor.GOLD),
        new Tag("IDK", "❔", ChatColor.YELLOW)
    );

    public TagManager(SunlightPlayerSettings plugin, SunflowerManager sunflower) {
        this.plugin = plugin; this.sunflower = sunflower;
        this.file = new File(plugin.getDataFolder(), "tags.yml");
        load();
    }

    public List<Tag> getTags() { return TAGS; }
    public boolean owns(UUID id, String name) { return owned.getOrDefault(id, Set.of()).contains(name); }
    public String equipped(UUID id) { return equipped.get(id); }

    public String displayTag(UUID id) {
        String tag = equipped.get(id);
        if (tag == null) return "";
        Tag t = find(tag);
        return t == null ? "" : t.color() + "[" + t.name() + "] " + ChatColor.RESET;
    }

    public synchronized PurchaseResult purchase(Player player, String name) {
        Tag t = find(name);
        if (t == null) return PurchaseResult.INVALID;
        if (owns(player.getUniqueId(), t.name())) return PurchaseResult.ALREADY_OWNED;
        if (sunflower.get(player) < TAG_COST) return PurchaseResult.INSUFFICIENT;
        if (!sunflower.remove(player, TAG_COST)) return PurchaseResult.INSUFFICIENT;
        owned.computeIfAbsent(player.getUniqueId(), k -> new LinkedHashSet<>()).add(t.name());
        equipped.put(player.getUniqueId(), t.name());
        save();
        return PurchaseResult.PURCHASED;
    }

    public synchronized boolean equip(Player player, String name) {
        Tag t = find(name);
        if (t == null || !owns(player.getUniqueId(), t.name())) return false;
        equipped.put(player.getUniqueId(), t.name());
        save();
        return true;
    }

    public Tag find(String name) {
        return TAGS.stream().filter(t -> t.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> e : owned.entrySet())
            y.set("players." + e.getKey() + ".owned", new ArrayList<>(e.getValue()));
        for (Map.Entry<UUID, String> e : equipped.entrySet())
            y.set("players." + e.getKey() + ".equipped", e.getValue());
        try { y.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Could not save tags.yml: " + e.getMessage()); }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        var sec = y.getConfigurationSection("players");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(id);
                owned.put(uuid, new LinkedHashSet<>(y.getStringList("players." + id + ".owned")));
                String eq = y.getString("players." + id + ".equipped");
                if (eq != null && owns(uuid, eq)) equipped.put(uuid, eq);
            } catch (Exception ignored) {}
        }
    }

    public enum PurchaseResult { PURCHASED, INSUFFICIENT, ALREADY_OWNED, INVALID }
    public record Tag(String name, String icon, ChatColor color) {}
}
