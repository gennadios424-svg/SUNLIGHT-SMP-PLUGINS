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
    private final SunlightPlayerSettings plugin; private final AuctionHouseManager manager; private final Map<UUID,Long> pending=new HashMap<>();
    private final Map<UUID,Integer> pages=new HashMap<>(); private final Map<UUID,String> searchQueries=new HashMap<>(); private final Map<UUID,Integer> sortModes=new HashMap<>(); private final Set<UUID> awaitingPrice=new HashSet<>(); private final Set<UUID> awaitingSearch=new HashSet<>();
    public AuctionHouseMenu(SunlightPlayerSettings plugin,AuctionHouseManager manager){this.plugin=plugin;this.manager=manager;}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta x=i.getItemMeta();x.setDisplayName(name);x.setLore(Arrays.asList(lore));i.setItemMeta(x);return i;}
    private String money(double n){return NumberFormat.getNumberInstance(Locale.US).format(n);}
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
    private void frame(Inventory inv){\n        ItemStack side=item(Material.BLACK_STAINED_GLASS_PANE," "); ItemStack accent=item(Material.YELLOW_STAINED_GLASS_PANE,ChatColor.GOLD+"☀");\n        for(int r=0;r<inv.getSize()/9;r++){inv.setItem(r*9,side);inv.setItem(r*9+8,side);}\n        for(int s=0;s<9;s++){inv.setItem(s,accent);inv.setItem(inv.getSize()-9+s,accent);}\n    }\n    public void open(Player p){searchQueries.remove(p.getUniqueId());sortModes.put(p.getUniqueId(),0);open(p,0);}

    public void openSearch(Player p,String query){
        String q=query.trim();
        if(q.isEmpty()){p.sendMessage(ChatColor.RED+"Enter something to search for.");return;}
        searchQueries.put(p.getUniqueId(),q);
        open(p,0,q);
    }
    private void open(Player p,int page){ open(p,page,searchQueries.get(p.getUniqueId())); }

    private List<AuctionListing> visibleListings(Player p,String query){ List<AuctionListing> all=manager.all(); List<AuctionListing> ls=new ArrayList<>(); String q=query==null?null:query.toLowerCase(Locale.ROOT); for(AuctionListing listing:all){ if(q==null||q.isBlank()){ls.add(listing);}else{String type=listing.item().getType().name().toLowerCase(Locale.ROOT);String display=listing.item().hasItemMeta()&&listing.item().getItemMeta().hasDisplayName()?ChatColor.stripColor(listing.item().getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT):"";String seller=listing.sellerName().toLowerCase(Locale.ROOT);if(type.contains(q)||display.contains(q)||seller.contains(q))ls.add(listing);}} int sort=sortModes.getOrDefault(p.getUniqueId(),0); if(sort==1)ls.sort(Comparator.comparingDouble(AuctionListing::price)); else if(sort==2)ls.sort(Comparator.comparingDouble(AuctionListing::price).reversed()); return ls; }
    private String sortName(Player p){return switch(sortModes.getOrDefault(p.getUniqueId(),0)){case 1->"Price: Low → High";case 2->"Price: High → Low";default->"Sort: Default";};}

    private void open(Player p,int page,String query){
        pages.put(p.getUniqueId(),page); Inventory inv=Bukkit.createInventory(null,54,TITLE);
        frame(inv);
        List<AuctionListing> ls=visibleListings(p,query);

        int start=page*45;
        for(int i=start;i<Math.min(start+45,ls.size());i++){AuctionListing l=ls.get(i);ItemStack x=l.item();ItemMeta m=x.getItemMeta();List<String> lore=m!=null&&m.getLore()!=null?new ArrayList<>(m.getLore()):new ArrayList<>();lore.add("");lore.add(ChatColor.YELLOW+"Price: "+ChatColor.GOLD+"$"+money(l.price()));lore.add(ChatColor.GRAY+"Seller: "+l.sellerName());lore.add(ChatColor.DARK_GRAY+"ID: "+l.id());lore.add(ChatColor.GREEN+"Click to view");if(m==null){m=x.getItemMeta();}m.setLore(lore);x.setItemMeta(m);inv.setItem(i-start,x);}
        inv.setItem(45,item(Material.CHEST,ChatColor.YELLOW+"Your Listings","",ChatColor.GRAY+"Manage your active auctions."));
        if(page>0)inv.setItem(48,item(Material.ARROW,ChatColor.YELLOW+"Previous Page"));
        if((page+1)*45<ls.size())inv.setItem(50,item(Material.ARROW,ChatColor.YELLOW+"Next Page"));
        inv.setItem(47,item(Material.HOPPER,ChatColor.YELLOW+"Sort","",ChatColor.GRAY+sortName(p),ChatColor.GRAY+"Click to cycle")); inv.setItem(49,item(Material.SUNFLOWER,ChatColor.GOLD+"☀ Sunlight AH","",ChatColor.GRAY+"Listings: "+ls.size())); inv.setItem(53,item(Material.GOLD_INGOT,ChatColor.YELLOW+"Sell Item","",ChatColor.GRAY+"Hold an item, then click here.",ChatColor.GRAY+"Enter the price in chat."));
        p.openInventory(inv);
    }
    public void openSelling(Player p){
        Inventory inv=Bukkit.createInventory(null,54,SELLING);
        for(int i=45;i<54;i++)inv.setItem(i,item(Material.GRAY_STAINED_GLASS_PANE," ")); inv.setItem(49,item(Material.YELLOW_STAINED_GLASS_PANE," "));
        int slot=0;for(AuctionListing l:manager.all())if(l.seller().equals(p.getUniqueId())){ItemStack x=l.item();ItemMeta m=x.getItemMeta();List<String> lore=m!=null&&m.getLore()!=null?new ArrayList<>(m.getLore()):new ArrayList<>();lore.add("");lore.add(ChatColor.YELLOW+"Price: "+ChatColor.GOLD+"$"+money(l.price()));lore.add(ChatColor.RED+"Click to cancel & return item");lore.add(ChatColor.DARK_GRAY+"ID: "+l.id());if(m==null)m=x.getItemMeta();m.setLore(lore);x.setItemMeta(m);if(slot<45)inv.setItem(slot++,x);}
        inv.setItem(49,item(Material.BARRIER,ChatColor.RED+"Close"));p.openInventory(inv);
    }
    public void confirm(Player p,AuctionListing l){ pending.put(p.getUniqueId(),l.id());
        Inventory inv=Bukkit.createInventory(null,27,CONFIRM);
        inv.setItem(11,item(Material.LIME_CONCRETE,ChatColor.GREEN+"CONFIRM","",ChatColor.YELLOW+"Buy for $"+money(l.price()),ChatColor.GRAY+"Seller: "+l.sellerName()));
        inv.setItem(13,l.item());inv.setItem(15,item(Material.RED_CONCRETE,ChatColor.RED+"CANCEL"));
        p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String t=e.getView().getTitle();if(!t.equals(TITLE)&&!t.equals(CONFIRM)&&!t.equals(SELLING))return;
        e.setCancelled(true);int s=e.getRawSlot();if(s<0||s>=e.getInventory().getSize())return;
        if(t.equals(TITLE)){if(s==46){open(p,pages.getOrDefault(p.getUniqueId(),0),searchQueries.get(p.getUniqueId()));return;}
        if(s==47){int next=(sortModes.getOrDefault(p.getUniqueId(),0)+1)%3;sortModes.put(p.getUniqueId(),next);open(p,0,searchQueries.get(p.getUniqueId()));return;}
        if(s==52){
            p.closeInventory(); awaitingSearch.add(p.getUniqueId());
            p.sendMessage(ChatColor.YELLOW+"☀ Type what you want to search for in chat, or type "+ChatColor.RED+"cancel"+ChatColor.YELLOW+" to stop.");
            return;
        }if(s==53){p.closeInventory();if(p.getInventory().getItemInMainHand().getType().isAir()){p.sendMessage(ChatColor.RED+"Hold the item you want to sell in your main hand.");return;}awaitingPrice.add(p.getUniqueId());p.sendMessage(ChatColor.YELLOW+"☀ Type the price in chat, or type "+ChatColor.RED+"cancel"+ChatColor.YELLOW+" to stop.");return;}int page=pages.getOrDefault(p.getUniqueId(),0);if(s==48&&page>0){open(p,page-1);return;}if(s==50&&(page+1)*45<visibleListings(p,searchQueries.get(p.getUniqueId())).size()){open(p,page+1);return;}if(s==45){openSelling(p);return;}if(s<45){List<AuctionListing> ls=visibleListings(p,searchQueries.get(p.getUniqueId()));int idx=page*45+s;if(idx<ls.size())confirm(p,ls.get(idx));}}
        else if(t.equals(CONFIRM)){if(s==11){ItemStack shown=e.getInventory().getItem(13);long id=findId(p);AuctionListing l=manager.get(id);if(l!=null&&manager.buy(p,id)){p.closeInventory();p.sendMessage(ChatColor.GREEN+"☀ Purchase complete!");p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.2f);}else{p.sendMessage(ChatColor.RED+"Purchase failed: listing changed, insufficient funds, or inventory full.");}}else if(s==15)p.closeInventory();}
        else {if(s==49){open(p);return;}if(s<45){List<AuctionListing> mine=manager.all().stream().filter(x->x.seller().equals(p.getUniqueId())).toList();if(s<mine.size()){long id=mine.get(s).id();if(manager.cancel(p,id)){p.sendMessage(ChatColor.YELLOW+"Listing cancelled and item returned.");p.playSound(p.getLocation(),org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,1f,1.4f);openSelling(p);}}}}
    }
    @EventHandler public void chat(org.bukkit.event.player.AsyncPlayerChatEvent e){
        Player p=e.getPlayer();
        if(awaitingSearch.remove(p.getUniqueId())){
            e.setCancelled(true);
            String msg=e.getMessage().trim();
            if(msg.equalsIgnoreCase("cancel")){p.sendMessage(ChatColor.GRAY+"Search cancelled.");open(p);return;}
            Bukkit.getScheduler().runTask(plugin,()->openSearch(p,msg));
            return;
        }
        if(!awaitingPrice.remove(p.getUniqueId()))return;e.setCancelled(true);String msg=e.getMessage().trim();if(msg.equalsIgnoreCase("cancel")){p.sendMessage(ChatColor.GRAY+"Sell cancelled.");return;}try{double price=parsePrice(msg);Bukkit.getScheduler().runTask(plugin,()->{AuctionListing x=manager.create(p,price);if(x==null)p.sendMessage(ChatColor.RED+"Could not list item. Check that you are holding an item and the price is valid.");else{p.sendMessage(ChatColor.GREEN+"☀ Listed "+x.item().getAmount()+"x "+x.item().getType()+" for $"+money(price)+"!");open(p);}});}catch(NumberFormatException ex){p.sendMessage(ChatColor.RED+"Invalid price. Sell cancelled.");}}
    private long findId(Player p){ // confirmation inventories are opened directly from a listing; identify by stored holder
        return pending.getOrDefault(p.getUniqueId(),-1L);
    }
}
