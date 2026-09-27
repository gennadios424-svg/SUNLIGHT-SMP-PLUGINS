package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.text.NumberFormat;
import java.util.*;

public final class WorthCommand implements CommandExecutor,TabCompleter {
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!(s instanceof Player p)){s.sendMessage("Only players can use /worth.");return true;}
        Material mat; int amount=1;
        if(a.length==0){
            ItemStack held=p.getInventory().getItemInMainHand();
            if(held.getType().isAir()){p.sendMessage(ChatColor.RED+"Hold an item or use /worth <item>.");return true;}
            mat=held.getType(); amount=held.getAmount();
        }else{
            String key=String.join("_",a).toUpperCase(Locale.ROOT).replace('-','_').replace(' ','_');
            if(a.length>=2){try{int maybe=Integer.parseInt(a[a.length-1]);if(maybe>0){amount=maybe;key=String.join("_",Arrays.copyOf(a,a.length-1)).toUpperCase(Locale.ROOT).replace('-','_').replace(' ','_');}}catch(NumberFormatException ignored){}}
            try{mat=Material.valueOf(key);}catch(IllegalArgumentException ex){p.sendMessage(ChatColor.RED+"Unknown item. Example: /worth diamond_block or /worth sea_pickle");return true;}
        }
        double each=SellPricing.price(mat);
        p.sendMessage(ChatColor.GOLD+"☀ "+ChatColor.YELLOW+mat.name()+ChatColor.GRAY+" = "+ChatColor.GREEN+"$"+money(each)+ChatColor.GRAY+" each"+(amount>1?ChatColor.GRAY+" | "+ChatColor.GREEN+"$"+money(each*amount)+ChatColor.GRAY+" for "+amount:""));
        return true;
    }
    private String money(double n){return NumberFormat.getNumberInstance(Locale.US).format(n);}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){
        if(a.length!=1)return Collections.emptyList();
        String q=a[0].toUpperCase(Locale.ROOT);List<String> out=new ArrayList<>();
        for(Material m:Material.values())if(m.name().startsWith(q))out.add(m.name().toLowerCase(Locale.ROOT));
        return out.subList(0,Math.min(20,out.size()));
    }
}