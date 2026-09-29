package net.sunlightsmp.auth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class StarterKitManager implements Listener, CommandExecutor, TabCompleter {
    private static final String TITLE = ChatColor.GOLD + "☀ SUNLIGHT SMP " + ChatColor.DARK_GRAY + "— " + ChatColor.YELLOW + "CHOOSE YOUR PATH";
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, String> selections = new HashMap<>();
    private final Set<UUID> selecting = new HashSet<>();
    private final NamespacedKey choiceKey;

    public StarterKitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "starter-kits.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.choiceKey = new NamespacedKey(plugin, "starter_kit_choice");
        load();
    }

    private void load() {
        ConfigurationSection s = data.getConfigurationSection("players");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            try {
                String kit = data.getString("players." + k);
                if (kit != null) selections.put(UUID.fromString(k), kit.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        for (Map.Entry<UUID, String> e : selections.entrySet()) data.set("players." + e.getKey(), e.getValue());
        try { data.save(file); } catch (IOException e) {
            plugin.getLogger().warning("Could not save starter-kits.yml: " + e.getMessage());
        }
    }

    public boolean hasKit(UUID id) { return selections.containsKey(id); }
    public String getKit(UUID id) { return selections.get(id); }

    public void openSelection(Player p) {
        if (hasKit(p.getUniqueId())) return;
        selecting.add(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE, " ");
        inv.setItem(4, item(Material.SUNFLOWER, ChatColor.GOLD + "☀ " + ChatColor.YELLOW + "WELCOME TO SUNLIGHT SMP",
                List.of(ChatColor.WHITE + "Choose your permanent starter path.", "", ChatColor.YELLOW + "Your choice cannot be changed by players.")));
        inv.setItem(20, kitIcon("CPVP"));
        inv.setItem(22, kitIcon("BUILDER"));
        inv.setItem(24, kitIcon("MINER"));
        inv.setItem(40, item(Material.BARRIER, ChatColor.RED + "✕ " + ChatColor.WHITE + "Permanent Choice",
                List.of(ChatColor.GRAY + "Choose carefully — normal players", ChatColor.GRAY + "cannot change their starter kit.")));
        p.openInventory(inv);
    }

    private ItemStack kitIcon(String kit) {
        Material mat = switch (kit) {
            case "CPVP" -> Material.END_CRYSTAL;
            case "BUILDER" -> Material.BRICKS;
            default -> Material.DIAMOND_PICKAXE;
        };
        String title = switch (kit) {
            case "CPVP" -> ChatColor.GOLD + "⚔ " + ChatColor.YELLOW + "CPVP KIT";
            case "BUILDER" -> ChatColor.GOLD + "▣ " + ChatColor.YELLOW + "BUILDER KIT";
            default -> ChatColor.GOLD + "⛏ " + ChatColor.YELLOW + "MINER KIT";
        };
        String desc = switch (kit) {
            case "CPVP" -> "Built for combat and Crystal PvP.";
            case "BUILDER" -> "Built for bases, structures and building.";
            default -> "Built for caves, mining and resources.";
        };
        return item(mat, title, List.of(ChatColor.WHITE + desc, "", ChatColor.GOLD + "[ " + ChatColor.YELLOW + "CHOOSE " + kit + ChatColor.GOLD + " ]",
                "", ChatColor.GRAY + "Click to permanently select this path."));
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        m.setLore(lore);
        i.setItemMeta(m);
        return i;
    }

    private void fill(Inventory inv, Material mat, String name) {
        ItemStack i = item(mat, name, Collections.emptyList());
        for (int x = 0; x < inv.getSize(); x++) inv.setItem(x, i);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!TITLE.equals(e.getView().getTitle())) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (!selecting.contains(p.getUniqueId()) || hasKit(p.getUniqueId())) {
            p.closeInventory();
            return;
        }
        String kit = switch (e.getRawSlot()) {
            case 20 -> "CPVP";
            case 22 -> "BUILDER";
            case 24 -> "MINER";
            default -> null;
        };
        if (kit == null) return;
        select(p, kit);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!TITLE.equals(e.getView().getTitle())) return;
        if (!hasKit(p.getUniqueId())) {
            selecting.remove(p.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && !hasKit(p.getUniqueId())) openSelection(p);
            }, 2L);
        }
    }

    private void select(Player p, String kit) {
        if (hasKit(p.getUniqueId())) return;
        selections.put(p.getUniqueId(), kit);
        save();
        selecting.remove(p.getUniqueId());
        p.closeInventory();
        giveKit(p, kit, true);
        p.sendMessage("");
        p.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        p.sendMessage(ChatColor.GREEN + "☀ KIT SELECTED");
        p.sendMessage(ChatColor.WHITE + "You chose the " + ChatColor.YELLOW + pretty(kit) + ChatColor.WHITE + ".");
        p.sendMessage(ChatColor.GRAY + "This choice is permanent for your account.");
        p.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.1f);
    }

    public void giveKit(Player p, String kit, boolean clearInventory) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("starter-kits." + kit.toLowerCase(Locale.ROOT) + ".items");
        if (section == null) return;
        if (clearInventory && plugin.getConfig().getBoolean("starter-kits.replace-inventory-on-respawn", true)) p.getInventory().clear();
        for (String key : section.getKeys(false)) {
            String materialName = section.getString(key + ".material", "STONE");
            int amount = Math.max(1, section.getInt(key + ".amount", 1));
            try {
                Material material = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                ItemStack stack = new ItemStack(material, amount);
                String name = section.getString(key + ".name");
                List<String> lore = section.getStringList(key + ".lore");
                if (name != null || !lore.isEmpty()) {
                    ItemMeta meta = stack.getItemMeta();
                    if (name != null) meta.setDisplayName(color(name));
                    if (!lore.isEmpty()) meta.setLore(lore.stream().map(this::color).toList());
                    meta.getPersistentDataContainer().set(choiceKey, PersistentDataType.STRING, kit);
                    stack.setItemMeta(meta);
                }
                Map<Integer, ItemStack> left = p.getInventory().addItem(stack);
                left.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid starter kit material: " + materialName);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!hasKit(e.getEntity().getUniqueId())) return;
        // Normal death handling is left intact; the selected loadout is restored on respawn.
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        String kit = getKit(e.getPlayer().getUniqueId());
        if (kit == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (e.getPlayer().isOnline()) giveKit(e.getPlayer(), kit, true);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!hasKit(p.getUniqueId())) Bukkit.getScheduler().runTaskLater(plugin, () -> openSelection(p), 10L);
    }

    public boolean reset(String name, CommandSender sender) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online for this command.");
            return true;
        }
        selections.remove(target.getUniqueId());
        save();
        selecting.remove(target.getUniqueId());
        target.closeInventory();
        sender.sendMessage(ChatColor.GREEN + "☀ Reset starter kit for " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
        Bukkit.getScheduler().runTaskLater(plugin, () -> openSelection(target), 2L);
        return true;
    }

    public boolean set(String name, String kit, CommandSender sender) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) { sender.sendMessage(ChatColor.RED + "Player must be online for this command."); return true; }
        kit = kit.toUpperCase(Locale.ROOT);
        if (!Set.of("CPVP", "BUILDER", "MINER").contains(kit)) {
            sender.sendMessage(ChatColor.RED + "Kit must be CPVP, BUILDER or MINER.");
            return true;
        }
        selections.put(target.getUniqueId(), kit);
        save();
        selecting.remove(target.getUniqueId());
        target.closeInventory();
        giveKit(target, kit, true);
        sender.sendMessage(ChatColor.GREEN + "☀ Assigned " + ChatColor.YELLOW + pretty(kit) + ChatColor.GREEN + " to " + target.getName() + ".");
        return true;
    }

    private String pretty(String kit) {
        return kit.substring(0, 1) + kit.substring(1).toLowerCase(Locale.ROOT) + " Kit";
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sunlightsmp.kitadmin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "☀ /kitadmin reset <player>");
            sender.sendMessage(ChatColor.YELLOW + "☀ /kitadmin set <player> <cpvp|builder|miner>");
            return true;
        }
        if (args[0].equalsIgnoreCase("reset")) return reset(args[1], sender);
        if (args[0].equalsIgnoreCase("set") && args.length >= 3) return set(args[1], args[2], sender);
        sender.sendMessage(ChatColor.RED + "Usage: /kitadmin reset <player> OR /kitadmin set <player> <kit>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reset", "set").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) return List.of("cpvp", "builder", "miner");
        return Collections.emptyList();
    }
}
