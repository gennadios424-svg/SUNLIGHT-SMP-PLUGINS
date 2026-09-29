package net.sunlightsmp.playersettings;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class TeamManager {
    public static final int MAX_MEMBERS = 10;
    private final SunlightPlayerSettings plugin;
    private final File file;
    private final Map<UUID, TeamData> teams = new HashMap<>();
    private final Map<UUID, UUID> playerTeams = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();

    public TeamManager(SunlightPlayerSettings plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "team-data.yml");
        load();
    }

    public synchronized TeamData create(UUID owner, String name) {
        if (playerTeams.containsKey(owner)) return null;
        TeamData t = new TeamData(UUID.randomUUID(), name, owner, ChatColor.YELLOW, "");
        t.members.put(owner, Role.OWNER);
        teams.put(t.id, t);
        playerTeams.put(owner, t.id);
        save();
        return t;
    }

    public synchronized boolean delete(TeamData team) {
        if (team == null || !teams.containsKey(team.id)) return false;
        for (UUID u : new ArrayList<>(team.members.keySet())) playerTeams.remove(u);
        teams.remove(team.id);
        invitations.values().removeIf(i -> i.teamId.equals(team.id));
        save();
        return true;
    }

    public synchronized TeamData getTeam(UUID player) {
        UUID id = playerTeams.get(player);
        return id == null ? null : teams.get(id);
    }

    public synchronized TeamData getByName(String name) {
        return teams.values().stream().filter(t -> t.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public synchronized Collection<TeamData> getTeams() {
        return new ArrayList<>(teams.values());
    }

    public synchronized boolean invite(UUID inviter, UUID target, String targetName) {
        TeamData t = getTeam(inviter);
        if (t == null || !canManage(t, inviter) || playerTeams.containsKey(target) || t.members.size() >= MAX_MEMBERS) return false;
        invitations.put(target, new Invitation(t.id, inviter, targetName, System.currentTimeMillis()));
        return true;
    }

    public synchronized Invitation getInvitation(UUID target) { return invitations.get(target); }

    public synchronized boolean accept(UUID target) {
        Invitation inv = invitations.remove(target);
        if (inv == null || playerTeams.containsKey(target)) return false;
        TeamData t = teams.get(inv.teamId);
        if (t == null || t.members.size() >= MAX_MEMBERS) return false;
        t.members.put(target, Role.MEMBER);
        playerTeams.put(target, t.id);
        save();
        return true;
    }

    public synchronized Invitation deny(UUID target) { return invitations.remove(target); }

    public synchronized boolean kick(UUID actor, UUID target) {
        TeamData t = getTeam(actor);
        if (t == null || !canManage(t, actor) || !t.members.containsKey(target) || target.equals(t.owner)) return false;
        t.members.remove(target);
        playerTeams.remove(target);
        save();
        return true;
    }

    public synchronized boolean promote(UUID actor, UUID target) {
        TeamData t = getTeam(actor);
        if (t == null || !isOwner(t, actor) || t.members.get(target) != Role.MEMBER) return false;
        t.members.put(target, Role.OFFICER); save(); return true;
    }

    public synchronized boolean demote(UUID actor, UUID target) {
        TeamData t = getTeam(actor);
        if (t == null || !isOwner(t, actor) || t.members.get(target) != Role.OFFICER) return false;
        t.members.put(target, Role.MEMBER); save(); return true;
    }

    public synchronized boolean leave(UUID player) {
        TeamData t = getTeam(player);
        if (t == null || player.equals(t.owner)) return false;
        t.members.remove(player); playerTeams.remove(player); save(); return true;
    }

    public synchronized boolean transfer(UUID owner, UUID target) {
        TeamData t = getTeam(owner);
        if (t == null || !isOwner(t, owner) || !t.members.containsKey(target) || target.equals(owner)) return false;
        t.members.put(owner, Role.OFFICER);
        t.members.put(target, Role.OWNER);
        t.owner = target;
        save();
        return true;
    }

    public synchronized boolean rename(UUID actor, String name) {
        TeamData t = getTeam(actor);
        if (t == null || !isOwner(t, actor) || !validName(name) || getTeamByNameExcluding(name, t) != null) return false;
        t.name = name.trim(); save(); return true;
    }

    public synchronized boolean setTag(UUID actor, String tag) {
        TeamData t = getTeam(actor);
        if (t == null || !isOwner(t, actor) || tag.length() > 6) return false;
        t.tag = tag.trim(); save(); return true;
    }

    public synchronized boolean setColor(UUID actor, ChatColor color) {
        TeamData t = getTeam(actor);
        if (t == null || !canManage(t, actor) || color == null || !color.isColor()) return false;
        t.color = color; save(); return true;
    }

    public boolean canManage(TeamData t, UUID actor) {
        Role r = t.members.get(actor);
        return r == Role.OWNER || r == Role.OFFICER;
    }

    public boolean isOwner(TeamData t, UUID actor) { return t.owner.equals(actor); }

    public String prefix(TeamData t) {
        if (t == null || t.tag.isBlank()) return "";
        return t.color + "[" + t.tag + "] " + ChatColor.RESET;
    }

    private TeamData getTeamByNameExcluding(String name, TeamData excluded) {
        return teams.values().stream().filter(t -> t != excluded && t.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public boolean validName(String name) {
        return name != null && name.trim().matches("[A-Za-z0-9_ -]{2,16}");
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (TeamData t : teams.values()) {
            String p = "teams." + t.id;
            y.set(p + ".name", t.name);
            y.set(p + ".owner", t.owner.toString());
            y.set(p + ".color", t.color.name());
            y.set(p + ".tag", t.tag);
            for (Map.Entry<UUID, Role> e : t.members.entrySet())
                y.set(p + ".members." + e.getKey(), e.getValue().name());
        }
        try { y.save(file); } catch (IOException e) { plugin.getLogger().severe("Could not save team data: " + e.getMessage()); }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("teams");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            try {
                UUID teamId = UUID.fromString(id);
                String name = sec.getString(id + ".name");
                UUID owner = UUID.fromString(sec.getString(id + ".owner"));
                ChatColor color;
                try { color = ChatColor.valueOf(sec.getString(id + ".color", "YELLOW")); } catch (Exception e) { color = ChatColor.YELLOW; }
                TeamData t = new TeamData(teamId, name, owner, color, sec.getString(id + ".tag", ""));
                ConfigurationSection members = sec.getConfigurationSection(id + ".members");
                if (members != null) for (String u : members.getKeys(false)) {
                    try { t.members.put(UUID.fromString(u), Role.valueOf(members.getString(u, "MEMBER"))); } catch (Exception ignored) {}
                }
                if (!t.members.containsKey(owner)) t.members.put(owner, Role.OWNER);
                teams.put(teamId, t);
                for (UUID u : t.members.keySet()) playerTeams.put(u, teamId);
            } catch (Exception ignored) {}
        }
    }

    public enum Role { OWNER, OFFICER, MEMBER }

    public static final class TeamData {
        public final UUID id;
        public String name, tag;
        public UUID owner;
        public ChatColor color;
        public final Map<UUID, Role> members = new LinkedHashMap<>();
        TeamData(UUID id, String name, UUID owner, ChatColor color, String tag) { this.id=id; this.name=name; this.owner=owner; this.color=color; this.tag=tag; }
        public String memberNames() {
            return members.keySet().stream().map(u -> {
                var p = org.bukkit.Bukkit.getPlayer(u);
                return p == null ? u.toString().substring(0,8) : p.getName();
            }).collect(Collectors.joining(", "));
        }
    }

    public record Invitation(UUID teamId, UUID inviter, String targetName, long createdAt) {}
}
