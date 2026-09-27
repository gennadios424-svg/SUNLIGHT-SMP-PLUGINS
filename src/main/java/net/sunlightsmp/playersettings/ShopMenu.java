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
    private static final String BUY_TITLE=ChatColor.GOLD+"☀ Purchase Amount";
    private final SunlightPlayerSettings plugin;
    private final Map<UUID,String> cats=new HashMap<>();
    private final Map<UUID,Integer> pages=new HashMap<>();
    private final Map<UUID,Material> selected=new HashMap<>();
    private final Map<UUID,Integer> amounts=new HashMap<>();

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
            inv.setItem(10,item(Material.REDSTONE,ChatColor.RED+"Redstone","","",ChatColor.GRAY+"Redstone & automation"));
            inv.setItem(12,item(Material.WHEAT,ChatColor.GREEN+"Farm","",ChatColor.GRAY+"Farming supplies"));
            inv.setItem(14,item(Material.COOKED_BEEF,ChatColor.GOLD+"Food","",ChatColor.GRAY+"Food & consumables"));
            inv.setItem(16,item(Material.END_CRYSTAL,ChatColor.LIGHT_PURPLE+"PvP","",ChatColor.GRAY+"Crystal PvP supplies — no armor/weapons"));
            inv.setItem(20,item(Material.BRICKS,ChatColor.YELLOW+"Blocks","",ChatColor.GRAY+"Building blocks"));
            inv.setItem(24,item(Material.SPAWNER,ChatColor.AQUA+"Spawners","",ChatColor.GRAY+"$2,000,000 each"));
            inv.setItem(49,item(Material.SUNFLOWER,ChatColor.GOLD+"☀ Sunlight Shop","",ChatColor.GRAY+"Choose a category"));
        }else{
            List<Material> list=c.equals("spawners")?List.of(Material.SPAWNER):ShopPricing.category(c); int pg=pages.getOrDefault(p.getUniqueId(),0),start=pg*27;
            for(int i=0;i<27&&start+i<list.size();i++){Material m=list.get(start+i);String price=m==Material.SPAWNER?"$2,000,000":"$"+money(ShopPricing.buyPrice(m));inv.setItem(i,item(m,ChatColor.WHITE+pretty(m),"",ChatColor.GREEN+"Buy: "+price,ChatColor.GRAY+"Click to choose amount")); }
            inv.setItem(27,item(Material.ARROW,ChatColor.YELLOW+"Previous"));inv.setItem(31,item(Material.BARRIER,ChatColor.RED+"Back"));inv.setItem(35,item(Material.ARROW,ChatColor.YELLOW+"Next"));
        }
        p.openInventory(inv);
    }

    private void drawPurchase(Player p){
        Material m=selected.get(p.getUniqueId());
        if(m==null){draw(p);return;}
        int amount=Math.max(1,Math.min(2304,amounts.getOrDefault(p.getUniqueId(),1)));
        amounts.put(p.getUniqueId(),amount);
        Inventory inv=Bukkit.createInventory(null,54,BUY_TITLE);
        for(int i=0;i<54;i++)inv.setItem(i,item(Material.GRAY_STAINED_GLASS_PANE," "));
        String price=m==Material.SPAWNER?"$2,000,000":"$"+money(ShopPricing.buyPrice(m));
        double total=m==Material.SPAWNER?2000000D*amount:ShopPricing.buyPrice(m)*amount;
        inv.setItem(22,item(m,ChatColor.WHITE+pretty(m),"",ChatColor.YELLOW+"Amount: "+amount,ChatColor.GREEN+"Total: "+money(total)+"$"));
        inv.setItem(10,item(Material.RED_STAINED_GLASS_PANE,ChatColor.RED+"- 64",ChatColor.GRAY+"Remove 64"));
        inv.setItem(19,item(Material.RED_STAINED_GLASS_PANE,ChatColor.RED+"- 16",ChatColor.GRAY+"Remove 16"));
        inv.setItem(28,item(Material.RED_STAINED_GLASS_PANE,ChatColor.RED+"- 1",ChatColor.GRAY+"Remove 1"));
        inv.setItem(14,item(Material.LIME_STAINED_GLASS_PANE,ChatColor.GREEN+"+ 1",ChatColor.GRAY+"Add 1"));
        inv.setItem(23,item(Material.LIME_STAINED_GLASS_PANE,ChatColor.GREEN+"+ 16",ChatColor.GRAY+"Add 16"));
        inv.setItem(32,item(Material.LIME_STAINED_GLASS_PANE,ChatColor.GREEN+"+ 64",ChatColor.GRAY+"Add 64"));
        inv.setItem(40,item(Material.GOLD_INGOT,ChatColor.GOLD+"BUY "+amount,ChatColor.GRAY+"Buy "+amount+" item(s)",ChatColor.GREEN+"Total: $"+money(total)));
        inv.setItem(49,item(Material.BARRIER,ChatColor.RED+"Cancel"));
        p.openInventory(inv);
    }

    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;
        if(e.getView().getTitle().equals(BUY_TITLE)){
            e.setCancelled(true);
            int s=e.getRawSlot();
            if(s<0||s>=54)return;
            Material m=selected.get(p.getUniqueId());
            if(m==null){draw(p);return;}
            int amount=amounts.getOrDefault(p.getUniqueId(),1);
            if(s==10)amount-=64;
            else if(s==19)amount-=16;
            else if(s==28)amount-=1;
            else if(s==14)amount+=1;
            else if(s==23)amount+=16;
            else if(s==32)amount+=64;
            else if(s==40){buy(p,m,amount);return;}
            else if(s==49){selected.remove(p.getUniqueId());amounts.remove(p.getUniqueId());draw(p);return;}
            amount=Math.max(1,Math.min(2304,amount));
            amounts.put(p.getUniqueId(),amount);
            drawPurchase(p);
            return;
        }

        if(!e.getView().getTitle().equals(TITLE))return;
        e.setCancelled(true);
        int s=e.getRawSlot();
        if(s<0||s>=54)return;
        String c=cats.getOrDefault(p.getUniqueId(),"home");
        if(c.equals("home")){
            String n=s==10?"redstone":s==12?"farm":s==14?"food":s==16?"pvp":s==20?"blocks":s==24?"spawners":null;
            if(n!=null){cats.put(p.getUniqueId(),n);pages.put(p.getUniqueId(),0);draw(p);}
            return;
        }
        List<Material> list=c.equals("spawners")?List.of(Material.SPAWNER):ShopPricing.category(c);int pg=pages.getOrDefault(p.getUniqueId(),0);
        if(s==31){cats.put(p.getUniqueId(),"home");pages.put(p.getUniqueId(),0);draw(p);return;}
        if(s==27){if(pg>0){pages.put(p.getUniqueId(),pg-1);draw(p);}return;}
        if(s==35){if((pg+1)*27<list.size()){pages.put(p.getUniqueId(),pg+1);draw(p);}return;}
        int idx=pg*27+s;
        if(s<27&&idx<list.size()){
            Material m=list.get(idx);
            selected.put(p.getUniqueId(),m);
            amounts.put(p.getUniqueId(),1);
            drawPurchase(p);
        }
    }

    private void buy(Player p,Material m,int amount){
        amount=Math.max(1,Math.min(2304,amount));
        if(m==Material.SPAWNER){p.sendMessage(ChatColor.YELLOW+"Spawners cost 2,000,000 each. Shard currency needs to be connected to the server's shard system.");return;}
        Economy eco=economy();if(eco==null){p.sendMessage(ChatColor.RED+"Economy is unavailable.");return;}
        double total=ShopPricing.buyPrice(m)*amount;
        if(eco.getBalance(p)<total){p.sendMessage(ChatColor.RED+"You need $"+money(total)+" to buy "+amount+"x "+pretty(m)+".");return;}
        eco.withdrawPlayer(p,total);
        Map<Integer,ItemStack> left=p.getInventory().addItem(new ItemStack(m,amount));
        if(!left.isEmpty()){
            eco.depositPlayer(p,total);
            p.sendMessage(ChatColor.RED+"Not enough inventory space.");
            return;
        }
        selected.remove(p.getUniqueId());amounts.remove(p.getUniqueId());
        p.sendMessage(ChatColor.GREEN+"☀ Bought "+amount+"x "+pretty(m)+" for "+ChatColor.GOLD+"$"+money(total)+ChatColor.GREEN+".");
        p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.2f);
        draw(p);
    }
}