package net.sunlightsmp.playersettings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.text.NumberFormat;
import java.util.*;

public final class ShopMenu implements Listener {
    private static final String TITLE=ChatColor.GOLD+"☀ Sunlight Shop";
    private final SunlightPlayerSettings plugin;
    private final Map<UUID,String> cats=new HashMap<>();
    private final Map<UUID,Integer> pages=new HashMap<>();
    public ShopMenu(SunlightPlayerSettings plugin){this.plugin=plugin;}
    private Economy economy(){var r=plugin.getServer().getServicesManager().getRegistration(Economy.class);return r==null?null:r.getProvider();}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta x=i.getItemMeta();x.setDisplayName(name);x.setLore(Arrays.asList(lore));i.setItemMeta(x);return i;}
    private String pretty(Material m){StringBuilder s=new StringBuilder();for(String x:m.name().toLowerCase(Locale.ROOT).split("_")){if(s.length()>0)s.append(' ');s.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));}return s.toString();}
    private String money(double n){return NumberFormat.getNumberInstance(Locale.US).format(n);}
    public void open(Player p){cats.put(p.getUniqueId(),"home");pages.put(p.getUniqueId(),0);draw(p);}
    private void draw(Player p){
        String c=cats.getOrDefault(p.getUniqueId(),"home"); Inventory inv=Bukkit.createInventory(null,54,TITLE);
        for(int i=45;i<54;i++)inv.setItem(i,item(Material.GRAY_STAINED_GLASS_PANE," "));
        if(c.equals("home")){
            inv.setItem(10,item(Material.REDSTONE,ChatColor.RED+"Redstone","",ChatColor.GRAY+"Redstone & automation"));
            inv.setItem(12,item(Material.WHEAT,ChatColor.GREEN+"Farm","",ChatColor.GRAY+"Farming supplies"));
            inv.setItem(14,item(Material.COOKED_BEEF,ChatColor.GOLD+"Food","",ChatColor.GRAY+"Food & consumables"));
            inv.setItem(16,item(Material.END_CRYSTAL,ChatColor.LIGHT_PURPLE+"PvP","",ChatColor.GRAY+"Crystal PvP supplies — no armor/weapons"));
            inv.setItem(20,item(Material.BRICKS,ChatColor.YELLOW+"Blocks","",ChatColor.GRAY+"Building blocks"));
            inv.setItem(24,item(Material.SPAWNER,ChatColor.AQUA+"Spawners","",ChatColor.GRAY+"$2,000,000 each"));
            inv.setItem(49,item(Material.SUNFLOWER,ChatColor.GOLD+"☀ Sunlight Shop","",ChatColor.GRAY+"Choose a category"));
        }else{
            List<Material> list=c.equals("spawners")?List.of(Material.SPAWNER):ShopPricing.category(c); int pg=pages.getOrDefault(p.getUniqueId(),0),start=pg*45;
            for(int i=0;i<45&&start+i<list.size();i++){Material m=list.get(start+i);String price=m==Material.SPAWNER?"$2,000,000":"$"+money(ShopPricing.buyPrice(m));inv.setItem(i,item(m,ChatColor.WHITE+pretty(m),"",ChatColor.GREEN+"Buy: "+price,ChatColor.GRAY+"Left-click: Buy 1",ChatColor.GRAY+"Shift-click: Buy 16"));}
            inv.setItem(45,item(Material.ARROW,ChatColor.YELLOW+"Previous"));inv.setItem(49,item(Material.BARRIER,ChatColor.RED+"Back"));inv.setItem(53,item(Material.ARROW,ChatColor.YELLOW+"Next"));
        }
        p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p)||!e.getView().getTitle().equals(TITLE))return;e.setCancelled(true);int s=e.getRawSlot();if(s<0||s>=54)return;String c=cats.getOrDefault(p.getUniqueId(),"home");
        if(c.equals("home")){String n=s==10?"redstone":s==12?"farm":s==14?"food":s==16?"pvp":s==20?"blocks":s==24?"spawners":null;if(n!=null){cats.put(p.getUniqueId(),n);pages.put(p.getUniqueId(),0);draw(p);}return;}
        List<Material> list=c.equals("spawners")?List.of(Material.SPAWNER):ShopPricing.category(c);int pg=pages.getOrDefault(p.getUniqueId(),0);
        if(s==49){cats.put(p.getUniqueId(),"home");pages.put(p.getUniqueId(),0);draw(p);return;}
        if(s==45){if(pg>0){pages.put(p.getUniqueId(),pg-1);draw(p);}return;}
        if(s==53){if((pg+1)*45<list.size()){pages.put(p.getUniqueId(),pg+1);draw(p);}return;}
        int idx=pg*45+s;if(s<45&&idx<list.size())buy(p,list.get(idx),e.isShiftClick()?16:1);
    }
    private void buy(Player p,Material m,int amount){
        if(m==Material.SPAWNER){p.sendMessage(ChatColor.YELLOW+"Spawners cost 2,000,000 each. Shard currency needs to be connected to the server's shard system.");return;}
        Economy eco=economy();if(eco==null){p.sendMessage(ChatColor.RED+"Economy is unavailable.");return;}double total=ShopPricing.buyPrice(m)*amount;
        if(eco.getBalance(p)<total){p.sendMessage(ChatColor.RED+"You need $"+money(total)+" to buy "+amount+"x "+pretty(m)+".");return;}
        if(!p.getInventory().addItem(new ItemStack(m,amount)).isEmpty()){p.sendMessage(ChatColor.RED+"Not enough inventory space.");return;}
        eco.withdrawPlayer(p,total);p.sendMessage(ChatColor.GREEN+"☀ Bought "+amount+"x "+pretty(m)+" for "+ChatColor.GOLD+"$"+money(total)+ChatColor.GREEN+".");p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.2f);
    }
}