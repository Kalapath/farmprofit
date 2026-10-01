package dev.farmprofit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Settings saved in .minecraft/config/farmprofit/config.json */
public final class Config {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("farmprofit");
    private static final Path FILE = DIR.resolve("config.json");
    private static Config instance;

    /** Minutes without activity before a session resets. */
    @Setting(category = "General", label = "Reset after (minutes)", desc = "Minutes without activity before a session ends and is saved to history.", min = 1, max = 600)
    public int resetMinutes = 15;
    /**
     * "instasell" = Bazaar instant-sell, "sellorder" = Bazaar sell offer,
     * "npc" = NPC sell price, "best" = whichever of instasell / NPC pays more.
     */
    @Setting(category = "General", label = "Price source", desc = "best = Bazaar instasell or NPC, whichever pays more. instasell / sellorder = Bazaar. npc = NPC sell price.", options = {"best", "instasell", "sellorder", "npc"})
    public String priceMode = "best";
    /** The session timer pauses after this many seconds without activity (AFK doesn't ruin your /h). */
    @Setting(category = "General", label = "Pause timer after (seconds)", desc = "The session clock pauses after this long without activity, so AFK time doesn't lower profit/h.", min = 5, max = 600)
    public int pauseSeconds = 30;
    /** On the HUD, items worth less than this (in total) are grouped into one "cheap items" line. */
    @Setting(category = "HUD", label = "Group items cheaper than", desc = "Items worth less than this (in total) are grouped into one 'cheap items' line.", min = 0, max = 1000000000000.0)
    public double minItemValue = 1000;
    /** Items you never want counted (e.g. "Hay Bale"). Add with /profit ignore <item> */
    @Setting(category = "Items & areas", label = "Ignored items", desc = "Comma separated. These never count toward profit. Right-click an item on the HUD (chat open) to add it.")
    public java.util.List<String> ignoredItems = new java.util.ArrayList<>();
    @Setting(category = "HUD", label = "Show HUD", desc = "Turn the whole HUD on or off.")
    public boolean hudEnabled = true;
    @Setting(category = "Farming", label = "Farming HUD", desc = "Show the Farming HUD (tracking keeps running when hidden).")
    public boolean showFarmingHud = true;
    @Setting(category = "Mining", label = "Mining HUD", desc = "Show the Mining HUD (tracking keeps running when hidden).")
    public boolean showMiningHud = true;
    @Setting(category = "Foraging", label = "Foraging HUD", desc = "Show the Foraging HUD (tracking keeps running when hidden).")
    public boolean showForagingHud = true;
    @Setting(category = "Fishing", label = "Fishing HUD", desc = "Show the Fishing HUD (tracking keeps running when hidden).")
    public boolean showFishingHud = true;
    @Setting(category = "Combat & Slayers", label = "Combat HUD", desc = "Show the Combat / slayer HUD (tracking keeps running when hidden).")
    public boolean showCombatHud = true;
    @Setting(category = "Dungeons", label = "Catacombs HUD", desc = "Show the Catacombs HUD (tracking keeps running when hidden).")
    public boolean showDungeonsHud = true;
    @Setting(category = "Kuudra", label = "Kuudra HUD", desc = "Show the Kuudra HUD (tracking keeps running when hidden).")
    public boolean showKuudraHud = true;
    @Setting(category = "Diana", label = "Diana HUD", desc = "Show the Diana (Mythological Ritual) HUD (tracking keeps running when hidden).")
    public boolean showDianaHud = true;
    @Setting(category = "HUD", label = "Show all-time line", desc = "Adds your all-time profit and profit/h for the activity under the session numbers.")
    public boolean hudShowTotal = false;
    @Setting(category = "HUD", label = "Show mayor", desc = "Shows the mayor on the HUD of the activity they boost.")
    public boolean showMayor = true;
    @Setting(category = "HUD", label = "Price tooltips", desc = "Adds Bazaar, lowest BIN and NPC prices to item tooltips.")
    public boolean priceTooltips = true;
    @Setting(category = "Farming", label = "Show Jacob's contests", desc = "Next contest crops and countdown on the Farming HUD.")
    public boolean showContests = true;
    @Setting(category = "Farming", label = "Contest data URL", desc = "Where upcoming contests come from (community data; Hypixel doesn't publish them). Empty = off.")
    public String jacobContestsUrl = "https://api.elitebot.dev/contests/at/now";
    @Setting(category = "Farming", label = "Copper value (coins)", desc = "Coins each copper from Garden visitors is worth to you. 0 = don't count copper.", min = 0, max = 1e9)
    public double copperValue = 0;
    @Setting(category = "Bazaar flipping", label = "Orders panel X", desc = "-1 = right under the main HUD. Or drag it with chat open.", min = -1, max = 10000)
    public int bazaarHudX = -1;
    @Setting(category = "Bazaar flipping", label = "Orders panel Y", desc = "-1 = right under the main HUD. Or drag it with chat open.", min = -1, max = 10000)
    public int bazaarHudY = -1;
    @Setting(category = "Bazaar flipping", label = "Warn about odd orders", desc = "Chat warning when an order you place is far from the market price (possible typo).")
    public boolean bzWarnMistakes = true;
    @Setting(category = "HUD", label = "HUD X position", desc = "Or drag the HUD with chat open.", min = 0, max = 10000)
    public int hudX = 5;
    @Setting(category = "HUD", label = "HUD Y position", desc = "Or drag the HUD with chat open.", min = 0, max = 10000)
    public int hudY = 5;
    @Setting(category = "HUD", label = "Items shown", desc = "How many item lines the HUD lists before 'and X more'.", min = 1, max = 50)
    public int hudMaxItems = 6;
    @Setting(category = "HUD", label = "HUD scale", desc = "0.5 to 3.", min = 0.5, max = 3)
    public double hudScale = 1.0;
    @Setting(category = "HUD", label = "Item icons", desc = "Small icons next to items you've had in your inventory.")
    public boolean hudIcons = true;
    @Setting(category = "Mining", label = "Show commissions", desc = "Commissions on the Mining HUD.")
    public boolean showCommissions = true;
    @Setting(category = "HUD", label = "Show rare drops", desc = "Rare drops section on the HUD.")
    public boolean showRareDrops = true;
    @Setting(category = "HUD", label = "Show shards", desc = "Attribute shards line on the HUD.")
    public boolean showShards = true;
    /** false = HUD only shows profit lines (no stats, powder, commissions...). Toggle with /profit details */
    @Setting(category = "HUD", label = "Show details", desc = "Stats, powder, commissions, BPS... Off = profit lines only.")
    public boolean hudDetails = true;
    /** "Best now" line on the farming and mining HUD. */
    @Setting(category = "HUD", label = "Show 'Best now' tip", desc = "Best crop / ore to farm right now on the Farming and Mining HUDs.")
    public boolean showSuggestion = true;
    /** Dungeon secret finder on the Catacombs HUD. */
    @Setting(category = "Dungeons", label = "Dungeon secret finder", desc = "Room counter and nearby secret list on the Catacombs HUD.")
    public boolean secretFinder = true;
    @Setting(category = "Dungeons", label = "Chest profit in chat", desc = "When you open a reward chest or Croesus, show each chest's value, cost and profit.")
    public boolean chestProfit = true;
    @Setting(category = "Items & areas", label = "Count items in menus", desc = "Comma separated menu titles where items you receive count as profit (reward chests).")
    public java.util.List<String> countInMenus = new java.util.ArrayList<>(java.util.List.of(
            "Wood Chest", "Gold Chest", "Diamond Chest", "Emerald Chest", "Obsidian Chest", "Bedrock Chest"));
    /** Blocks per second used for farming suggestions until you've farmed for a minute. */
    @Setting(category = "Farming", label = "Default blocks/s", desc = "Used for farming suggestions until you've farmed for a minute.", min = 1, max = 40)
    public double defaultBps = 19;
    /** Share of the theoretical mining rate you really get (walking, aiming, abilities...). */
    @Setting(category = "Mining", label = "Mining efficiency", desc = "Share of the theoretical mining rate you really get (walking, aiming). 0.6 = 60%.", min = 0.05, max = 1)
    public double miningEfficiency = 0.6;

