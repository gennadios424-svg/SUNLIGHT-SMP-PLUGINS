package net.sunlightsmp.playersettings;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class SellCommand implements CommandExecutor {
    private final SellMenu menu;
    public SellCommand(SellMenu menu){this.menu=menu;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player p)){s.sendMessage("Only players can use /sell.");return true;}
        menu.open(p); return true;
    }
}