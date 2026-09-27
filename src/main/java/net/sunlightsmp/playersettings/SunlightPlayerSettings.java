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
    private SunlightPickaxe sunlightPickaxe;
    private SellMenu sellMenu;
    private ShopMenu shopMenu;
    private WorthCommand worthCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        settings = new PlayerSettingsManager(this);

        SettingsListener settingsListener = new SettingsListener(this);
        getServer().getPluginManager().registerEvents(settingsListener, this);

        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);

        tpaManager = new TPAManager(this);
        getServer().getPluginManager().registerEvents(tpaManager, this);
        getServer().getPluginManager().registerEvents(new TPAPlayerListener(tpaManager), this);

        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                org.bukkit.entity.Player player = event.getPlayer();
                player.sendMessage(org.bukkit.ChatColor.GOLD + "✦ " + org.bukkit.ChatColor.YELLOW + "Welcome to Sunlight SMP!" + org.bukkit.ChatColor.GOLD + " ✦");
                player.sendMessage(org.bukkit.ChatColor.GRAY + "☀ Discord: " + org.bukkit.ChatColor.YELLOW + "/discord" + org.bukkit.ChatColor.DARK_GRAY + " | " + org.bukkit.ChatColor.GRAY + "Store: " + org.bukkit.ChatColor.YELLOW + "/store");
                player.sendTitle(org.bukkit.ChatColor.GOLD + "Welcome to Sunlight SMP!", "", 10, 60, 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 8.0f, 1.2f);
            }
        }, this);

        if (getCommand("settings") != null) getCommand("settings").setExecutor(new SettingsCommand(settingsListener));

        sunlightPickaxe = new SunlightPickaxe(this);
        getServer().getPluginManager().registerEvents(sunlightPickaxe, this);

        getServer().getPluginManager().registerEvents(new SunlightAntiDupe(this), this);

        if (getCommand("sunlightpickaxe") != null) getCommand("sunlightpickaxe").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) { sender.sendMessage("Only players can use this command."); return true; }
            if (!player.hasPermission("sunlightsmp.pickaxe")) { player.sendMessage(org.bukkit.ChatColor.RED+"No permission."); return true; }
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> left = player.getInventory().addItem(sunlightPickaxe.create());
            left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            player.sendMessage(org.bukkit.ChatColor.GOLD+"☀ "+org.bukkit.ChatColor.YELLOW+"Sunlight Pickaxe received! "+org.bukkit.ChatColor.GRAY+"It expires in 2 days.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
            return true;
        });

        sellMenu = new SellMenu(this);
        getServer().getPluginManager().registerEvents(sellMenu, this);
        if (getCommand("sell") != null) getCommand("sell").setExecutor(new SellCommand(sellMenu));

        worthCommand = new WorthCommand(this);
        getServer().getPluginManager().registerEvents(worthCommand, this);
        if (getCommand("worth") != null) {
            getCommand("worth").setExecutor(worthCommand);
            getCommand("worth").setTabCompleter(worthCommand);
        }

        shopMenu = new ShopMenu(this);
        getServer().getPluginManager().registerEvents(shopMenu, this);
        if (getCommand("shop") != null) getCommand("shop").setExecutor(new ShopCommand(shopMenu));

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

        if (getCommand("discord") != null) getCommand("discord").setExecutor((sender, command, label, args) -> {
            sender.sendMessage(org.bukkit.ChatColor.GOLD + "☀ " + org.bukkit.ChatColor.YELLOW + "Sunlight SMP Discord");
            sender.sendMessage(org.bukkit.ChatColor.GRAY + "Join the community: " + org.bukkit.ChatColor.AQUA + "https://discord.gg/9a9THhwDT");
            return true;
        });

        if (getCommand("store") != null) getCommand("store").setExecutor((sender, command, label, args) -> {
            sender.sendMessage(org.bukkit.ChatColor.GOLD + "☀ " + org.bukkit.ChatColor.YELLOW + "Sunlight SMP Store");
            sender.sendMessage(org.bukkit.ChatColor.GRAY + "Shop here: " + org.bukkit.ChatColor.YELLOW + "https://sunlight-dawn-shop.lovable.app/");
            return true;
        });

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                if (!settings.get(player, Setting.NOTIFICATIONS)) continue;
                player.sendMessage(org.bukkit.ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage(org.bukkit.ChatColor.GOLD + "☀ " + org.bukkit.ChatColor.YELLOW + "SUNLIGHT SMP");
                player.sendMessage(org.bukkit.ChatColor.GRAY + "Discord: " + org.bukkit.ChatColor.AQUA + "https://discord.gg/9a9THhwDT");
                player.sendMessage(org.bukkit.ChatColor.GRAY + "Store: " + org.bukkit.ChatColor.YELLOW + "https://sunlight-dawn-shop.lovable.app/");
                player.sendMessage(org.bukkit.ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
        }, 3600L, 3600L);

        getLogger().info("Sunlight Player Settings + TPA + Auction House + Sell/Worth/Shop enabled.");
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
