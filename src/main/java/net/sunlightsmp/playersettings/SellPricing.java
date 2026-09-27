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
        if (n.contains("END_CRYSTAL")) return 400;
        if (n.contains("ELYTRA")) return 100_000;
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
        if (n.contains("OBSIDIAN") || n.contains("CRYING_OBSIDIAN")) return 100;
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
        if (n.contains("SHULKER_BOX")) return 100;
        if (n.contains("NOTE_BLOCK")) return 50;
        if (n.contains("ENCHANTING_TABLE")) return 10_000;
        if (n.contains("ANVIL")) return 3_000;
        if (n.contains("BREWING_STAND")) return 500;
        if (n.contains("HOPPER")) return 750;
        if (n.contains("PISTON")) return 150;
        if (n.contains("OBSERVER")) return 300;
        if (n.contains("DISPENSER") || n.contains("DROPPER")) return 180;
        if (n.contains("RAIL")) return 25;
        if (n.contains("TNT")) return 250;

        if (n.equals("TOTEM_OF_UNDYING")) return 25_000;
        if (n.equals("NETHER_STAR")) return 50_000;
        if (n.equals("HEART_OF_THE_SEA")) return 15_000;
        if (n.equals("NAUTILUS_SHELL")) return 1_500;
        if (n.equals("WITHER_SKELETON_SKULL")) return 8_000;
        if (n.equals("PLAYER_HEAD") || n.equals("ZOMBIE_HEAD") || n.equals("SKELETON_SKULL") || n.equals("CREEPER_HEAD") || n.equals("PIGLIN_HEAD") || n.equals("DRAGON_HEAD")) return 5_000;

        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")) return armorPrice(n);
        if (n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE") || n.endsWith("_SWORD")) return toolPrice(n);
        if (n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("MACE")) return 5_000;

        if (n.equals("ENCHANTED_GOLDEN_APPLE")) return 15_000;
        if (n.equals("GOLDEN_APPLE")) return 2_500;
        if (n.equals("ENDER_PEARL")) return 250;
        if (n.equals("BLAZE_ROD")) return 300;
        if (n.equals("GHAST_TEAR")) return 1_000;
        if (n.equals("ENDER_EYE")) return 500;
        if (n.equals("SHULKER_SHELL")) return 2_000;
        if (n.equals("FIREWORK_ROCKET")) return 25;
        if (n.equals("EXPERIENCE_BOTTLE")) return 150;
        if (n.equals("GUNPOWDER")) return 20;

        return m.isBlock() ? 10 : 5;
    }

    private static double armorPrice(String n) {
        if (n.contains("NETHERITE")) return 60_000;
        if (n.contains("DIAMOND")) return 15_000;
        if (n.contains("IRON")) return 2_500;
        if (n.contains("CHAINMAIL")) return 1_500;
        if (n.contains("GOLDEN")) return 1_000;
        if (n.contains("LEATHER")) return 500;
        return 250;
    }

    private static double toolPrice(String n) {
        if (n.contains("NETHERITE")) return 50_000;
        if (n.contains("DIAMOND")) return 12_000;
        if (n.contains("IRON")) return 2_000;
        if (n.contains("GOLDEN")) return 800;
        if (n.contains("STONE")) return 250;
        return 100;
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
        m.put("AMETHYST_BLOCK",1_200D); m.put("ANCIENT_DEBRIS",8_000D); m.put("OBSIDIAN",100D);
        m.put("SHULKER_BOX",100D); m.put("END_CRYSTAL",400D); m.put("NOTE_BLOCK",50D);
        return Collections.unmodifiableMap(m);
    }
}
