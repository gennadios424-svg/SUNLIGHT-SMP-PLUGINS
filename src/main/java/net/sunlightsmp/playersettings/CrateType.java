package net.sunlightsmp.playersettings;

import org.bukkit.Material;

public enum CrateType {
    COMMON("Common", Material.CHEST),
    RARE("Rare", Material.ENDER_CHEST),
    SPAWNER("Spawner", Material.SPAWNER),
    CRIMSON("Crimson", Material.CRIMSON_STEM),
    SUNLIGHT("Sunlight", Material.BEACON);

    private final String displayName;
    private final Material icon;
    CrateType(String displayName, Material icon) { this.displayName = displayName; this.icon = icon; }
    public String displayName() { return displayName; }
    public Material icon() { return icon; }
}