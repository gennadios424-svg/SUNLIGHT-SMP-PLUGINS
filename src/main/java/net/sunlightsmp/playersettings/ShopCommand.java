package net.sunlightsmp.playersettings;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public final class ShopCommand implements CommandExecutor {
    private final ShopMenu menu;
    public ShopCommand(ShopMenu menu){this.menu=menu;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p)){s.sendMessage("Only players can use this command.");return true;}menu.open(p);return true;}
}