package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.*;

public final class TeamChatManager {
    private final TeamManager teams;
    private final Set<UUID> toggled = new HashSet<>();
    public TeamChatManager(TeamManager teams){this.teams=teams;}
    public void toggle(Player p){ if(toggled.remove(p.getUniqueId())) p.sendMessage("§7Team chat §cdisabled§7."); else {toggled.add(p.getUniqueId());p.sendMessage("§7Team chat §aenabled§7.");} }
    public boolean isToggled(Player p){return toggled.contains(p.getUniqueId());}
    public void send(Player p,String message){
        var t=teams.getTeam(p.getUniqueId()); if(t==null){p.sendMessage("§cYou are not in a team.");return;}
        String line=t.color+"[TEAM] "+ChatColor.WHITE+p.getName()+ChatColor.GRAY+": "+ChatColor.WHITE+message;
        for(UUID u:t.members.keySet()){Player member=org.bukkit.Bukkit.getPlayer(u);if(member!=null)member.sendMessage(line);}
    }
    public void clear(Player p){toggled.remove(p.getUniqueId());}
}
