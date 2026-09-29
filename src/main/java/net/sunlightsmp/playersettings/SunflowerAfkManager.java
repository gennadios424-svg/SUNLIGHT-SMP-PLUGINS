package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class SunflowerAfkManager implements CommandExecutor, Listener {
    private static final String ITEM_KEY = "sunflower_giver";
    private final SunlightPlayerSettings plugin;
    private final SunflowerManager currency;
    private final NamespacedKey giverKey;
    private final File file;
    private final Set<String> givers = new HashSet<>();
    private final Map<UUID, Long> insideSince = new HashMap<>();
    private Location afkLocation;

    public SunflowerAfkManager(SunlightPlayerSettings plugin, SunflowerManager currency) {
        this.plugin = plugin; this.currency = currency; this.giverKey = new NamespacedKey(plugin, ITEM_KEY);
        this.file = new File(plugin.getDataFolder(), "afk.yml"); load();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length == 0) {
            if (afkLocation == null) { msg(p, ChatColor.RED+"✕ "+ChatColor.WHITE+"The AFK location has not been set yet."); return true; }
            p.teleport(afkLocation.clone()); msg(p, ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP "+ChatColor.WHITE+"• Teleported to the "+ChatColor.YELLOW+"AFK Zone"+ChatColor.WHITE+"."); return true;
        }
        if (!p.hasPermission("sunlight.afk.admin") && !p.isOp()) { msg(p, ChatColor.RED+"✕ "+ChatColor.WHITE+"You don't have permission to use that."); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> { afkLocation=p.getLocation().clone(); save(); msg(p,ChatColor.GREEN+"☀ Success! "+ChatColor.WHITE+"AFK location set to "+ChatColor.YELLOW+p.getWorld().getName()+" "+p.getLocation().getBlockX()+", "+p.getLocation().getBlockY()+", "+p.getLocation().getBlockZ()+ChatColor.WHITE+"."); }
            case "give" -> { int amount=1; if(args.length>1)try{amount=Math.max(1,Integer.parseInt(args[1]));}catch(NumberFormatException ignored){} ItemStack item=createGiverItem(); item.setAmount(Math.min(64,amount)); Map<Integer,ItemStack> left=p.getInventory().addItem(item); left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i)); msg(p,ChatColor.GREEN+"☀ Success! "+ChatColor.WHITE+"You received "+ChatColor.YELLOW+amount+" "+ChatColor.WHITE+"Sunflower Giver."); }
            case "reload" -> { plugin.reloadConfig(); load(); msg(p,ChatColor.GREEN+"☀ Success! "+ChatColor.WHITE+"AFK configuration reloaded."); }
            case "remove" -> removeLookingAt(p);
            default -> msg(p,ChatColor.YELLOW+"☀ "+ChatColor.WHITE+"/sunafk "+ChatColor.GRAY+"[set|give|remove|reload]");
        } return true;
    }
    private void removeLookingAt(Player p){Location target=null;for(String key:new HashSet<>(givers)){Location l=parse(key);if(l!=null&&l.getWorld().equals(p.getWorld())&&l.distanceSquared(p.getEyeLocation())<=36&&p.hasLineOfSight(l.clone().add(.5,.5,.5))){target=l;break;}}if(target==null){msg(p,ChatColor.RED+"✕ "+ChatColor.WHITE+"Look at a Sunflower Giver within 6 blocks.");return;}givers.remove(key(target));save();msg(p,ChatColor.GREEN+"☀ Success! "+ChatColor.WHITE+"Sunflower Giver removed. The original block was untouched.");}
    @EventHandler public void onPlace(PlayerInteractEvent e){if(e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;ItemStack item=e.getItem();if(!isGiverItem(item))return;Player p=e.getPlayer();if(!p.hasPermission("sunlight.afk.admin")&&!p.isOp()){e.setCancelled(true);msg(p,ChatColor.RED+"✕ "+ChatColor.WHITE+"Only AFK admins can place Sunflower Givers.");return;}e.setCancelled(true);Location loc=e.getClickedBlock().getLocation().clone();givers.add(key(loc));save();if(item.getAmount()<=1)p.getInventory().setItemInMainHand(null);else item.setAmount(item.getAmount()-1);msg(p,ChatColor.GREEN+"☀ Giver placed! "+ChatColor.WHITE+"The original block remains unchanged.");p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,1f,1.5f);}
    private boolean isInside(Player p,Location loc){if(!p.getWorld().equals(loc.getWorld()))return false;BoundingBox b=p.getBoundingBox();BoundingBox zone=new BoundingBox(loc.getX(),loc.getY(),loc.getZ(),loc.getX()+1,loc.getY()+1,loc.getZ()+1);return b.overlaps(zone);}
    private void tick(){if(!plugin.getConfig().getBoolean("afk.giver-enabled",true))return;long now=System.currentTimeMillis();long interval=Math.max(1L,plugin.getConfig().getLong("afk.interval-minutes",5L))*60000L;long reward=Math.max(1L,plugin.getConfig().getLong("afk.reward",3L));for(Player p:plugin.getServer().getOnlinePlayers()){boolean inside=false;for(String key:givers){Location l=parse(key);if(l!=null&&isInside(p,l)){inside=true;break;}}UUID id=p.getUniqueId();if(!inside){if(insideSince.remove(id)!=null)msg(p,ChatColor.YELLOW+"☀ "+ChatColor.WHITE+"AFK reward timer stopped.");continue;}long start=insideSince.computeIfAbsent(id,k->{msg(p,ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"SUNLIGHT SMP");msg(p,ChatColor.YELLOW+"💤 "+ChatColor.WHITE+"You entered the "+ChatColor.YELLOW+"AFK Zone"+ChatColor.WHITE+".");return now;});if(now-start>=interval){currency.add(p,reward);insideSince.put(id,now);msg(p,ChatColor.GOLD+"🌻 "+ChatColor.YELLOW+"AFK REWARD "+ChatColor.WHITE+"• You received "+ChatColor.GREEN+"+"+reward+" "+ChatColor.WHITE+"Sunflowers!");msg(p,ChatColor.GRAY+"⏱ Next reward in "+ChatColor.YELLOW+format(interval));p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.7f,1.6f);}}}
    private String format(long ms){long sec=Math.max(0,ms/1000);return String.format("%d:%02d",sec/60,sec%60);}
    public ItemStack createGiverItem(){ItemStack item=new ItemStack(Material.SUNFLOWER);ItemMeta meta=item.getItemMeta();meta.setDisplayName(ChatColor.GOLD+"🌻 "+ChatColor.YELLOW+"Sunflower Giver");meta.setLore(List.of(ChatColor.GRAY+"Place an invisible AFK reward detection point.","",ChatColor.WHITE+"Players inside receive:",ChatColor.GREEN+"+"+plugin.getConfig().getLong("afk.reward",3L)+" "+ChatColor.GOLD+"🌻 Sunflowers",ChatColor.GRAY+"Every "+plugin.getConfig().getLong("afk.interval-minutes",5L)+" minutes",""," "+ChatColor.DARK_GRAY+"Admin item • Original blocks are untouched"));meta.getPersistentDataContainer().set(giverKey,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;}
    private boolean isGiverItem(ItemStack item){if(item==null||!item.hasItemMeta())return false;Byte value=item.getItemMeta().getPersistentDataContainer().get(giverKey,PersistentDataType.BYTE);return value!=null&&value==(byte)1;}
    private String key(Location l){return l.getWorld().getUID()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();}
    private Location parse(String key){try{String[] a=key.split(":");World w=Bukkit.getWorld(UUID.fromString(a[0]));if(w==null)return null;return new Location(w,Integer.parseInt(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3]));}catch(Exception e){return null;}}
    public void save(){YamlConfiguration y=new YamlConfiguration();if(afkLocation!=null){y.set("afk-location.world",afkLocation.getWorld().getName());y.set("afk-location.x",afkLocation.getX());y.set("afk-location.y",afkLocation.getY());y.set("afk-location.z",afkLocation.getZ());y.set("afk-location.yaw",afkLocation.getYaw());y.set("afk-location.pitch",afkLocation.getPitch());}y.set("givers",new ArrayList<>(givers));try{y.save(file);}catch(IOException e){plugin.getLogger().warning("Could not save afk.yml: "+e.getMessage());}}
    public void load(){if(!file.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file);String world=y.getString("afk-location.world");if(world!=null){World w=Bukkit.getWorld(world);if(w!=null)afkLocation=new Location(w,y.getDouble("afk-location.x"),y.getDouble("afk-location.y"),y.getDouble("afk-location.z"),(float)y.getDouble("afk-location.yaw"),(float)y.getDouble("afk-location.pitch"));}givers.clear();givers.addAll(y.getStringList("givers"));}
    private void msg(Player p,String s){p.sendMessage(s);}
}
