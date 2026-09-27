package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.*;

public final class WorthCommand implements CommandExecutor, Listener, TabCompleter {
    private static final String TITLE = ChatColor.GOLD + "☀ Sunlight Worth";
    private final SunlightPlayerSettings plugin;
    private final Map<UUID, Integer> pages = new HashMap<>();

    public WorthCommand(SunlightPlayerSettings plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use /worth."); return true; }
        if (args.length > 0) {
            String query = String.join(" ", args).replace("_", " ").trim().toLowerCase(Locale.ROOT);
            List<Material> matches = new ArrayList<>();
            for (Material m : obtainableItems()) {
                String name = pretty(m).toLowerCase(Locale.ROOT);
                String raw = m.name().toLowerCase(Locale.ROOT).replace("_", " ");
                if (name.contains(query) || raw.contains(query)) matches.add(m);
            }
            if (matches.isEmpty()) {
                p.sendMessage(ChatColor.RED + "☀ No obtainable items matched: " + String.join(" ", args));
                return true;
            }
            pages.put(p.getUniqueId(), 0);
            drawSearch(p, matches, String.join(" ", args));
            return true;
        }

        open(p);
        return true;
    }

    private void drawSearch(Player p, List<Material> items, String query) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE + ChatColor.DARK_GRAY + " • " + query);
        for (int i = 45; i < 54; i++) inv.setItem(i, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        for (int i = 0; i < Math.min(45, items.size()); i++) {
            Material m = items.get(i);
            double each = SellPricing.price(m);
            inv.setItem(i, item(m, ChatColor.WHITE + pretty(m),
                    "",
                    ChatColor.GREEN + "Sell: $" + money(each) + " each",
                    ChatColor.GRAY + "64 items: $" + money(each * 64)));
        }
        inv.setItem(45, item(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        inv.setItem(49, item(Material.SUNFLOWER, ChatColor.GOLD + "☀ " + query,
                ChatColor.GRAY + "Matching items: " + items.size()));
        inv.setItem(53, item(Material.ARROW, ChatColor.YELLOW + "Next Page"));
        p.openInventory(inv);
    }

    public void open(Player p) {
        pages.put(p.getUniqueId(), 0);
        draw(p);
    }

    private void draw(Player p) {
        List<Material> items = obtainableItems();
        int page = pages.getOrDefault(p.getUniqueId(), 0);
        int maxPage = Math.max(0, (items.size() - 1) / 45);
        if (page > maxPage) page = maxPage;
        pages.put(p.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 45; i < 54; i++) inv.setItem(i, item(Material.GRAY_STAINED_GLASS_PANE, " "));

        int start = page * 45;
        for (int i = 0; i < 45 && start + i < items.size(); i++) {
            Material m = items.get(start + i);
            double each = SellPricing.price(m);
            inv.setItem(i, item(m, ChatColor.WHITE + pretty(m),
                    "",
                    ChatColor.GREEN + "Sell: $" + money(each) + " each",
                    ChatColor.GRAY + "64 items: $" + money(each * 64)));
        }

        inv.setItem(45, item(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
        inv.setItem(49, item(Material.SUNFLOWER, ChatColor.GOLD + "☀ Sunlight Worth",
                ChatColor.GRAY + "Every survival-obtainable item",
                ChatColor.GRAY + "is listed here."));
        inv.setItem(53, item(Material.ARROW, ChatColor.YELLOW + "Next Page"));
        p.openInventory(inv);
    }

    private List<Material> obtainableItems() {
        List<Material> out = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!isWorthListed(m)) continue;
            out.add(m);
        }
        out.sort(Comparator.comparing(m -> m.name()));
        return out;
    }

    private boolean isWorthListed(Material m) {
        if (m == null || m == Material.AIR || !m.isItem()) return false;
        String n = m.name();
        if (n.endsWith("_SPAWN_EGG")) return false;
        return switch (m) {
            case BEDROCK, BARRIER, END_PORTAL_FRAME, END_PORTAL, NETHER_PORTAL,
                 COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK,
                 STRUCTURE_BLOCK, STRUCTURE_VOID, JIGSAW, LIGHT,
                 DEBUG_STICK, KNOWLEDGE_BOOK, BUDDING_AMETHYST,
                 REINFORCED_DEEPSLATE, PETRIFIED_OAK_SLAB -> false;
            default -> true;
        };
    }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        i.setItemMeta(meta);
        return i;
    }

    private String pretty(Material m) {
        StringBuilder s = new StringBuilder();
        for (String x : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (s.length() > 0) s.append(' ');
            s.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));
        }
        return s.toString();
    }

    private String money(double n) {
        return NumberFormat.getNumberInstance(Locale.US).format(n);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        int s = e.getRawSlot();
        if (s == 45) {
            int page = pages.getOrDefault(p.getUniqueId(), 0);
            if (page > 0) { pages.put(p.getUniqueId(), page - 1); draw(p); }
        } else if (s == 53) {
            List<Material> items = obtainableItems();
            int page = pages.getOrDefault(p.getUniqueId(), 0);
            if ((page + 1) * 45 < items.size()) { pages.put(p.getUniqueId(), page + 1); draw(p); }
        }
    }
}
