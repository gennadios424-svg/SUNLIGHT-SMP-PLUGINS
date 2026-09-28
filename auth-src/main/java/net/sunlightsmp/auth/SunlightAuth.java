package net.sunlightsmp.auth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class SunlightAuth extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final SecureRandom random = new SecureRandom();
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> timeouts = new ConcurrentHashMap<>();
    private File accountsFile;
    private org.bukkit.configuration.file.YamlConfiguration accounts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        accountsFile = new File(getDataFolder(), "accounts.yml");
        accounts = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(accountsFile);
        Bukkit.getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("register")).setExecutor(this);
        Objects.requireNonNull(getCommand("login")).setExecutor(this);
        Objects.requireNonNull(getCommand("changepassword")).setExecutor(this);
        Objects.requireNonNull(getCommand("logout")).setExecutor(this);
        Objects.requireNonNull(getCommand("sunauth")).setExecutor(this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            startAuth(player);
        }
        getLogger().info("SunlightAuth enabled. Cracked players use /register and /login; premium UUIDs can bypass auth when forwarded securely.");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : timeouts.values()) task.cancel();
        timeouts.clear();
        saveAccounts();
    }

    private String msg(String key) {
        String s = getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.prefix", "") + s);
    }

    private void send(Player p, String key) {
        p.sendMessage(msg(key));
    }

    private boolean isPremium(Player p) {
        if (!getConfig().getBoolean("premium.auto-detect-velocity-uuid", true)) return false;
        UUID id = p.getUniqueId();

        // Velocity modern forwarding supplies the official Mojang UUID.
        // Offline-mode UUIDs are name-based UUID v3.
        if (id.version() == 4) return true;

        List<String> trusted = getConfig().getStringList("premium.trusted-uuids");
        return trusted.stream().anyMatch(s -> s.equalsIgnoreCase(id.toString()));
    }

    private String key(Player p) {
        return "players." + p.getUniqueId();
    }

    private boolean registered(Player p) {
        return accounts.contains(key(p) + ".hash");
    }

    private boolean isAuthenticated(Player p) {
        return authenticated.contains(p.getUniqueId());
    }

    private void startAuth(Player p) {
        authenticated.remove(p.getUniqueId());

        if (isPremium(p)) {
            authenticated.add(p.getUniqueId());
            send(p, "premium");
            return;
        }

        long sessionSeconds = Math.max(0, getConfig().getLong("session-seconds", 43200));
        Long sessionUntil = sessions.get(p.getUniqueId());
        if (sessionUntil != null && sessionUntil > System.currentTimeMillis()) {
            authenticated.add(p.getUniqueId());
            return;
        }

        if (registered(p)) send(p, "login");
        else send(p, "register");

        p.setWalkSpeed(0f);
        p.setFlySpeed(0f);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline() && !isAuthenticated(p)) {
                send(p, "timeout");
                p.kickPlayer(ChatColor.RED + "Authentication timed out.");
            }
        }, Math.max(20L, getConfig().getLong("login-timeout-seconds", 60) * 20L));
        BukkitTask old = timeouts.put(p.getUniqueId(), task);
        if (old != null) old.cancel();
    }

    private void authenticate(Player p) {
        authenticated.add(p.getUniqueId());
        BukkitTask task = timeouts.remove(p.getUniqueId());
        if (task != null) task.cancel();
        p.setWalkSpeed(0.2f);
        p.setFlySpeed(0.1f);
        sessions.put(p.getUniqueId(), System.currentTimeMillis() + Math.max(0, getConfig().getLong("session-seconds", 43200)) * 1000L);
    }

    private void saveAccounts() {
        try {
            accounts.save(accountsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save accounts.yml: " + e.getMessage());
        }
    }

    private byte[] salt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private String hash(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 210_000, 256);
        byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }

    private boolean verify(String password, String saltText, String expected) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltText);
            byte[] actual = Base64.getDecoder().decode(hash(password, salt));
            byte[] wanted = Base64.getDecoder().decode(expected);
            return MessageDigest.isEqual(actual, wanted);
        } catch (Exception e) {
            return false;
        }
    }

    private void register(Player p, String password, String confirmation) {
        if (registered(p)) { send(p, "already-registered"); return; }
        int min = Math.max(1, getConfig().getInt("min-password-length", 6));
        int max = Math.max(min, getConfig().getInt("max-password-length", 64));
        if (password.length() < min) { p.sendMessage(msg("password-short").replace("%min%", String.valueOf(min))); return; }
        if (password.length() > max) { send(p, "password-long"); return; }
        if (!password.equals(confirmation)) { send(p, "passwords-differ"); return; }

        byte[] salt = salt();
        try {
            accounts.set(key(p) + ".name", p.getName());
            accounts.set(key(p) + ".salt", Base64.getEncoder().encodeToString(salt));
            accounts.set(key(p) + ".hash", hash(password, salt));
            accounts.set(key(p) + ".created", System.currentTimeMillis());
            saveAccounts();
            authenticate(p);
            send(p, "registered");
        } catch (Exception e) {
            getLogger().warning("Registration failed for " + p.getName() + ": " + e.getMessage());
            p.sendMessage(ChatColor.RED + "Registration failed. Try again.");
        }
    }

    private void login(Player p, String password) {
        if (isAuthenticated(p)) { send(p, "already-authenticated"); return; }
        if (!registered(p)) { send(p, "not-registered"); return; }
        String salt = accounts.getString(key(p) + ".salt");
        String expected = accounts.getString(key(p) + ".hash");
        if (salt == null || expected == null || !verify(password, salt, expected)) {
            send(p, "wrong-password");
            return;
        }
        authenticate(p);
        send(p, "logged-in");
    }

    private void changePassword(Player p, String oldPassword, String newPassword) {
        if (!isAuthenticated(p)) { send(p, "only-auth"); return; }
        if (!registered(p)) { send(p, "not-registered"); return; }
        String salt = accounts.getString(key(p) + ".salt");
        String expected = accounts.getString(key(p) + ".hash");
        if (salt == null || expected == null || !verify(oldPassword, salt, expected)) {
            send(p, "old-wrong");
            return;
        }
        int min = Math.max(1, getConfig().getInt("min-password-length", 6));
        if (newPassword.length() < min) { p.sendMessage(msg("password-short").replace("%min%", String.valueOf(min))); return; }
        if (newPassword.length() > getConfig().getInt("max-password-length", 64)) { send(p, "password-long"); return; }
        byte[] newSalt = salt();
        try {
            accounts.set(key(p) + ".salt", Base64.getEncoder().encodeToString(newSalt));
            accounts.set(key(p) + ".hash", hash(newPassword, newSalt));
            saveAccounts();
            send(p, "changed");
        } catch (Exception e) {
            p.sendMessage(ChatColor.RED + "Could not change your password.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask(this, () -> startAuth(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        authenticated.remove(id);
        BukkitTask task = timeouts.remove(id);
        if (task != null) task.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) {
        if (isAuthenticated(e.getPlayer())) return;
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX() ||
            e.getFrom().getBlockY() != e.getTo().getBlockY() ||
            e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (isAuthenticated(e.getPlayer())) return;
        String command = e.getMessage().trim().toLowerCase(Locale.ROOT);
        if (command.startsWith("/login ") || command.equals("/login") ||
            command.startsWith("/l ") || command.equals("/l") ||
            command.startsWith("/register ") || command.equals("/register") ||
            command.startsWith("/reg ") || command.equals("/reg")) return;
        e.setCancelled(true);
        send(e.getPlayer(), "only-auth");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!isAuthenticated(e.getPlayer())) {
            e.setCancelled(true);
            send(e.getPlayer(), "only-auth");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && !isAuthenticated(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && !isAuthenticated(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p && !isAuthenticated(p)) e.setCancelled(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            if (command.getName().equalsIgnoreCase("sunauth")) {
                sender.sendMessage("SunlightAuth admin commands are player-focused.");
                return true;
            }
            sender.sendMessage("This command is player-only.");
            return true;
        }

        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "register" -> {
                if (args.length != 2) { send(p, "usage-register"); return true; }
                register(p, args[0], args[1]);
            }
            case "login" -> {
                if (args.length != 1) { send(p, "usage-login"); return true; }
                login(p, args[0]);
            }
            case "changepassword" -> {
                if (args.length != 2) { send(p, "usage-change"); return true; }
                changePassword(p, args[0], args[1]);
            }
            case "logout" -> {
                if (isPremium(p)) {
                    p.sendMessage(ChatColor.RED + "Premium players cannot use /logout.");
                    return true;
                }
                authenticated.remove(p.getUniqueId());
                sessions.remove(p.getUniqueId());
                send(p, "logout");
                send(p, "login");
            }
            case "sunauth" -> {
                if (!p.hasPermission("sunlightsmp.auth.admin")) return true;
                if (args.length == 0 || args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    p.sendMessage(ChatColor.GREEN + "SunlightAuth config reloaded.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("status")) {
                    p.sendMessage(ChatColor.YELLOW + "Authenticated: " + authenticated.size());
                    p.sendMessage(ChatColor.YELLOW + "Registered accounts: " + countRegistered());
                    return true;
                }
            }
        }
        return true;
    }

    private int countRegistered() {
        if (!accounts.isConfigurationSection("players")) return 0;
        return accounts.getConfigurationSection("players").getKeys(false).size();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
