package net.sunlightsmp.playersettings;
import org.bukkit.Bukkit; import org.bukkit.ChatColor; import org.bukkit.Material; import org.bukkit.entity.Player; import org.bukkit.event.*; import org.bukkit.event.inventory.InventoryClickEvent; import org.bukkit.inventory.*; import org.bukkit.inventory.meta.ItemMeta; import java.util.List;
public final class SettingsListener implements Listener {
  private final SunlightPlayerSettings plugin; private static final String TITLE=ChatColor.DARK_AQUA+"⚙ Sunlight Settings";
  public SettingsListener(SunlightPlayerSettings plugin){this.plugin=plugin;}
  public void open(Player p){
    Inventory inv=Bukkit.createInventory(null,27,TITLE);
    inv.setItem(4,item(Material.NETHER_STAR,ChatColor.AQUA+"Your Settings",List.of(ChatColor.GRAY+"Click a setting to toggle it.",ChatColor.GRAY+"Changes save automatically.")));
    Setting[] values=Setting.values(); for(int i=0;i<values.length;i++)inv.setItem(i+9,settingItem(p,values[i]));
    inv.setItem(22,item(Material.BARRIER,ChatColor.RED+"Close",List.of(ChatColor.GRAY+"Click to close."))); p.openInventory(inv);
  }
  private ItemStack settingItem(Player p,Setting s){boolean on=plugin.getSettings().get(p,s);return item(s.icon(),ChatColor.WHITE+s.displayName(),List.of("",ChatColor.GRAY+"Status: "+(on?ChatColor.GREEN+"ON":ChatColor.RED+"OFF"),ChatColor.YELLOW+"Click to toggle"));}
  private ItemStack item(Material m,String name,List<String> lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);meta.setLore(lore);i.setItemMeta(meta);return i;}
  @EventHandler public void click(InventoryClickEvent e){
    if(!(e.getWhoClicked() instanceof Player p)||!e.getView().getTitle().equals(TITLE))return;
    e.setCancelled(true); int slot=e.getRawSlot(); if(slot==22){p.closeInventory();return;} if(slot<9||slot>15)return;
    Setting s=Setting.values()[slot-9]; plugin.getSettings().toggle(p,s);
    p.sendMessage(ChatColor.GRAY+s.displayName()+": "+(plugin.getSettings().get(p,s)?ChatColor.GREEN+"ON":ChatColor.RED+"OFF"));
    Bukkit.getScheduler().runTask(plugin,()->open(p));
  }
}