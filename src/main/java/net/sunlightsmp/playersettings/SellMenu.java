package net.sunlightsmp.playersettings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import java.text.NumberFormat;
import java.util.*;

public final class SellMenu implements Listener {
    private static final String TITLE=ChatColor.GOLD+"☀ Sunlight Sell";
    private final SunlightPlayerSettings plugin;
    public SellMenu(SunlightPlayerSettings plugin){this.plugin=plugin;}
    private Economy economy(){var reg=plugin.getServer().getServicesManager().getRegistration(Economy.class);return reg==null?null:reg.getProvider();}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));i.setItemMeta(meta);return i;}
    private String money(double n){return NumberFormat.getNumberInstance(Locale.US).format(n);}
    private void frame(Inventory inv){ ItemStack side=item(Material.ORANGE_STAINED_GLASS_PANE," "); ItemStack accent=item(Material.YELLOW_STAINED_GLASS_PANE,ChatColor.GOLD+"☀"); for(int r=0;r<inv.getSize()/9;r++){inv.setItem(r*9,side);inv.setItem(r*9+8,side);} for(int s=0;s<9;s++){inv.setItem(s,accent);inv.setItem(inv.getSize()-9+s,accent);} }
    public void open(Player p){Inventory inv=Bukkit.createInventory(null,54,TITLE);frame(inv);inv.setItem(45,item(Material.SUNFLOWER,ChatColor.GOLD+"☀ Sunlight Sell","",ChatColor.GRAY+"Place items in the top 45 slots."));inv.setItem(49,item(Material.GOLD_INGOT,ChatColor.GREEN+"SELL ALL","",ChatColor.GRAY+"Shulkers are opened and their contents are sold too."));inv.setItem(53,item(Material.BARRIER,ChatColor.RED+"Close",ChatColor.GRAY+"Items are returned to you."));p.openInventory(inv);}
    private double value(ItemStack x){if(x==null||x.getType().isAir())return 0;double total=0;if(x.getType().name().endsWith("SHULKER_BOX")&&x.getItemMeta() instanceof BlockStateMeta meta&&meta.hasBlockState()&&meta.getBlockState() instanceof ShulkerBox box){for(ItemStack inside:box.getInventory().getContents())total+=value(inside);}else total+=SellPricing.price(x.getType())*x.getAmount();return total;}
    private double total(Inventory inv){double total=0;for(int i=0;i<45;i++)total+=value(inv.getItem(i));return total;}
    private void returnItems(Player p,Inventory inv){for(int i=0;i<45;i++){ItemStack x=inv.getItem(i);if(x==null||x.getType().isAir())continue;Map<Integer,ItemStack> left=p.getInventory().addItem(x.clone());left.values().forEach(y->p.getWorld().dropItemNaturally(p.getLocation(),y));inv.setItem(i,null);}}
    private void sell(Player p,Inventory inv){Economy eco=economy();double value=total(inv);if(eco==null){p.sendMessage(ChatColor.RED+"Economy is unavailable.");return;}if(value<=0){p.sendMessage(ChatColor.RED+"Place items in the sell menu first.");return;}for(int i=0;i<45;i++)inv.setItem(i,null);eco.depositPlayer(p,value);p.sendMessage(ChatColor.GREEN+"☀ Sold items for "+ChatColor.GOLD+"$"+money(value)+ChatColor.GREEN+"! Shulker contents were included.");p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.25f);p.closeInventory();}
    @EventHandler public void click(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p)||!e.getView().getTitle().equals(TITLE))return;int s=e.getRawSlot();if(s<0||s>=54)return;if(s==49){e.setCancelled(true);sell(p,e.getInventory());return;}if(s>=45){e.setCancelled(true);return;}}
    @EventHandler public void drag(InventoryDragEvent e){if(!e.getView().getTitle().equals(TITLE))return;for(int slot:e.getRawSlots())if(slot>=45){e.setCancelled(true);break;}}
    @EventHandler public void close(InventoryCloseEvent e){if(!(e.getPlayer() instanceof Player p)||!e.getView().getTitle().equals(TITLE))return;Inventory inv=e.getInventory();Bukkit.getScheduler().runTask(plugin,()->returnItems(p,inv));}
}