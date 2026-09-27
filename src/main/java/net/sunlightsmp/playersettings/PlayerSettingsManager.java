package net.sunlightsmp.playersettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File; import java.io.IOException; import java.util.*;
public final class PlayerSettingsManager {
  private final JavaPlugin plugin; private final File file; private final FileConfiguration data;
  public PlayerSettingsManager(JavaPlugin plugin){
    this.plugin=plugin; file=new File(plugin.getDataFolder(),"players.yml");
    if(!file.exists()) try{plugin.getDataFolder().mkdirs();file.createNewFile();}catch(IOException e){plugin.getLogger().severe(e.getMessage());}
    data=YamlConfiguration.loadConfiguration(file);
  }
  public boolean get(Player p,Setting s){return data.getBoolean(p.getUniqueId()+"."+s.key(),plugin.getConfig().getBoolean("settings."+s.key(),true));}
  public void set(Player p,Setting s,boolean value){data.set(p.getUniqueId()+"."+s.key(),value);save();}
  public void toggle(Player p,Setting s){set(p,s,!get(p,s));}
  public void save(){try{data.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save players.yml: "+e.getMessage());}}
}