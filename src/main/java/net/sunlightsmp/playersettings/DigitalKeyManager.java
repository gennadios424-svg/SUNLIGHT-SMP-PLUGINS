package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DigitalKeyManager {
    private static final List<String> TYPES=List.of("common","spawner","sunlight","crimson","sunset");
    private final SunlightPlayerSettings plugin;
    private final File file;
    private final YamlConfiguration data;
    private final NamespacedKey physicalKey;

    public DigitalKeyManager(SunlightPlayerSettings plugin){
        this.plugin=plugin;
        this.file=new File(plugin.getDataFolder(),"digital-keys.yml");
        this.data=YamlConfiguration.loadConfiguration(file);
        this.physicalKey=new NamespacedKey(plugin,"sunlight_crate_key_type");
    }
    private String type(String t){return t.toLowerCase(Locale.ROOT).replace("_","");}
    public boolean valid(String t){return TYPES.contains(type(t));}
    private String path(Player p,String t){return p.getUniqueId()+"."+type(t);}
    public int get(Player p,String t){return Math.max(0,data.getInt(path(p,t),0));}
    public void add(Player p,String t,int amount){if(!valid(t)||amount<=0)return;String k=path(p,t);data.set(k,get(p,t)+amount);save();}
    public boolean take(Player p,String t){if(get(p,t)<=0)return false;data.set(path(p,t),get(p,t)-1);save();return true;}
    public void save(){try{data.save(file);}catch(IOException e){plugin.getLogger().warning("Could not save digital keys: "+e.getMessage());}}
    public void keyAll(String t,int amount){if(!valid(t))return;amount=Math.max(1,Math.min(100000,amount));for(Player p:plugin.getServer().getOnlinePlayers())add(p,t,amount);}
    public void convertPhysicalKeys(Player p){
        for(int slot=0;slot<p.getInventory().getSize();slot++){
            ItemStack x=p.getInventory().getItem(slot);if(x==null||x.getType()!=Material.TRIPWIRE_HOOK||!x.hasItemMeta())continue;
            ItemMeta m=x.getItemMeta();String t=m.getPersistentDataContainer().get(physicalKey,PersistentDataType.STRING);if(!valid(t))continue;
            int amount=x.getAmount();add(p,t,amount);p.getInventory().setItem(slot,null);
            p.sendMessage(ChatColor.GREEN+"☀ Converted "+amount+" "+pretty(t)+" physical key(s) into digital keys.");
        }
    }
    public String pretty(String t){t=type(t);return Character.toUpperCase(t.charAt(0))+t.substring(1);}
    public String summary(Player p){StringBuilder s=new StringBuilder(ChatColor.GOLD+"☀ YOUR DIGITAL KEYS\n");for(String t:TYPES)s.append(color(t)).append(pretty(t)).append(ChatColor.GRAY+": ").append(ChatColor.WHITE).append(get(p,t)).append("\n");return s.toString();}
    private ChatColor color(String t){return switch(type(t)){case "common"->ChatColor.GREEN;case "spawner"->ChatColor.YELLOW;case "sunlight"->ChatColor.GOLD;case "crimson"->ChatColor.RED;default->ChatColor.LIGHT_PURPLE;};}
}