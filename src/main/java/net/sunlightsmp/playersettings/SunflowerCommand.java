package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class SunflowerCommand implements org.bukkit.command.CommandExecutor, org.bukkit.event.Listener {
    private static final String TITLE = ChatColor.GOLD + "☀ Sunflower Balance";
    private final SunlightPlayerSettings plugin;
    private final SunflowerManager manager;

    public SunflowerCommand(SunlightPlayerSettings plugin, SunflowerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack i = new ItemStack(material);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        i.setItemMeta(meta);
        return i;
    }

    private String fmt(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    public void open(Player p) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54, TITLE);

        for (int i = 0; i < 54; i++) {
            if (i < 45) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));
            else inv.setItem(i, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        inv.setItem(4, item(Material.SUNFLOWER,
                ChatColor.GOLD + "☀ YOUR SUNFLOWERS",
                "",
                ChatColor.YELLOW + fmt(manager.get(p)),
                ChatColor.GRAY + "Your current Sunflower balance"));

        inv.setItem(22, item(Material.SUNFLOWER,
                ChatColor.YELLOW + "☀ Sunflower Wallet",
                "",
                ChatColor.WHITE + "Balance: " + ChatColor.GOLD + fmt(manager.get(p)),
                ChatColor.GRAY + "Use Sunflowers to buy",
                ChatColor.GRAY + "Sunlight Spawners and more"));

        inv.setItem(20, item(Material.CLOCK,
                ChatColor.AQUA + "5 Minute Claim",
                "",
                ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/sunflower claim",
                ChatColor.GRAY + "to claim your timed reward"));

        inv.setItem(24, item(Material.PLAYER_HEAD,
                ChatColor.LIGHT_PURPLE + "Sunflower Leaderboard",
                "",
                ChatColor.GRAY + "See the top Sunflower balances",
                ChatColor.GRAY + "across the server"));

        inv.setItem(40, item(Material.HONEYCOMB,
                ChatColor.GREEN + "AFK Earnings",
                "",
                ChatColor.GRAY + "Stay still for 5 minutes",
                ChatColor.GRAY + "and automatically earn Sunflowers",
                ChatColor.DARK_GRAY + "4x with the Sunflower Booster"));

        inv.setItem(49, item(Material.GOLD_BLOCK,
                ChatColor.GOLD + "☀ SUNLIGHT ECONOMY",
                "",
                ChatColor.GRAY + "Earn • Save • Spend",
                ChatColor.YELLOW + "1,500 Sunflowers = 1 Spawner"));

        inv.setItem(53, item(Material.BARRIER, ChatColor.RED + "Close"));
        p.openInventory(inv);
    }

    private void openLeaderboard(Player p) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 54,
                ChatColor.GOLD + "☀ Sunflower Top");

        for (int i = 0; i < 54; i++) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));

        int slot = 10;
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : manager.topBalances(10).entrySet()) {
            if (slot > 43) break;
            Player online = plugin.getServer().getPlayer(entry.getKey());
            org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(entry.getKey());
            String name = online != null ? online.getName() : player.getName();
            if (name == null) name = "Unknown";
            Material icon = rank == 1 ? Material.GOLD_BLOCK : rank == 2 ? Material.IRON_BLOCK : rank == 3 ? Material.COPPER_BLOCK : Material.SUNFLOWER;
            inv.setItem(slot, item(icon,
                    ChatColor.YELLOW + "#" + rank + " " + ChatColor.WHITE + name,
                    "",
                    ChatColor.GOLD + "☀ " + fmt(entry.getValue()) + " Sunflowers"));
            slot += 2;
            rank++;
        }

        inv.setItem(49, item(Material.ARROW, ChatColor.YELLOW + "Back"));
        inv.setItem(53, item(Material.BARRIER, ChatColor.RED + "Close"));
        p.openInventory(inv);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use this command."); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (!p.hasPermission("sunlightsmp.sunflower.admin")) { p.sendMessage(ChatColor.RED + "No permission."); return true; }
            if (args.length < 3) { p.sendMessage(ChatColor.YELLOW + "☀ /sunflower give <player> <amount>"); return true; }
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) { p.sendMessage(ChatColor.RED + "Player not found or offline."); return true; }
            long amount; try { amount = Long.parseLong(args[2].replace(",", "")); } catch (Exception ex) { p.sendMessage(ChatColor.RED + "Amount must be a number."); return true; }
            if (amount <= 0) { p.sendMessage(ChatColor.RED + "Amount must be greater than 0."); return true; }
            if (amount > 1_000_000_000L) { p.sendMessage(ChatColor.RED + "Amount is too large."); return true; }
            manager.add(target, amount);
            p.sendMessage(ChatColor.GREEN + "☀ Gave " + ChatColor.YELLOW + fmt(amount) + ChatColor.GREEN + " Sunflowers to " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
            target.sendMessage(ChatColor.GOLD + "☀ You received " + ChatColor.YELLOW + fmt(amount) + ChatColor.GOLD + " Sunflowers.");
            return true;
        }
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
        open(p);
        return true;
    }

    private boolean hasBooster(Player p) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (i == null || !i.hasItemMeta()) continue;
            var c = i.getItemMeta().getPersistentDataContainer();
            Byte b = c.get(new org.bukkit.NamespacedKey(plugin, "sunlight_shard_booster"), org.bukkit.persistence.PersistentDataType.BYTE);
            Long t = c.get(new org.bukkit.NamespacedKey(plugin, "sunlight_shard_booster_expiry"), org.bukkit.persistence.PersistentDataType.LONG);
            if (b != null && b == 1 && t != null && System.currentTimeMillis() < t) return true;
        }
        return false;
    }

    @org.bukkit.event.EventHandler
    public void click(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(TITLE) && !e.getView().getTitle().equals(ChatColor.GOLD + "☀ Sunflower Top")) return;
        e.setCancelled(true);
        if (e.getRawSlot() < 0 || e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        if (e.getView().getTitle().equals(TITLE) && e.getRawSlot() == 24) {
            openLeaderboard(p);
        } else if (e.getView().getTitle().equals(ChatColor.GOLD + "☀ Sunflower Top") && e.getRawSlot() == 49) {
            open(p);
        } else if ((e.getView().getTitle().equals(TITLE) || e.getView().getTitle().equals(ChatColor.GOLD + "☀ Sunflower Top")) && e.getRawSlot() == 53) {
            p.closeInventory();
        }
    }
}