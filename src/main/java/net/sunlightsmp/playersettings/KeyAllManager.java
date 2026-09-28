package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;import org.bukkit.entity.Player;import java.util.*;

public final class KeyAllManager {
    private final SunlightPlayerSettings plugin;private final DigitalKeyManager digital;
    public KeyAllManager(SunlightPlayerSettings plugin){this.plugin=plugin;this.digital=new DigitalKeyManager(plugin);}
    public void run(String type,int amount){
        String t=type.toLowerCase(Locale.ROOT);if(!digital.valid(t))return;amount=Math.min(100000,Math.max(1,amount));final int finalAmount=amount;digital.keyAll(t,amount);
        for(int i=0;i<18;i++){final int frame=i;plugin.getServer().getScheduler().runTaskLater(plugin,()->{for(Player p:plugin.getServer().getOnlinePlayers()){String roll=frame%4==0?"◆ ◆ ◆":frame%4==1?"◇ ◆ ◇":frame%4==2?"◆ ◇ ◆":"◇ ◇ ◇";p.sendActionBar(color(t)+"☀ KEY ALL "+ChatColor.WHITE+"» "+roll+"  "+ChatColor.GRAY+"["+(frame+1)+"/18]");p.playSound(p.getLocation(),org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,.55f,.65f+frame*.045f);if(frame%3==0)p.sendTitle(ChatColor.GOLD+"☀ KEY ALL ☀",color(t)+pretty(t)+" Digital Key",0,6,0);}},i*4L);}
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{for(Player p:plugin.getServer().getOnlinePlayers()){p.sendTitle(ChatColor.GOLD+"☀ KEY ALL! ☀",color(t)+"+"+finalAmount+" "+pretty(t)+" Digital Key"+(finalAmount==1?"":"s"),5,45,10);p.sendActionBar(ChatColor.YELLOW+"☀ Everyone received "+color(t)+finalAmount+"x "+pretty(t)+ChatColor.YELLOW+" Digital Key!");p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,1f,1.15f);p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.35f);}},76L);
    }
    private String pretty(String t){return Character.toUpperCase(t.charAt(0))+t.substring(1);}private ChatColor color(String t){return switch(t){case "common"->ChatColor.GREEN;case "spawner"->ChatColor.YELLOW;case "sunlight"->ChatColor.GOLD;case "crimson"->ChatColor.RED;default->ChatColor.LIGHT_PURPLE;};}
}