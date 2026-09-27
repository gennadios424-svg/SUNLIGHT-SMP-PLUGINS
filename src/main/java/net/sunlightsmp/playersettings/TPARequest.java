package net.sunlightsmp.playersettings;

import org.bukkit.entity.Player;

public final class TPARequest {
    private final Player requester;
    private final Player target;
    private final boolean tpHere;
    private final long expiresAt;

    public TPARequest(Player requester, Player target, boolean tpHere, long expiresAt) {
        this.requester = requester;
        this.target = target;
        this.tpHere = tpHere;
        this.expiresAt = expiresAt;
    }

    public Player requester() { return requester; }
    public Player target() { return target; }
    public boolean tpHere() { return tpHere; }
    public long expiresAt() { return expiresAt; }
    public boolean expired() { return System.currentTimeMillis() >= expiresAt; }
}
