package dev.farmprofit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds the HUD and command text. */
public final class Hud {

    /** One HUD line; item is set for lines that can be hidden with a right-click. */
    public record HudLine(String text, String item) {}

    public static final class Lines extends java.util.ArrayList<HudLine> {
        public boolean add(String text) { return add(new HudLine(text, null)); }
        public void item(String text, String item) { add(new HudLine(text, item)); }
        public List<String> texts() { return stream().map(HudLine::text).toList(); }
    }

    public static String title(String type) {
        if (Tracker.isMiningType(type)) {
            boolean here = Tracker.MINING.equals(Tracker.area) && Tracker.areaName != null && Tracker.MINING.equals(type);
            return "§2§l⛏ Mining" + (here ? " §7(" + Tracker.areaName + ")" : "");
        }
        return switch (type) {
            case Tracker.FORAGING -> "§a§l♣ Foraging" + (Tracker.FORAGING.equals(Tracker.area) && Tracker.areaName != null ? " §7(" + Tracker.areaName + ")" : "");
            case Tracker.FISHING -> "§9§l≈ Fishing";
            case Tracker.COMBAT -> "§c§l⚔ Combat";
            case Tracker.DUNGEONS -> "§4§l☠ Catacombs" + (Tracker.dungeonFloor != null ? " §7(" + Tracker.dungeonFloor + ")" : "");
            case Tracker.KUUDRA -> "§6§l♨ Kuudra";
            case Tracker.DIANA -> "§e§l✿ Diana";
            default -> "§6§lFarming";
        };
    }

    // ================= HUD =================

    public static Lines lines() {
        boolean details = Config.get().hudDetails;
        String type = Tracker.shownType();
        Lines out = new Lines();
        if (type == null || !Config.get().hudFor(type)) return out;
        Session s = Tracker.sessions.get(type);

        if (s == null) {
            out.add(title(type));
            out.add(switch (type) {
                case Tracker.MINING -> "§7Start mining to begin tracking";
                case Tracker.FORAGING -> "§7Start chopping to begin tracking";
                case Tracker.DUNGEONS, Tracker.KUUDRA -> "§7Start a run to begin tracking";
                default -> "§7Break a crop to start tracking";
            });
            String tip = Suggest.hudLine(null, type);
            if (tip != null) out.add(tip);
            if (details && Tracker.FARMING.equals(type)) { String c = Contests.hudLine(); if (c != null) out.add(c); }
            if (details) { String m = Election.hudLine(type); if (m != null) out.add(m); }
            if (details) addCommissions(out, type);
            if (Tracker.DUNGEONS.equals(type)) Secrets.addHudLines(out);
            return out;
        }

        long now = System.currentTimeMillis();
        String main = s.mainCrop();
        out.add(title(type));
        out.add("§7Time: §f" + Fmt.duration(s.durationMs(now)));
        if (details) addActivityLine(out, s, now);
        if (s.isCombat()) addBossLine(out, s, now);
        if (s.isDungeons() || s.isKuudra()) addRunLine(out, s, now);
        if (s.isDiana()) out.add("§7Burrows: §e" + Fmt.num(s.burrows) + rate(s.burrows, s, now));
        if (details) {
            addStats(out, type, main);
            addPowder(out, s, now);
            if (s.isFarming() || Tracker.FARMING.equals(type)) { String c = Contests.hudLine(); if (c != null) out.add(c); }
            String m = Election.hudLine(type);
            if (m != null) out.add(m);
        }

        if (!Prices.loaded()) out.add("§cLoading Bazaar prices...");
        out.add("§7Profit: §6" + Fmt.coins(s.value()) + " coins");
        if (s.coins > 0) out.add("§7Coins found: §6+" + Fmt.coins(s.coins));
        addCostLines(out, s);
        out.add("§7Profit/h: §6" + (s.durationMs(now) < 60_000 ? "§8wait 1 min" : Fmt.coins(s.perHour(now)) + "/h"));
        if (Config.get().hudShowTotal) addTotalLine(out, type);

        String tip = Suggest.hudLine(s, type);
        if (tip != null) out.add(tip);

        if (details) addExtras(out, s);
        addShards(out, s, 4);
        addRareDrops(out, s, 4);

        addItems(out, s, type);

        if (details) addCommissions(out, type);
        if (s.isDungeons()) Secrets.addHudLines(out);

        if (s.paused(now)) out.add("§ePaused §7- resets in " + Fmt.clock(Tracker.resetMs() - Tracker.idleMs(s)));
        return out;
    }

    // ================= /command output: profit only =================

    public static List<String> profitLines(Session s) {
        return profitLinesRaw(s).texts();
    }

