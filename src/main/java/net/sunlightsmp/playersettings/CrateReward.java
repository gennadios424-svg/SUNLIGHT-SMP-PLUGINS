package net.sunlightsmp.playersettings;

import org.bukkit.Material;
import java.util.List;

public record CrateReward(String name, Material material, int amount, double chance, List<String> commands) {}