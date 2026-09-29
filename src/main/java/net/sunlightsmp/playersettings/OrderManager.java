package net.sunlightsmp.playersettings;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;

public final class OrderManager {
    private final SunlightPlayerSettings plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<Long, Order> active = new LinkedHashMap<>();
    private final Map<Long, Order> history = new LinkedHashMap<>();
    private long nextId = 1;

    public OrderManager(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "orders.yml");
        load();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::expire, 20L, 100L);
    }

    private Economy economy() {
        var registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }

    public synchronized void load() {
        try {
            plugin.getDataFolder().mkdirs();
            if (!file.exists()) file.createNewFile();
        } catch (IOException ignored) {}
        data = YamlConfiguration.loadConfiguration(file);
        nextId = Math.max(1, data.getLong("next-id", 1));
        active.clear();
        history.clear();
        readOrders("active", active);
        readOrders("history", history);
        expire();
    }

    private void readOrders(String section, Map<Long, Order> target) {
        var root = data.getConfigurationSection(section);
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                var c = data.getConfigurationSection(section + "." + key);
                if (c == null) continue;
                ItemStack item = c.getItemStack("item");
                if (item == null || item.getType().isAir()) continue;
                long id = Long.parseLong(key);
                target.put(id, new Order(id, UUID.fromString(c.getString("buyer")),
                        c.getString("buyer-name", "Unknown"), item,
                        c.getInt("remaining"), c.getDouble("total"), c.getDouble("per"),
                        c.getLong("expires"), c.getString("status", "UNKNOWN")));
            } catch (Exception ignored) {}
        }
    }

    public synchronized void save() {
        YamlConfiguration out = new YamlConfiguration();
        out.set("next-id", nextId);
        writeOrders(out, "active", active);
        writeOrders(out, "history", history);
        try {
            out.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("orders.yml: " + e.getMessage());
        }
        data = out;
    }

    private void writeOrders(YamlConfiguration out, String section, Map<Long, Order> orders) {
        for (Order o : orders.values()) {
            String path = section + "." + o.id();
            out.set(path + ".buyer", o.buyer().toString());
            out.set(path + ".buyer-name", o.buyerName());
            out.set(path + ".item", o.item());
            out.set(path + ".remaining", o.remaining());
            out.set(path + ".total", o.totalPrice());
            out.set(path + ".per", o.pricePerItem());
            out.set(path + ".expires", o.expiresAt());
            out.set(path + ".status", o.status());
        }
    }

    private boolean blocked(ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        return plugin.getConfig().getStringList("order.blocked-materials").stream()
                .anyMatch(x -> x.equalsIgnoreCase(item.getType().name()));
    }

    public synchronized Order create(Player buyer, ItemStack sample, int amount, double total, long expiresAt) {
        Economy eco = economy();
        if (eco == null || blocked(sample) || amount <= 0 || total <= 0 ||
                !Double.isFinite(total) || expiresAt <= System.currentTimeMillis()) return null;

        int maxAmount = plugin.getConfig().getInt("order.max-amount", 2304);
        double maxPrice = plugin.getConfig().getDouble("order.max-price", 1_000_000_000D);
        if (amount > maxAmount || total > maxPrice || !eco.has(buyer, total)) return null;
        int maxOrders = plugin.getConfig().getInt("order.max-orders-per-player", 20);
        long ownActive = active.values().stream().filter(o -> o.buyer().equals(buyer.getUniqueId())).count();
        if (ownActive >= maxOrders) return null;

        if (!eco.withdrawPlayer(buyer, total).transactionSuccess()) return null;

        Order order = new Order(nextId++, buyer.getUniqueId(), buyer.getName(),
                sample.clone(), amount, total, total / amount, expiresAt, "ACTIVE");
        active.put(order.id(), order);
        save();
        return order;
    }

    public synchronized Order get(long id) {
        expire();
        return active.get(id);
    }

    public synchronized List<Order> active() {
        expire();
        return new ArrayList<>(active.values());
    }

    public synchronized List<Order> byBuyer(UUID uuid, boolean includeHistory) {
        expire();
        List<Order> result = new ArrayList<>();
        for (Order o : active.values()) if (o.buyer().equals(uuid)) result.add(o);
        if (includeHistory) for (Order o : history.values()) if (o.buyer().equals(uuid)) result.add(o);
        result.sort(Comparator.comparingLong(Order::id).reversed());
        return result;
    }

    public synchronized List<Order> search(String materialName, String sort) {
        expire();
        List<Order> result = new ArrayList<>();
        for (Order o : active.values()) {
            if (o.item().getType().name().equalsIgnoreCase(materialName)) result.add(o);
        }
        switch (sort.toLowerCase(Locale.ROOT)) {
            case "highest" -> result.sort(Comparator.comparingDouble(Order::pricePerItem).reversed());
            case "lowest" -> result.sort(Comparator.comparingDouble(Order::pricePerItem));
            case "newest" -> result.sort(Comparator.comparingLong(Order::id).reversed());
            case "reward" -> result.sort(Comparator.comparingDouble(Order::totalPrice).reversed());
            default -> {}
        }
        return result;
    }

    public synchronized boolean cancel(Player buyer, long id) {
        Order order = active.get(id);
        if (order == null || !order.buyer().equals(buyer.getUniqueId())) return false;

        active.remove(id);
        refund(order.buyer(), order.paymentFor(order.remaining()));
        history.put(id, new Order(order.id(), order.buyer(), order.buyerName(), order.item(),
                order.remaining(), order.totalPrice(), order.pricePerItem(), order.expiresAt(), "CANCELLED"));
        save();
        return true;
    }

    /*
     * The supplied stack is already inside the fulfillment GUI, meaning it has
     * been removed from the seller's normal inventory by Bukkit's inventory UI.
     * This method therefore performs the protected money/item settlement and
     * restores the seller's items if any step fails.
     */
    public synchronized boolean fulfill(Player seller, long id, ItemStack supplied) {
        Order order = active.get(id);
        Economy eco = economy();
        Player buyer = order == null ? null : Bukkit.getPlayer(order.buyer());

        if (eco == null || order == null || order.expired() ||
                order.buyer().equals(seller.getUniqueId()) ||
                buyer == null || !buyer.isOnline() ||
                supplied == null || supplied.getType().isAir() ||
                !supplied.isSimilar(order.item())) return false;

        int amount = Math.min(supplied.getAmount(), order.remaining());
        if (amount <= 0) return false;

        ItemStack delivered = supplied.clone();
        delivered.setAmount(amount);
        double payment = order.paymentFor(amount);

        // First reserve the exact delivered stack by placing it in the buyer inventory.
        Map<Integer, ItemStack> leftovers = buyer.getInventory().addItem(delivered);
        if (!leftovers.isEmpty()) {
            restoreSeller(seller, delivered);
            return false;
        }

        // Then settle payment. If the economy transaction fails, remove exactly what
        // was delivered and restore it to the seller, so neither side loses items.
        if (!eco.depositPlayer(seller, payment).transactionSuccess()) {
            removeExact(buyer, delivered);
            restoreSeller(seller, delivered);
            return false;
        }

        int remaining = order.remaining() - amount;
        active.remove(id);
        if (remaining > 0) {
            active.put(id, new Order(order.id(), order.buyer(), order.buyerName(), order.item(),
                    remaining, order.totalPrice(), order.pricePerItem(), order.expiresAt(), "ACTIVE"));
        } else {
            history.put(id, new Order(order.id(), order.buyer(), order.buyerName(), order.item(),
                    0, order.totalPrice(), order.pricePerItem(), order.expiresAt(), "COMPLETED"));
        }
        save();

        String state = remaining > 0 ? "partially " : "";
        buyer.sendMessage(ChatColor.GREEN + "☀ Your order #" + id + " was " + state +
                "fulfilled! " + amount + " supplied. Remaining: " + remaining + ".");
        seller.sendMessage(ChatColor.GREEN + "☀ You received $" + money(payment) +
                " for fulfilling order #" + id + ".");
        return true;
    }

    private void removeExact(Player player, ItemStack wanted) {
        int left = wanted.getAmount();
        for (int slot = 0; slot < player.getInventory().getSize() && left > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType().isAir() || !stack.isSimilar(wanted)) continue;
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) player.getInventory().setItem(slot, null);
            left -= take;
        }
    }

    private void restoreSeller(Player seller, ItemStack stack) {
        Map<Integer, ItemStack> leftovers = seller.getInventory().addItem(stack.clone());
        leftovers.values().forEach(item -> seller.getWorld().dropItemNaturally(seller.getLocation(), item));
    }

    private void refund(UUID uuid, double amount) {
        Economy eco = economy();
        if (amount > 0 && eco != null) eco.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount);
    }

    private void expire() {
        boolean changed = false;
        Iterator<Order> iterator = active.values().iterator();
        while (iterator.hasNext()) {
            Order order = iterator.next();
            if (!order.expired()) continue;
            iterator.remove();
            double refund = order.paymentFor(order.remaining());
            refund(order.buyer(), refund);
            history.put(order.id(), new Order(order.id(), order.buyer(), order.buyerName(), order.item(),
                    order.remaining(), order.totalPrice(), order.pricePerItem(), order.expiresAt(), "EXPIRED"));
            Player buyer = Bukkit.getPlayer(order.buyer());
            if (buyer != null) {
                buyer.sendMessage(ChatColor.RED + "☀ Order #" + order.id() +
                        " expired. $" + money(refund) + " was refunded.");
            }
            changed = true;
        }
        if (changed) save();
    }

    public String money(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