    // ----- Bazaar flipping (/flips) -----
    @Setting(category = "Bazaar flipping", label = "Budget", desc = "Coins you want to put into flips.", min = 0, max = 10000000000000.0)
    public double bzBudget = 10_000_000;
    @Setting(category = "Bazaar flipping", label = "Min weekly volume", desc = "Higher = flips fill faster.", min = 0, max = 1000000000000.0)
    public double bzMinVolume = 20_000;      // weekly
    @Setting(category = "Bazaar flipping", label = "Min margin %", desc = "After tax.", min = 0, max = 1000)
    public double bzMinMargin = 1;           // % after tax
    @Setting(category = "Bazaar flipping", label = "Max item price", desc = "0 = no limit.", min = 0, max = 1000000000000.0)
    public double bzMaxPrice = 0;            // 0 = no limit
    @Setting(category = "Bazaar flipping", label = "Bazaar tax %", desc = "Lower with the Bazaar Flipper upgrade.", min = 0, max = 10)
    public double bzTax = 1.25;              // % (lower with the Bazaar Flipper upgrade)
    @Setting(category = "Bazaar flipping", label = "Your volume share %", desc = "How much of an item's trading you expect to get (competition estimate).", min = 0, max = 100)
    public double bzShare = 10;              // % of the item's volume you expect to get
    @Setting(category = "Bazaar flipping", label = "Hot flip alert (coins/h)", desc = "Ping when a new flip beats this. 0 = off.", min = 0, max = 1000000000000.0)
    public double bzFlipAlert = 0;           // ping when a flip beats this many coins/h (0 = off)
    @Setting(category = "Bazaar flipping", label = "Flips listed", desc = "How many flips /flips shows.", min = 1, max = 50)
    public int bzTop = 10;
    @Setting(category = "Bazaar flipping", label = "Order check every (s)", desc = "How often prices are checked while you have orders out.", min = 10, max = 600)
    public int bzRefreshSeconds = 20;        // price checks while you have orders out
    @Setting(category = "Bazaar flipping", label = "Show orders on HUD", desc = "Your open bazaar orders under the HUD.")
    public boolean bazaarHud = true;
    @Setting(category = "Bazaar flipping", label = "Alert sound", desc = "Ding when you're outbid/undercut or an order fills.")
    public boolean bzSound = true;
    /** Coins subtracted from profit every time a slayer quest starts (set to what your tier costs). */
    @Setting(category = "Combat & Slayers", label = "Slayer quest cost", desc = "Coins subtracted per slayer quest. 0 = detect automatically from your purse.", min = 0, max = 1000000000.0)
    public double slayerQuestCost = 0;
    /** Tab-list "Area:" names that count as mining. */
    @Setting(category = "Mining", label = "Mining areas", desc = "Comma separated tab-list Area names that use the Mining HUD.")
    public java.util.List<String> miningAreas = new java.util.ArrayList<>(java.util.List.of(
            "Dwarven Mines", "Crystal Hollows", "Mineshaft", "Glacite", "Deep Caverns", "Gold Mine"));
    /** Lowest-BIN prices for auction-house items (rare drops). Set to "" to disable. */
    @Setting(category = "General", label = "Lowest BIN price URL", desc = "Where auction-house prices come from. Leave empty to turn auction prices off.")
    public String lowestBinUrl = "https://moulberry.codes/lowestbin.json";
    /** "Item Name": "BAZAAR_ID" */
    @Setting(category = "Items & areas", label = "Extra item prices", desc = "name=BAZAAR_ID, separated by commas. For items the mod can't price.")
    public Map<String, String> extraItems = new HashMap<>();
    /** "block_id": "Crop Name" */
    @Setting(category = "Items & areas", label = "Extra crop blocks", desc = "block_id=Crop Name, separated by commas.")
    public Map<String, String> extraCropBlocks = new HashMap<>();
    /** "block_id": "Ore Name" */
    @Setting(category = "Items & areas", label = "Extra ore blocks", desc = "block_id=Ore Name, separated by commas.")
    public Map<String, String> extraOreBlocks = new HashMap<>();
    /** "block_id": "Wood Name" */
    @Setting(category = "Items & areas", label = "Extra log blocks", desc = "block_id=Wood Name, separated by commas.")
    public Map<String, String> extraLogBlocks = new HashMap<>();
    /** Tab-list "Area:" names that count as foraging islands. */
    @Setting(category = "Foraging", label = "Foraging areas", desc = "Comma separated tab-list Area names that use the Foraging HUD.")
    public java.util.List<String> foragingAreas = new java.util.ArrayList<>(java.util.List.of("Galatea", "The Park"));

