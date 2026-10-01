package dev.farmprofit;

import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Position, scale and visibility of every HUD panel (saved in panels.json).
 * Panels: main, bazaar, secrets, contest, suggest. The main panel can have its own spot per activity.
 */
public final class Panels {
    public static final List<String> ALL = List.of("main", "bazaar", "secrets", "contest", "suggest");

    public static final class Pos {
        public int x = -1, y = -1;        // -1 = stack under the previous panel
        public double scale = 1.0;
        public boolean hidden;
    }

    private static final Path FILE = Config.DIR.resolve("panels.json");
    private static Map<String, Pos> map;

    public static String title(String id) {
        return switch (id) {
            case "bazaar" -> "Bazaar orders";
            case "secrets" -> "Dungeon secrets";
            case "contest" -> "Jacob's contest";
            case "suggest" -> "Best now";
            default -> "Profit HUD";
        };
    }

    /** Key for this panel: the main panel gets one per activity if that setting is on. */
    public static String key(String id, String activity) {
        if (id.equals("main") && Config.get().perActivityPositions && activity != null) return "main:" + Tracker.normalType(activity);
        return id;
    }

    public static Pos get(String key) {
        if (map == null) load();
        Pos p = map.get(key);
        if (p == null && key.startsWith("main:")) {          // first time for this activity: copy the shared spot
            Pos base = map.get("main");
            p = new Pos();
            if (base != null) { p.x = base.x; p.y = base.y; p.scale = base.scale; p.hidden = base.hidden; }
            else { p.x = Config.get().hudX; p.y = Config.get().hudY; p.scale = Config.get().hudScale; }
            map.put(key, p);
        }
        if (p == null) {
            p = new Pos();
            if (key.equals("main")) { p.x = Config.get().hudX; p.y = Config.get().hudY; p.scale = Config.get().hudScale; }
            map.put(key, p);
        }
        return p;
    }

    public static void save() {
        try {
            Files.createDirectories(Config.DIR);
            Files.writeString(FILE, Config.GSON.toJson(map));
        } catch (Exception e) {
            FarmProfitClient.LOG.warn("Could not save panels", e);
        }
    }

    public static void reset() {
        map = new LinkedHashMap<>();
        save();
    }

    private static void load() {
        try {
            if (Files.exists(FILE)) map = Config.GSON.fromJson(Files.readString(FILE), new TypeToken<LinkedHashMap<String, Pos>>() {}.getType());
        } catch (Exception e) {
            FarmProfitClient.LOG.warn("Could not read panels", e);
        }
        if (map == null) map = new LinkedHashMap<>();
    }

    private Panels() {}
}
