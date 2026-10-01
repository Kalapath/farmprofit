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

    /** Minutes without breaking a crop / killing a pest before the session resets. */
    public int resetMinutes = 15;
    /** "instasell" = Bazaar instant-sell price, "sellorder" = sell-offer price. */
    public String priceMode = "instasell";
    public boolean hudEnabled = true;
    public int hudX = 5;
    public int hudY = 5;
    public int hudMaxItems = 6;
    /** Add your own items here: "Item Name": "BAZAAR_ID" */
    public Map<String, String> extraItems = new HashMap<>();
    /** Add your own crop blocks here: "block_id": "Crop Name" */
    public Map<String, String> extraCropBlocks = new HashMap<>();

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                instance = GSON.fromJson(Files.readString(FILE), Config.class);
            }
        } catch (Exception e) {
            FarmProfitClient.LOG.warn("Could not read config, using defaults", e);
        }
        if (instance == null) instance = new Config();
        if (instance.extraItems == null) instance.extraItems = new HashMap<>();
        if (instance.extraCropBlocks == null) instance.extraCropBlocks = new HashMap<>();
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