    private static Lines profitLinesRaw(Session s) {
        long now = System.currentTimeMillis();
        Lines out = new Lines();
        out.add(title(s.type));
        out.add("§7Time: §f" + Fmt.duration(s.durationMs(now)));
        if (s.isCombat()) addBossLine(out, s, now);
        if (s.isDungeons() || s.isKuudra()) addRunLine(out, s, now);
        if (s.isDiana()) out.add("§7Burrows: §e" + Fmt.num(s.burrows));
        out.add("§7Profit: §6" + Fmt.coins(s.value()) + " coins");
        if (s.coins > 0) out.add("§7Coins found: §6+" + Fmt.coins(s.coins));
        addCostLines(out, s);
        out.add("§7Profit/h: §6" + (s.durationMs(now) < 60_000 ? "§8wait 1 min" : Fmt.coins(s.perHour(now)) + "/h"));
        addTotalLine(out, s.type);
        addShards(out, s, Integer.MAX_VALUE);
        addRareDrops(out, s, Integer.MAX_VALUE);
        var items = sortedItems(s);
        if (!items.isEmpty()) out.add("§7Items:");
        for (var e : items) out.item(itemLine(e), e.getKey());
        if (s.spent != null && !s.spent.isEmpty()) {
            out.add("§7Spent:");
            for (var e : s.spent.entrySet()) out.item(" §c-" + Fmt.num(e.getValue()) + " §f" + e.getKey() + " §8(" + Fmt.coins(e.getValue() * Prices.price(e.getKey())) + ")", e.getKey());
        }
        return out;
    }

    private static void addCostLines(Lines out, Session s) {
        if (s.costs > 0) out.add("§7" + (s.isCombat() ? "Quest costs" : "Costs") + ": §c-" + Fmt.coins(s.costs));
        double spent = s.spentValue();
        if (spent > 0) out.add("§7Spent: §c-" + Fmt.coins(spent) + " §8(" + s.spent.size() + " item" + (s.spent.size() > 1 ? "s" : "") + ")");
        if (s.copper > 0) out.add("§7Copper: §c" + Fmt.num(s.copper) + (Config.get().copperValue > 0 ? " §8(" + Fmt.coins(s.copper * Config.get().copperValue) + ")" : ""));
    }

    private static void addTotalLine(Lines out, String type) {
        var t = Totals.all().get(Tracker.normalType(type));
        if (t == null || t.sessions == 0) return;
        double h = t.ms / 3_600_000.0;
        out.add("§8All-time: §6" + Fmt.coins(t.profit) + " §8(" + (h > 0 ? Fmt.coins(t.profit / h) : "0") + "/h, " + t.sessions + " sessions)");
    }

    private static String rate(int count, Session s, long now) {
        double h = s.hours(now);
        return h < 1.0 / 60 ? "" : String.format(Locale.US, " §8(%.0f/h)", count / h);
    }

    // ================= pieces =================

    private static void addActivityLine(Lines out, Session s, long now) {
        String bps = String.format(Locale.US, "%.1f", s.breaksPerSecond(now)) + " BPS)";
        if (s.isMining()) out.add("§7Mining: §a" + s.mainCrop() + " §8(" + Fmt.num(s.totalBreaks()) + " blocks, " + bps);
        else if (s.isForaging()) {
            out.add("§7Chopping: §a" + s.mainCrop() + " §8(" + Fmt.num(s.totalBreaks()) + " logs, " + bps);
            if (s.treeGifts > 0) {
                double min = s.durationMs(now) / 60_000.0;
                out.add("§7Trees: §a" + Fmt.num(s.treeGifts) + " gifts" + (min < 1 ? "" : String.format(Locale.US, " §8(%.1f/min)", s.treeGifts / min)));
            }
        } else if (s.isFishing()) {
            if (s.location != null) out.add("§7Location: §b" + s.location);
        } else if (s.isFarming()) {
            out.add("§7Farming: §a" + s.mainCrop() + " §8(" + Fmt.num(s.totalBreaks()) + " broken, " + bps);
        }
    }

    private static void addBossLine(Lines out, Session s, long now) {
        int bosses = s.totalBreaks();
        double h = s.hours(now);
        String rate = h < 1.0 / 60 ? "" : String.format(Locale.US, " §8(%.1f/h)", bosses / h);
        out.add("§7Slayer: §c" + (bosses == 0 ? "no bosses yet" : s.mainCrop() + " §7x" + bosses) + rate);
    }

