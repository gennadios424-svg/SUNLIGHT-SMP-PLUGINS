package net.sunlightsmp.playersettings;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class TPACommand implements CommandExecutor, TabCompleter {
    private final TPAManager manager;
    public TPACommand(TPAManager manager) { this.manager = manager; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use TPA commands."); return true; }
        String name = command.getName().toLowerCase();
        if (name.equals("tpa") || name.equals("tpahere")) {
            if (args.length != 1) { p.sendMessage("Usage: /" + name + " <player>"); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { p.sendMessage("That player is not online."); return true; }
            manager.sendRequest(p, target, name.equals("tpahere"));
            return true;
        }
        if (name.equals("tpaccept")) { manager.acceptLatest(p); return true; }
        if (name.equals("tpdeny")) { manager.denyLatest(p); return true; }
        if (name.equals("tpacancel")) { manager.cancelOutgoing(p); return true; }
        return true;
    }

    @Override public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(command.getName().equalsIgnoreCase("tpa") || command.getName().equalsIgnoreCase("tpahere"))) return java.util.Collections.emptyList();
        String prefix = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(prefix)).sorted().toList();
    }
}
