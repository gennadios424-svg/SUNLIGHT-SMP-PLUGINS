package net.sunlightsmp.playersettings;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public final class AuctionListing {
    private final long id;
    private final UUID seller;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long expiresAt;

    public AuctionListing(long id, UUID seller, String sellerName, ItemStack item, double price, long expiresAt) {
        this.id=id; this.seller=seller; this.sellerName=sellerName; this.item=item.clone(); this.price=price; this.expiresAt=expiresAt;
    }
    public long id(){return id;} public UUID seller(){return seller;} public String sellerName(){return sellerName;}
    public ItemStack item(){return item.clone();} public double price(){return price;} public long expiresAt(){return expiresAt;}
    public boolean expired(){return System.currentTimeMillis()>=expiresAt;}
}
