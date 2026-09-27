package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.entity.Player;

public final class SunflowerCommand implements org.bukkit.command.CommandExecutor {
    private final SunlightPlayerSettings plugin;
    private final SunflowerManager manager;
    public SunflowerCommand(SunlightPlayerSettings plugin, SunflowerManager manager) { this.plugin=plugin; this.manager=manager; }

    @Override public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use this command."); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("claim")) {
            if (!manager.claim(p)) {
                p.sendMessage(ChatColor.RED + "☀ You can claim Sunflowers once every 5 minutes.");
                return true;
            }
            long amount = hasBooster(p) ? 4 : 1;
            p.sendMessage(ChatColor.GREEN + "☀ You claimed " + ChatColor.YELLOW + amount + " Sunflower(s)"
                    + ChatColor.GREEN + "! Balance: " + ChatColor.YELLOW + manager.get(p));
            return true;
        }
        p.sendMessage(ChatColor.GOLD + "☀ Sunflowers: " + ChatColor.YELLOW + manager.get(p));
        p.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/sunflower claim" + ChatColor.GRAY + " every 5 minutes.");
        p.sendMessage(ChatColor.GRAY + "Go AFK to automatically earn Sunflowers every 5 minutes.");
        return true;
    }

    private boolean hasBooster(Player p) {
        for (org.bukkit.inventory.ItemStack i : p.getInventory().getContents()) {
            if (i == null || !i.hasItemMeta()) continue;
            var c=i.getItemMeta().getPersistentDataContainer();
            Byte b=c.get(new NamespacedKey(plugin,"sunlight_shard_booster"),org.bukkit.persistence.PersistentDataType.BYTE);
            Long t=c.get(new NamespacedKey(plugin,"sunlight_shard_booster_expiry"),org.bukkit.persistence.PersistentDataType.LONG);
            if(b!=null&&b==1&&t!=null&&System.currentTimeMillis()<t)return true;
        }
        return false;
    }
}