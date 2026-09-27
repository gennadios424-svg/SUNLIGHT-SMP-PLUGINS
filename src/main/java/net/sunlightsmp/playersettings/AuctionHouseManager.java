package net.sunlightsmp.playersettings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AuctionHouseManager {
    private final SunlightPlayerSettings plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<Long,AuctionListing> listings=new LinkedHashMap<>();
    private long nextId=1;
    

    public AuctionHouseManager(SunlightPlayerSettings plugin){
        this.plugin=plugin; file=new File(plugin.getDataFolder(),"auction.yml"); load();
    }
    private Economy setupEconomy(){
        var rsp=plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return rsp==null?null:rsp.getProvider();
    }
    private Economy economy(){ return setupEconomy(); }
    public synchronized void load(){
        if(!file.exists()) try{plugin.getDataFolder().mkdirs();file.createNewFile();}catch(IOException e){plugin.getLogger().severe(e.getMessage());}
        data=YamlConfiguration.loadConfiguration(file); nextId=data.getLong("next-id",1);
        listings.clear();
        for(String key:data.getConfigurationSection("listings")==null?Collections.<String>emptyList():data.getConfigurationSection("listings").getKeys(false)){
            var c=data.getConfigurationSection("listings."+key); if(c==null)continue;
            ItemStack item=c.getItemStack("item"); if(item==null)continue;
            try{listings.put(Long.parseLong(key),new AuctionListing(Long.parseLong(key),UUID.fromString(c.getString("seller")),c.getString("seller-name","Unknown"),item,c.getDouble("price"),c.getLong("expires")));}catch(Exception ignored){}
        }
        cleanupExpired();
    }
    public synchronized void save(){
        data=new YamlConfiguration(); data.set("next-id",nextId);
        for(AuctionListing l:listings.values()){
            String p="listings."+l.id(); data.set(p+".seller",l.seller().toString()); data.set(p+".seller-name",l.sellerName());
            data.set(p+".price",l.price()); data.set(p+".expires",l.expiresAt()); data.set(p+".item",l.item());
        }
        try{data.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save auction.yml: "+e.getMessage());}
    }
    public synchronized void cleanupExpired(){
        boolean changed=false; Iterator<AuctionListing> it=listings.values().iterator();
        while(it.hasNext()){AuctionListing l=it.next(); if(l.expired() && Bukkit.getPlayer(l.seller())!=null){giveItem(l.seller(),l.item());it.remove();changed=true;}}
        if(changed)save();
    }
    private void giveItem(UUID uuid,ItemStack item){
        Player p=Bukkit.getPlayer(uuid);
        if(p!=null&&p.isOnline()){Map<Integer,ItemStack> left=p.getInventory().addItem(item);left.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));}
        else return;
    }
    public synchronized List<AuctionListing> all(){cleanupExpired(); List<AuctionListing> out=new ArrayList<>(); for(AuctionListing l:listings.values()) if(!l.expired()) out.add(l); return out;}
    public synchronized AuctionListing get(long id){return listings.get(id);}
    public synchronized AuctionListing create(Player seller,double price){
        ItemStack hand=seller.getInventory().getItemInMainHand(); if(hand.getType().isAir())return null;
        if(price<=0||price>plugin.getConfig().getDouble("auction.max-price",1000000000D))return null;
        ItemStack item=hand.clone(); seller.getInventory().setItemInMainHand(null);
        long days=plugin.getConfig().getLong("auction.expire-days",7);
        AuctionListing l=new AuctionListing(nextId++,seller.getUniqueId(),seller.getName(),item,price,System.currentTimeMillis()+days*86400000L);
        listings.put(l.id(),l);save();return l;
    }
    public synchronized boolean cancel(Player seller,long id){
        AuctionListing l=listings.get(id); if(l==null||!l.seller().equals(seller.getUniqueId()))return false;
        listings.remove(id);giveItem(seller.getUniqueId(),l.item());save();return true;
    }
    public synchronized boolean buy(Player buyer,long id){
        AuctionListing l=listings.get(id); if(l==null||l.expired()||l.seller().equals(buyer.getUniqueId()))return false;
        Economy economy=economy(); if(economy==null||!economy.has(buyer,l.price()))return false;
        if(buyer.getInventory().firstEmpty()==-1)return false;
        if(!economy.withdrawPlayer(buyer,l.price()).transactionSuccess())return false;
        if(!listings.remove(id,l)){economy.depositPlayer(buyer,l.price());return false;}
        if(economy.depositPlayer(Bukkit.getOfflinePlayer(l.seller()),l.price()).transactionSuccess()){
            buyer.getInventory().addItem(l.item()); save(); return true;
        }
        listings.put(id,l); economy.depositPlayer(buyer,l.price()); save(); return false;
    }
}
