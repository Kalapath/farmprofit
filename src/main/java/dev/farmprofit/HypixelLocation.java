package dev.farmprofit;

/**
 * Exact location from Hypixel through the official Hypixel Mod API (a separate mod).
 * Only touched if that mod is installed; otherwise the tab list is used.
 */
public final class HypixelLocation {
    public static volatile boolean active;
    public static volatile String serverType, mode, map;

    public static void init() {
        try {
            HypixelApiBridge.register();
            active = true;
        } catch (Throwable t) {
            FarmProfitClient.LOG.warn("Hypixel Mod API found but couldn't be used", t);
        }
    }

    /** Maps Hypixel's mode to one of our HUDs, or null if it doesn't decide. */
    public static String activity() {
        String m = mode;
        if (m == null) return null;
        m = m.toLowerCase(java.util.Locale.ROOT);
        if (m.equals("garden")) return Tracker.FARMING;
        if (m.startsWith("mining_") || m.equals("crystal_hollows") || m.equals("mineshaft")) return Tracker.MINING;
        if (m.startsWith("foraging_") || m.equals("galatea")) return Tracker.FORAGING;
        if (m.equals("dungeon")) return Tracker.DUNGEONS;
        if (m.equals("kuudra")) return Tracker.KUUDRA;
        return "";   // known place that isn't a special HUD area
    }

    private HypixelLocation() {}
}
