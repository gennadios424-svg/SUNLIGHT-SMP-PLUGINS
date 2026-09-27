package net.sunlightsmp.playersettings;
import org.bukkit.plugin.java.JavaPlugin;
public final class SunlightPlayerSettings extends JavaPlugin {
  private PlayerSettingsManager settings;
  public void onEnable(){saveDefaultConfig();settings=new PlayerSettingsManager(this);SettingsListener l=new SettingsListener(this);getCommand("settings").setExecutor(new SettingsCommand(l));getServer().getPluginManager().registerEvents(l,this);getLogger().info("Sunlight Player Settings enabled.");}
  public void onDisable(){if(settings!=null)settings.save();}
  public PlayerSettingsManager getSettings(){return settings;}
}