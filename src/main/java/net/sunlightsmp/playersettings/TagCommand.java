package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class TagCommand implements org.bukkit.command.CommandExecutor, Listener {
    private final SunlightPlayerSettings plugin;
    private final TagManager manager;
    private final Map<UUID, String> pending = new HashMap<>();
    private static final String SHOP = ChatColor.GOLD + "☀ " + ChatColor.YELLOW + "SUNLIGHT SMP" + ChatColor.DARK_GRAY + " • " + ChatColor.WHITE + "TAG SHOP";
    private static final String CONFIRM = ChatColor.GOLD + "☀ " + ChatColor.YELLOW + "SUNLIGHT SMP" + ChatColor.DARK_GRAY + " • " + ChatColor.WHITE + "PURCHASE TAG";

    public TagCommand(SunlightPlayerSettings plugin, TagManager manager) { this.plugin=plugin; this.manager=manager; }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        i.setItemMeta(m);
        return i;
    }

    private String money(long n) { return String.format(Locale.US, "%,d", n); }

    private void fill(Inventory inv) {
        for (int i=0;i<inv.getSize();i++)
            inv.setItem(i,item(Material.BLACK_STAINED_GLASS_PANE," "));
    }

    public void open(Player p) {
        Inventory inv=Bukkit.createInventory(null,45,SHOP);
        fill(inv);
        inv.setItem(4,item(Material.SUNFLOWER,ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"YOUR SUNFLOWERS",
            "",ChatColor.WHITE+"Balance: "+ChatColor.GOLD+money(plugin.getSunflowerManager().get(p))+" 🌻",
            ChatColor.GRAY+"Every tag costs "+ChatColor.YELLOW+"1,000 Sunflowers"));

        int[] slots={20,22,24,30,32};
        Material[] mats={Material.SUNFLOWER,Material.GOLD_INGOT,Material.SPYGLASS,Material.PLAYER_HEAD,Material.PAPER};
        for(int i=0;i<manager.getTags().size();i++){
            TagManager.Tag t=manager.getTags().get(i);
            boolean own=manager.owns(p.getUniqueId(),t.name());
            boolean eq=t.name().equalsIgnoreCase(manager.equipped(p.getUniqueId()));
            String status=eq?ChatColor.GREEN+"● EQUIPPED":own?ChatColor.GREEN+"● OWNED":ChatColor.YELLOW+"● 1,000 🌻";
            inv.setItem(slots[i],item(mats[i],t.color()+t.icon()+" ["+t.name()+"]",
                "",status,own?(eq?ChatColor.GRAY+"Currently equipped":ChatColor.WHITE+"Click to equip for free"):
                ChatColor.WHITE+"Click to purchase",own?"":ChatColor.GOLD+"Cost: "+ChatColor.YELLOW+"1,000 Sunflowers"));
        }
        inv.setItem(40,item(Material.BARRIER,ChatColor.RED+"✕ CLOSE"));
        p.openInventory(inv);
    }

    private void confirm(Player p, String tag) {
        TagManager.Tag t=manager.find(tag); if(t==null)return;
        pending.put(p.getUniqueId(),t.name());
        Inventory inv=Bukkit.createInventory(null,27,CONFIRM);
        fill(inv);
        inv.setItem(4,item(Material.SUNFLOWER,ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"PURCHASE TAG",
            "",ChatColor.WHITE+"Tag: "+t.color()+"["+t.name()+"]",
            ChatColor.WHITE+"Cost: "+ChatColor.GOLD+"1,000 🌻 Sunflowers",
            ChatColor.WHITE+"Your balance: "+ChatColor.YELLOW+money(plugin.getSunflowerManager().get(p))+" 🌻"));
        inv.setItem(11,item(Material.LIME_STAINED_GLASS_PANE,ChatColor.GREEN+"✔ PURCHASE",
            ChatColor.GRAY+"Confirm purchase for "+ChatColor.YELLOW+"1,000 Sunflowers"));
        inv.setItem(15,item(Material.RED_STAINED_GLASS_PANE,ChatColor.RED+"✕ CANCEL",
            ChatColor.GRAY+"Return to the Tag Shop"));
        p.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p))return;
        String title=e.getView().getTitle();
        if(!SHOP.equals(title)&&!CONFIRM.equals(title))return;
        e.setCancelled(true);
        if(e.getRawSlot()<0||e.getRawSlot()>=e.getView().getTopInventory().getSize())return;

        if(SHOP.equals(title)){
            if(e.getRawSlot()==40){p.closeInventory();return;}
            int[] slots={20,22,24,30,32};
            for(int i=0;i<slots.length;i++) if(e.getRawSlot()==slots[i]){
                TagManager.Tag t=manager.getTags().get(i);
                if(manager.owns(p.getUniqueId(),t.name())){
                    manager.equip(p,t.name()); plugin.getTabList().refreshNow();
                    p.sendMessage(ChatColor.GREEN+"☀ Success! "+ChatColor.WHITE+"Equipped "+t.color()+"["+t.name()+"]"+ChatColor.WHITE+" for free.");
                    p.playSound(p.getLocation(),Sound.UI_BUTTON_CLICK,1f,1.2f);
                    open(p);
                } else confirm(p,t.name());
                return;
            }
            return;
        }

        if(e.getRawSlot()==15){pending.remove(p.getUniqueId());open(p);return;}
        if(e.getRawSlot()!=11)return;
        String tag=pending.get(p.getUniqueId());
        if(tag==null){open(p);return;}
        TagManager.PurchaseResult result=manager.purchase(p,tag);
        if(result==TagManager.PurchaseResult.PURCHASED){
            pending.remove(p.getUniqueId());
            plugin.getTabList().refreshNow();
            p.sendMessage(ChatColor.GREEN+"☀ Tag Purchased! "+ChatColor.WHITE+"You purchased "+ChatColor.YELLOW+"["+tag+"]"+ChatColor.WHITE+" for "+ChatColor.GOLD+"1,000 🌻 Sunflowers.");
            p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,1f,1.4f);
            open(p);
        } else if(result==TagManager.PurchaseResult.INSUFFICIENT){
            p.sendMessage(ChatColor.RED+"✕ Not enough Sunflowers! "+ChatColor.WHITE+"You need "+ChatColor.YELLOW+"1,000 🌻 Sunflowers"+ChatColor.WHITE+". Your balance: "+ChatColor.GOLD+money(plugin.getSunflowerManager().get(p))+" 🌻");
            p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_BASS,1f,0.7f);
            confirm(p,tag);
        } else if(result==TagManager.PurchaseResult.ALREADY_OWNED){
            manager.equip(p,tag); pending.remove(p.getUniqueId()); plugin.getTabList().refreshNow(); open(p);
        }
    }
}
