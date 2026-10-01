package dev.farmprofit;

import java.util.HashMap;
import java.util.Map;

/** Maps in-game item names to Bazaar product IDs, and crop blocks to crop names. */
public final class Items {
    private static final Map<String, String> NAME_TO_ID = new HashMap<>();
    private static final Map<String, String> CROP_BLOCKS = new HashMap<>();

    static {
        // Wheat & seeds
        id("Wheat", "WHEAT"); id("Enchanted Wheat", "ENCHANTED_WHEAT");
        id("Hay Bale", "HAY_BLOCK"); id("Enchanted Hay Bale", "ENCHANTED_HAY_BLOCK");
        id("Seeds", "SEEDS"); id("Enchanted Seeds", "ENCHANTED_SEEDS"); id("Box of Seeds", "BOX_OF_SEEDS");
        // Carrot
        id("Carrot", "CARROT_ITEM"); id("Enchanted Carrot", "ENCHANTED_CARROT");
        id("Enchanted Golden Carrot", "ENCHANTED_GOLDEN_CARROT");
        // Potato
        id("Potato", "POTATO_ITEM"); id("Enchanted Potato", "ENCHANTED_POTATO");
        id("Enchanted Baked Potato", "ENCHANTED_BAKED_POTATO");
        id("Poisonous Potato", "POISONOUS_POTATO"); id("Enchanted Poisonous Potato", "ENCHANTED_POISONOUS_POTATO");
        // Pumpkin & melon
        id("Pumpkin", "PUMPKIN"); id("Enchanted Pumpkin", "ENCHANTED_PUMPKIN"); id("Polished Pumpkin", "POLISHED_PUMPKIN");
        id("Melon", "MELON"); id("Melon Slice", "MELON"); id("Enchanted Melon", "ENCHANTED_MELON");
        id("Enchanted Melon Block", "ENCHANTED_MELON_BLOCK");
        // Sugar cane
        id("Sugar Cane", "SUGAR_CANE"); id("Enchanted Sugar", "ENCHANTED_SUGAR");
        id("Enchanted Sugar Cane", "ENCHANTED_SUGAR_CANE");
        // Cactus
        id("Cactus", "CACTUS"); id("Enchanted Cactus Green", "ENCHANTED_CACTUS_GREEN");
        id("Enchanted Cactus", "ENCHANTED_CACTUS");
        // Cocoa
        id("Cocoa Beans", "INK_SACK:3"); id("Enchanted Cocoa Beans", "ENCHANTED_COCOA");
        id("Enchanted Cookie", "ENCHANTED_COOKIE");
        // Nether wart
        id("Nether Wart", "NETHER_STALK"); id("Enchanted Nether Wart", "ENCHANTED_NETHER_STALK");
        id("Mutant Nether Wart", "MUTANT_NETHER_STALK");
        // Mushrooms
        id("Red Mushroom", "RED_MUSHROOM"); id("Brown Mushroom", "BROWN_MUSHROOM");
        id("Enchanted Red Mushroom", "ENCHANTED_RED_MUSHROOM"); id("Enchanted Brown Mushroom", "ENCHANTED_BROWN_MUSHROOM");
        id("Red Mushroom Block", "HUGE_MUSHROOM_2"); id("Brown Mushroom Block", "HUGE_MUSHROOM_1");
        id("Enchanted Red Mushroom Block", "ENCHANTED_HUGE_MUSHROOM_2");
        id("Enchanted Brown Mushroom Block", "ENCHANTED_HUGE_MUSHROOM_1");
        // Common farming / pest extras
        id("Cropie", "CROPIE"); id("Squash", "SQUASH"); id("Fermento", "FERMENTO");
        id("Compost", "COMPOST"); id("Dung", "DUNG"); id("Honey Jar", "HONEY_JAR");
        id("Plant Matter", "PLANT_MATTER"); id("Tasty Cheese", "CHEESE_FUEL");

        crop("wheat", "Wheat"); crop("carrots", "Carrot"); crop("potatoes", "Potato");
        crop("nether_wart", "Nether Wart"); crop("sugar_cane", "Sugar Cane");
        crop("melon", "Melon"); crop("pumpkin", "Pumpkin"); crop("cocoa", "Cocoa Beans");
        crop("cactus", "Cactus"); crop("red_mushroom", "Mushroom"); crop("brown_mushroom", "Mushroom");
    }

    private static void id(String name, String id) { NAME_TO_ID.put(name, id); }
    private static void crop(String block, String name) { CROP_BLOCKS.put(block, name); }

    public static String idFor(String itemName) {
        String extra = Config.get().extraItems.get(itemName);
        return extra != null ? extra : NAME_TO_ID.get(itemName);
    }

    public static boolean isFarmingItem(String itemName) { return idFor(itemName) != null; }

    public static String cropFor(String blockPath) {
        String extra = Config.get().extraCropBlocks.get(blockPath);
        return extra != null ? extra : CROP_BLOCKS.get(blockPath);
    }

    private Items() {}
}
