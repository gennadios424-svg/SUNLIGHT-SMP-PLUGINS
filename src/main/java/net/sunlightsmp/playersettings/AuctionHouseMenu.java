package net.sunlightsmp.playersettings;

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

public final class AuctionHouseMenu implements Listener {
    private static final String TITLE=ChatColor.GOLD+"☀ Sunlight Auction House";
    private static final String CONFIRM=ChatColor.GOLD+"☀ Confirm Purchase";
    private static final String SELLING=ChatColor.GOLD+"☀ Your Listings";
    private final SunlightPlayerSettings plugin; private final AuctionHouseManager manager;
    private final Map<UUID,Integer> pages=new HashMap<>();
    public AuctionHouseMenu(SunlightPlayerSettings plugin,AuctionHouseManager manager){this.plugin=plugin;this.manager=manager;}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta x=i.getItemMeta();x.setDisplayName(name);x.setLore(Arrays.asList(lore));i.setItemMeta(x);return i;}
    private String money(double n){return NumberFormat.getNumberInstance(Locale.US).format(n);}
    public void open(Player p){open(p,0);}
    private void open(Player p,int page){
        pages.put(p.getUniqueId(),page); Inventory inv=Bukkit.createInventory(null,54,TITLE);
        for(int i=0;i<54;i++)inv.setItem(i,item(Material.YELLOW_STAINED_GLASS_PANE," "));
        List<AuctionListing> ls=manager.all(); int start=page*45;
        for(int i=start;i<Math.min(start+45,ls.size());i++){AuctionListing l=ls.get(i);ItemStack x=l.item();ItemMeta m=x.getItemMeta();List<String> lore=m!=null&&m.getLore()!=null?new ArrayList<>(m.getLore()):new ArrayList<>();lore.add("");lore.add(ChatColor.YELLOW+"Price: "+ChatColor.GOLD+"$"+money(l.price()));lore.add(ChatColor.GRAY+"Seller: "+l.sellerName());lore.add(ChatColor.DARK_GRAY+"ID: "+l.id());lore.add(ChatColor.GREEN+"Click to view");if(m==null){m=x.getItemMeta();}m.setLore(lore);x.setItemMeta(m);inv.setItem(i-start,x);}
        inv.setItem(45,item(Material.CHEST,ChatColor.YELLOW+"Your Listings","",ChatColor.GRAY+"Manage your active auctions."));
        if(page>0)inv.setItem(48,item(Material.ARROW,ChatColor.YELLOW+"Previous Page"));
        if((page+1)*45<ls.size())inv.setItem(50,item(Material.ARROW,ChatColor.YELLOW+"Next Page"));
        inv.setItem(49,item(Material.SUNFLOWER,ChatColor.GOLD+"☀ Sunlight AH","",ChatColor.GRAY+"Listings: "+ls.size(),ChatColor.GRAY+"/ah sell <price>"));
        p.openInventory(inv);
    }
    public void openSelling(Player p){
        Inventory inv=Bukkit.createInventory(null,54,SELLING);
        for(int i=0;i<54;i++)inv.setItem(i,item(Material.YELLOW_STAINED_GLASS_PANE," "));
        int slot=0;for(AuctionListing l:manager.all())if(l.seller().equals(p.getUniqueId())){ItemStack x=l.item();ItemMeta m=x.getItemMeta();List<String> lore=m!=null&&m.getLore()!=null?new ArrayList<>(m.getLore()):new ArrayList<>();lore.add("");lore.add(ChatColor.YELLOW+"Price: "+ChatColor.GOLD+"$"+money(l.price()));lore.add(ChatColor.RED+"Click to cancel & return item");lore.add(ChatColor.DARK_GRAY+"ID: "+l.id());if(m==null)m=x.getItemMeta();m.setLore(lore);x.setItemMeta(m);if(slot<45)inv.setItem(slot++,x);}
        inv.setItem(49,item(Material.BARRIER,ChatColor.RED+"Close"));p.openInventory(inv);
    }
    public void confirm(Player p,AuctionListing l){
        Inventory inv=Bukkit.createInventory(null,27,CONFIRM);
        inv.setItem(11,item(Material.LIME_CONCRETE,ChatColor.GREEN+"CONFIRM","",ChatColor.YELLOW+"Buy for $"+money(l.price()),ChatColor.GRAY+"Seller: "+l.sellerName()));
        inv.setItem(13,l.item());inv.setItem(15,item(Material.RED_CONCRETE,ChatColor.RED+"CANCEL"));
        p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String t=e.getView().getTitle();if(!t.equals(TITLE)&&!t.equals(CONFIRM)&&!t.equals(SELLING))return;
        e.setCancelled(true);int s=e.getRawSlot();if(s<0||s>=e.getInventory().getSize())return;
        if(t.equals(TITLE)){int page=pages.getOrDefault(p.getUniqueId(),0);if(s==48&&page>0){open(p,page-1);return;}if(s==50&&(page+1)*45<manager.all().size()){open(p,page+1);return;}if(s==45){openSelling(p);return;}if(s<45){List<AuctionListing> ls=manager.all();int idx=page*45+s;if(idx<ls.size())confirm(p,ls.get(idx));}}
        else if(t.equals(CONFIRM)){if(s==11){ItemStack shown=e.getInventory().getItem(13);long id=findId(p);AuctionListing l=manager.get(id);if(l!=null&&manager.buy(p,id)){p.closeInventory();p.sendMessage(ChatColor.GREEN+"☀ Purchase complete!");p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.2f);}else{p.sendMessage(ChatColor.RED+"Purchase failed: listing changed, insufficient funds, or inventory full.");}}else if(s==15)p.closeInventory();}
        else {if(s==49){open(p);return;}if(s<45){List<AuctionListing> mine=manager.all().stream().filter(x->x.seller().equals(p.getUniqueId())).toList();if(s<mine.size()){long id=mine.get(s).id();if(manager.cancel(p,id)){p.sendMessage(ChatColor.YELLOW+"Listing cancelled and item returned.");p.playSound(p.getLocation(),org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,1f,1.4f);openSelling(p);}}}}
    }
    private long findId(Player p){ // confirmation inventories are opened directly from a listing; identify by stored holder
        return plugin.getPendingAuction(p.getUniqueId());
    }
}
