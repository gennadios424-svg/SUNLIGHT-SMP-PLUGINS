package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public final class CrateMenu implements Listener {
    private final CrateManager manager;
    private static final String TITLE = ChatColor.DARK_AQUA + "✦ Sunlight Crates";
    public CrateMenu(CrateManager manager) { this.manager = manager; }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        CrateType[] types = CrateType.values();
        for (int i = 0; i < types.length; i++) {
            ItemStack item = new ItemStack(types[i].icon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "✦ " + types[i].displayName() + " Crate");
            meta.setLore(List.of(ChatColor.GRAY + "Click to preview rewards.", ChatColor.GRAY + "Sneak + right-click the physical crate to open."));
            item.setItemMeta(meta);
            inv.setItem(10 + i, item);
        }
        p.openInventory(inv);
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot >= 10 && slot <= 14) manager.preview(p, CrateType.values()[slot - 10]);
    }
}