package net.sunlightsmp.playersettings;

import org.bukkit.Material;
import java.util.*;

public final class ShopPricing {
    private ShopPricing() {}

    public static double buyPrice(Material m) {
        if (m == null || !m.isItem()) return 0;
        return switch (m) {
            case SPAWNER -> 2_000_000;
            case OBSIDIAN -> 250;
            case CRYING_OBSIDIAN -> 350;
            case END_CRYSTAL -> 1_250;
            case RESPAWN_ANCHOR -> 2_500;
            case GLOWSTONE -> 25;
            case TNT -> 750;
            case COBWEB -> 75;
            case ENDER_PEARL -> 500;
            case EXPERIENCE_BOTTLE -> 350;
            case TOTEM_OF_UNDYING -> 25_000;
            case SHULKER_BOX -> 1_500;
            case CHEST -> 150;
            case TRAPPED_CHEST -> 250;
            case BARREL -> 150;
            case ENDER_CHEST -> 2_500;
            case HOPPER -> 1_500;
            case CRAFTER -> 2_500;
            case CARROT -> 15_000;
            case WHEAT_SEEDS, BEETROOT_SEEDS -> 25;
            case POTATO -> 75;
            case SUGAR_CANE -> 100;
            case BAMBOO -> 50;
            case CACTUS -> 75;
            case KELP -> 25;
            case MELON -> 150;
            case PUMPKIN -> 150;
            case COCOA_BEANS -> 100;
            case NETHER_WART -> 150;
            case BONE_MEAL -> 40;
            case OAK_SAPLING -> 50;
            case COMPOSTER -> 500;
            case REDSTONE -> 75;
            case REDSTONE_TORCH -> 125;
            case NOTE_BLOCK -> 250;
            case PISTON -> 400;
            case STICKY_PISTON -> 650;
            case OBSERVER -> 750;
            case DISPENSER, DROPPER -> 500;
            case REPEATER -> 250;
            case COMPARATOR -> 350;
            case LEVER -> 50;
            case STONE_BUTTON -> 40;
            case STONE_PRESSURE_PLATE -> 60;
            case SMOKER -> 600;
            case WATER_BUCKET -> 1_000;
            case LAVA_BUCKET -> 1_500;
            case RAIL -> 100;
            case APPLE -> 100;
            case BREAD -> 75;
            case COOKED_BEEF, COOKED_PORKCHOP, COOKED_MUTTON, COOKED_SALMON -> 125;
            case COOKED_CHICKEN, COOKED_COD -> 100;
            case GOLDEN_CARROT -> 750;
            case GOLDEN_APPLE -> 5_000;
            case BAKED_POTATO -> 75;
            case COOKIE -> 50;
            case PUMPKIN_PIE -> 100;
            case MELON_SLICE -> 40;
            case CHORUS_FRUIT -> 100;
            case STONE, COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE -> 20;
            case NETHERRACK, BLACKSTONE, BASALT -> 25;
            case END_STONE -> 40;
            case SAND, GRAVEL, DIRT, CLAY -> 15;
            case GLASS -> 50;
            case BRICKS -> 75;
            case PRISMARINE, PURPUR_BLOCK -> 150;
            case WHITE_WOOL, BLACK_WOOL, RED_WOOL, BLUE_WOOL, GREEN_WOOL, YELLOW_WOOL -> 75;
            case WHITE_CONCRETE, BLACK_CONCRETE, RED_CONCRETE, BLUE_CONCRETE, GREEN_CONCRETE, YELLOW_CONCRETE -> 100;
            default -> Math.max(1, Math.ceil(SellPricing.price(m) * 2.0));
        };
    }

    private static boolean isSurvivalObtainable(Material m) {
        String n = m.name();
        if (n.endsWith("_SPAWN_EGG")) return false;
        return switch(m) {
            case BEDROCK, BARRIER, END_PORTAL_FRAME, END_PORTAL, NETHER_PORTAL,
                 COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK,
                 STRUCTURE_BLOCK, STRUCTURE_VOID, JIGSAW, LIGHT,
                 DEBUG_STICK, KNOWLEDGE_BOOK, BUDDING_AMETHYST,
                 REINFORCED_DEEPSLATE, PETRIFIED_OAK_SLAB -> false;
            default -> true;
        };
    }

    public static List<Material> category(String name) {
        String c = name.toLowerCase(Locale.ROOT);
        List<Material> items = switch(c) {
            case "redstone" -> List.of(
                Material.REDSTONE, Material.REDSTONE_TORCH, Material.NOTE_BLOCK,
                Material.PISTON, Material.STICKY_PISTON, Material.OBSERVER,
                Material.DISPENSER, Material.DROPPER, Material.HOPPER, Material.CRAFTER,
                Material.REPEATER, Material.COMPARATOR, Material.LEVER,
                Material.STONE_BUTTON, Material.STONE_PRESSURE_PLATE, Material.SMOKER,
                Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.RAIL, Material.TNT
            );
            case "farm" -> List.of(Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO, Material.BEETROOT_SEEDS, Material.SUGAR_CANE, Material.BAMBOO, Material.CACTUS, Material.KELP, Material.MELON, Material.PUMPKIN, Material.COCOA_BEANS, Material.NETHER_WART, Material.BONE_MEAL, Material.OAK_SAPLING, Material.COMPOSTER);
            case "food" -> List.of(Material.APPLE, Material.BREAD, Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN, Material.COOKED_MUTTON, Material.COOKED_COD, Material.COOKED_SALMON, Material.GOLDEN_CARROT, Material.GOLDEN_APPLE, Material.BAKED_POTATO, Material.COOKIE, Material.PUMPKIN_PIE, Material.MELON_SLICE, Material.CHORUS_FRUIT);
            case "pvp" -> List.of(Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.END_CRYSTAL, Material.RESPAWN_ANCHOR, Material.GLOWSTONE, Material.TNT, Material.COBWEB, Material.ENDER_PEARL, Material.EXPERIENCE_BOTTLE, Material.TOTEM_OF_UNDYING);
            case "blocks" -> List.of(Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.NETHERRACK, Material.BLACKSTONE, Material.BASALT, Material.END_STONE, Material.SAND, Material.GRAVEL, Material.DIRT, Material.CLAY, Material.GLASS, Material.BRICKS, Material.PRISMARINE, Material.PURPUR_BLOCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.SHULKER_BOX, Material.ENDER_CHEST, Material.WHITE_WOOL, Material.BLACK_WOOL, Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL, Material.WHITE_CONCRETE, Material.BLACK_CONCRETE, Material.RED_CONCRETE, Material.BLUE_CONCRETE, Material.GREEN_CONCRETE, Material.YELLOW_CONCRETE);
            default -> List.of();
        };
        List<Material> out = new ArrayList<>();
        for (Material m : items) if (m != Material.AIR && m.isItem() && isSurvivalObtainable(m)) out.add(m);
        return out;
    }
}
