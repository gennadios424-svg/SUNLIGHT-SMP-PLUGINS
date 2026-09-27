package net.sunlightsmp.playersettings;

import org.bukkit.event.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class SunflowerListener implements Listener {
    private final SunflowerManager manager;
    public SunflowerListener(SunflowerManager manager) { this.manager = manager; }

    @EventHandler public void join(PlayerJoinEvent e) { manager.join(e.getPlayer()); }
    @EventHandler public void move(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            manager.markActive(e.getPlayer());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent e) { manager.markActive(e.getPlayer()); }
    @EventHandler public void kill(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || killer.equals(e.getEntity())) return;
        long amount = hasBooster(killer) ? 4 : 1;
        manager.add(killer, amount);
        killer.sendMessage(org.bukkit.ChatColor.GOLD + "☀ Player Kill Sunflowers: " + org.bukkit.ChatColor.YELLOW + "+" + amount);
    }
    private boolean hasBooster(Player p) {
        for (org.bukkit.inventory.ItemStack i : p.getInventory().getContents()) {
            if (i == null || !i.hasItemMeta()) continue;
            var pc=i.getItemMeta().getPersistentDataContainer();
            Byte b=pc.get(new org.bukkit.NamespacedKey(managerPlugin(),"sunlight_shard_booster"),org.bukkit.persistence.PersistentDataType.BYTE);
            Long t=pc.get(new org.bukkit.NamespacedKey(managerPlugin(),"sunlight_shard_booster_expiry"),org.bukkit.persistence.PersistentDataType.LONG);
            if(b!=null&&b==1&&t!=null&&System.currentTimeMillis()<t)return true;
        }
        return false;
    }
    private SunlightPlayerSettings managerPlugin() {
        try { var f=SunflowerManager.class.getDeclaredField("plugin"); f.setAccessible(true); return (SunlightPlayerSettings)f.get(manager); }
        catch(Exception e){ throw new IllegalStateException(e); }
    }
}