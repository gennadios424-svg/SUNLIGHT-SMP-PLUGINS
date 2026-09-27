package net.sunlightsmp.playersettings;
import org.bukkit.command.*; import org.bukkit.entity.Player;
public final class SettingsCommand implements CommandExecutor {
  private final SettingsListener listener; public SettingsCommand(SettingsListener listener){this.listener=listener;}
  public boolean onCommand(CommandSender sender,Command command,String label,String[] args){if(!(sender instanceof Player p)){sender.sendMessage("Only players can use this command.");return true;}listener.open(p);return true;}
}