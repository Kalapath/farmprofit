package dev.farmprofit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds the HUD text. Each area gets its own layout. */
public final class Hud {

    public static String title(String type) {
        return switch (type) {
            case Tracker.DWARVEN -> "§2§l⛏ Dwarven Mines";
            case Tracker.HOLLOWS -> "§d§l⛏ Crystal Hollows";
            case Tracker.MINESHAFT -> "§b§l⛏ Glacite Mineshaft";
            case Tracker.FORAGING -> "§a§l♣ Foraging" + (Tracker.areaName != null && Tracker.FORAGING.equals(Tracker.area) ? " §7(" + Tracker.areaName + ")" : "");
            default -> "§6§lFarming Profit";
        };
    }

    public static List<String> lines() {
        String type = Tracker.shownType();
        Session s = Tracker.shown();
        List<String> out = new ArrayList<>();

        if (s == null) {
            boolean inGarden = "Garden".equals(Tracker.tab.get("Area"));
            if (Tracker.area == null && !inGarden) return out;     // nothing to show here
            out.add(title(type));
            out.add(Tracker.area == null ? "§7Break a crop to start tracking"
                    : Tracker.FORAGING.equals(Tracker.area) ? "§7Start chopping to begin tracking" : "§7Start mining to begin tracking");
            addCommissions(out, type);
            return out;
        }

        long now = System.currentTimeMillis();
        String main = s.mainCrop();
        out.add(title(type));
        out.add("§7Time: §f" + Fmt.duration(s.durationMs(now)));
        String verb = s.isMining() ? "Mining" : s.isForaging() ? "Chopping" : "Farming";
        String unit = s.isMining() ? " blocks, " : s.isForaging() ? " logs, " : " broken, ";
        out.add("§7" + verb + ": §a" + main + " §8(" + Fmt.num(s.totalBreaks())
                + unit + String.format(Locale.US, "%.1f", s.breaksPerSecond(now)) + " BPS)");
        if (s.isForaging() && s.treeGifts > 0) {
            double min = s.durationMs(now) / 60_000.0;
            out.add("§7Trees: §a" + Fmt.num(s.treeGifts) + " gifts" + (min < 1 ? "" : String.format(Locale.US, " §8(%.1f/min)", s.treeGifts / min)));
        }

        addStats(out, type, main);
        addPowder(out, s, type, now);

        if (!Prices.loaded()) out.add("§cLoading Bazaar prices...");
        out.add("§7Profit: §6" + Fmt.coins(s.value()) + " coins");
        out.add("§7Profit/h: §6" + (s.durationMs(now) < 60_000 ? "§8wait 1 min" : Fmt.coins(s.perHour(now)) + "/h"));

        if (!s.isMining() && s.totalPests() > 0) {
            var pests = new ArrayList<>(s.pests.entrySet());
            pests.sort((a, b) -> b.getValue() - a.getValue());
            StringBuilder sb = new StringBuilder("§7Pests: §c" + s.totalPests() + " killed §8(");
            for (int i = 0; i < Math.min(3, pests.size()); i++) {
                if (i > 0) sb.append(", ");
                sb.append(pests.get(i).getKey()).append(" ").append(pests.get(i).getValue());
            }
            out.add(sb.append(pests.size() > 3 ? ", ...)" : ")").toString());
        }
        if (s.isMining() && !Tracker.DWARVEN.equals(type)) {
            out.add("§7Pristine procs: §d" + s.pristine);
        }
        addShards(out, s);
        addRareDrops(out, s);

        // Crystal Hollows / Mineshaft: show which gemstones / ores you've been hitting
        if ((s.isForaging() || (s.isMining() && !Tracker.DWARVEN.equals(type))) && s.breaks.size() > 1) {
            out.add("§7Blocks:");
            var breaks = new ArrayList<>(s.breaks.entrySet());
            breaks.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(4, breaks.size()); i++) {
                out.add(" §f" + Fmt.num(breaks.get(i).getValue()) + " §d" + breaks.get(i).getKey());
            }
        }

        var items = sortedItems(s);
        if (!items.isEmpty()) out.add(s.isMining() ? "§7Mined:" : s.isForaging() ? "§7Chopped:" : "§7Farmed:");
        int max = Config.get().hudMaxItems;
        for (int i = 0; i < Math.min(max, items.size()); i++) out.add(itemLine(items.get(i)));
        if (items.size() > max) out.add("§8 ...and " + (items.size() - max) + " more (/" + command(type) + ")");

        addCommissions(out, type);

