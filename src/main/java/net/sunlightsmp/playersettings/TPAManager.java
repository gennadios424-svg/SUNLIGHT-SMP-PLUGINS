package net.sunlightsmp.playersettings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TPAManager implements Listener {
    private final SunlightPlayerSettings plugin;
    private final Map<UUID, TPARequest> incoming = new ConcurrentHashMap<>();
    private final Map<UUID, TPARequest> outgoing = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> teleports = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> countdowns = new ConcurrentHashMap<>();

    public TPAManager(SunlightPlayerSettings plugin) { this.plugin = plugin; }

    public void sendRequest(Player requester, Player target, boolean tpHere) {
        if (requester.equals(target)) {
            requester.sendMessage(Component.text("You cannot send a TPA request to yourself.", NamedTextColor.RED)); return;
        }

        if (tpHere) {
            if (!plugin.getSettings().get(target, Setting.TPA_HERE_NOTIFICATIONS)) {
                requester.sendMessage(Component.text("That player has TPAHere notifications disabled.", NamedTextColor.RED));
                return;
            }
        } else if (!plugin.getSettings().get(target, Setting.TP_REQUESTS)) {
            requester.sendMessage(Component.text("That player has TPA requests disabled.", NamedTextColor.RED));
            return;
        }

        TPARequest old = outgoing.remove(requester.getUniqueId());
        if (old != null) incoming.remove(old.target().getUniqueId());

        int timeout = plugin.getConfig().getInt("tpa.request-timeout-seconds", 30);
        long now = System.currentTimeMillis();
        TPARequest request = new TPARequest(requester, target, tpHere, now + timeout * 1000L);
        outgoing.put(requester.getUniqueId(), request);
        incoming.put(target.getUniqueId(), request);

        requester.sendMessage(Component.text("TPA request sent to ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.YELLOW)).append(Component.text(".", NamedTextColor.GRAY)));
        target.sendMessage(Component.text(requester.getName(), NamedTextColor.YELLOW)
                .append(Component.text(tpHere ? " wants you to teleport to them. " : " wants to teleport to you. ", NamedTextColor.GRAY))
                .append(button("ACCEPT", NamedTextColor.GREEN, "/tpaccept"))
                .append(Component.text(" ")).append(button("DENY", NamedTextColor.RED, "/tpdeny")));
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (outgoing.get(requester.getUniqueId()) == request) {
                outgoing.remove(requester.getUniqueId());
                incoming.remove(target.getUniqueId());
                requester.sendMessage(Component.text("Your TPA request to " + target.getName() + " expired.", NamedTextColor.RED));
                target.sendMessage(Component.text("The TPA request from " + requester.getName() + " expired.", NamedTextColor.GRAY));
            }
        }, timeout * 20L);
    }

    private Component button(String text, NamedTextColor color, String command) {
        return Component.text("[" + text + "]", color)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text("Click to " + text.toLowerCase() + " this request.", color)));
    }

    public void acceptLatest(Player target) {
        TPARequest request = incoming.get(target.getUniqueId());
        if (request == null || request.expired()) {
            if (request != null) remove(request);
            target.sendMessage(Component.text("You have no pending TPA requests.", NamedTextColor.RED)); return;
        }
        remove(request);
        Player requester = request.requester();
        Player teleporter = request.tpHere() ? target : requester;
        Player destination = request.tpHere() ? requester : target;
        startTeleport(teleporter, destination);
    }

    public void denyLatest(Player target) {
        TPARequest request = incoming.remove(target.getUniqueId());
        if (request == null || request.expired()) {
            if (request != null) outgoing.remove(request.requester().getUniqueId());
            target.sendMessage(Component.text("You have no pending TPA requests.", NamedTextColor.RED)); return;
        }
        outgoing.remove(request.requester().getUniqueId());
        target.sendMessage(Component.text("TPA request denied.", NamedTextColor.GRAY));
        request.requester().sendMessage(Component.text(target.getName() + " denied your TPA request.", NamedTextColor.RED));
    }

    public void cancelOutgoing(Player requester) {
        TPARequest request = outgoing.remove(requester.getUniqueId());
        if (request == null) {
            requester.sendMessage(Component.text("You have no outgoing TPA request.", NamedTextColor.RED)); return;
        }
        incoming.remove(request.target().getUniqueId());
        requester.sendMessage(Component.text("TPA request cancelled.", NamedTextColor.GRAY));
        request.target().sendMessage(Component.text(requester.getName() + " cancelled their TPA request.", NamedTextColor.GRAY));
    }

    private void remove(TPARequest request) {
        outgoing.remove(request.requester().getUniqueId(), request);
        incoming.remove(request.target().getUniqueId(), request);
    }

    private void startTeleport(Player teleporter, Player destination) {
        cancelTeleport(teleporter);
        int delay = plugin.getConfig().getInt("tpa.teleport-delay-seconds", 5);
        Location start = teleporter.getLocation().clone();
        teleporter.sendMessage(Component.text("✦ Teleport initiated — stay still!", NamedTextColor.YELLOW));
        destination.sendMessage(Component.text("✦ " + teleporter.getName() + " is teleporting to you!", NamedTextColor.YELLOW));
        teleporter.sendTitle(ChatColor.YELLOW + "TELEPORTING", ChatColor.GRAY + "Stay still!", 0, 25, 5);
        destination.sendTitle(ChatColor.YELLOW + "INCOMING TELEPORT", ChatColor.GRAY + teleporter.getName() + " is teleporting to you", 0, 25, 5);
        teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
        destination.playSound(destination.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);

        UUID id = teleporter.getUniqueId();
        BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override public void run() {
                if (!teleporter.isOnline() || !teleports.containsKey(id)) {
                    BukkitTask self = countdowns.remove(id); if (self != null) self.cancel(); return;
                }
                teleporter.sendActionBar(Component.text("✦ TELEPORTING — STAY STILL ✦", NamedTextColor.YELLOW));
                destination.sendActionBar(Component.text("✦ " + teleporter.getName() + " IS TELEPORTING TO YOU ✦", NamedTextColor.YELLOW));
                teleporter.playSound(teleporter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, ticks >= Math.max(0, delay - 1) ? 1.8f : 1.4f);
                destination.playSound(destination.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, ticks >= Math.max(0, delay - 1) ? 1.8f : 1.4f);
                ticks++;
            }
        }, 0L, 20L);
        countdowns.put(id, countdownTask);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask countdown = countdowns.remove(id); if (countdown != null) countdown.cancel();
            teleports.remove(id);
            plugin.getTpaStartLocations().remove(id);
            if (!teleporter.isOnline() || !destination.isOnline()) return;
            Location safe = findSafeOverworldSurface(destination.getLocation());
            if (safe == null) { teleporter.sendMessage(Component.text("✦ Could not find a safe surface location near " + destination.getName() + ".", NamedTextColor.RED)); return; }
            teleporter.teleport(safe);
            teleporter.sendActionBar(Component.text("✦ TELEPORTED SUCCESSFULLY! ✦", NamedTextColor.YELLOW));
            destination.sendActionBar(Component.text("✦ " + teleporter.getName() + " HAS ARRIVED! ✦", NamedTextColor.GREEN));
            teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            destination.playSound(destination.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.1f);
        }, delay * 20L);
        teleports.put(id, task);
        plugin.getTpaStartLocations().put(id, start);
    }

    private void cancelTeleport(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = teleports.remove(id), countdown = countdowns.remove(id);
        if (task != null) task.cancel();
        if (countdown != null) countdown.cancel();
        if (task != null || countdown != null) {
            player.sendActionBar(Component.empty());
            plugin.getTpaStartLocations().remove(id);
        }
    }

    @EventHandler public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!teleports.containsKey(p.getUniqueId()) || event.getTo() == null) return;
        Location from = event.getFrom(), to = event.getTo();
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancelTeleport(p);
            p.sendActionBar(Component.text("✦ Teleport cancelled — you moved.", NamedTextColor.RED));
            p.sendMessage(Component.text("Teleport cancelled because you moved.", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && teleports.containsKey(p.getUniqueId())) {
            cancelTeleport(p);
            p.sendActionBar(Component.text("✦ Teleport cancelled — you took damage.", NamedTextColor.RED));
            p.sendMessage(Component.text("Teleport cancelled because you took damage.", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    public void cleanup(Player p) {
        cancelTeleport(p);
        TPARequest out = outgoing.remove(p.getUniqueId());
        if (out != null) incoming.remove(out.target().getUniqueId());
        TPARequest in = incoming.remove(p.getUniqueId());
        if (in != null) outgoing.remove(in.requester().getUniqueId());
    }
}
