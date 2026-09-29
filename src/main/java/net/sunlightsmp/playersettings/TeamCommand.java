package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class TeamCommand implements CommandExecutor, TabCompleter {
    private final SunlightPlayerSettings plugin;
    private final TeamManager manager;
    private final TeamMenu menu;

    public TeamCommand(SunlightPlayerSettings plugin, TeamManager manager, TeamMenu menu) { this.plugin=plugin; this.manager=manager; this.menu=menu; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] a) {
        if (!(sender instanceof Player p)) return true;
        if (a.length == 0) { menu.openMain(p); return true; }
        String sub=a[0].toLowerCase(Locale.ROOT);
        switch(sub) {
            case "create" -> { if(a.length<2){msg(p,"§e/team create <name>");return true;} if(!manager.validName(a[1])||manager.getTeam(p.getUniqueId())!=null||manager.getByName(a[1])!=null){msg(p,"§cUnable to create that team. Check the name or your current team.");return true;} manager.create(p.getUniqueId(),a[1]);msg(p,"§a☀ Team created: §e"+a[1]); plugin.getTabList().refreshNow(); }
            case "invite" -> { if(a.length<2){msg(p,"§e/team invite <player>");return true;} Player target=plugin.getServer().getPlayerExact(a[1]); if(target==null){msg(p,"§cThat player is not online.");return true;} if(manager.invite(p.getUniqueId(),target.getUniqueId(),target.getName())){msg(p,"§aInvitation sent to §e"+target.getName()+"§a.");target.sendMessage("§6📩 §eTeam Invitation§r\n§f"+p.getName()+" invited you to join §e"+manager.getTeam(p.getUniqueId()).name+"§f. §a/team accept §7or §c/team deny");}else msg(p,"§cYou cannot invite that player."); }
            case "join" -> { if(a.length<2){msg(p,"§e/team join <team>");return true;} if(manager.join(p.getUniqueId(),a[1])){msg(p,"§aYou joined §e"+manager.getByName(a[1]).name+"§a.");plugin.getTabList().refreshNow();}else msg(p,"§cThat team does not exist, is full, or you are already in a team."); }
            case "list" -> { StringBuilder b=new StringBuilder("§6☀ §eAvailable Teams§7: "); manager.getTeams().stream().limit(10).forEach(t->b.append("§f").append(t.name).append(" §7(").append(t.members.size()).append("/").append(TeamManager.MAX_MEMBERS).append(")  ")); msg(p,b.toString()); }
            case "accept" -> { if(manager.accept(p.getUniqueId())){msg(p,"§aYou joined the team!");plugin.getTabList().refreshNow();}else msg(p,"§cYou have no valid pending team invitation."); }
            case "deny" -> { if(manager.deny(p.getUniqueId())!=null)msg(p,"§7Team invitation declined.");else msg(p,"§cYou have no pending team invitation."); }
            case "kick" -> { if(a.length<2){msg(p,"§e/team kick <player>");return true;} Player t=plugin.getServer().getPlayerExact(a[1]);if(t!=null&&manager.kick(p.getUniqueId(),t.getUniqueId())){msg(p,"§aRemoved §e"+t.getName()+"§a from the team.");plugin.getTabList().refreshNow();}else msg(p,"§cYou cannot remove that player."); }
            case "promote" -> { if(a.length<2){msg(p,"§e/team promote <player>");return true;} Player t=plugin.getServer().getPlayerExact(a[1]);if(t!=null&&manager.promote(p.getUniqueId(),t.getUniqueId())){msg(p,"§aPromoted §e"+t.getName()+"§a to Officer.");}else msg(p,"§cYou cannot promote that player."); }
            case "demote" -> { if(a.length<2){msg(p,"§e/team demote <player>");return true;} Player t=plugin.getServer().getPlayerExact(a[1]);if(t!=null&&manager.demote(p.getUniqueId(),t.getUniqueId())){msg(p,"§aDemoted §e"+t.getName()+"§a to Member.");}else msg(p,"§cYou cannot demote that player."); }
            case "leave" -> { var t=manager.getTeam(p.getUniqueId());if(t==null){msg(p,"§cYou are not in a team.");}else if(manager.isOwner(t,p.getUniqueId())){msg(p,"§cYou are the owner. Transfer ownership or disband the team from the Team Settings GUI.");}else{manager.leave(p.getUniqueId());msg(p,"§7You left §e"+t.name+"§7.");plugin.getTabList().refreshNow();} }
            case "chat", "tc" -> plugin.getTeamChat().toggle(p);
            case "info" -> { var t=manager.getTeam(p.getUniqueId());if(t==null){msg(p,"§7You are not in a team.");}else{msg(p,"§6☀ §e"+t.name+" §7("+t.members.size()+"/"+TeamManager.MAX_MEMBERS+")");msg(p,"§7Owner: §f"+name(t.owner)+" §7| Color: "+t.color+"■");msg(p,"§7Members: §f"+t.memberNames());} }
            case "rename" -> { if(a.length<2||!manager.rename(p.getUniqueId(),a[1]))msg(p,"§cOwner only. Name: 2-16 letters/numbers/spaces/_");else{msg(p,"§aTeam renamed.");plugin.getTabList().refreshNow();} }
            case "tag" -> { if(a.length<2||!manager.setTag(p.getUniqueId(),a[1]))msg(p,"§cOwner only. Tag must be 0-6 characters.");else{msg(p,"§aTeam tag updated.");plugin.getTabList().refreshNow();} }
            case "color" -> { if(a.length<2){msg(p,"§e/team color <yellow|gold|green|aqua|blue|light_purple|red|white|gray>");return true;}try{ChatColor c=ChatColor.valueOf(a[1].toUpperCase(Locale.ROOT));if(manager.setColor(p.getUniqueId(),c)){msg(p,"§aTeam color updated.");plugin.getTabList().refreshNow();}else msg(p,"§cYou cannot change the team color.");}catch(Exception e){msg(p,"§cInvalid color.");} }
            default -> msg(p,"§e/team §7| create | invite | accept | deny | kick | promote | demote | leave | chat | info");
        }
        return true;
    }

    private String name(UUID u){var p=plugin.getServer().getPlayer(u);return p==null?u.toString().substring(0,8):p.getName();}
    private void msg(Player p,String s){p.sendMessage(ChatColor.translateAlternateColorCodes('&',s));}

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if(a.length==1)return List.of("create","list","join","invite","accept","deny","kick","promote","demote","leave","chat","info","rename","tag","color");
        if(a.length==2 && List.of("invite","kick","promote","demote").contains(a[0].toLowerCase()))return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(n->n.toLowerCase().startsWith(a[1].toLowerCase())).toList();
        if(a.length==2&&a[0].equalsIgnoreCase("color"))return List.of("yellow","gold","green","aqua","blue","light_purple","red","white","gray");
        return List.of();
    }
}