    private static void addRunLine(Lines out, Session s, long now) {
        if (s.runs == 0) { out.add("§7Runs: §f0 §8(finish a run to see your pace)"); return; }
        double h = s.hours(now);
        long avg = s.durationMs(now) / s.runs;
        out.add("§7Runs: §f" + s.runs + " §8(avg " + Fmt.duration(avg) + (h > 0 ? String.format(Locale.US, ", %.1f/h", s.runs / h) : "") + ")");
        if (s.lastScore != null) out.add("§7Last score: §f" + s.lastScore);
        if (s.runs > 0) out.add("§7Profit/run: §6" + Fmt.coins(s.value() / s.runs));
    }

    private static void addStats(Lines out, String type, String main) {
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
        if (!any && (Tracker.FARMING.equals(type) || Tracker.isMiningType(type) || Tracker.FORAGING.equals(type) || Tracker.FISHING.equals(type))) out.add("§7Stats: §8enable the Stats tab widget");
    }

    private static void addPowder(Lines out, Session s, long now) {
        if (s.powder == null || (!s.isMining() && !s.isForaging())) return;
        double h = s.hours(now);
        List<String> kinds = new ArrayList<>();
        if (s.isForaging()) kinds.add("Forest Whispers");
        else {
            for (String p : new String[]{"Mithril", "Gemstone", "Glacite"}) if (s.powder.getOrDefault(p, 0L) > 0) kinds.add(p);
            if (kinds.isEmpty()) {
                String an = Tracker.areaName == null ? "" : Tracker.areaName;
                kinds.add(an.contains("Hollows") ? "Gemstone" : (an.contains("Mineshaft") || an.contains("Glacite")) ? "Glacite" : "Mithril");
            }
        }
        for (String p : kinds) {
            long gained = s.powder.getOrDefault(p, 0L);
            String color = switch (p) { case "Gemstone" -> "§d"; case "Glacite" -> "§b"; case "Forest Whispers" -> "§3"; default -> "§2"; };
            String rate = h < 1.0 / 60 ? "" : " §8(" + Fmt.coins(gained / h) + "/h)";
            out.add("§7" + (p.endsWith("Whispers") ? p : p + " Powder") + ": " + color + "+" + Fmt.num(gained) + rate);
        }
    }

