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
    private SunlightSellaxe sellaxe;
    private SunlightTreeChopper treeChopper;
    private SunlightShardBooster shardBooster;
    private SunlightBucket sunlightBucket;
    private SunflowerManager sunflowerManager;
    private SunlightCrateManager crateManager;
    private KeyAllManager keyAllManager;

    @Override public void onEnable() {
        saveDefaultConfig(); settings = new PlayerSettingsManager(this);
        SettingsListener settingsListener = new SettingsListener(this);
        getServer().getPluginManager().registerEvents(settingsListener, this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatTagListener(this), this);
        tpaManager = new TPAManager(this);
        getServer().getPluginManager().registerEvents(tpaManager, this);
        getServer().getPluginManager().registerEvents(new TPAPlayerListener(tpaManager), this);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() { @org.bukkit.event.EventHandler public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) { org.bukkit.entity.Player player=event.getPlayer(); player.sendMessage(org.bukkit.ChatColor.GOLD+"✦ "+org.bukkit.ChatColor.YELLOW+"Welcome to Sunlight SMP!"+org.bukkit.ChatColor.GOLD+" ✦"); player.sendMessage(org.bukkit.ChatColor.GRAY+"☀ Discord: "+org.bukkit.ChatColor.YELLOW+"/discord"+org.bukkit.ChatColor.DARK_GRAY+" | "+org.bukkit.ChatColor.GRAY+"Store: "+org.bukkit.ChatColor.YELLOW+"/store"); player.sendTitle(org.bukkit.ChatColor.GOLD+"Welcome to Sunlight SMP!","",10,60,20); player.playSound(player.getLocation(),org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,1f,1f); player.playSound(player.getLocation(),org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,8f,1.2f); } }, this);
        if(getCommand("settings")!=null)getCommand("settings").setExecutor(new SettingsCommand(settingsListener));
        sunlightPickaxe=new SunlightPickaxe(this); getServer().getPluginManager().registerEvents(sunlightPickaxe,this); getServer().getPluginManager().registerEvents(new SunlightAntiDupe(this),this);
        sellaxe=new SunlightSellaxe(this); treeChopper=new SunlightTreeChopper(this); shardBooster=new SunlightShardBooster(this); sunlightBucket=new SunlightBucket(this); sunflowerManager=new SunflowerManager(this); crateManager=new SunlightCrateManager(this); keyAllManager=new KeyAllManager(this);
        getServer().getPluginManager().registerEvents(new SunflowerListener(sunflowerManager),this); getServer().getPluginManager().registerEvents(sellaxe,this); getServer().getPluginManager().registerEvents(treeChopper,this); getServer().getPluginManager().registerEvents(shardBooster,this); getServer().getPluginManager().registerEvents(sunlightBucket,this); getServer().getPluginManager().registerEvents(crateManager,this);
        if(getCommand("sunlightsellaxe")!=null)getCommand("sunlightsellaxe").setExecutor((s,c,l,a)->{if(!(s instanceof org.bukkit.entity.Player p))return true; var left=p.getInventory().addItem(sellaxe.create());left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));p.sendMessage(org.bukkit.ChatColor.GOLD+"☀ Sunlight Sellaxe received! "+org.bukkit.ChatColor.GRAY+"Expires in 5 days.");return true;});
        if(getCommand("sunlighttreechopper")!=null)getCommand("sunlighttreechopper").setExecutor((s,c,l,a)->{if(!(s instanceof org.bukkit.entity.Player p))return true;var left=p.getInventory().addItem(treeChopper.create());left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));return true;});
        if(getCommand("sunlightshardbooster")!=null)getCommand("sunlightshardbooster").setExecutor((s,c,l,a)->{if(!(s instanceof org.bukkit.entity.Player p))return true;var left=p.getInventory().addItem(shardBooster.create());left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));return true;});
        if(getCommand("sunlightbucket")!=null)getCommand("sunlightbucket").setExecutor((s,c,l,a)->{if(!(s instanceof org.bukkit.entity.Player p))return true;var left=p.getInventory().addItem(sunlightBucket.create());left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));return true;});
        if(getCommand("sunflower")!=null)getCommand("sunflower").setExecutor(new SunflowerCommand(this,sunflowerManager)); if(getCommand("sunflowerspawner")!=null)getCommand("sunflowerspawner").setExecutor(new SunflowerSpawnerCommand(this,sunflowerManager));
        if(getCommand("keyall")!=null)getCommand("keyall").setExecutor((s,c,l,a)->{if(!s.hasPermission("sunlightsmp.keyall")){s.sendMessage(org.bukkit.ChatColor.RED+"No permission.");return true;}if(a.length==1&&a[0].equalsIgnoreCase("reload")){crateManager.reload();s.sendMessage(org.bukkit.ChatColor.GREEN+"☀ Crates reloaded!");return true;}if(a.length<2){s.sendMessage(org.bukkit.ChatColor.YELLOW+"☀ /keyall <common|spawner|sunlight|crimson|sunset> <amount>");return true;}if(!java.util.List.of("common","spawner","sunlight","crimson","sunset").contains(a[0].toLowerCase())){s.sendMessage(org.bukkit.ChatColor.RED+"☀ Invalid crate. Use common, spawner, sunlight, crimson or sunset.");return true;}int n;try{n=Integer.parseInt(a[1]);}catch(Exception e){s.sendMessage(org.bukkit.ChatColor.RED+"Amount must be a number.");return true;}keyAllManager.run(a[0],n);return true;});
        if(getCommand("crates")!=null)getCommand("crates").setExecutor((s,c,l,a)->{if(s instanceof org.bukkit.entity.Player p)crateManager.openMenu(p);return true;});
        if(getCommand("crate")!=null)getCommand("crate").setExecutor((s,c,l,a)->{if(!(s instanceof org.bukkit.entity.Player p))return true;if(a.length==1){if(a[0].equalsIgnoreCase("reload")){if(!p.hasPermission("sunlightsmp.crates.admin")){p.sendMessage(org.bukkit.ChatColor.RED+"No permission.");return true;}crateManager.reload();p.sendMessage(org.bukkit.ChatColor.GREEN+"☀ Crates reloaded!");return true;}if(a[0].equalsIgnoreCase("edit")){p.sendMessage(org.bukkit.ChatColor.YELLOW+"☀ /crate edit <crate>");return true;}crateManager.openCrate(p,a[0]);return true;}if(a.length>=2&&a[0].equalsIgnoreCase("edit")){crateManager.openEditor(p,a[1]);return true;}if(a.length>=2&&a[0].equalsIgnoreCase("key")){if(!p.hasPermission("sunlightsmp.crates.admin")){p.sendMessage(org.bukkit.ChatColor.RED+"No permission.");return true;}int n=1;try{if(a.length>=3)n=Integer.parseInt(a[2]);}catch(Exception ignored){}crateManager.giveKey(p,a[1],n);return true;}p.sendMessage(org.bukkit.ChatColor.YELLOW+"☀ /crate <crate> | /crate edit <crate> | /crate reload");return true;});
        if(getCommand("sunlightpickaxe")!=null)getCommand("sunlightpickaxe").setExecutor((s,c,l,a)->{if(!(s instanceof org.bukkit.entity.Player p)){s.sendMessage("Only players can use this command.");return true;}var left=p.getInventory().addItem(sunlightPickaxe.create());left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));return true;});
        sellMenu=new SellMenu(this);getServer().getPluginManager().registerEvents(sellMenu,this);if(getCommand("sell")!=null)getCommand("sell").setExecutor(new SellCommand(sellMenu)); worthCommand=new WorthCommand(this);getServer().getPluginManager().registerEvents(worthCommand,this);if(getCommand("worth")!=null){getCommand("worth").setExecutor(worthCommand);getCommand("worth").setTabCompleter(worthCommand);} shopMenu=new ShopMenu(this);getServer().getPluginManager().registerEvents(shopMenu,this);if(getCommand("shop")!=null)getCommand("shop").setExecutor(new ShopCommand(shopMenu));
        RTPCommand rtp=new RTPCommand(this);getServer().getPluginManager().registerEvents(rtp,this);if(getCommand("rtp")!=null)getCommand("rtp").setExecutor(rtp); auctionManager=new AuctionHouseManager(this);auctionMenu=new AuctionHouseMenu(this,auctionManager);getServer().getPluginManager().registerEvents(auctionMenu,this);AuctionHouseCommand ah=new AuctionHouseCommand(this,auctionManager,auctionMenu);if(getCommand("ah")!=null){getCommand("ah").setExecutor(ah);getCommand("ah").setTabCompleter(ah);} TPACommand tc=new TPACommand(tpaManager);for(String x:new String[]{"tpa","tpahere","tpaccept","tpdeny","tpacancel"})if(getCommand(x)!=null){getCommand(x).setExecutor(tc);getCommand(x).setTabCompleter(tc);}
        if(getCommand("discord")!=null)getCommand("discord").setExecutor((s,c,l,a)->{s.sendMessage(org.bukkit.ChatColor.GOLD+"☀ "+org.bukkit.ChatColor.YELLOW+"Sunlight SMP Discord");s.sendMessage(org.bukkit.ChatColor.GRAY+"Join the community: "+org.bukkit.ChatColor.AQUA+"https://discord.gg/9a9THhwDT");return true;}); if(getCommand("store")!=null)getCommand("store").setExecutor((s,c,l,a)->{s.sendMessage(org.bukkit.ChatColor.GOLD+"☀ "+org.bukkit.ChatColor.YELLOW+"Sunlight SMP Store");s.sendMessage(org.bukkit.ChatColor.GRAY+"Shop here: "+org.bukkit.ChatColor.YELLOW+"https://sunlight-dawn-shop.lovable.app/");return true;});
        getServer().getScheduler().runTaskTimer(this,()->{for(org.bukkit.entity.Player p:getServer().getOnlinePlayers()){if(!settings.get(p,Setting.NOTIFICATIONS))continue;p.sendMessage(org.bukkit.ChatColor.GOLD+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━");p.sendMessage(org.bukkit.ChatColor.GOLD+"☀ "+org.bukkit.ChatColor.YELLOW+"SUNLIGHT SMP");p.sendMessage(org.bukkit.ChatColor.GRAY+"Discord: "+org.bukkit.ChatColor.AQUA+"https://discord.gg/9a9THhwDT");p.sendMessage(org.bukkit.ChatColor.GRAY+"Store: "+org.bukkit.ChatColor.YELLOW+"https://sunlight-dawn-shop.lovable.app/");p.sendMessage(org.bukkit.ChatColor.GOLD+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━");}},3600L,3600L);
    }
    @Override public void onDisable(){if(tpaManager!=null)for(org.bukkit.entity.Player p:getServer().getOnlinePlayers())tpaManager.cleanup(p);if(settings!=null)settings.save();if(sunflowerManager!=null)sunflowerManager.save();if(auctionManager!=null)auctionManager.save();}
    public PlayerSettingsManager getSettings(){return settings;} public Map<UUID,Location> getTpaStartLocations(){return tpaStartLocations;}
}