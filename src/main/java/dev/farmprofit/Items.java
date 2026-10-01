package dev.farmprofit;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Maps item names to Bazaar IDs, and blocks to crop / ore names. */
public final class Items {
    private static final Map<String, String> NAME_TO_ID = new HashMap<>();
    private static final Map<String, String> CROP_BLOCKS = new HashMap<>();
    private static final Map<String, String> ORE_BLOCKS = new HashMap<>();
    private static final Map<String, String> LOG_BLOCKS = new HashMap<>();

    static {
        // ===== FARMING ITEMS =====
        id("Wheat", "WHEAT"); id("Enchanted Wheat", "ENCHANTED_WHEAT");
        id("Hay Bale", "HAY_BLOCK"); id("Enchanted Hay Bale", "ENCHANTED_HAY_BLOCK");
        id("Seeds", "SEEDS"); id("Enchanted Seeds", "ENCHANTED_SEEDS"); id("Box of Seeds", "BOX_OF_SEEDS");
        id("Carrot", "CARROT_ITEM"); id("Enchanted Carrot", "ENCHANTED_CARROT");
        id("Enchanted Golden Carrot", "ENCHANTED_GOLDEN_CARROT");
        id("Potato", "POTATO_ITEM"); id("Enchanted Potato", "ENCHANTED_POTATO");
        id("Enchanted Baked Potato", "ENCHANTED_BAKED_POTATO");
        id("Poisonous Potato", "POISONOUS_POTATO"); id("Enchanted Poisonous Potato", "ENCHANTED_POISONOUS_POTATO");
        id("Pumpkin", "PUMPKIN"); id("Enchanted Pumpkin", "ENCHANTED_PUMPKIN"); id("Polished Pumpkin", "POLISHED_PUMPKIN");
        id("Melon", "MELON"); id("Melon Slice", "MELON"); id("Enchanted Melon", "ENCHANTED_MELON");
        id("Enchanted Melon Block", "ENCHANTED_MELON_BLOCK");
        id("Sugar Cane", "SUGAR_CANE"); id("Enchanted Sugar", "ENCHANTED_SUGAR");
        id("Enchanted Sugar Cane", "ENCHANTED_SUGAR_CANE");
        id("Cactus", "CACTUS"); id("Enchanted Cactus Green", "ENCHANTED_CACTUS_GREEN");
        id("Enchanted Cactus", "ENCHANTED_CACTUS");
        id("Cocoa Beans", "INK_SACK:3"); id("Enchanted Cocoa Beans", "ENCHANTED_COCOA");
        id("Enchanted Cookie", "ENCHANTED_COOKIE");
        id("Nether Wart", "NETHER_STALK"); id("Enchanted Nether Wart", "ENCHANTED_NETHER_STALK");
        id("Mutant Nether Wart", "MUTANT_NETHER_STALK");
        id("Red Mushroom", "RED_MUSHROOM"); id("Brown Mushroom", "BROWN_MUSHROOM");
        id("Enchanted Red Mushroom", "ENCHANTED_RED_MUSHROOM"); id("Enchanted Brown Mushroom", "ENCHANTED_BROWN_MUSHROOM");
        id("Red Mushroom Block", "HUGE_MUSHROOM_2"); id("Brown Mushroom Block", "HUGE_MUSHROOM_1");
        id("Enchanted Red Mushroom Block", "ENCHANTED_HUGE_MUSHROOM_2");
        id("Enchanted Brown Mushroom Block", "ENCHANTED_HUGE_MUSHROOM_1");
        id("Cropie", "CROPIE"); id("Squash", "SQUASH"); id("Fermento", "FERMENTO");
        id("Compost", "COMPOST"); id("Dung", "DUNG"); id("Honey Jar", "HONEY_JAR");
        id("Plant Matter", "PLANT_MATTER"); id("Tasty Cheese", "CHEESE_FUEL");
        // Pest drops
        id("Biofuel", "BIOFUEL"); id("Beady Eyes", "BEADY_EYES"); id("Clipped Wings", "CLIPPED_WINGS");
        id("Chirping Stereo", "CHIRPING_STEREO"); id("Atmospheric Filter", "ATMOSPHERIC_FILTER");
        id("Wriggling Larva", "WRIGGLING_LARVA"); id("Overclocker 3000", "OVERCLOCKER_3000");
        id("Pesthunting Guide", "PESTHUNTING_GUIDE");

        // ===== MINING ITEMS =====
        id("Cobblestone", "COBBLESTONE"); id("Enchanted Cobblestone", "ENCHANTED_COBBLESTONE");
        id("Coal", "COAL"); id("Enchanted Coal", "ENCHANTED_COAL"); id("Enchanted Block of Coal", "ENCHANTED_COAL_BLOCK");
        id("Iron Ingot", "IRON_INGOT"); id("Enchanted Iron", "ENCHANTED_IRON"); id("Enchanted Iron Block", "ENCHANTED_IRON_BLOCK");
        id("Gold Ingot", "GOLD_INGOT"); id("Enchanted Gold", "ENCHANTED_GOLD"); id("Enchanted Gold Block", "ENCHANTED_GOLD_BLOCK");
        id("Diamond", "DIAMOND"); id("Enchanted Diamond", "ENCHANTED_DIAMOND"); id("Enchanted Diamond Block", "ENCHANTED_DIAMOND_BLOCK");
        id("Emerald", "EMERALD"); id("Enchanted Emerald", "ENCHANTED_EMERALD"); id("Enchanted Emerald Block", "ENCHANTED_EMERALD_BLOCK");
        id("Lapis Lazuli", "INK_SACK:4"); id("Enchanted Lapis Lazuli", "ENCHANTED_LAPIS_LAZULI");
        id("Enchanted Lapis Block", "ENCHANTED_LAPIS_LAZULI_BLOCK");
        id("Redstone", "REDSTONE"); id("Enchanted Redstone", "ENCHANTED_REDSTONE");
        id("Enchanted Redstone Block", "ENCHANTED_REDSTONE_BLOCK");
        id("Mithril", "MITHRIL_ORE"); id("Enchanted Mithril", "ENCHANTED_MITHRIL"); id("Refined Mithril", "REFINED_MITHRIL");
        id("Titanium", "TITANIUM_ORE"); id("Enchanted Titanium", "ENCHANTED_TITANIUM"); id("Refined Titanium", "REFINED_TITANIUM");
        id("Hard Stone", "HARD_STONE"); id("Enchanted Hard Stone", "ENCHANTED_HARD_STONE");
        id("Concentrated Stone", "CONCENTRATED_STONE");
        id("Sulphur", "SULPHUR_ORE"); id("Enchanted Sulphur", "ENCHANTED_SULPHUR");
        id("Glacite", "GLACITE"); id("Enchanted Glacite", "ENCHANTED_GLACITE");
        id("Tungsten", "TUNGSTEN"); id("Enchanted Tungsten", "ENCHANTED_TUNGSTEN");
        id("Umber", "UMBER"); id("Enchanted Umber", "ENCHANTED_UMBER");
        // ===== FORAGING ITEMS =====
        id("Oak Wood", "LOG"); id("Spruce Wood", "LOG:1"); id("Birch Wood", "LOG:2"); id("Jungle Wood", "LOG:3");
        id("Acacia Wood", "LOG_2"); id("Dark Oak Wood", "LOG_2:1");
        id("Enchanted Oak Wood", "ENCHANTED_OAK_LOG"); id("Enchanted Spruce Wood", "ENCHANTED_SPRUCE_LOG");
        id("Enchanted Birch Wood", "ENCHANTED_BIRCH_LOG"); id("Enchanted Jungle Wood", "ENCHANTED_JUNGLE_LOG");
        id("Enchanted Acacia Wood", "ENCHANTED_ACACIA_LOG"); id("Enchanted Dark Oak Wood", "ENCHANTED_DARK_OAK_LOG");
        id("Fig Log", "FIG_LOG"); id("Enchanted Fig Log", "ENCHANTED_FIG_LOG");
        id("Mangrove Log", "MANGROVE_LOG"); id("Enchanted Mangrove Log", "ENCHANTED_MANGROVE_LOG");
        id("Deep Root", "DEEP_ROOT"); id("Lushlilac", "LUSHLILAC"); id("Sea Lumies", "SEA_LUMIES");
        id("Starfall", "STARFALL"); id("Treasurite", "TREASURITE");
        for (String gem : new String[]{"Ruby", "Amber", "Sapphire", "Jade", "Amethyst", "Topaz", "Jasper", "Opal",
                "Aquamarine", "Citrine", "Peridot", "Onyx"}) {
            for (String tier : new String[]{"Rough", "Flawed", "Fine", "Flawless", "Perfect"}) {
                id(tier + " " + gem + " Gemstone", tier.toUpperCase() + "_" + gem.toUpperCase() + "_GEM");
            }
        }

        // ===== CROP BLOCKS =====
        crop("wheat", "Wheat"); crop("carrots", "Carrot"); crop("potatoes", "Potato");
        crop("nether_wart", "Nether Wart"); crop("sugar_cane", "Sugar Cane");
        crop("melon", "Melon"); crop("pumpkin", "Pumpkin"); crop("cocoa", "Cocoa Beans");
        crop("cactus", "Cactus"); crop("red_mushroom", "Mushroom"); crop("brown_mushroom", "Mushroom");

        // ===== ORE BLOCKS =====
        for (String b : new String[]{"gray_wool", "light_blue_wool", "cyan_terracotta",
                "prismarine", "prismarine_bricks", "dark_prismarine"}) ore(b, "Mithril");
        ore("polished_diorite", "Titanium");
        ore("stone", "Hard Stone"); ore("cobblestone", "Cobblestone");
        ore("coal_ore", "Coal"); ore("coal_block", "Coal");
        ore("iron_ore", "Iron"); ore("iron_block", "Iron");
        ore("gold_ore", "Gold"); ore("gold_block", "Gold");
        ore("diamond_ore", "Diamond"); ore("diamond_block", "Diamond");
        ore("emerald_ore", "Emerald"); ore("emerald_block", "Emerald");
        ore("lapis_ore", "Lapis"); ore("lapis_block", "Lapis");
        ore("redstone_ore", "Redstone"); ore("redstone_block", "Redstone");
        ore("sponge", "Sulphur");
        ore("packed_ice", "Glacite");
        ore("clay", "Tungsten");
        ore("terracotta", "Umber"); ore("brown_terracotta", "Umber"); ore("smooth_red_sandstone", "Umber");
        // ===== LOG BLOCKS (foraging) =====
        for (String w : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"}) {
            String name = Character.toUpperCase(w.charAt(0)) + w.substring(1).replace("_o", " O");
            log(w + "_log", name); log(w + "_wood", name);
        }
        log("stripped_spruce_log", "Fig"); log("stripped_spruce_wood", "Fig");
        log("mangrove_log", "Mangrove"); log("mangrove_wood", "Mangrove"); log("mangrove_roots", "Mangrove");
        log("muddy_mangrove_roots", "Mangrove");

        gem("red", "Ruby"); gem("orange", "Amber"); gem("light_blue", "Sapphire"); gem("lime", "Jade");
        gem("purple", "Amethyst"); gem("yellow", "Topaz"); gem("magenta", "Jasper"); gem("white", "Opal");
        gem("blue", "Aquamarine"); gem("brown", "Citrine"); gem("green", "Peridot"); gem("black", "Onyx");
    }

