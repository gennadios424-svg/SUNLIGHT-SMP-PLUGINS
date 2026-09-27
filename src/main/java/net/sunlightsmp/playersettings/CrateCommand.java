package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.stream.Collectors;

public final class CrateCommand implements CommandExecutor, TabCompleter {
    private final SunlightPlayerSettings plugin;
    private final CrateManager manager;
    public CrateCommand(SunlightPlayerSettings plugin, CrateManager manager) { this.plugin = plugin; this.manager = manager; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use crate commands."); return true; }
        if (args.length == 0) { p.sendMessage(ChatColor.GOLD + "✦ Sunlight Crates"); p.sendMessage(ChatColor.YELLOW + "/crates preview <crate>"); p.sendMessage(ChatColor.YELLOW + "/crates set <crate>"); p.sendMessage(ChatColor.YELLOW + "/crates remove"); p.sendMessage(ChatColor.YELLOW + "/crates give <player> <crate> <amount>"); p.sendMessage(ChatColor.YELLOW + "/crates key <player> <crate> <amount>"); p.sendMessage(ChatColor.YELLOW + "/crates reload"); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("preview")) {
            CrateType type = type(args, 1); if (type == null) { p.sendMessage(ChatColor.RED + "Unknown crate."); return true; }
            manager.preview(p, type); return true;
        }
        if (!p.hasPermission("sunlightsmp.crates.admin")) { p.sendMessage(ChatColor.RED + "You don't have permission."); return true; }
        if (sub.equals("set")) {
            CrateType type = type(args, 1); if (type == null) { p.sendMessage(ChatColor.RED + "Unknown crate."); return true; }
            var block = p.getTargetBlockExact(8); if (block == null) { p.sendMessage(ChatColor.RED + "Look directly at the block you want to register."); return true; }
            manager.setCrate(block.getLocation(), type); p.sendMessage(ChatColor.GREEN + "✦ " + type.displayName() + " Crate set."); return true;
        }
        if (sub.equals("remove")) {
            var block = p.getTargetBlockExact(8); if (block == null || manager.getCrate(block.getLocation()) == null) { p.sendMessage(ChatColor.RED + "Look at a registered crate."); return true; }
            manager.removeCrate(block.getLocation()); p.sendMessage(ChatColor.GREEN + "✦ Crate removed."); return true;
        }
        if (sub.equals("give") || sub.equals("key")) {
            if (args.length < 4) { p.sendMessage(ChatColor.RED + "Usage: /crates " + sub + " <player> <crate> <amount>"); return true; }
            Player target = Bukkit.getPlayerExact(args[1]); CrateType type = type(args, 2);
            if (target == null || type == null) { p.sendMessage(ChatColor.RED + "Invalid player or crate."); return true; }
            int amount; try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException e) { p.sendMessage(ChatColor.RED + "Invalid amount."); return true; }
            if (sub.equals("key")) target.getInventory().addItem(manager.createKey(type, amount));
            else target.getInventory().addItem(new ItemStack(type.icon(), amount));
            p.sendMessage(ChatColor.GREEN + "✦ Given " + amount + " " + type.displayName() + (sub.equals("key") ? " Key" : " Crate item") + " to " + target.getName() + ".");
            return true;
        }
        if (sub.equals("reload")) { plugin.reloadConfig(); manager.loadLocations(); p.sendMessage(ChatColor.GREEN + "✦ Sunlight Crates reloaded."); return true; }
        p.sendMessage(ChatColor.RED + "Unknown crate command."); return true;
    }

    private CrateType type(String[] args, int index) {
        if (args.length <= index) return null;
        try { return CrateType.valueOf(args[index].toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { return null; }
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("preview","set","remove","give","key","reload").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && (args[0].equalsIgnoreCase("preview") || args[0].equalsIgnoreCase("set"))) return Arrays.stream(CrateType.values()).map(CrateType::name).map(String::toLowerCase).toList();
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("key"))) return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("key"))) return Arrays.stream(CrateType.values()).map(CrateType::name).map(String::toLowerCase).toList();
        return List.of();
    }
}