package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class CoinflipCommand implements org.bukkit.command.CommandExecutor, Listener {
    private static final String TITLE = ChatColor.GOLD + "☀ Coinflip";
    private static final String ROLLING = ChatColor.GOLD + "☀ Coinflip • Rolling...";
    private final SunlightPlayerSettings plugin;
    private final CoinflipManager manager;
    private final Set<UUID> rolling = new HashSet<>();

    public CoinflipCommand(SunlightPlayerSettings plugin, CoinflipManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack i = new ItemStack(material);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        i.setItemMeta(m);
        return i;
    }

    private String pct(long part, long total) {
        if (total <= 0) return "0.0%";
        return String.format(Locale.US, "%.1f%%", (part * 100.0) / total);
    }

    private void fill(org.bukkit.inventory.Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));
    }

    public void open(Player p) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        fill(inv);

        CoinflipManager.Stats s = manager.get(p.getUniqueId());
        inv.setItem(13, item(Material.SUNFLOWER,
                ChatColor.GOLD + "☀ Coinflip",
                "",
                ChatColor.GRAY + "Flip the coin and see what you get",
                ChatColor.DARK_GRAY + "No items are consumed"));

        inv.setItem(20, item(Material.PLAYER_HEAD,
                ChatColor.YELLOW + "Your Stats",
                "",
                ChatColor.GRAY + "Total flips: " + ChatColor.WHITE + s.flips,
                ChatColor.GRAY + "Heads: " + ChatColor.WHITE + s.heads + ChatColor.DARK_GRAY + " (" + pct(s.heads, s.flips) + ")",
                ChatColor.GRAY + "Tails: " + ChatColor.WHITE + s.tails + ChatColor.DARK_GRAY + " (" + pct(s.tails, s.flips) + ")"));

        inv.setItem(24, item(Material.SUNFLOWER,
                ChatColor.YELLOW + "Start Flip",
                "",
                ChatColor.GRAY + "Click to flip the coin",
                ChatColor.DARK_GRAY + "The result is shown after the roll"));

        inv.setItem(31, item(Material.GOLD_NUGGET,
                ChatColor.GOLD + "Coin Stats",
                "",
                ChatColor.GRAY + "Heads: " + ChatColor.WHITE + s.heads,
                ChatColor.GRAY + "Tails: " + ChatColor.WHITE + s.tails,
                ChatColor.GRAY + "Total: " + ChatColor.WHITE + s.flips));

        inv.setItem(40, item(Material.BARRIER, ChatColor.RED + "Close"));
        p.openInventory(inv);
    }

    private void openRolling(Player p) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 45, ROLLING);
        fill(inv);
        inv.setItem(13, item(Material.SUNFLOWER, ChatColor.GOLD + "☀ FLIPPING...",
                "", ChatColor.GRAY + "The coin is rolling"));
        inv.setItem(20, item(Material.IRON_NUGGET, ChatColor.WHITE + "HEADS"));
        inv.setItem(24, item(Material.GOLD_NUGGET, ChatColor.GOLD + "TAILS"));
        inv.setItem(31, item(Material.CLOCK, ChatColor.AQUA + "Please wait...",
                "", ChatColor.GRAY + "The result will appear shortly"));
        p.openInventory(inv);
    }

    private void finish(Player p) {
        rolling.remove(p.getUniqueId());
        boolean heads = ThreadLocalRandom.current().nextBoolean();
        manager.record(p.getUniqueId(), heads);

        p.closeInventory();
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, heads ? 1.4f : 0.8f);
        p.sendTitle(
                heads ? ChatColor.WHITE + "HEADS" : ChatColor.GOLD + "TAILS",
                ChatColor.GRAY + "☀ Coinflip result",
                5, 35, 10
        );
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(p), 45L);
    }

    private void animate(Player p) {
        final UUID id = p.getUniqueId();
        rolling.add(id);
        openRolling(p);

        Material[] faces = {Material.GOLD_NUGGET, Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.IRON_NUGGET,
                Material.GOLD_NUGGET, Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.IRON_NUGGET};
        String[] names = {ChatColor.GOLD + "TAILS", ChatColor.WHITE + "HEADS", ChatColor.GOLD + "TAILS", ChatColor.WHITE + "HEADS",
                ChatColor.GOLD + "TAILS", ChatColor.WHITE + "HEADS", ChatColor.GOLD + "TAILS", ChatColor.WHITE + "HEADS"};

        for (int i = 0; i < faces.length; i++) {
            final int step = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!rolling.contains(id) || !p.isOnline()) return;
                org.bukkit.inventory.Inventory inv = p.getOpenInventory().getTopInventory();
                if (!p.getOpenInventory().getTitle().equals(ROLLING)) return;
                inv.setItem(13, item(faces[step], names[step], "",
                        ChatColor.GRAY + "Rolling...",
                        ChatColor.DARK_GRAY + "Flip #" + (step + 1)));
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.55f, 1.0f + (step * 0.05f));
            }, 8L + (i * 6L));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (rolling.contains(id) && p.isOnline()) finish(p);
        }, 62L);
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (rolling.contains(p.getUniqueId())) return true;
        open(p);
        return true;
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (!title.equals(TITLE) && !title.equals(ROLLING)) return;
        e.setCancelled(true);
        if (title.equals(ROLLING)) return;

        int slot = e.getRawSlot();
        if (slot == 24) {
            animate(p);
        } else if (slot == 40) {
            p.closeInventory();
        } else if (slot == 20) {
            open(p);
        }
    }

    @EventHandler
    public void close(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (e.getView().getTitle().equals(ROLLING)) {
            rolling.remove(p.getUniqueId());
        }
    }
}