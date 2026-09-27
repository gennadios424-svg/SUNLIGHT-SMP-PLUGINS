package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CrateListener implements Listener {
    private final CrateManager manager;
    public CrateListener(CrateManager manager) { this.manager = manager; }

    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        CrateType type = manager.getCrate(event.getClickedBlock().getLocation());
        if (type == null) return;
        event.setCancelled(true);
        Player p = event.getPlayer();
        if (!p.hasPermission("sunlightsmp.crates.use")) { p.sendMessage(ChatColor.RED + "You don't have permission."); return; }
        if (p.isSneaking()) manager.preview(p, type); else manager.openCrate(p, type);
    }

    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (manager.isOpening(p) || event.getView().getTitle().contains("Sunlight Crates") ||
            event.getView().getTitle().contains("Rewards") || event.getView().getTitle().contains(" Crate"))
            event.setCancelled(true);
    }
}