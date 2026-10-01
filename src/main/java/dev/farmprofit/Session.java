package dev.farmprofit;

import java.util.LinkedHashMap;
import java.util.Map;

/** One farming or mining session. Public fields so it saves straight to JSON. */
public final class Session {
    /** "Farming", "Dwarven Mines", "Crystal Hollows" or "Glacite Mineshaft" */
    public String type = Tracker.FARMING;
    public long start;
    public long lastActivity;
    public long end;                                   // 0 while running
    public Map<String, Integer> breaks = new LinkedHashMap<>();
    public Map<String, Long> items = new LinkedHashMap<>();
    public Map<String, Long> powder = new LinkedHashMap<>();
    /** Items announced in chat (RARE DROP!, PET DROP!, RARE CROP! ...) */
    public Map<String, Integer> rareDrops = new LinkedHashMap<>();
    /** Pest kills by pest type */
    public Map<String, Integer> pests = new LinkedHashMap<>();
    public int pristine;
    public int treeGifts;
    /** Attribute shards sent to the Hunting Box */
    public Map<String, Integer> shards = new LinkedHashMap<>();
    public int pestActions;
    public String fortune;
    // Filled in when the session ends (prices frozen at that moment)
    public double profit;
    public double profitPerHour;
    public String mainCrop;                            // main crop / main ore

    public Session() {}

    public Session(String type, long now) {
        this.type = type;
        this.start = now;
        this.lastActivity = now;
    }

    public boolean isMining() {
        return Tracker.DWARVEN.equals(type) || Tracker.HOLLOWS.equals(type) || Tracker.MINESHAFT.equals(type);
    }

    public boolean isForaging() { return Tracker.FORAGING.equals(type); }

    public void addBreak(String what) { breaks.merge(what, 1, Integer::sum); }

    public void addItem(String name, long delta) {
        long v = items.merge(name, delta, Long::sum);
        if (v == 0) items.remove(name);
    }

    public void addPowder(String kind, long amount) { powder.merge(kind, amount, Long::sum); }

    public long durationMs(long now) { return Math.max(0, (end > 0 ? end : now) - start); }

    public double hours(long now) { return durationMs(now) / 3_600_000.0; }

    public int totalBreaks() {
        int t = 0;
        for (int v : breaks.values()) t += v;
        return t;
    }

    public String mainCrop() {
        if (mainCrop != null) return mainCrop;
        String best = null;
        int max = -1;
        for (var e : breaks.entrySet()) if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        return best == null ? "Nothing yet" : best;
    }

    public double value() {
        double total = 0;
        for (var e : items.entrySet()) total += e.getValue() * Prices.price(e.getKey());
        if (rareDrops != null) for (var e : rareDrops.entrySet()) total += rareValue(e.getKey(), e.getValue());
        total += shardValue();
        return total;
    }

    /** True if this rare drop was already counted through the inventory or sacks. */
    public boolean rareAlreadyCounted(String name) { return items.containsKey(name); }

    /** Value a rare drop adds to profit (0 if it was already counted as a normal item). */
    public double rareValue(String name, int count) {
        return rareAlreadyCounted(name) ? 0 : count * Prices.price(name);
    }

    public double shardValue() {
        double total = 0;
        if (shards != null) for (var e : shards.entrySet()) total += e.getValue() * Prices.price(e.getKey());
        return total;
    }

    public int totalShards() {
        int t = 0;
        if (shards != null) for (int v : shards.values()) t += v;
        return t;
    }

    public int totalPests() {
        int t = 0;
        if (pests != null) for (int v : pests.values()) t += v;
        return t;
    }

    public double perHour(long now) {
        double h = hours(now);
        return h < 1.0 / 60 ? 0 : value() / h;
    }

    public double breaksPerSecond(long now) {
        double sec = durationMs(now) / 1000.0;
        return sec < 1 ? 0 : totalBreaks() / sec;
    }

    /** Freeze values and close the session. Duration excludes the idle time at the end. */
    public void finish() {
        end = lastActivity;
        mainCrop = mainCrop();
        profit = value();
        profitPerHour = perHour(end);
    }
}
