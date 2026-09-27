package net.sunlightsmp.playersettings;

import org.bukkit.Material;
import java.util.*;

public final class ShopPricing {
    private ShopPricing() {}
    public static double buyPrice(Material m) {
        if (m == null || !m.isItem()) return 0;
        if (m == Material.SPAWNER) return 1500;
        return Math.max(1, Math.ceil(SellPricing.price(m) * 2.5));
    }
    public static List<Material> category(String name) {
        List<Material> out=new ArrayList<>();
        for(Material m:Material.values()){
            if(!m.isItem()||m==Material.AIR||m==Material.SPAWNER)continue;
            String n=m.name(); boolean add=switch(name.toLowerCase(Locale.ROOT)){
                case "redstone"->n.contains("REDSTONE")||n.contains("PISTON")||n.contains("OBSERVER")||n.contains("DISPENSER")||n.contains("DROPPER")||n.contains("HOPPER")||n.contains("REPEATER")||n.contains("COMPARATOR")||n.contains("NOTE_BLOCK")||n.contains("TARGET")||n.contains("RAIL")||n.contains("TNT")||n.contains("LEVER")||n.contains("BUTTON")||n.contains("PRESSURE_PLATE");
                case "farm"->n.contains("SEED")||n.contains("SAPLING")||n.contains("WHEAT")||n.contains("CARROT")||n.contains("POTATO")||n.contains("BEETROOT")||n.contains("MELON")||n.contains("PUMPKIN")||n.contains("SUGAR_CANE")||n.contains("BAMBOO")||n.contains("CACTUS")||n.contains("KELP")||n.contains("VINE")||n.contains("MOSS")||n.contains("BONE_MEAL")||n.contains("BONE")||n.contains("HAY_BLOCK")||n.contains("COMPOSTER");
                case "food"->n.contains("APPLE")||n.contains("BREAD")||n.contains("CARROT")||n.contains("POTATO")||n.contains("BEETROOT")||n.contains("BEEF")||n.contains("PORKCHOP")||n.contains("CHICKEN")||n.contains("MUTTON")||n.contains("RABBIT")||n.contains("COD")||n.contains("SALMON")||n.contains("TROPICAL_FISH")||n.contains("PUFFERFISH")||n.contains("MELON_SLICE")||n.contains("COOKIE")||n.contains("PUMPKIN_PIE")||n.contains("CAKE")||n.contains("GOLDEN_APPLE")||n.contains("GOLDEN_CARROT")||n.contains("BERRIES")||n.contains("STEW")||n.contains("SOUP")||n.contains("CHORUS_FRUIT");
                case "pvp"->n.contains("OBSIDIAN")||n.contains("CRYING_OBSIDIAN")||n.contains("END_CRYSTAL")||n.contains("RESPAWN_ANCHOR")||n.contains("GLOWSTONE")||n.contains("TNT")||n.contains("COBWEB")||n.contains("ENDER_PEARL")||n.contains("EXPERIENCE_BOTTLE")||n.contains("TOTEM");
                case "blocks"->m.isBlock();
                default->false;};
            if(add)out.add(m);
        }
        out.sort(Comparator.comparing(Material::name)); return out;
    }
}