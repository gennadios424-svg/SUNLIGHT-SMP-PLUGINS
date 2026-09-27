package net.sunlightsmp.playersettings;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SunlightPlayerSettings extends JavaPlugin {
    private PlayerSettingsManager settings;
    private final Map<UUID, Location> tpaStartLocations = new ConcurrentHashMap<>();
    private TPAManager tpaManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new PlayerSettingsManager(this);

        SettingsListener settingsListener = new SettingsListener(this);
        getServer().getPluginManager().registerEvents(settingsListener, this);

        tpaManager = new TPAManager(this);
        getServer().getPluginManager().registerEvents(tpaManager, this);
        getServer().getPluginManager().registerEvents(new TPAPlayerListener(tpaManager), this);

        if (getCommand("settings") != null) {
            getCommand("settings").setExecutor(new SettingsCommand(settingsListener));
        }

        TPACommand tpaCommand = new TPACommand(tpaManager);
        for (String command : new String[]{"tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel"}) {
            if (getCommand(command) != null) {
                getCommand(command).setExecutor(tpaCommand);
                getCommand(command).setTabCompleter(tpaCommand);
            }
        }

        getLogger().info("Sunlight Player Settings + TPA enabled.");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                tpaManager.cleanup(player);
            }
        }
        if (settings != null) settings.save();
    }

    public PlayerSettingsManager getSettings() {
        return settings;
    }

    public Map<UUID, Location> getTpaStartLocations() {
        return tpaStartLocations;
    }
}
