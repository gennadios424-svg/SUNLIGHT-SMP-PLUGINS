package net.sunlightsmp.playersettings;
import org.bukkit.inventory.ItemStack;import java.util.UUID;
public record Order(long id,UUID buyer,String buyerName,ItemStack item,int remaining,double totalPrice,double pricePerItem,long expiresAt,String status){
 public boolean expired(){return System.currentTimeMillis()>=expiresAt;} public double paymentFor(int n){return pricePerItem*n;}
}