    private static void addExtras(Lines out, Session s) {
        if (s.isFarming() && s.totalPests() > 0) {
            var pests = new ArrayList<>(s.pests.entrySet());
            pests.sort((a, b) -> b.getValue() - a.getValue());
            StringBuilder sb = new StringBuilder("§7Pests: §c" + s.totalPests() + " killed §8(");
            for (int i = 0; i < Math.min(3, pests.size()); i++) {
                if (i > 0) sb.append(", ");
                sb.append(pests.get(i).getKey()).append(" ").append(pests.get(i).getValue());
            }
            out.add(sb.append(pests.size() > 3 ? ", ...)" : ")").toString());
        }
        if (s.isMining() && s.pristine > 0) out.add("§7Pristine procs: §d" + s.pristine);
        if (s.isFishing() && s.trophyFish > 0) {
            var tr = new ArrayList<>(s.trophies.entrySet());
            tr.sort((a, b) -> b.getValue() - a.getValue());
            StringBuilder sb = new StringBuilder("§7Trophy fish: §6" + s.trophyFish + " §8(");
            for (int i = 0; i < Math.min(3, tr.size()); i++) sb.append(i > 0 ? ", " : "").append(tr.get(i).getKey()).append(" ").append(tr.get(i).getValue());
            out.add(sb.append(")").toString());
        }
        if (s.isFarming() && s.visitors > 0) out.add("§7Visitors: §a" + s.visitors + " accepted");
        if (s.isMining() && s.breaks.size() > 1) {
            out.add("§7Blocks:");
            var breaks = new ArrayList<>(s.breaks.entrySet());
            breaks.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(4, breaks.size()); i++) {
                out.add(" §f" + Fmt.num(breaks.get(i).getValue()) + " §d" + breaks.get(i).getKey());
            }
        }
    }

    private static void addShards(Lines out, Session s, int max) {
        if (!Config.get().showShards) return;
        if (s.totalShards() == 0) {
            if (s.isForaging()) out.add("§7Shards: §b0");
            return;
        }
        var shards = new ArrayList<>(s.shards.entrySet());
        shards.removeIf(e -> Session.ignored(e.getKey()));
        shards.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
        if (shards.isEmpty()) return;
        double value = s.shardValue();
        out.add("§7Shards: §b" + s.totalShards() + " §8(" + (value == 0 ? "?" : Fmt.coins(value)) + ")");
        for (int i = 0; i < Math.min(max, shards.size()); i++) out.item(shardLine(shards.get(i)), shards.get(i).getKey());
        if (shards.size() > max) out.add("§8 ...and " + (shards.size() - max) + " more");
    }

    public static String shardLine(Map.Entry<String, Integer> e) {
        double v = e.getValue() * Prices.price(e.getKey());
        return " §b" + e.getValue() + "x §f" + e.getKey() + " §8(" + (v == 0 ? "?" : Fmt.coins(v)) + ")";
    }

    private static void addRareDrops(Lines out, Session s, int max) {
        if (!Config.get().showRareDrops || s.rareDrops == null || s.rareDrops.isEmpty()) return;
        var drops = new ArrayList<>(s.rareDrops.entrySet());
        drops.removeIf(e -> Session.ignored(e.getKey()));
        if (drops.isEmpty()) return;
        drops.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
        int total = 0;
        for (var d : drops) total += d.getValue();
        out.add("§7Rare drops: §d" + total);
        for (int i = 0; i < Math.min(max, drops.size()); i++) out.item(rareLine(s, drops.get(i)), drops.get(i).getKey());
        if (drops.size() > max) out.add("§8 ...and " + (drops.size() - max) + " more");
    }

    public static String rareLine(Session s, Map.Entry<String, Integer> d) {
        double each = Prices.price(d.getKey());
        String value = each == 0 ? "?" : Fmt.coins(each * d.getValue());
        if (s.rareAlreadyCounted(d.getKey())) value += ", in items";
        return " §d" + d.getValue() + "x §f" + d.getKey() + " §8(" + value + ")";
    }

    private static void addCommissions(Lines out, String type) {
        if (!Config.get().showCommissions || Tracker.commissions.isEmpty() || !Tracker.MINING.equals(type)) return;
        out.add("§7Commissions:");
        for (String c : Tracker.commissions) {
            out.add(" §f" + c.replace("DONE", "§aDONE").replaceAll("(\\d+(\\.\\d+)?%)", "§e$1"));
        }
    }

    /** Item list for the HUD: top items, then one line for everything cheap. */
    private static void addItems(Lines out, Session s, String type) {
        var items = sortedItems(s);
        if (items.isEmpty()) return;
        double min = Config.get().minItemValue;
        int max = Config.get().hudMaxItems;
        int shown = 0, cheapCount = 0, rest = 0;
        double cheapValue = 0;
        out.add("§7Items:");
        for (var e : items) {
            double v = e.getValue() * Prices.price(e.getKey());
            if (Math.abs(v) < min) { cheapCount++; cheapValue += v; continue; }
            if (shown < max) { out.item(itemLine(e), e.getKey()); shown++; } else rest++;
        }
        if (rest > 0) out.add("§8 ...and " + rest + " more (/" + command(type) + ")");
        if (cheapCount > 0) out.add("§8 " + cheapCount + " cheap item" + (cheapCount > 1 ? "s" : "") + " (" + Fmt.coins(cheapValue) + ")");
    }

    /** The item that made the most money, e.g. "Enchanted Wheat (1.2M)". */
    public static String bestItem(Session s) {
        String best = null;
        double bestV = 0;
        for (var e : s.items.entrySet()) {
            double v = e.getValue() * Prices.price(e.getKey());
            if (v > bestV && !Session.ignored(e.getKey())) { bestV = v; best = e.getKey(); }
        }
        if (s.rareDrops != null) for (var e : s.rareDrops.entrySet()) {
            double v = s.rareValue(e.getKey(), e.getValue());
            if (v > bestV) { bestV = v; best = e.getKey(); }
        }
        return best == null ? null : "§a" + best + " §8(" + Fmt.coins(bestV) + ")";
    }

    public static List<Map.Entry<String, Long>> sortedItems(Session s) {
        var items = new ArrayList<>(s.items.entrySet());
        items.removeIf(e -> Session.ignored(e.getKey()));
        items.sort((a, b) -> Double.compare(b.getValue() * Prices.price(b.getKey()), a.getValue() * Prices.price(a.getKey())));
        return items;
    }

    public static String itemLine(Map.Entry<String, Long> e) {
        double v = e.getValue() * Prices.price(e.getKey());
        String sign = e.getValue() > 0 ? "+" : "";
        return " §f" + sign + Fmt.num(e.getValue()) + " §a" + e.getKey() + " §8(" + (v == 0 ? "?" : Fmt.coins(v)) + ")";
    }

    public static String command(String type) {
        if (Tracker.isMiningType(type)) return "miningprofit";
        return switch (type) {
            case Tracker.FORAGING -> "foragingprofit";
            case Tracker.FISHING -> "fishingprofit";
            case Tracker.COMBAT -> "combatprofit";
            case Tracker.DUNGEONS -> "dungeonprofit";
            case Tracker.KUUDRA -> "kuudraprofit";
            case Tracker.DIANA -> "dianaprofit";
            default -> "farmprofit";
        };
    }

    private Hud() {}
}
