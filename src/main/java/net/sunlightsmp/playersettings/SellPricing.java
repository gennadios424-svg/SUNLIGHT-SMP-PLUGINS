package net.sunlightsmp.playersettings;

import org.bukkit.Material;
import java.util.*;

public final class SellPricing {
    private SellPricing() {}

    public static double price(Material m) {
        if (m == null || !m.isItem()) return 0;
        String n = m.name();
        Double v = EXACT.get(n);
        if (v != null) return v;

        if (n.contains("NETHERITE")) return 225_000;
        if (n.contains("DIAMOND")) return 50_000;
        if (n.contains("EMERALD")) return 35_000;
        if (n.contains("ANCIENT_DEBRIS")) return 8_000;
        if (n.contains("GOLD")) return 12_000;
        if (n.contains("IRON")) return 3_000;
        if (n.contains("COPPER")) return 900;
        if (n.contains("LAPIS")) return 1_500;
        if (n.contains("REDSTONE")) return 1_000;
        if (n.contains("COAL")) return 350;
        if (n.contains("QUARTZ")) return 700;
        if (n.contains("AMETHYST")) return 1_200;
        if (n.contains("SPAWNER")) return 75_000;
        if (n.contains("BEACON")) return 75_000;
        if (n.contains("END_CRYSTAL")) return 5_000;
        if (n.contains("ELYTRA")) return 100_000;
        if (n.contains("SHULKER")) return 20_000;
        if (n.contains("DRAGON_EGG")) return 1_000_000;

        if (n.contains("LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE") || n.endsWith("_PLANKS")) return 16;
        if (n.contains("LEAVES")) return 4;
        if (n.contains("SAPLING")) return 12;
        if (n.contains("FLOWER") || n.contains("PETALS") || n.contains("BLOSSOM")) return 125;
        if (n.contains("KELP")) return 200;
        if (n.contains("GRASS") || n.contains("FERN") || n.contains("MOSS")) return 8;
        if (n.contains("DIRT") || n.contains("SAND") || n.contains("GRAVEL") || n.contains("CLAY")) return 6;
        if (n.contains("STONE") || n.contains("DEEPSLATE") || n.contains("COBBLE")) return 8;
        if (n.contains("NETHERRACK") || n.contains("BASALT") || n.contains("BLACKSTONE")) return 10;
        if (n.contains("END_STONE")) return 15;
        if (n.contains("OBSIDIAN") || n.contains("CRYING_OBSIDIAN")) return 1_500;
        if (n.contains("ICE") || n.contains("SNOW")) return 12;
        if (n.contains("CORAL")) return 80;
        if (n.contains("SUGAR_CANE") || n.contains("BAMBOO")) return 35;
        if (n.contains("CACTUS")) return 30;
        if (n.contains("WHEAT") || n.contains("CARROT") || n.contains("POTATO") || n.contains("BEETROOT")) return 25;
        if (n.contains("MELON") || n.contains("PUMPKIN")) return 60;
        if (n.contains("HONEY")) return 100;
        if (n.contains("SLIME")) return 250;
        if (n.contains("BONE")) return 20;
        if (n.contains("TERRACOTTA") || n.contains("CONCRETE") || n.contains("GLAZED")) return 30;
        if (n.contains("WOOL")) return 25;
        if (n.contains("GLASS")) return 20;
        if (n.contains("BRICK")) return 45;
        if (n.contains("PRISMARINE")) return 120;
        if (n.contains("PURPUR")) return 100;
        if (n.contains("ENCHANTING_TABLE")) return 10_000;
        if (n.contains("ANVIL")) return 3_000;
        if (n.contains("BREWING_STAND")) return 500;
        if (n.contains("HOPPER")) return 750;
        if (n.contains("PISTON")) return 150;
        if (n.contains("OBSERVER")) return 300;
        if (n.contains("DISPENSER") || n.contains("DROPPER")) return 180;
        if (n.contains("RAIL")) return 25;
        if (n.contains("TNT")) return 250;

        return m.isBlock() ? 10 : 5;
    }

    private static final Map<String, Double> EXACT = createExact();

    private static Map<String, Double> createExact() {
        Map<String, Double> m = new HashMap<>();
        m.put("NETHERITE_BLOCK", 225_000D);
        m.put("DIAMOND_BLOCK", 50_000D);
        m.put("EMERALD_BLOCK", 35_000D);
        m.put("SEA_PICKLE", 150D);
        m.put("PINK_PETALS", 125D);
        m.put("KELP", 200D);
        String[] logs={"OAK_LOG","SPRUCE_LOG","BIRCH_LOG","JUNGLE_LOG","ACACIA_LOG","DARK_OAK_LOG","MANGROVE_LOG","CHERRY_LOG","PALE_OAK_LOG","CRIMSON_STEM","WARPED_STEM","OAK_WOOD","SPRUCE_WOOD","BIRCH_WOOD","JUNGLE_WOOD","ACACIA_WOOD","DARK_OAK_WOOD","MANGROVE_WOOD","CHERRY_WOOD","PALE_OAK_WOOD","BAMBOO_BLOCK"};
        for(String s:logs)m.put(s,16D);
        m.put("COAL_BLOCK",350D); m.put("IRON_BLOCK",3_000D); m.put("GOLD_BLOCK",12_000D);
        m.put("REDSTONE_BLOCK",1_000D); m.put("LAPIS_BLOCK",1_500D); m.put("COPPER_BLOCK",900D);
        m.put("AMETHYST_BLOCK",1_200D); m.put("ANCIENT_DEBRIS",8_000D); m.put("OBSIDIAN",250D);
        return Collections.unmodifiableMap(m);
    }
}