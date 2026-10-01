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
    public int resetMinutes = 15;
    /** "instasell" = Bazaar instant-sell price, "sellorder" = sell-offer price. */
    public String priceMode = "instasell";
    public boolean hudEnabled = true;
    public int hudX = 5;
    public int hudY = 5;
    public int hudMaxItems = 6;
    public boolean showCommissions = true;
    public boolean showRareDrops = true;
    public boolean showShards = true;
    /** Lowest-BIN prices for auction-house items (rare drops). Set to "" to disable. */
    public String lowestBinUrl = "https://moulberry.codes/lowestbin.json";
    /** "Item Name": "BAZAAR_ID" */
    public Map<String, String> extraItems = new HashMap<>();
    /** "block_id": "Crop Name" */
    public Map<String, String> extraCropBlocks = new HashMap<>();
    /** "block_id": "Ore Name" */
    public Map<String, String> extraOreBlocks = new HashMap<>();
    /** "block_id": "Wood Name" */
    public Map<String, String> extraLogBlocks = new HashMap<>();
    /** Tab-list "Area:" names that count as foraging islands. */
    public java.util.List<String> foragingAreas = new java.util.ArrayList<>(java.util.List.of("Galatea", "The Park"));

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
