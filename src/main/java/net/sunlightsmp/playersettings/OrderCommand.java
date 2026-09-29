package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;import org.bukkit.command.CommandExecutor;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class OrderCommand implements CommandExecutor, Listener {
    private final SunlightPlayerSettings plugin;
    private final OrderManager manager;
    private final Map<UUID, CreateSession> creating = new HashMap<>();
    private final Map<UUID, Long> fulfilling = new HashMap<>();

    private static final String MAIN = "☀ Player Orders";
    private static final String BROWSE = "☀ Browse Orders";
    private static final String MY = "☀ My Orders";
    private static final String HISTORY = "☀ Order History";

    public OrderCommand(SunlightPlayerSettings plugin, OrderManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    private void frame(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,45,46,47,48,50,51,52,53}) {
            if (i < inv.getSize()) inv.setItem(i, item(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + "☀"));
        }
    }

    private String nice(Material material) {
        String s = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String remainingTime(long expires) {
        long seconds = Math.max(0, expires - System.currentTimeMillis()) / 1000;
        long days = seconds / 86400;
        long hours = seconds % 86400 / 3600;
        long minutes = seconds % 3600 / 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    private long duration(String input) {
        try {
            String s = input.trim().toLowerCase(Locale.ROOT);
            long unit = s.endsWith("h") ? 3_600_000L : s.endsWith("d") ? 86_400_000L : 0;
            if (unit == 0) return -1;
            long number = Long.parseLong(s.substring(0, s.length() - 1));
            if (number <= 0) return -1;
            long max = plugin.getConfig().getLong("order.max-expiration-days", 7) * 86_400_000L;
            return Math.min(number * unit, max);
        } catch (Exception ignored) {
            return -1;
        }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            main(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length >= 4) {
                    createFromCommand(player, args);
                } else {
                    startCreate(player);
                }
            }
            case "search" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "☀ /order search <item> [highest|lowest|newest|reward]");
                } else {
                    String sort = args.length >= 3 ? args[2] : "newest";
                    browse(player, manager.search(args[1], sort));
                }
            }
            case "cancel" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "☀ /order cancel <id>");
                } else {
                    try {
                        boolean ok = manager.cancel(player, Long.parseLong(args[1]));
                        player.sendMessage(ok ? ChatColor.GREEN + "☀ Order cancelled and escrow refunded."
                                : ChatColor.RED + "☀ You can only cancel your own active order.");
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatColor.RED + "☀ Invalid order ID.");
                    }
                }
            }
            case "myorders", "my" -> myOrders(player);
            case "history" -> history(player);
            default -> main(player);
        }
        return true;
    }

    private void createFromCommand(Player player, String[] args) {
        Material material = Material.matchMaterial(args[1]);
        try {
            int amount = Integer.parseInt(args[2]);
            double total = Double.parseDouble(args[3]);
            long duration = duration(args.length >= 5 ? args[4] : "7d");
            if (material == null || material.isAir() || amount <= 0 || total <= 0 ||
                    !Double.isFinite(total) || duration <= 0) throw new IllegalArgumentException();
            Order order = manager.create(player, new ItemStack(material), amount, total,
                    System.currentTimeMillis() + duration);
            player.sendMessage(order == null ? ChatColor.RED + "☀ Order could not be created. Check your balance, limits, or item."
                    : ChatColor.GREEN + "☀ Order #" + order.id() + " created. $" + manager.money(total) + " is held in escrow.");
        } catch (Exception ex) {
            player.sendMessage(ChatColor.RED + "☀ Invalid order. Example: /order create diamond 128 2000 1d");
        }
    }

    private void startCreate(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "☀ Hold the item you want to order in your main hand.");
            return;
        }
        creating.put(player.getUniqueId(), new CreateSession(held.clone()));
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "☀ Enter the amount in chat:");
    }

    private void main(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54, MAIN);
        frame(inv);
        inv.setItem(20, item(Material.EMERALD, ChatColor.GREEN + "🛒 Create Order",
                "Choose an item from your main hand", "Then enter amount, price and expiration"));
        inv.setItem(22, item(Material.CHEST, ChatColor.YELLOW + "📦 My Orders",
                "View active, completed, expired and cancelled orders"));
        inv.setItem(24, item(Material.COMPASS, ChatColor.AQUA + "🔎 Browse Orders",
                "Find orders other players created"));
        inv.setItem(31, item(Material.WRITABLE_BOOK, ChatColor.GOLD + "📋 My Order History",
                "View your completed, expired and cancelled orders"));
        player.openInventory(inv);
    }

    private void browse(Player player, List<Order> orders) {
        Inventory inv = plugin.getServer().createInventory(null, 54, BROWSE);
        frame(inv);
        int slot = 10;
        for (Order order : orders) {
            if (slot >= 44) break;
            inv.setItem(slot, item(order.item().getType(),
                    ChatColor.AQUA + "#" + order.id() + " " + nice(order.item().getType()),
                    ChatColor.GRAY + "📦 " + order.remaining() + " needed",
                    ChatColor.GREEN + "💰 $" + manager.money(order.totalPrice()) + " total reward",
                    ChatColor.YELLOW + "💵 $" + manager.money(order.pricePerItem()) + " / item",
                    ChatColor.GRAY + "👤 Ordered by: " + order.buyerName(),
                    ChatColor.GRAY + "⏰ Expires in: " + remainingTime(order.expiresAt()),
                    ChatColor.DARK_GRAY + "Click for details"));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        if (orders.isEmpty()) inv.setItem(22, item(Material.BARRIER, ChatColor.RED + "No orders found"));
        inv.setItem(49, item(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    private void detail(Player player, Order order) {
        Inventory inv = plugin.getServer().createInventory(null, 27, ChatColor.AQUA + "☀ Order #" + order.id());
        frame(inv);
        inv.setItem(4, order.item().clone());
        inv.setItem(11, item(Material.PAPER, ChatColor.WHITE + "Order Details",
                "Item: " + nice(order.item().getType()),
                "Remaining: " + order.remaining(),
                "Reward: $" + manager.money(order.totalPrice()),
                "Price per item: $" + manager.money(order.pricePerItem()),
                "Ordered by: " + order.buyerName(),
                "Expires in: " + remainingTime(order.expiresAt())));
        if (!order.buyer().equals(player.getUniqueId())) {
            inv.setItem(15, item(Material.HOPPER, ChatColor.GREEN + "FULFILL ORDER",
                    "Supply all or part of this order",
                    "You will be paid automatically"));
        }
        inv.setItem(22, item(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    private void fulfillment(Player player, Order order) {
        if (order.buyer().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "☀ You cannot fulfill your own order.");
            return;
        }
        fulfilling.put(player.getUniqueId(), order.id());
        Inventory inv = plugin.getServer().createInventory(null, 27, ChatColor.GREEN + "☀ Fulfill #" + order.id());
        frame(inv);
        inv.setItem(4, order.item().clone());
        inv.setItem(13, item(order.item().getType(), ChatColor.WHITE + "PUT ITEMS HERE",
                "Place the exact requested item here",
                "Maximum: " + order.remaining()));
        inv.setItem(15, item(Material.LIME_WOOL, ChatColor.GREEN + "CONFIRM FULFILLMENT",
                "Items and payment are settled safely"));
        inv.setItem(22, item(Material.BARRIER, ChatColor.RED + "CANCEL",
                "Your items will be returned"));
        player.openInventory(inv);
    }

    private void myOrders(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54, MY);
        frame(inv);
        int slot = 10;
        for (Order order : manager.byBuyer(player.getUniqueId(), true)) {
            if (slot >= 44) break;
            String action = "ACTIVE".equals(order.status()) ? "Click to cancel" : "View status";
            inv.setItem(slot, item(order.item().getType(),
                    ChatColor.YELLOW + "#" + order.id() + " " + nice(order.item().getType()),
                    "Remaining: " + order.remaining(),
                    "Status: " + order.status(),
                    "Original reward: $" + manager.money(order.totalPrice()),
                    action));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        if (slot == 10) inv.setItem(22, item(Material.BARRIER, ChatColor.RED + "No orders yet"));
        inv.setItem(49, item(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    private void history(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54, HISTORY);
        frame(inv);
        int slot = 10;
        for (Order order : manager.byBuyer(player.getUniqueId(), true)) {
            if (slot >= 44) break;
            if ("ACTIVE".equals(order.status())) continue;
            inv.setItem(slot, item(order.item().getType(),
                    ChatColor.GOLD + "#" + order.id() + " " + nice(order.item().getType()),
                    "Status: " + order.status(),
                    "Original reward: $" + manager.money(order.totalPrice()),
                    "Remaining: " + order.remaining()));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        if (slot == 10) inv.setItem(22, item(Material.BARRIER, ChatColor.RED + "No history yet"));
        inv.setItem(49, item(Material.ARROW, ChatColor.YELLOW + "Back"));
        player.openInventory(inv);
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent event) {
        CreateSession session = creating.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage().trim();
        try {
            if (session.step == 1) {
                session.amount = Integer.parseInt(message);
                if (session.amount <= 0) throw new IllegalArgumentException();
                session.step = 2;
                player.sendMessage(ChatColor.YELLOW + "☀ Enter the total price:");
            } else if (session.step == 2) {
                session.total = Double.parseDouble(message);
                if (session.total <= 0 || !Double.isFinite(session.total)) throw new IllegalArgumentException();
                session.step = 3;
                player.sendMessage(ChatColor.YELLOW + "☀ Enter expiration: 1h, 6h, 1d, 3d or 7d:");
            } else {
                long duration = duration(message);
                if (duration <= 0) throw new IllegalArgumentException();
                creating.remove(player.getUniqueId());
                Order order = manager.create(player, session.item, session.amount, session.total,
                        System.currentTimeMillis() + duration);
                player.sendMessage(order == null ? ChatColor.RED + "☀ Order could not be created. Check your balance, limits, or item."
                        : ChatColor.GREEN + "☀ Order #" + order.id() + " created. $" + manager.money(session.total) + " is held in escrow.");
            }
        } catch (Exception ex) {
            player.sendMessage(ChatColor.RED + "☀ Invalid value. Please try again.");
        }
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.equals(MAIN)) {
            event.setCancelled(true);
            switch (event.getRawSlot()) {
                case 20 -> startCreate(player);
                case 22 -> myOrders(player);
                case 24 -> browse(player, manager.search("", "newest"));
                case 31 -> history(player);
                default -> {}
            }
            return;
        }

        if (title.equals(BROWSE)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49) { main(player); return; }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (name.startsWith("#")) {
                try {
                    long id = Long.parseLong(name.substring(1).split(" ")[0]);
                    Order order = manager.get(id);
                    if (order != null) detail(player, order);
                } catch (Exception ignored) {}
            }
            return;
        }

        if (title.startsWith(ChatColor.AQUA + "☀ Order #")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 15) {
                try {
                    long id = Long.parseLong(title.replaceAll("[^0-9]+", ""));
                    Order order = manager.get(id);
                    if (order != null) fulfillment(player, order);
                } catch (Exception ignored) {}
            } else if (event.getRawSlot() == 22) {
                browse(player, manager.active());
            }
            return;
        }

        if (title.startsWith(ChatColor.GREEN + "☀ Fulfill #")) {
            if (event.getRawSlot() != 13) event.setCancelled(true);

            if (event.getRawSlot() == 15) {
                event.setCancelled(true);
                Long id = fulfilling.get(player.getUniqueId());
                Order order = id == null ? null : manager.get(id);
                ItemStack supplied = event.getView().getTopInventory().getItem(13);

                if (order == null || supplied == null || supplied.getType().isAir() ||
                        !supplied.isSimilar(order.item())) {
                    player.sendMessage(ChatColor.RED + "☀ Put the exact requested item in the slot.");
                    return;
                }

                int amount = Math.min(supplied.getAmount(), order.remaining());
                ItemStack settlement = supplied.clone();
                settlement.setAmount(amount);

                // Remove the exact amount from the GUI before settlement.
                supplied.setAmount(supplied.getAmount() - amount);
                if (supplied.getAmount() <= 0) event.getView().getTopInventory().setItem(13, null);

                if (manager.fulfill(player, id, settlement)) {
                    fulfilling.remove(player.getUniqueId());
                    player.closeInventory();
                } else {
                    // Roll back the GUI removal on any failed settlement.
                    ItemStack rollback = event.getView().getTopInventory().getItem(13);
                    if (rollback == null || rollback.getType().isAir()) {
                        event.getView().getTopInventory().setItem(13, settlement);
                    } else {
                        rollback.setAmount(rollback.getAmount() + settlement.getAmount());
                    }
                    player.sendMessage(ChatColor.RED + "☀ Fulfillment failed safely. Nothing was lost.");
                }
            } else if (event.getRawSlot() == 22) {
                fulfilling.remove(player.getUniqueId());
                player.closeInventory();
            }
            return;
        }

        if (title.equals(MY)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49) { main(player); return; }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (!name.startsWith("#")) return;
            try {
                long id = Long.parseLong(name.substring(1).split(" ")[0]);
                Order order = manager.get(id);
                if (order != null && "ACTIVE".equals(order.status())) {
                    if (manager.cancel(player, id)) myOrders(player);
                }
            } catch (Exception ignored) {}
            return;
        }

        if (title.equals(HISTORY)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49) main(player);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(ChatColor.GREEN + "☀ Fulfill #")) return;

        Inventory top = event.getView().getTopInventory();
        ItemStack item = top.getItem(13);
        if (item != null && !item.getType().isAir()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            leftovers.values().forEach(x -> player.getWorld().dropItemNaturally(player.getLocation(), x));
            top.setItem(13, null);
        }
        fulfilling.remove(player.getUniqueId());
    }

    @EventHandler
    public void quit(org.bukkit.event.player.PlayerQuitEvent event) {
        creating.remove(event.getPlayer().getUniqueId());
        fulfilling.remove(event.getPlayer().getUniqueId());
    }

    private static final class CreateSession {
        final ItemStack item;
        int step = 1;
        int amount;
        double total;
        CreateSession(ItemStack item) { this.item = item; }
    }
}
