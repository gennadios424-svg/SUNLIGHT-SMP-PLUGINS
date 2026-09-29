package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.inventory.*;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class OrderCommand implements CommandExecutor, Listener {
    private final SunlightPlayerSettings plugin;
    private final OrderManager manager;
    private final Map<UUID, CreateSession> creating = new HashMap<>();
    private final Map<UUID, Long> fulfilling = new HashMap<>();
    private final Map<UUID, SignSession> signs = new HashMap<>();

    private static final String MAIN = "☀ Player Orders";
    private static final String SEARCH = "☀ Search Orders";
    private static final String MY = "☀ My Orders";
    private static final String HISTORY = "☀ Order History";
    private static final String DETAIL_PREFIX = "☀ Order #";
    private static final String FULFILL_PREFIX = "☀ Fulfill #";

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

    /** Clean Sunlight frame: yellow top/bottom, orange sides, open center. */
    private void frame(Inventory inv) {
        ItemStack side = item(Material.ORANGE_STAINED_GLASS_PANE, " ");
        ItemStack accent = item(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + "☀");
        int rows = inv.getSize() / 9;
        for (int row = 0; row < rows; row++) {
            inv.setItem(row * 9, side);
            inv.setItem(row * 9 + 8, side);
        }
        for (int slot = 0; slot < 9; slot++) {
            inv.setItem(slot, accent);
            inv.setItem(inv.getSize() - 9 + slot, accent);
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
            main(player, "newest");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length >= 4) createFromCommand(player, args);
                else openCreate(player);
            }
            case "search" -> {
                if (args.length < 2) searchMenu(player);
                else {
                    String sort = args.length >= 3 ? args[2] : "newest";
                    browse(player, manager.search(args[1], sort), sort);
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
            default -> main(player, "newest");
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

    private void openCreate(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "☀ Hold the item you want to order in your main hand.");
            return;
        }
        creating.put(player.getUniqueId(), new CreateSession(held.clone()));
        player.closeInventory();
        openAmountSign(player);
    }

    private void main(Player player, String sort) {
        browse(player, manager.search("", sort), sort);
    }

    private void browse(Player player, List<Order> orders, String sort) {
        Inventory inv = plugin.getServer().createInventory(null, 54, MAIN);
        frame(inv);

        int slot = 10;
        for (Order order : orders) {
            if (slot >= 45) break;
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

        if (orders.isEmpty()) {
            inv.setItem(22, item(Material.BARRIER, ChatColor.RED + "No active orders",
                    "There are no orders matching this view."));
        }

        // Bottom controls: create is always bottom-left, search is always bottom-right.
        inv.setItem(45, item(Material.EMERALD, ChatColor.GREEN + "MAKE AN ORDER",
                "Create your own item request",
                "Choose the item, amount and price"));
        inv.setItem(47, item(Material.GOLD_INGOT, ChatColor.GOLD + "Highest Reward",
                "Sort active orders by total reward"));
        inv.setItem(48, item(Material.IRON_INGOT, ChatColor.WHITE + "Lowest Reward",
                "Sort active orders by lowest reward"));
        inv.setItem(49, item(Material.CLOCK, ChatColor.YELLOW + "Newest",
                "Show newest orders first"));
        inv.setItem(50, item(Material.EMERALD, ChatColor.GREEN + "Reward / Item",
                "Sort by price per item"));
        inv.setItem(51, item(Material.CHEST, ChatColor.YELLOW + "MY ORDERS",
                "View your active and finished orders"));
        inv.setItem(53, item(Material.BARREL, ChatColor.GOLD + "SEARCH ORDERS",
                "Choose an item to search for",
                "Like the /worth item selector"));

        player.openInventory(inv);
    }

    private void searchMenu(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54, SEARCH);
        frame(inv);

        // Show each currently requested material once, so the selector stays clean.
        Set<Material> materials = new LinkedHashSet<>();
        for (Order order : manager.active()) materials.add(order.item().getType());

        int slot = 10;
        for (Material material : materials) {
            if (slot >= 45) break;
            inv.setItem(slot, item(material, ChatColor.YELLOW + nice(material),
                    ChatColor.GRAY + "Click to see active orders",
                    ChatColor.DARK_GRAY + "Search this item"));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (materials.isEmpty()) {
            inv.setItem(22, item(Material.BARRIER, ChatColor.RED + "No items are currently ordered"));
        }

        inv.setItem(45, item(Material.ARROW, ChatColor.YELLOW + "BACK",
                "Return to active orders"));
        player.openInventory(inv);
    }

    private void detail(Player player, Order order) {
        Inventory inv = plugin.getServer().createInventory(null, 27, DETAIL_PREFIX + order.id());
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
        inv.setItem(22, item(Material.ARROW, ChatColor.YELLOW + "BACK"));
        player.openInventory(inv);
    }

    private void fulfillment(Player player, Order order) {
        if (order.buyer().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "☀ You cannot fulfill your own order.");
            return;
        }
        fulfilling.put(player.getUniqueId(), order.id());
        Inventory inv = plugin.getServer().createInventory(null, 27, ChatColor.GREEN + FULFILL_PREFIX + order.id());
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
            if (slot >= 45) break;
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
        inv.setItem(49, item(Material.ARROW, ChatColor.YELLOW + "BACK"));
        player.openInventory(inv);
    }

    private void history(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 54, HISTORY);
        frame(inv);
        int slot = 10;
        for (Order order : manager.byBuyer(player.getUniqueId(), true)) {
            if (slot >= 45) break;
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
        inv.setItem(49, item(Material.ARROW, ChatColor.YELLOW + "BACK"));
        player.openInventory(inv);
    }

    private void openAmountSign(Player player) {
        CreateSession session = creating.get(player.getUniqueId());
        if (session == null) return;
        openSign(player, 0);
    }

    private void openPriceSign(Player player) {
        openSign(player, 1);
    }

    private void openSign(Player player, int step) {
        Location loc = player.getLocation().getBlock().getRelative(BlockFace.UP).getLocation();
        Block block = loc.getBlock();
        if (!block.getType().isAir()) {
            loc = player.getLocation().getBlock().getRelative(BlockFace.UP, 2).getLocation();
            block = loc.getBlock();
        }
        SignSession old = signs.remove(player.getUniqueId());
        if (old != null) restoreSign(old);

        Material type = block.getType();
        BlockDataSnapshot snapshot = new BlockDataSnapshot(block);
        block.setType(Material.OAK_SIGN, false);
        Sign sign = (Sign) block.getState();
        sign.setLine(0, step == 0 ? "ENTER AMOUNT" : "PRICE PER ITEM");
        sign.setLine(1, step == 0 ? "Example: 128" : "Example: 25.50");
        sign.setLine(2, "Sunlight SMP");
        sign.update(true, false);
        signs.put(player.getUniqueId(), new SignSession(loc, snapshot, step));
        player.openSign(sign);
    }

    private void restoreSign(SignSession session) {
        Block block = session.location.getBlock();
        block.setBlockData(session.snapshot.data, false);
    }

    @EventHandler
    public void sign(SignChangeEvent event) {
        Player player = event.getPlayer();
        SignSession signSession = signs.remove(player.getUniqueId());
        CreateSession session = creating.get(player.getUniqueId());
        if (signSession == null || session == null) return;

        restoreSign(signSession);
        String value = event.getLine(0).trim();

        try {
            if (signSession.step == 0) {
                int amount = Integer.parseInt(value);
                if (amount <= 0) throw new IllegalArgumentException();
                session.amount = amount;
                session.step = 2;
                Bukkit.getScheduler().runTask(plugin, () -> openPriceSign(player));
            } else if (signSession.step == 1) {
                double price = Double.parseDouble(value);
                if (price <= 0 || !Double.isFinite(price)) throw new IllegalArgumentException();
                session.total = price;
                session.step = 3;
                Bukkit.getScheduler().runTask(plugin, () -> openDurationSign(player));
            } else {
                completeSignOrder(player, signSession, value);
            }
        } catch (Exception ex) {
            player.sendMessage(ChatColor.RED + "☀ Invalid value. Please try again or close the sign to cancel.");
        }
    }

    private void openDurationSign(Player player) {
        CreateSession session = creating.get(player.getUniqueId());
        if (session == null) return;
        Location loc = player.getLocation().getBlock().getRelative(BlockFace.UP).getLocation();
        Block block = loc.getBlock();
        if (!block.getType().isAir()) {
            loc = player.getLocation().getBlock().getRelative(BlockFace.UP, 2).getLocation();
            block = loc.getBlock();
        }
        BlockDataSnapshot snapshot = new BlockDataSnapshot(block);
        block.setType(Material.OAK_SIGN, false);
        Sign sign = (Sign) block.getState();
        sign.setLine(0, "EXPIRATION");
        sign.setLine(1, "1h / 1d / 7d");
        sign.setLine(2, "Sunlight SMP");
        sign.update(true, false);
        signs.put(player.getUniqueId(), new SignSession(loc, snapshot, 2));
        player.openSign(sign);
    }

    private void completeSignOrder(Player player, SignSession signSession, String value) {
        CreateSession session = creating.remove(player.getUniqueId());
        if (session == null) return;
        long duration = duration(value);
        if (duration <= 0) {
            player.sendMessage(ChatColor.RED + "☀ Invalid expiration. Use 1h, 1d or 7d. Order cancelled.");
            return;
        }
        Order order = manager.create(player, session.item, session.amount, session.total,
                System.currentTimeMillis() + duration);
        player.sendMessage(order == null ? ChatColor.RED + "☀ Order could not be created. Check your balance, limits, or item."
                : ChatColor.GREEN + "☀ Order #" + order.id() + " created. $" + manager.money(session.total) + " is held in escrow.");
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.equals(MAIN)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 45) { openCreate(player); return; }
            if (slot == 53) { searchMenu(player); return; }
            if (slot == 51) { myOrders(player); return; }
            if (slot == 47) { browse(player, manager.search("", "highest"), "highest"); return; }
            if (slot == 48) { browse(player, manager.search("", "lowest"), "lowest"); return; }
            if (slot == 49) { browse(player, manager.search("", "newest"), "newest"); return; }
            if (slot == 50) { browse(player, manager.search("", "reward"), "reward"); return; }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (name.startsWith("#")) {
                try {
                    long id = Long.parseLong(name.substring(1).split(" ")[0]);
                    Order order = manager.get(id);
                    if (order != null && "ACTIVE".equals(order.status())) detail(player, order);
                } catch (Exception ignored) {}
            }
            return;
        }

        if (title.equals(SEARCH)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 45) { main(player, "newest"); return; }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            Material material = clicked.getType();
            if (material.isAir() || material.name().contains("STAINED_GLASS") || material == Material.BARREL) return;
            browse(player, manager.search(material.name(), "newest"), "newest");
            return;
        }

        if (title.startsWith(DETAIL_PREFIX)) {
            event.setCancelled(true);
            if (event.getRawSlot() == 15) {
                try {
                    long id = Long.parseLong(title.substring(DETAIL_PREFIX.length()));
                    Order order = manager.get(id);
                    if (order != null && "ACTIVE".equals(order.status())) fulfillment(player, order);
                } catch (Exception ignored) {}
            } else if (event.getRawSlot() == 22) {
                main(player, "newest");
            }
            return;
        }

        if (title.startsWith(FULFILL_PREFIX)) {
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
                supplied.setAmount(supplied.getAmount() - amount);
                if (supplied.getAmount() <= 0) event.getView().getTopInventory().setItem(13, null);

                if (manager.fulfill(player, id, settlement)) {
                    fulfilling.remove(player.getUniqueId());
                    player.closeInventory();
                } else {
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
            if (event.getRawSlot() == 49) { main(player, "newest"); return; }
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
            if (event.getRawSlot() == 49) main(player, "newest");
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith(FULFILL_PREFIX)) return;

        Inventory top = event.getView().getTopInventory();
        ItemStack supplied = top.getItem(13);
        if (supplied != null && !supplied.getType().isAir()) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(supplied.clone());
            leftovers.values().forEach(x -> player.getWorld().dropItemNaturally(player.getLocation(), x));
            top.setItem(13, null);
        }
        fulfilling.remove(player.getUniqueId());
    }

    @EventHandler
    public void quit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        creating.remove(uuid);
        fulfilling.remove(uuid);
        SignSession sign = signs.remove(uuid);
        if (sign != null) restoreSign(sign);
    }

    private static final class CreateSession {
        final ItemStack item;
        int step = 1;
        int amount;
        double total;
        CreateSession(ItemStack item) { this.item = item; }
    }

    private static final class SignSession {
        final Location location;
        final BlockDataSnapshot snapshot;
        final int step;
        SignSession(Location location, BlockDataSnapshot snapshot, int step) {
            this.location = location; this.snapshot = snapshot; this.step = step;
        }
    }

    private static final class BlockDataSnapshot {
        final org.bukkit.block.data.BlockData data;
        BlockDataSnapshot(Block block) { this.data = block.getBlockData().clone(); }
    }
}
