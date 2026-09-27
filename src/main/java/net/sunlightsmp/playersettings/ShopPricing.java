package net.sunlightsmp.playersettings;

import org.bukkit.Material;
import java.util.*;

public final class ShopPricing {
    private ShopPricing() {}

    public static double buyPrice(Material m) {
        if (m == null || !m.isItem()) return 0;
        if (m == Material.SPAWNER) return 2_000_000;
        if (m == Material.SHULKER_BOX) return 250;
        if (m == Material.END_CRYSTAL) return 1_000;
        if (m == Material.CARROT) return 15_000;
        return Math.max(1, Math.ceil(SellPricing.price(m) * 2.5));
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
                Material.REDSTONE, Material.REDSTONE_TORCH,
                Material.NOTE_BLOCK, Material.PISTON, Material.STICKY_PISTON,
                Material.OBSERVER, Material.DISPENSER, Material.DROPPER,
                Material.HOPPER, Material.CRAFTER, Material.REPEATER,
                Material.COMPARATOR, Material.LEVER, Material.STONE_BUTTON,
                Material.STONE_PRESSURE_PLATE, Material.RAIL, Material.TNT
            );

            case "farm" -> List.of(
                Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO,
                Material.BEETROOT_SEEDS, Material.SUGAR_CANE, Material.BAMBOO,
                Material.CACTUS, Material.KELP, Material.MELON, Material.PUMPKIN,
                Material.COCOA_BEANS, Material.NETHER_WART, Material.BONE_MEAL,
                Material.OAK_SAPLING, Material.COMPOSTER, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.CHEST, Material.BARREL
            );

            case "food" -> List.of(
                Material.APPLE, Material.BREAD, Material.COOKED_BEEF,
                Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
                Material.COOKED_MUTTON, Material.COOKED_COD, Material.COOKED_SALMON,
                Material.GOLDEN_CARROT, Material.GOLDEN_APPLE, Material.BAKED_POTATO,
                Material.COOKIE, Material.PUMPKIN_PIE, Material.MELON_SLICE,
                Material.CHORUS_FRUIT
            );

            case "pvp" -> List.of(
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.END_CRYSTAL,
                Material.RESPAWN_ANCHOR, Material.GLOWSTONE, Material.TNT,
                Material.COBWEB, Material.ENDER_PEARL, Material.EXPERIENCE_BOTTLE,
                Material.TOTEM_OF_UNDYING
            );

            case "blocks" -> List.of(
                Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
                Material.COBBLED_DEEPSLATE, Material.NETHERRACK,
                Material.BLACKSTONE, Material.BASALT, Material.END_STONE,
                Material.SAND, Material.GRAVEL, Material.DIRT, Material.CLAY,
                Material.GLASS, Material.BRICKS, Material.PRISMARINE,
                Material.PURPUR_BLOCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
                Material.SHULKER_BOX, Material.ENDER_CHEST,
                Material.WHITE_WOOL, Material.BLACK_WOOL, Material.RED_WOOL,
                Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL,
                Material.WHITE_CONCRETE, Material.BLACK_CONCRETE, Material.RED_CONCRETE,
                Material.BLUE_CONCRETE, Material.GREEN_CONCRETE, Material.YELLOW_CONCRETE
            );

            default -> List.of();
        };

        List<Material> out = new ArrayList<>();
        for (Material m : items) {
            if (m != Material.AIR && m.isItem() && isSurvivalObtainable(m)) out.add(m);
        }
        return out;
    }
}
