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
    private AuctionHouseManager auctionManager;
    private AuctionHouseMenu auctionMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new PlayerSettingsManager(this);

        SettingsListener settingsListener = new SettingsListener(this);
        getServer().getPluginManager().registerEvents(settingsListener, this);

        tpaManager = new TPAManager(this);
        getServer().getPluginManager().registerEvents(tpaManager, this);
        getServer().getPluginManager().registerEvents(new TPAPlayerListener(tpaManager), this);


        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                org.bukkit.entity.Player player = event.getPlayer();
                player.sendMessage(org.bukkit.ChatColor.GOLD + "✦ " + org.bukkit.ChatColor.YELLOW + "Welcome to Sunlight SMP!" + org.bukkit.ChatColor.GOLD + " ✦");
                player.sendTitle(org.bukkit.ChatColor.GOLD + "Welcome to Sunlight SMP!", "", 10, 60, 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 8.0f, 1.2f);
            }
        }, this);

        if (getCommand("settings") != null) getCommand("settings").setExecutor(new SettingsCommand(settingsListener));

        auctionManager = new AuctionHouseManager(this);
        auctionMenu = new AuctionHouseMenu(this, auctionManager);
        getServer().getPluginManager().registerEvents(auctionMenu, this);
        AuctionHouseCommand auctionCommand = new AuctionHouseCommand(this, auctionManager, auctionMenu);
        if (getCommand("ah") != null) { getCommand("ah").setExecutor(auctionCommand); getCommand("ah").setTabCompleter(auctionCommand); }

        TPACommand tpaCommand = new TPACommand(tpaManager);
        for (String command : new String[]{"tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel"}) {
            if (getCommand(command) != null) {
                getCommand(command).setExecutor(tpaCommand);
                getCommand(command).setTabCompleter(tpaCommand);
            }
        }


        getLogger().info("Sunlight Player Settings + TPA + Auction House enabled.");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) tpaManager.cleanup(player);
        }
        if (settings != null) settings.save();
        if (auctionManager != null) auctionManager.save();
    }

    public PlayerSettingsManager getSettings() { return settings; }
    public Map<UUID, Location> getTpaStartLocations() { return tpaStartLocations; }
}