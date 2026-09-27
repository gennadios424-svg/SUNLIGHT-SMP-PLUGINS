package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatTagListener implements Listener {
    private static final int COMBAT_SECONDS = 20;
    private final SunlightPlayerSettings plugin;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();

    public CombatTagListener(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    Long until = combatUntil.get(p.getUniqueId());
                    if (until == null) continue;
                    long left = (until - now + 999) / 1000;
                    if (left <= 0) {
                        combatUntil.remove(p.getUniqueId());
                        p.sendActionBar(ChatColor.YELLOW + "COMBAT " + ChatColor.GRAY + "0 SECONDS");
                    } else {
                        p.sendActionBar(ChatColor.YELLOW + "COMBAT " + ChatColor.WHITE + left + " SECONDS");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player p) attacker = p;
        else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player p) attacker = p;
        if (attacker == null || attacker.equals(victim)) return;

        tag(attacker);
        tag(victim);
    }

    private void tag(Player p) {
        combatUntil.put(p.getUniqueId(), System.currentTimeMillis() + COMBAT_SECONDS * 1000L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatUntil.remove(event.getPlayer().getUniqueId());
    }
}