    /** Whether the HUD for this activity is switched on. */
    public boolean hudFor(String type) {
        if (Tracker.isMiningType(type)) return showMiningHud;
        return switch (type) {
            case Tracker.FORAGING -> showForagingHud;
            case Tracker.FISHING -> showFishingHud;
            case Tracker.COMBAT -> showCombatHud;
            case Tracker.DUNGEONS -> showDungeonsHud;
            case Tracker.KUUDRA -> showKuudraHud;
            case Tracker.DIANA -> showDianaHud;
            default -> showFarmingHud;
        };
    }

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) instance = GSON.fromJson(Files.readString(FILE), Config.class);
        } catch (Exception e) {
            FarmProfitClient.LOG.warn("Could not read config, using defaults", e);
        }
        if (instance == null) instance = new Config();
        if (instance.extraItems == null) instance.extraItems = new HashMap<>();
        if (instance.extraCropBlocks == null) instance.extraCropBlocks = new HashMap<>();
        if (instance.extraOreBlocks == null) instance.extraOreBlocks = new HashMap<>();
        if (instance.countInMenus == null) instance.countInMenus = new java.util.ArrayList<>(java.util.List.of(
                "Wood Chest", "Gold Chest", "Diamond Chest", "Emerald Chest", "Obsidian Chest", "Bedrock Chest"));
        if (instance.ignoredItems == null) instance.ignoredItems = new java.util.ArrayList<>();
        if (instance.miningAreas == null) instance.miningAreas = new java.util.ArrayList<>(java.util.List.of(
                "Dwarven Mines", "Crystal Hollows", "Mineshaft", "Glacite", "Deep Caverns", "Gold Mine"));
        if (instance.extraLogBlocks == null) instance.extraLogBlocks = new HashMap<>();
        if (instance.foragingAreas == null) instance.foragingAreas = new java.util.ArrayList<>(java.util.List.of("Galatea", "The Park"));
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (Exception e) {
            FarmProfitClient.LOG.warn("Could not save config", e);
        }
    }
}
