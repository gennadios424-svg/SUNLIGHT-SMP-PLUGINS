package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class SunlightCrateManager implements Listener {
    private static final String TITLE = "☀ Sunlight Crates";
    private final SunlightPlayerSettings plugin;
    private final Map<UUID, String> menus = new HashMap<>();
    private final Random random = new Random();

    public SunlightCrateManager(SunlightPlayerSettings plugin) { this.plugin = plugin; }

    public void openMenu(Player p) {
        Inventory inv = plugin.getServer().createInventory(null, 27, TITLE);
        String[] types = {"Common","Spawner","Sunlight","Crimson","Sunset"};
        Material[] mats = {Material.BARREL,Material.SPAWNER,Material.SUNFLOWER,Material.CRIMSON_FUNGUS,Material.SUNSET_ARMOR_TRIM_SMITHING_TEMPLATE};
        for (int i=0;i<5;i++) {
            ItemStack item = new ItemStack(mats[i]);
            ItemMeta meta=item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD+"☀ "+types[i]+" Crate");
            meta.setLore(List.of(ChatColor.GRAY+"Click to see this crate's key",ChatColor.YELLOW+"/crate open "+types[i].toLowerCase()));
            item.setItemMeta(meta);
            inv.setItem(10+i*2,item);
        }
        menus.put(p.getUniqueId(),"menu");
        p.openInventory(inv);
    }

    public void openCrate(Player p,String type) {
        String t=normalize(type);
        if (!List.of("common","spawner","sunlight","crimson","sunset").contains(t)) { p.sendMessage(ChatColor.RED+"Unknown crate."); return; }
        if (!takeKey(p,t)) { p.sendMessage(ChatColor.RED+"☀ You need a "+pretty(t)+" Crate Key."); return; }
        Inventory inv=plugin.getServer().createInventory(null,27,ChatColor.DARK_PURPLE+"✦ "+pretty(t)+" Crate");
        for(int i=0;i<27;i++) if(i<9||i>17) inv.setItem(i,glass());
        inv.setItem(13,crateIcon(t));
        inv.setItem(22,item(Material.TRIPWIRE_HOOK,ChatColor.YELLOW+"Opening "+pretty(t)+" Crate...",List.of(ChatColor.GRAY+"Your reward is being selected...")));
        p.openInventory(inv);
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            if(!p.isOnline()) return;
            if(!p.getOpenInventory().getTitle().equals(ChatColor.DARK_PURPLE+"✦ "+pretty(t)+" Crate")) return;
            ItemStack reward=reward(t);
            HashMap<Integer,ItemStack> left=p.getInventory().addItem(reward);
            left.values().forEach(x->p.getWorld().dropItemNaturally(p.getLocation(),x));
            p.closeInventory();
            p.sendMessage(ChatColor.GOLD+"☀ "+ChatColor.YELLOW+"CRATE OPENED! "+ChatColor.GRAY+"You won "+ChatColor.WHITE+name(reward)+" x"+reward.getAmount()+ChatColor.GRAY+"!");
            p.playSound(p.getLocation(),org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,1f,1.4f);
        },30L);
    }

    public void giveKey(Player p,String type,int amount) {
        String t=normalize(type);
        if(!List.of("common","spawner","sunlight","crimson","sunset").contains(t)) { p.sendMessage(ChatColor.RED+"Unknown crate."); return; }
        ItemStack key=item(Material.TRIPWIRE_HOOK,ChatColor.GOLD+"☀ "+pretty(t)+" Crate Key",List.of(ChatColor.GRAY+"Use with /crate open "+t));
        key.setAmount(Math.min(64,Math.max(1,amount)));
        HashMap<Integer,ItemStack> left=p.getInventory().addItem(key);
        left.values().forEach(x->p.getWorld().dropItemNaturally(p.getLocation(),x));
        p.sendMessage(ChatColor.GREEN+"☀ Given "+amount+" "+pretty(t)+" Crate Key(s).");
    }

    private boolean takeKey(Player p,String t) {
        String wanted=ChatColor.GOLD+"☀ "+pretty(t)+" Crate Key";
        for(ItemStack s:p.getInventory().getContents()) if(s!=null&&s.getType()==Material.TRIPWIRE_HOOK&&s.hasItemMeta()&&wanted.equals(s.getItemMeta().getDisplayName())){
            if(s.getAmount()==1) s.setAmount(0); else s.setAmount(s.getAmount()-1); return true;
        }
        return false;
    }
    private ItemStack reward(String t) {
        List<ItemStack> r=new ArrayList<>();
        switch(t) {
            case "common" -> { r.add(item(Material.DIAMOND,ChatColor.AQUA+"Diamond",List.of())); r.add(item(Material.GOLD_INGOT,ChatColor.GOLD+"Gold",List.of())); r.add(item(Material.EMERALD,ChatColor.GREEN+"Emerald",List.of())); r.add(item(Material.EXPERIENCE_BOTTLE,ChatColor.LIGHT_PURPLE+"XP Bottle",List.of())); }
            case "spawner" -> { r.add(item(Material.SKELETON_SPAWN_EGG,ChatColor.GRAY+"Skeleton Spawner Key Reward",List.of())); r.add(item(Material.ZOMBIE_SPAWN_EGG,ChatColor.GREEN+"Zombie Spawner Key Reward",List.of())); r.add(item(Material.SPIDER_SPAWN_EGG,ChatColor.DARK_GRAY+"Spider Spawner Key Reward",List.of())); }
            case "sunlight" -> { r.add(item(Material.SUNFLOWER,ChatColor.YELLOW+"Sunflowers",List.of())); r.add(item(Material.TOTEM_OF_UNDYING,ChatColor.GOLD+"Totem of Undying",List.of())); r.add(item(Material.ENDER_CHEST,ChatColor.DARK_PURPLE+"Ender Chest",List.of())); }
            case "crimson" -> { r.add(item(Material.NETHERITE_SCRAP,ChatColor.DARK_GRAY+"Netherite Scrap",List.of())); r.add(item(Material.CRIMSON_FUNGUS,ChatColor.RED+"Crimson Fungus",List.of())); r.add(item(Material.BLAZE_ROD,ChatColor.GOLD+"Blaze Rod",List.of())); }
            default -> { r.add(item(Material.GOLDEN_APPLE,ChatColor.GOLD+"Golden Apple",List.of())); r.add(item(Material.ENDER_PEARL,ChatColor.LIGHT_PURPLE+"Ender Pearl",List.of())); r.add(item(Material.SHULKER_SHELL,ChatColor.LIGHT_PURPLE+"Shulker Shell",List.of())); r.add(item(Material.DIAMOND_BLOCK,ChatColor.AQUA+"Diamond Block",List.of())); }
        }
        ItemStack x=r.get(random.nextInt(r.size())).clone();
        x.setAmount(t.equals("sunlight")&&x.getType()==Material.SUNFLOWER?random.nextInt(10)+5:random.nextInt(3)+1);
        return x;
    }
    private ItemStack crateIcon(String t){ return item(Material.CHEST,ChatColor.GOLD+"☀ "+pretty(t)+" Crate",List.of(ChatColor.GRAY+"Reward incoming...")); }
    private ItemStack glass(){return item(Material.GRAY_STAINED_GLASS_PANE," ",List.of());}
    private ItemStack item(Material m,String n,List<String> l){ItemStack x=new ItemStack(m);ItemMeta z=x.getItemMeta();z.setDisplayName(n);z.setLore(l);x.setItemMeta(z);return x;}
    private String normalize(String s){return s.toLowerCase(Locale.ROOT).replace("_","");}
    private String pretty(String s){return Character.toUpperCase(s.charAt(0))+s.substring(1)+" Crate";}
    private String name(ItemStack s){return s.hasItemMeta()&&s.getItemMeta().hasDisplayName()?s.getItemMeta().getDisplayName():s.getType().name().toLowerCase(Locale.ROOT);}
    @EventHandler public void click(InventoryClickEvent e){ if(!e.getView().getTitle().equals(TITLE)) return; e.setCancelled(true); }
}