        long idle = Tracker.idleMs(s);
        if (idle > 20_000) out.add("§eIdle - resets in " + Fmt.clock(Tracker.resetMs() - idle));
        return out;
    }

    private static void addStats(List<String> out, String type, String main) {
        boolean any = false;
        if (Tracker.FARMING.equals(type)) {
            String f = Tracker.tab.get("Farming Fortune");
            String c = Tracker.tab.get(main + " Fortune");
            if (f != null || c != null) {
                any = true;
                out.add("§7Fortune: §6" + (f != null ? f : "") + (c != null ? (f != null ? " §7+ §6" : "") + c + " §7" + main : ""));
            }
        } else {
            for (String k : Tracker.statKeys(type)) {
                String v = Tracker.tab.get(k);
                if (v == null) continue;
                any = true;
                out.add("§7" + k + ": §6" + v);
            }
        }
        if (!any) out.add("§7Fortune: §8enable the Stats tab widget");
    }

    private static void addPowder(List<String> out, Session s, String type, long now) {
        double h = s.hours(now);
        for (String p : Tracker.powderKeys(type)) {
            long gained = s.powder.getOrDefault(p, 0L);
            String color = switch (p) { case "Gemstone" -> "§d"; case "Glacite" -> "§b"; case "Forest Whispers" -> "§3"; default -> "§2"; };
            String rate = h < 1.0 / 60 ? "" : " §8(" + Fmt.coins(gained / h) + "/h)";
            String label = p.endsWith("Whispers") ? p : p + " Powder";
            out.add("§7" + label + ": " + color + "+" + Fmt.num(gained) + rate);
        }
    }

    private static void addShards(List<String> out, Session s) {
        if (!Config.get().showShards) return;
        if (s.totalShards() == 0) {
            if (s.isForaging()) out.add("§7Shards: §b0");
            return;
        }
        var shards = new ArrayList<>(s.shards.entrySet());
        shards.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
        double value = s.shardValue();
        out.add("§7Shards: §b" + s.totalShards() + " §8(" + (value == 0 ? "?" : Fmt.coins(value)) + ")");
        for (int i = 0; i < Math.min(4, shards.size()); i++) out.add(shardLine(shards.get(i)));
        if (shards.size() > 4) out.add("§8 ...and " + (shards.size() - 4) + " more");
    }

    public static String shardLine(Map.Entry<String, Integer> e) {
        double v = e.getValue() * Prices.price(e.getKey());
        return " §b" + e.getValue() + "x §f" + e.getKey() + " §8(" + (v == 0 ? "?" : Fmt.coins(v)) + ")";
    }

    private static void addRareDrops(List<String> out, Session s) {
        if (!Config.get().showRareDrops || s.rareDrops == null || s.rareDrops.isEmpty()) return;
        var drops = new ArrayList<>(s.rareDrops.entrySet());
        drops.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
        int total = 0;
        for (var d : drops) total += d.getValue();
        out.add("§7Rare drops: §d" + total);
        for (int i = 0; i < Math.min(4, drops.size()); i++) out.add(rareLine(s, drops.get(i)));
        if (drops.size() > 4) out.add("§8 ...and " + (drops.size() - 4) + " more");
    }

    public static String rareLine(Session s, Map.Entry<String, Integer> d) {
        double each = Prices.price(d.getKey());
        String value = each == 0 ? "?" : Fmt.coins(each * d.getValue());
        if (s.rareAlreadyCounted(d.getKey())) value += ", in items";
        return " §d" + d.getValue() + "x §f" + d.getKey() + " §8(" + value + ")";
    }

    private static void addCommissions(List<String> out, String type) {
        if (!Config.get().showCommissions || Tracker.commissions.isEmpty()) return;
        if (!Tracker.DWARVEN.equals(type) && !Tracker.HOLLOWS.equals(type)) return;
        out.add("§7Commissions:");
        for (String c : Tracker.commissions) {
            out.add(" §f" + c.replace("DONE", "§aDONE").replaceAll("(\\d+(\\.\\d+)?%)", "§e$1"));
        }
    }

    public static List<Map.Entry<String, Long>> sortedItems(Session s) {
        var items = new ArrayList<>(s.items.entrySet());
        items.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
        return items;
    }

    public static String itemLine(Map.Entry<String, Long> e) {
        double v = e.getValue() * Prices.price(e.getKey());
        String sign = e.getValue() > 0 ? "+" : "";
        return " §f" + sign + Fmt.num(e.getValue()) + " §a" + e.getKey() + " §8(" + (v == 0 ? "?" : Fmt.coins(v)) + ")";
    }

    public static String command(String type) {
        if (Tracker.FARMING.equals(type)) return "farmprofit";
        return Tracker.FORAGING.equals(type) ? "foragingprofit" : "miningprofit";
    }

    private Hud() {}
}
