package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class KeyAllManager {
    private final SunlightPlayerSettings plugin;
    public KeyAllManager(SunlightPlayerSettings plugin){this.plugin=plugin;}

    public void run(String type,int amount){
        String t=type.toLowerCase(Locale.ROOT);
        if(!List.of("common","spawner","sunlight","crimson","sunset").contains(t))return;
        amount=Math.min(64,Math.max(1,amount));
        final int finalAmount = amount;
        ItemStack key=new ItemStack(Material.TRIPWIRE_HOOK,amount);
        ItemMeta meta=key.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD+"☀ "+pretty(t)+" Crate Key");
        meta.setLore(List.of(ChatColor.GRAY+"Use /crate "+t+" to open"));
        key.setItemMeta(meta);

        for(Player p:plugin.getServer().getOnlinePlayers()){
            var left=p.getInventory().addItem(key.clone());
            left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));
            p.playSound(p.getLocation(),org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,1f,0.7f);
            p.sendTitle(ChatColor.GOLD+"☀ KEY ALL ☀",ChatColor.YELLOW+"Rolling...",5,20,5);
        }
        for(int i=0;i<7;i++){
            final int frame=i;
            plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                for(Player p:plugin.getServer().getOnlinePlayers()){
                    String roll=frame%3==0?"◆ ◆ ◆":frame%3==1?"◇ ◆ ◇":"◆ ◇ ◆";
                    p.sendActionBar(ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"KEY ALL "+ChatColor.WHITE+"» "+roll);
                    p.playSound(p.getLocation(),org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,0.7f,0.75f+frame*0.1f);
                }
            },i*4L);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            for(Player p:plugin.getServer().getOnlinePlayers()){
                p.sendTitle(ChatColor.GOLD+"☀ KEY ALL! ☀",ChatColor.YELLOW+"+"+finalAmount+" "+pretty(t)+" Key"+(finalAmount==1?"":"s"),5,35,10);
                p.sendActionBar(ChatColor.YELLOW+"☀ Everyone received "+amount+"x "+pretty(t)+" Key!");
                p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.3f);
            }
        },32L);
    }
    private String pretty(String t){return Character.toUpperCase(t.charAt(0))+t.substring(1);}
}