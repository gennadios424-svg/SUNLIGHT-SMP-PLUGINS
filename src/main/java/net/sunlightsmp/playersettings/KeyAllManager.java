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
        meta.setDisplayName(color(t)+"☀ "+pretty(t)+" Crate Key");
        meta.setLore(List.of(ChatColor.GRAY+"Use /suncrate "+t+" to open",ChatColor.YELLOW+"☀ Sunlight Crate Key"));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin,"sunlight_crate_key_type"),org.bukkit.persistence.PersistentDataType.STRING,t);key.setItemMeta(meta);

        for(Player p:plugin.getServer().getOnlinePlayers()){
            var left=p.getInventory().addItem(key.clone());
            left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));
        }
        // Build a longer server-wide reveal before the keys are awarded.
        for(int i=0;i<18;i++){
            final int frame=i;
            plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                for(Player p:plugin.getServer().getOnlinePlayers()){
                    String roll=frame%4==0?"◆ ◆ ◆":frame%4==1?"◇ ◆ ◇":frame%4==2?"◆ ◇ ◆":"◇ ◇ ◇";
                    p.sendActionBar(color(t)+"☀ KEY ALL "+ChatColor.WHITE+"» "+roll+"  "+ChatColor.GRAY+"["+(frame+1)+"/18]");
                    p.playSound(p.getLocation(),org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,0.55f,0.65f+frame*0.045f);
                    if(frame%3==0)p.sendTitle(ChatColor.GOLD+"☀ KEY ALL ☀",color(t)+pretty(t)+" Key",0,6,0);
                }
            },i*4L);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            for(Player p:plugin.getServer().getOnlinePlayers()){
                p.sendTitle(ChatColor.GOLD+"☀ KEY ALL! ☀",color(t)+"+"+finalAmount+" "+pretty(t)+" Key"+(finalAmount==1?"":"s"),5,45,10);
                p.sendActionBar(ChatColor.YELLOW+"☀ Everyone received "+color(t)+finalAmount+"x "+pretty(t)+ChatColor.YELLOW+" Key!");
                p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,1f,1.15f);
                p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.35f);
            }
        },76L);
    }
    private String pretty(String t){return Character.toUpperCase(t.charAt(0))+t.substring(1);} private ChatColor color(String t){return switch(t){case "common"->ChatColor.GREEN;case "spawner"->ChatColor.YELLOW;case "sunlight"->ChatColor.GOLD;case "crimson"->ChatColor.RED;default->ChatColor.LIGHT_PURPLE;};}
}