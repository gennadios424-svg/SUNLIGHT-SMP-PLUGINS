package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.util.*;

public final class SunflowerSpawnerCommand implements org.bukkit.command.CommandExecutor {
    private final SunlightPlayerSettings plugin;
    private final SunflowerManager manager;
    public SunflowerSpawnerCommand(SunlightPlayerSettings plugin, SunflowerManager manager) { this.plugin=plugin; this.manager=manager; }

    public static ItemStack createSpawner(EntityType type) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta)item.getItemMeta();
        CreatureSpawner state = (CreatureSpawner)meta.getBlockState();
        state.setSpawnedType(type);
        meta.setBlockState(state);
        meta.setDisplayName(ChatColor.GOLD + "☀ " + pretty(type) + " Spawner");
        meta.setLore(List.of(ChatColor.GRAY+"Sunlight Spawner", ChatColor.YELLOW+"Cost: 1,500 Sunflowers"));
        item.setItemMeta(meta);
        return item;
    }

    private static String pretty(EntityType t) { String s=t.name().toLowerCase(Locale.ROOT).replace('_',' '); return Character.toUpperCase(s.charAt(0))+s.substring(1); }

    @Override public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if(args.length!=1){p.sendMessage(ChatColor.YELLOW+"/sunflowerspawner <skeleton|zombie|witch|iron_golem|spider|creeper>");return true;}
        EntityType t;
        try { t=EntityType.valueOf(args[0].toUpperCase(Locale.ROOT)); } catch(Exception e){p.sendMessage(ChatColor.RED+"Invalid spawner type.");return true;}
        if(t!=EntityType.SKELETON&&t!=EntityType.ZOMBIE&&t!=EntityType.WITCH&&t!=EntityType.IRON_GOLEM&&t!=EntityType.SPIDER&&t!=EntityType.CREEPER){p.sendMessage(ChatColor.RED+"That spawner is not available.");return true;}
        if(!manager.remove(p,1500)){p.sendMessage(ChatColor.RED+"☀ You need 1,500 Sunflowers. Balance: "+ChatColor.YELLOW+manager.get(p));return true;}
        Map<Integer,ItemStack> left=p.getInventory().addItem(createSpawner(t));
        if(!left.isEmpty()){manager.add(p,1500);p.sendMessage(ChatColor.RED+"Not enough inventory space.");return true;}
        p.sendMessage(ChatColor.GREEN+"☀ Purchased a "+ChatColor.YELLOW+pretty(t)+" Spawner "+ChatColor.GREEN+"for "+ChatColor.YELLOW+"1,500 Sunflowers.");
        return true;
    }
}