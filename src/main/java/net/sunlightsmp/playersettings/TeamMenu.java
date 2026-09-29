package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class TeamMenu implements Listener {
    private final SunlightPlayerSettings plugin;
    private final TeamManager manager;
    private final Set<UUID> creating = new HashSet<>();
    public TeamMenu(SunlightPlayerSettings plugin, TeamManager manager){this.plugin=plugin;this.manager=manager;}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);meta.setLore(Arrays.asList(lore));i.setItemMeta(meta);return i;}
    private void fill(Inventory inv){
        ItemStack side=item(Material.BLACK_STAINED_GLASS_PANE," ");
        ItemStack accent=item(Material.YELLOW_STAINED_GLASS_PANE,ChatColor.GOLD+"☀");
        int size=inv.getSize(), rows=size/9;
        for(int row=0;row<rows;row++){int left=row*9,right=left+8;inv.setItem(left,side);inv.setItem(right,side);}
        if(size>=27)for(int s=0;s<9;s++){inv.setItem(s,accent);inv.setItem(size-9+s,accent);}
    }
    public void openMain(Player p){
        var t=manager.getTeam(p.getUniqueId()); Inventory inv=Bukkit.createInventory(null,27,t==null?"☀ Team":"☀ "+t.name);
        if(t==null){inv.setItem(11,item(Material.SUNFLOWER,"§e➕ Create Team","§7Use §f/team create <name>"));inv.setItem(13,item(Material.COMPASS,"§b🔎 Find / Join Team","§7Use §f/team list §7then §f/team join <name>"));inv.setItem(15,item(Material.PAPER,"§d📩 Team Invitations","§7Use §f/team accept §7or §f/team deny"));} 
        else {inv.setItem(10,item(Material.PLAYER_HEAD,"§e👥 Team Members","§7"+t.members.size()+"/"+TeamManager.MAX_MEMBERS+" members"));inv.setItem(12,item(Material.PAPER,"§d✉ Invitations","§7Invite with §f/team invite <player>"));inv.setItem(14,item(Material.COMPARATOR,"§e⚙ Team Settings","§7Owner/officer controls"));inv.setItem(16,item(Material.NAME_TAG,"§b🏷 Team Name","§7"+t.name));inv.setItem(18,item(Material.LEATHER_CHESTPLATE,"§6🎨 Team Color","§7Current: "+t.color+"■"));inv.setItem(20,item(Material.BARRIER,"§c🚪 Leave Team","§7Owner must transfer/disband first"));inv.setItem(22,item(Material.BOOK,"§aℹ Team Info","§7View team details"));inv.setItem(24,item(Material.PAPER,"§b💬 Team Chat","§7Use §f/tc")); }
        fill(inv);p.openInventory(inv);
    }
    public void openMembers(Player p){
        var t=manager.getTeam(p.getUniqueId());if(t==null){openMain(p);return;}Inventory inv=Bukkit.createInventory(null,54,"☀ "+t.name+" • Members");int slot=0;
        for(var e:t.members.entrySet()){var member=Bukkit.getPlayer(e.getKey());ItemStack i=item(Material.PLAYER_HEAD,(e.getValue()==TeamManager.Role.OWNER?"§6👑 ":e.getValue()==TeamManager.Role.OFFICER?"§e⭐ ":"§f👤 ")+(member==null?e.getKey().toString().substring(0,8):member.getName()),"§7Role: §f"+e.getValue().name(),"§8Click for management options");inv.setItem(slot++,i);if(slot>=45)break;}
        inv.setItem(49,item(Material.ARROW,"§cBack"));fill(inv);p.openInventory(inv);
    }
    public void openSettings(Player p){
        var t=manager.getTeam(p.getUniqueId());if(t==null){openMain(p);return;}Inventory inv=Bukkit.createInventory(null,27,"⚙ "+t.name+" • Settings");
        inv.setItem(10,item(Material.NAME_TAG,"§eRename Team","§7/team rename <name>"));
        inv.setItem(12,item(Material.LIGHT,"§bSet Team Tag","§7/team tag <tag>","§7Max 6 chars"));
        inv.setItem(14,item(Material.LEATHER_CHESTPLATE,"§6Change Color","§7/team color <color>"));
        if(manager.isOwner(t,p.getUniqueId()))inv.setItem(16,item(Material.PLAYER_HEAD,"§dTransfer Ownership","§7Use the member menu"));
        inv.setItem(22,item(Material.ARROW,"§cBack"));fill(inv);p.openInventory(inv);
    }
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String title=e.getView().getTitle();\n        // Only handle TeamMenu inventories. Do not intercept Order, Shop, AH, or other ☀ menus.\n        if(!title.equals("☀ Team") && !title.contains(" • Members") && !title.contains(" • Settings"))return;\n        e.setCancelled(true);
        if(title.equals("☀ Team")){if(e.getSlot()==11){creating.add(p.getUniqueId());p.closeInventory();p.sendMessage("§e☀ Create Team §7» §fType the team name in chat §7(2-16 characters). §cType cancel to stop.");}else if(e.getSlot()==13)p.sendMessage("§7Use §f/team list §7to find a team, then §f/team join <name>§7.");else if(e.getSlot()==15){p.closeInventory();p.sendMessage("§7Use §f/team accept §7or §f/team deny§7 for your pending invitation.");}return;}
        if(title.contains("• Members")){if(e.getSlot()==49){openMain(p);return;}return;}
        if(title.contains("• Settings")){if(e.getSlot()==22)openMain(p);return;}
        var t=manager.getTeam(p.getUniqueId());if(t==null){openMain(p);return;}
        switch(e.getSlot()){case 10->openMembers(p);case 12->p.sendMessage("§7Invitations: §f/team invite <player>");case 14->openSettings(p);case 16->p.sendMessage("§7Team name: §f"+t.name);case 18->p.sendMessage("§7Color: "+t.color+"■ §7Use §f/team color <color>");case 20->p.sendMessage(manager.isOwner(t,p.getUniqueId())?"§cYou are the owner. Transfer ownership or disband the team first.":"§eUse /team leave to leave.");case 22->{p.closeInventory();p.performCommand("team info");}case 24->{p.closeInventory();plugin.getTeamChat().toggle(p);}}
    }
}

    @EventHandler public void chat(AsyncPlayerChatEvent e){
        Player p=e.getPlayer(); if(!creating.remove(p.getUniqueId())) return;
        e.setCancelled(true);
        String name=e.getMessage().trim();
        if(name.equalsIgnoreCase("cancel")){p.sendMessage("§7Team creation cancelled.");return;}
        if(!manager.validName(name)){p.sendMessage("§cInvalid team name. Use 2-16 letters, numbers, spaces or _. Try /team again.");return;}
        if(manager.getTeam(p.getUniqueId())!=null || manager.getByName(name)!=null){p.sendMessage("§cUnable to create that team. Check the name or your current team.");return;}
        manager.create(p.getUniqueId(),name); p.sendMessage("§a☀ Team created: §e"+name); plugin.getTabList().refreshNow();
    }
