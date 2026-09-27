package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class AuctionHouseCommand implements CommandExecutor,TabCompleter{
    private final SunlightPlayerSettings plugin; private final AuctionHouseManager manager; private final AuctionHouseMenu menu;
    public AuctionHouseCommand(SunlightPlayerSettings plugin,AuctionHouseManager manager,AuctionHouseMenu menu){this.plugin=plugin;this.manager=manager;this.menu=menu;}
    private double parsePrice(String input){
        String s=input.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("$", "");
        if(s.isEmpty())throw new NumberFormatException();
        double multiplier=1D;
        if(s.endsWith("k")){multiplier=1_000D;s=s.substring(0,s.length()-1);}
        else if(s.endsWith("m")){multiplier=1_000_000D;s=s.substring(0,s.length()-1);}
        else if(s.endsWith("b")){multiplier=1_000_000_000D;s=s.substring(0,s.length()-1);}
        double value=Double.parseDouble(s)*multiplier;
        if(!Double.isFinite(value))throw new NumberFormatException();
        return value;
    }
    public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player p)){s.sendMessage("Only players can use /ah.");return true;}
        if(a.length==0){menu.open(p);return true;}
        if(a[0].equalsIgnoreCase("sell")){
            if(a.length!=2){p.sendMessage(ChatColor.YELLOW+"Usage: /ah sell <price>");return true;}
            try{double price=parsePrice(a[1]);AuctionListing x=manager.create(p,price);if(x==null)p.sendMessage(ChatColor.RED+"Could not list item. Hold an item and use a valid price.");else{p.sendMessage(ChatColor.GREEN+"☀ Listed "+x.item().getAmount()+"x "+x.item().getType()+" for $"+price+"!");menu.open(p);}}catch(NumberFormatException ex){p.sendMessage(ChatColor.RED+"Invalid price.");}return true;
        }
        if(a[0].equalsIgnoreCase("listings")||a[0].equalsIgnoreCase("selling")){menu.openSelling(p);return true;}
        if(a[0].equalsIgnoreCase("cancel")&&a.length==2){try{long id=Long.parseLong(a[1]);p.sendMessage(manager.cancel(p,id)?ChatColor.YELLOW+"Listing cancelled and item returned.":ChatColor.RED+"Listing not found or not yours.");}catch(Exception e){p.sendMessage(ChatColor.RED+"Invalid listing ID.");}return true;}
        if(a[0].equalsIgnoreCase("search")){if(a.length<2){p.sendMessage(ChatColor.YELLOW+"Usage: /ah search <item>");return true;}String query=String.join(" ",Arrays.copyOfRange(a,1,a.length));menu.openSearch(p,query);return true;}
        menu.openSearch(p,String.join(" ",a));return true;
    }
    public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){
        if(args.length==1)return Arrays.asList("sell","listings","cancel","search");
        return Collections.emptyList();
    }
}