    private static void id(String name, String id) { NAME_TO_ID.put(name, id); }
    private static void crop(String block, String name) { CROP_BLOCKS.put(block, name); }
    private static void ore(String block, String name) { ORE_BLOCKS.put(block, name); }
    private static void log(String block, String name) { LOG_BLOCKS.put(block, name); }
    private static void gem(String color, String name) {
        ore(color + "_stained_glass", name);
        ore(color + "_stained_glass_pane", name);
    }

    public static String idFor(String itemName) {
        String extra = Config.get().extraItems.get(itemName);
        return extra != null ? extra : NAME_TO_ID.get(itemName);
    }

    private static final Pattern BOOK = Pattern.compile("^Enchanted Book \\((.+?) ([IVX]+)\\)$");
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    /** Best guess at a SkyBlock item ID from its name, e.g. "Overclocker 3000" -> OVERCLOCKER_3000. */
    public static String guessId(String name) {
        Matcher b = BOOK.matcher(name);
        if (b.matches()) {
            int level = 0;
            for (int i = 0; i < ROMAN.length; i++) if (ROMAN[i].equals(b.group(2))) level = i + 1;
            return "ENCHANTMENT_" + simple(b.group(1)) + "_" + level;
        }
        return simple(name);
    }

    private static String simple(String s) {
        return s.toUpperCase(Locale.ROOT).replace("'", "").replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static boolean isTracked(String itemName) { return idFor(itemName) != null; }

    public static String cropFor(String blockPath) {
        String extra = Config.get().extraCropBlocks.get(blockPath);
        return extra != null ? extra : CROP_BLOCKS.get(blockPath);
    }

    /** What block you're mining/chopping, depending on the area. */
    public static String blockFor(String blockPath, String area) {
        if (Tracker.FORAGING.equals(area)) {
            String extra = Config.get().extraLogBlocks.get(blockPath);
            return extra != null ? extra : LOG_BLOCKS.get(blockPath);
        }
        return oreFor(blockPath, area);
    }

    public static String oreFor(String blockPath, String area) {
        String name = Config.get().extraOreBlocks.get(blockPath);
        if (name == null) name = ORE_BLOCKS.get(blockPath);
        if ("Hard Stone".equals(name) && Tracker.DWARVEN.equals(area)) return "Stone";
        return name;
    }

    private Items() {}
}
