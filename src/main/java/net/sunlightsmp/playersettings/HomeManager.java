package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class HomeManager implements Listener {
    private static final String TITLE = "☀ Your Homes";
    private final SunlightPlayerSettings plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, TeleportTask> pending = new HashMap<>();
    private final Map<UUID, String> deleteConfirm = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public HomeManager(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "homes.yml");
        reload();
    }

    public void reload() { data = YamlConfiguration.loadConfiguration(file); }
    public void save() { try { data.save(file); } catch (IOException e) { plugin.getLogger().warning("Could not save homes.yml: " + e.getMessage()); } }

    private String key(UUID u, String name) { return "players." + u + ".homes." + name.toLowerCase(Locale.ROOT); }
    private int limit(Player p) {
        int configured = plugin.getConfig().getInt("homes.default-limit", 3);
        if (p.hasPermission("homes.admin")) return Math.max(configured, plugin.getConfig().getInt("homes.admin-limit", 100));
        for (int n = plugin.getConfig().getInt("homes.max-permission-check", 50); n >= 1; n--)
            if (p.hasPermission("homes.limit." + n)) return n;
        if (p.hasPermission("homes.premium")) return plugin.getConfig().getInt("homes.premium-limit", 10);
        if (p.hasPermission("homes.vip")) return plugin.getConfig().getInt("homes.vip-limit", 5);
        return configured;
    }
    private List<String> names(UUID u) {
        ConfigurationSection s = data.getConfigurationSection("players." + u + ".homes");
        if (s == null) return new ArrayList<>();
        return new ArrayList<>(s.getKeys(false));
    }
    private ItemStack item(Material m, String name, String... lore) {
        ItemStack x = new ItemStack(m); ItemMeta im = x.getItemMeta();
        im.setDisplayName(name); im.setLore(Arrays.asList(lore)); x.setItemMeta(im); return x;
    }
    private String worldName(String w) {
        World world = Bukkit.getWorld(w);
        if (world == null) return w;
        return switch (world.getEnvironment()) { case NETHER -> "Nether"; case THE_END -> "The End"; default -> "Overworld"; };
    }

    public void openHomes(Player p) {
        List<String> hs = names(p.getUniqueId());
        int pages = Math.max(1, (int)Math.ceil(hs.size() / 45.0));
        openPage(p, 0, hs, pages);
    }
    private void openPage(Player p, int page, List<String> hs, int pages) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE + " §8(" + (page + 1) + "/" + pages + ")");
        for (int i=0;i<54;i++) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        int start=page*45;
        for (int i=0;i<45 && start+i<hs.size();i++) {
            String n=hs.get(start+i), path=key(p.getUniqueId(),n), w=data.getString(path+".world","?");
            double x=data.getDouble(path+".x"), y=data.getDouble(path+".y"), z=data.getDouble(path+".z");
            inv.setItem(i, item(Material.COMPASS, ChatColor.YELLOW+"☀ "+n,
                    ChatColor.GRAY+"📍 "+worldName(w), ChatColor.GRAY+"X: "+fmt(x)+"  Y: "+fmt(y)+"  Z: "+fmt(z),
                    ChatColor.YELLOW+"Left-click: teleport", ChatColor.RED+"Right-click: delete"));
        }
        inv.setItem(45,item(Material.EMERALD,ChatColor.GREEN+"✚ CREATE HOME",ChatColor.GRAY+"Use /sethome <name>"));
        inv.setItem(48,item(Material.PAPER,ChatColor.YELLOW+"☀ HOME INFO",ChatColor.GRAY+"Use /homeinfo <name>"));
        inv.setItem(49,item(Material.BARRIER,ChatColor.RED+"CLOSE"));
        if(page>0) inv.setItem(47,item(Material.ARROW,ChatColor.YELLOW+"← PREVIOUS"));
        if(page<pages-1) inv.setItem(51,item(Material.ARROW,ChatColor.YELLOW+"NEXT →"));
        p.openInventory(inv);
    }
    private String fmt(double d){ return String.valueOf(Math.round(d*100.0)/100.0); }

    public boolean setHome(Player p, String raw) {
        if(!p.hasPermission("homes.sethome")){p.sendMessage(ChatColor.RED+"❌ You do not have permission to set homes.");return false;}
        if (!validName(raw)) { p.sendMessage(ChatColor.RED+"☀ Invalid home name."); return false; }
        String n=raw.toLowerCase(Locale.ROOT);
        String path=key(p.getUniqueId(),n);
        boolean exists=data.contains(path);
        if(!exists && names(p.getUniqueId()).size()>=limit(p)){
            p.sendMessage(ChatColor.RED+"❌ You have reached your home limit! "+names(p.getUniqueId()).size()+"/"+limit(p)); return false;
        }
        Location l=p.getLocation();
        data.set(path+".world",l.getWorld().getName()); data.set(path+".x",l.getX()); data.set(path+".y",l.getY()); data.set(path+".z",l.getZ());
        data.set(path+".yaw",l.getYaw()); data.set(path+".pitch",l.getPitch()); save();
        p.sendMessage(ChatColor.GREEN+"✅ Home "+ChatColor.YELLOW+n+ChatColor.GREEN+(exists?" has been replaced!":" has been created!"));
        return true;
    }
    private boolean validName(String n){ return n!=null && n.matches("[A-Za-z0-9_-]{1,24}"); }

    public void deleteHome(Player p,String raw) {
        if(!p.hasPermission("homes.delete")){p.sendMessage(ChatColor.RED+"❌ You do not have permission to delete homes.");return;}
        String n=raw.toLowerCase(Locale.ROOT);
        if(!data.contains(key(p.getUniqueId(),n))){p.sendMessage(ChatColor.RED+"❌ Home '"+n+"' does not exist.");return;}
        deleteConfirm.put(p.getUniqueId(),n);
        Inventory i=Bukkit.createInventory(null,27,"⚠ Delete Home: "+n);
        for(int s=0;s<27;s++) i.setItem(s,item(Material.BLACK_STAINED_GLASS_PANE," "));
        i.setItem(11,item(Material.LIME_CONCRETE,ChatColor.GREEN+"CONFIRM",ChatColor.GRAY+"Delete "+n));
        i.setItem(15,item(Material.RED_CONCRETE,ChatColor.RED+"CANCEL"));
        p.openInventory(i);
    }
    private void confirmDelete(Player p){
        String n=deleteConfirm.remove(p.getUniqueId()); if(n==null)return;
        data.set(key(p.getUniqueId(),n),null); save(); p.closeInventory(); p.sendMessage(ChatColor.GREEN+"✅ Home "+ChatColor.YELLOW+n+ChatColor.GREEN+" has been deleted.");
    }
    private Location getHome(UUID u,String n){
        String path=key(u,n); if(!data.contains(path))return null;
        World w=Bukkit.getWorld(data.getString(path+".world",""));
        if(w==null)return null;
        return new Location(w,data.getDouble(path+".x"),data.getDouble(path+".y"),data.getDouble(path+".z"),(float)data.getDouble(path+".yaw"),(float)data.getDouble(path+".pitch"));
    }
    public void teleport(Player p,String raw){
        if(!p.hasPermission("homes.teleport")){p.sendMessage(ChatColor.RED+"❌ You do not have permission to teleport to homes.");return;}
        String n=raw.toLowerCase(Locale.ROOT); Location l=getHome(p.getUniqueId(),n);
        if(l==null){p.sendMessage(ChatColor.RED+"❌ Home '"+n+"' does not exist or its world is unavailable.");return;}
        if(p.isDead()){p.sendMessage(ChatColor.RED+"❌ You cannot teleport while dead.");return;}
        if(plugin.getConfig().getBoolean("homes.combat-restriction",true) && plugin.isPlayerInCombat(p)){p.sendMessage(ChatColor.RED+"❌ You cannot teleport while in combat!");return;}
        long now=System.currentTimeMillis(), until=cooldownUntil.getOrDefault(p.getUniqueId(),0L);
        if(until>now){long sec=(long)Math.ceil((until-now)/1000.0);p.sendMessage(ChatColor.YELLOW+"⏳ You must wait "+sec+" seconds before teleporting again.");return;}
        cancelPending(p,false);
        int delay=Math.max(0,plugin.getConfig().getInt("homes.teleport-delay",5));
        if(delay==0){doTeleport(p,n,l);return;}
        TeleportTask tt=new TeleportTask(p,n,l); pending.put(p.getUniqueId(),tt);
        p.sendMessage(ChatColor.YELLOW+"⏳ Teleporting in "+delay+" seconds... "+ChatColor.GRAY+"Don't move!");
        tt.task=Bukkit.getScheduler().runTaskTimer(plugin,tt,0L,20L);
    }
    private void doTeleport(Player p,String n,Location l){
        Location safe=plugin.getConfig().getBoolean("homes.safety-check",true)?safe(l):l;
        if(safe==null){p.sendMessage(ChatColor.RED+"❌ Your home location is unsafe and no safe location was found.");return;}
        if(!safe.getWorld().equals(l.getWorld()) || safe.getX()!=l.getX() || safe.getY()!=l.getY() || safe.getZ()!=l.getZ())
            p.sendMessage(ChatColor.YELLOW+"⚠ Your home location is unsafe. Teleported you to the nearest safe location.");
        p.teleport(safe); int cd=Math.max(0,plugin.getConfig().getInt("homes.cooldown-seconds",0)); if(cd>0) cooldownUntil.put(p.getUniqueId(),System.currentTimeMillis()+cd*1000L); p.sendMessage(ChatColor.GREEN+"☀ Teleported to home "+ChatColor.YELLOW+n+ChatColor.GREEN+".");
    }
    private Location safe(Location base){
        World w=base.getWorld(); if(w==null)return null;
        int bx=base.getBlockX(), by=base.getBlockY(), bz=base.getBlockZ();
        for(int r=0;r<=4;r++) for(int dx=-r;dx<=r;dx++) for(int dz=-r;dz<=r;dz++){
            int x=bx+dx,z=bz+dz; int top=Math.min(w.getMaxHeight()-2,by+4), bot=Math.max(w.getMinHeight()+2,by-8);
            for(int y=top;y>=bot;y--) if(isSafe(w,x,y,z)) return new Location(w,x+.5,y,z+.5,base.getYaw(),base.getPitch());
        }
        return null;
    }
    private boolean isSafe(World w,int x,int y,int z){
        Block feet=w.getBlockAt(x,y,z), head=w.getBlockAt(x,y+1,z), ground=w.getBlockAt(x,y-1,z);
        if(!feet.isPassable()||!head.isPassable()||!ground.getType().isSolid())return false;
        Material gm=ground.getType(); if(gm==Material.LAVA||gm==Material.FIRE||gm==Material.SOUL_FIRE)return false;
        return !feet.isLiquid()&&!head.isLiquid()&&!ground.isLiquid();
    }
    private void cancelPending(Player p,boolean moved){
        TeleportTask t=pending.remove(p.getUniqueId()); if(t==null)return;
        if(t.task!=null)t.task.cancel();
        if(moved)p.sendMessage(ChatColor.RED+"❌ Teleport cancelled because you moved!");
    }
    private final class TeleportTask implements Runnable{
        final Player p; final String n; final Location l; final Location start; BukkitTask task; int left;
        TeleportTask(Player p,String n,Location l){this.p=p;this.n=n;this.l=l;this.start=p.getLocation().clone();this.left=Math.max(1,plugin.getConfig().getInt("homes.teleport-delay",5));}
        public void run(){
            if(!p.isOnline()||p.isDead()||(plugin.getConfig().getBoolean("homes.combat-restriction",true)&&plugin.isPlayerInCombat(p))){cancelPending(p,false);return;}
            if(p.getLocation().distanceSquared(start)>0.01){cancelPending(p,true);return;}
            if(left<=0){pending.remove(p.getUniqueId());task.cancel();doTeleport(p,n,l);return;}
            p.sendActionBar(ChatColor.YELLOW+"⏳ Teleporting in "+left+"s..."); left--;
        }
    }

    public void rename(Player p,String old,String nn){
        if(!p.hasPermission("homes.sethome")){p.sendMessage(ChatColor.RED+"❌ You do not have permission to rename homes.");return;}
        old=old.toLowerCase(Locale.ROOT); nn=nn.toLowerCase(Locale.ROOT);
        if(!validName(nn)){p.sendMessage(ChatColor.RED+"❌ Invalid new home name.");return;}
        String a=key(p.getUniqueId(),old), b=key(p.getUniqueId(),nn);
        if(!data.contains(a)){p.sendMessage(ChatColor.RED+"❌ Home '"+old+"' does not exist.");return;}
        if(data.contains(b)){p.sendMessage(ChatColor.RED+"❌ Home '"+nn+"' already exists.");return;}
        ConfigurationSection s=data.getConfigurationSection(a); if(s==null)return;
        data.set(b,s); data.set(a,null); save(); p.sendMessage(ChatColor.GREEN+"✅ Home renamed to "+ChatColor.YELLOW+nn+ChatColor.GREEN+".");
    }
    public void info(Player p,String raw){
        if(!p.hasPermission("homes.use")){p.sendMessage(ChatColor.RED+"❌ You do not have permission to view homes.");return;}
        String n=raw.toLowerCase(Locale.ROOT), path=key(p.getUniqueId(),n);
        if(!data.contains(path)){p.sendMessage(ChatColor.RED+"❌ Home '"+n+"' does not exist.");return;}
        String w=data.getString(path+".world","?"); double x=data.getDouble(path+".x"),y=data.getDouble(path+".y"),z=data.getDouble(path+".z");
        p.sendMessage(ChatColor.GOLD+"━━━━━━━━ HOME INFO ━━━━━━━━");
        p.sendMessage(ChatColor.YELLOW+"🏠 Home: "+ChatColor.WHITE+n);p.sendMessage(ChatColor.YELLOW+"🌎 World: "+ChatColor.WHITE+worldName(w));
        p.sendMessage(ChatColor.YELLOW+"📍 X: "+ChatColor.WHITE+fmt(x)+"  Y: "+fmt(y)+"  Z: "+fmt(z));
        p.sendMessage(ChatColor.YELLOW+"🧭 Yaw: "+ChatColor.WHITE+fmt(data.getDouble(path+".yaw"))+"  Pitch: "+fmt(data.getDouble(path+".pitch")));
        p.sendMessage(ChatColor.GOLD+"━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    public List<String> tab(UUID u){return names(u);}
    public void cancelOnQuit(Player p){cancelPending(p,false);deleteConfirm.remove(p.getUniqueId());}

    @EventHandler public void move(PlayerMoveEvent e){if(e.getTo()==null)return; if(e.getFrom().getX()!=e.getTo().getX()||e.getFrom().getY()!=e.getTo().getY()||e.getFrom().getZ()!=e.getTo().getZ()) cancelPending(e.getPlayer(),true);}
    @EventHandler public void quit(PlayerQuitEvent e){cancelOnQuit(e.getPlayer());}
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return; String t=e.getView().getTitle();
        if(t.startsWith(TITLE)){e.setCancelled(true); if(e.getClickedInventory()!=e.getView().getTopInventory())return; int s=e.getRawSlot(); List<String> hs=names(p.getUniqueId());
            if(s<45 && s<hs.size()){String n=hs.get(s); if(e.isRightClick()){deleteHome(p,n);} else teleport(p,n);return;}
            if(s==45){p.closeInventory();p.sendMessage(ChatColor.YELLOW+"☀ Use /sethome <name> to create a home.");}
            else if(s==47){openPage(p,Math.max(0,parsePage(t)-1),hs,Math.max(1,(int)Math.ceil(hs.size()/45.0)));}
            else if(s==51){openPage(p,Math.min(Math.max(0,(int)Math.ceil(hs.size()/45.0)-1),parsePage(t)+1),hs,Math.max(1,(int)Math.ceil(hs.size()/45.0)));}
            else if(s==49)p.closeInventory();
        } else if(t.startsWith("⚠ Delete Home: ")){e.setCancelled(true); if(e.getRawSlot()==11)confirmDelete(p); else if(e.getRawSlot()==15){deleteConfirm.remove(p.getUniqueId());p.closeInventory();}}
    }
    private int parsePage(String t){try{String q=t.substring(t.indexOf("(")+1,t.indexOf("/"));return Integer.parseInt(q)-1;}catch(Exception e){return 0;}}
}
