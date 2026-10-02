package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.farmprofit.MenuScreen.Action;
import dev.farmprofit.MenuScreen.Page;
import dev.farmprofit.MenuScreen.Row;
import dev.farmprofit.MenuScreen.Tab;

/** All the menus: hub, current session, history, totals, best now, Bazaar flips. */
public final class ProfitMenus {
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM HH:mm");
    private static final String[] ACTIVITIES = {Tracker.FARMING, Tracker.MINING, Tracker.FORAGING, Tracker.FISHING,
            Tracker.COMBAT, Tracker.DUNGEONS, Tracker.KUUDRA, Tracker.DIANA};

    private static String title(String type) { return Hud.title(type).replace("§l", "").replaceAll(" §7\\(.*\\)", ""); }

    /** Holder so row buttons can refresh the screen they're on. */
    private static final class Ref { MenuScreen screen; }

    // ======================================================================== hub

    public static Screen hub() {
        Ref ref = new Ref();
        List<Row> rows = new ArrayList<>();
        rows.add(entry("§fCurrent session", "Items, profit and costs of what you're doing now.", () -> Compat.setScreen(mc(), session(null, ref.screen))));
        rows.add(entry("§fHistory", "Every finished session, by activity.", () -> Compat.setScreen(mc(), history(null, ref.screen))));
        rows.add(entry("§fLifetime totals", "Time, profit and profit/h per activity, all time.", () -> Compat.setScreen(mc(), totals(ref.screen))));
        rows.add(entry("§fBest now", "Which crop / ore earns the most right now.", () -> Compat.setScreen(mc(), suggest(0, ref.screen))));
        rows.add(entry("§fBazaar flips", "Flip finder, plan, your orders and flip profit.", () -> Compat.setScreen(mc(), flips(0, ref.screen))));
        rows.add(entry("§fNext talismans", "Cheapest Magical Power you don't have yet.", () -> Compat.setScreen(mc(), new TalismansScreen())));
        rows.add(entry("§fSettings", "Every setting, with search.", () -> Compat.setScreen(mc(), new SettingsScreen(ref.screen))));
        rows.add(entry("§fCommands", "Every command with a short explanation.", () -> Compat.setScreen(mc(), Commands.screen(ref.screen))));
        rows.add(entry("§fHUD editor", "Move, resize and hide the HUD panels.", () -> { Compat.setScreen(mc(), null); GuiEditor.open(); }));
        rows.add(entry("§fSetup check", "What the mod can see, and what to turn on.", () -> Compat.setScreen(mc(), new SetupScreen())));
        Tab t = new Tab("Menu", () -> new Page(new String[]{"What", "", ""}, new int[]{150, 260, 40}, rows, List.of(),
                List.of("§8Open this menu with the P key (change it in Controls) or /profit menu.")));
        ref.screen = new MenuScreen("SkyAssist", List.of(t), 0, null);
        return ref.screen;
    }

    private static Row entry(String name, String what, Runnable open) {
        return new Row(new String[]{name, "§7" + what}, null, List.of(new Action("Open", null, open)));
    }

    private static Minecraft mc() { return Minecraft.getInstance(); }

    // ======================================================================== current session

    public static Screen session(String type, Screen parent) {
        Ref ref = new Ref();
        List<String> types = new ArrayList<>(Tracker.sessions.keySet());
        if (types.isEmpty()) types.add(type != null ? type : Tracker.FARMING);
        int start = Math.max(0, types.indexOf(type != null ? type : Tracker.shownType()));
        List<Tab> tabs = new ArrayList<>();
        for (String t : types) tabs.add(new Tab(title(t), () -> sessionPage(t, ref)));
        ref.screen = new MenuScreen("Current session", tabs, start, parent);
        return ref.screen;
    }

    private static Page sessionPage(String type, Ref ref) {
        Session s = Tracker.sessions.get(type);
        List<Row> rows = new ArrayList<>();
        List<Action> top = new ArrayList<>();
        List<String> footer = new ArrayList<>();
        if (s == null) {
            footer.add("§7No " + type.toLowerCase(Locale.ROOT) + " session running.");
            return new Page(new String[]{"Item", "Amount", "Value"}, new int[]{230, 90, 100}, rows, top, footer);
        }
        long now = System.currentTimeMillis();
        for (var e : Hud.sortedItems(s)) {
            double v = e.getValue() * Prices.price(e.getKey());
            rows.add(new Row(new String[]{"§a" + e.getKey(), "§f" + (e.getValue() > 0 ? "+" : "") + Fmt.num(e.getValue()), "§6" + (v == 0 ? "?" : Fmt.coins(v))},
                    "§7Each: §6" + Fmt.coins(Prices.price(e.getKey())), List.of(hide(e.getKey(), ref))));
        }
        if (s.shards != null) for (var e : s.shards.entrySet()) {
            double v = e.getValue() * Prices.price(e.getKey());
            rows.add(new Row(new String[]{"§b" + e.getKey(), "§f" + e.getValue(), "§6" + (v == 0 ? "?" : Fmt.coins(v))}, "§7Attribute shard", List.of(hide(e.getKey(), ref))));
        }
        if (s.rareDrops != null) for (var e : s.rareDrops.entrySet()) {
            double v = s.rareValue(e.getKey(), e.getValue());
            rows.add(new Row(new String[]{"§d" + e.getKey(), "§f" + e.getValue() + "x", "§6" + (v == 0 ? "in items / ?" : Fmt.coins(v))}, "§7Rare drop", List.of(hide(e.getKey(), ref))));
        }
        if (s.spent != null) for (var e : s.spent.entrySet()) {
            rows.add(new Row(new String[]{"§c" + e.getKey() + " §8(spent)", "§c-" + Fmt.num(e.getValue()), "§c-" + Fmt.coins(e.getValue() * Prices.price(e.getKey()))}));
        }
        top.add(new Action("End & save session", "Ends this session now and saves it to history.", () -> { Tracker.endSession(s, true); ref.screen.refresh(); }));
        top.add(new Action("Copy summary", "Copies \"" + type + ": profit in time (per hour)\" to the clipboard.", () -> Chat.copy(
                s.type + ": " + Fmt.coins(s.value()) + " coins in " + Fmt.duration(s.durationMs(now)) + " (" + Fmt.coins(s.perHour(now)) + "/h)")));
        top.add(new Action("Chat view", "Prints this session in chat instead.", () -> { Compat.setScreen(mc(), null); for (String l : Hud.profitLines(s)) Tracker.say(l); }));
        footer.add("§7Time §f" + Fmt.duration(s.durationMs(now)) + "  §7Profit §6" + Fmt.coins(s.value()) + "  §7Profit/h §6"
                + (s.durationMs(now) < 60_000 ? "wait 1 min" : Fmt.coins(s.perHour(now))));
        String extra = (s.costs > 0 ? "§7Costs §c-" + Fmt.coins(s.costs) + "  " : "") + (s.coins > 0 ? "§7Coins found §6+" + Fmt.coins(s.coins) + "  " : "")
                + (s.note != null ? "§7Note §f" + s.note : "");
        if (!extra.isBlank()) footer.add(extra);
        return new Page(new String[]{"Item", "Amount", "Value"}, new int[]{230, 90, 100}, rows, top, footer);
    }

    private static Action hide(String item, Ref ref) {
        return new Action("§8Hide", "Stop counting " + item + " (undo in Settings → Hidden items).", () -> {
            if (!Config.get().ignoredItems.contains(item)) Config.get().ignoredItems.add(item);
            Config.save();
            ref.screen.refresh();
        });
    }

    // ======================================================================== history

    public static Screen history(String type, Screen parent) {
        List<Tab> tabs = new ArrayList<>();
        tabs.add(new Tab("All", () -> historyPage(null)));
        int start = 0;
        for (String t : ACTIVITIES) {
            if (t.equals(type)) start = tabs.size();
            tabs.add(new Tab(title(t), () -> historyPage(t)));
        }
        return new MenuScreen("History", tabs, start, parent);
    }

    private static Page historyPage(String type) {
        List<Session> all = new ArrayList<>();
        for (Session s : History.all()) if (type == null || Tracker.normalType(s.type).equals(type)) all.add(s);
        List<Row> rows = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            Session s = all.get(i);
            String t = Tracker.normalType(s.type);
            StringBuilder tip = new StringBuilder(title(t) + "\n§7" + s.mainCrop);
            if (s.fortune != null) tip.append("\n§8").append(s.fortune);
            if (s.note != null) tip.append("\n§7Note: §f").append(s.note);
            if (s.shards != null && !s.shards.isEmpty()) tip.append("\n§bShards: ").append(joinCounts(s.shards));
            if (s.rareDrops != null && !s.rareDrops.isEmpty()) tip.append("\n§dRare: ").append(joinCounts(s.rareDrops));
            rows.add(new Row(new String[]{"§8" + DATE.format(new Date(s.start)), title(t), "§a" + s.mainCrop, "§f" + Fmt.duration(s.durationMs(0)),
                    "§6" + Fmt.coins(s.profit), "§6" + Fmt.coins(s.profitPerHour) + "/h"}, tip.toString(), List.of()));
        }
        List<String> footer = new ArrayList<>();
        String spark = FarmProfitClient.sparkline(all.subList(Math.max(0, all.size() - 40), all.size()));
        if (!spark.isEmpty()) footer.add("§7Profit/h trend (oldest → newest): §e" + spark);
        footer.add("§8Hover a session for details.");
        List<Action> top = List.of(new Action("Export CSV", "Saves all history to config/skyassist/history.csv for Excel / Google Sheets.", FarmProfitClient::exportCsv));
        return new Page(new String[]{"When", "Activity", "What", "Time", "Profit", "Per hour"}, new int[]{70, 90, 100, 70, 60, 60}, rows, top, footer);
    }

    private static String joinCounts(Map<String, Integer> m) {
        List<String> parts = new ArrayList<>();
        m.forEach((k, v) -> parts.add(v + "x " + k));
        return String.join(", ", parts);
    }

    // ======================================================================== totals

    public static Screen totals(Screen parent) {
        Tab t = new Tab("Totals", () -> {
            List<Row> rows = new ArrayList<>();
            double profit = 0;
            long ms = 0;
            for (var e : Totals.all().entrySet()) {
                var tt = e.getValue();
                double h = tt.ms / 3_600_000.0;
                profit += tt.profit;
                ms += tt.ms;
                rows.add(new Row(title(e.getKey()), "§f" + Fmt.duration(tt.ms), "§f" + tt.sessions, "§6" + Fmt.coins(tt.profit), "§6" + (h > 0 ? Fmt.coins(tt.profit / h) : "0") + "/h"));
            }
            double h = ms / 3_600_000.0;
            return new Page(new String[]{"Activity", "Time", "Sessions", "Profit", "Per hour"}, new int[]{110, 100, 70, 80, 80}, rows, List.of(),
                    List.of("§7All activities: §f" + Fmt.duration(ms) + " §7for §6" + Fmt.coins(profit) + " §7(§6" + (h > 0 ? Fmt.coins(profit / h) : "0") + "/h§7)"));
        });
        return new MenuScreen("Lifetime totals", List.of(t), 0, parent);
    }

    // ======================================================================== best now

    public static Screen suggest(int startTab, Screen parent) {
        return new MenuScreen("Best now", List.of(
                new Tab("Farming", () -> suggestPage(Suggest.farming(), "crop", "Based on your blocks/s and Farming Fortune.")),
                new Tab("Mining", () -> suggestPage(Suggest.mining(), "ore", Suggest.miningSpeed() > 0
                        ? "Based on your Mining Speed and fortune, for the island you're on." : "Add Mining Speed to the Stats tab widget to see this."))
        ), startTab, parent);
    }

    private static Page suggestPage(List<Suggest.Option> opts, String what, String note) {
        List<Row> rows = new ArrayList<>();
        int i = 1;
        for (Suggest.Option o : opts) {
            rows.add(new Row("§8" + i++ + ". §a" + o.name(), "§6~" + Fmt.coins(o.perHour()) + "/h", "§f" + o.sellAs(),
                    "§7" + String.format(Locale.US, "%.2f", o.perItem()) + " each"));
        }
        List<String> footer = new ArrayList<>();
        if (Election.mayor != null) footer.add("§7Mayor: §d" + Election.mayor + " §8(" + String.join(", ", Election.perks) + ")");
        String c = Contests.hudLine();
        if (c != null) footer.add(c);
        footer.add("§8" + note + " Pests, rare drops and powder aren't included.");
        return new Page(new String[]{"Best " + what, "Estimate", "Sell as", "Price"}, new int[]{130, 100, 150, 80}, rows, List.of(), footer);
    }

    // ======================================================================== Bazaar flips

    public static Screen flips(int startTab, Screen parent) {
        Ref ref = new Ref();
        ref.screen = new MenuScreen("Bazaar flips", List.of(
                new Tab("Best flips", () -> flipsPage(Bazaar.computeFlips(), ref, false)),
                new Tab("Plan", () -> flipsPage(FlipsCommand.planList(5), ref, true)),
                new Tab("My orders", () -> ordersPage(ref)),
                new Tab("Profit log", ProfitMenus::logPage)
        ), startTab, parent);
        return ref.screen;
    }

    private static List<Action> flipTop(Ref ref) {
        return List.of(
                new Action("Refresh prices", "Gets the newest Bazaar prices.", () -> { Prices.refresh(); ref.screen.refresh(); }),
                new Action("Flip settings", "Budget, minimum volume, tax and more.", () -> {
                    SettingsScreen.selectTab("Bazaar flipping");
                    Compat.setScreen(mc(), new SettingsScreen(ref.screen));
                }));
    }

    private static Page flipsPage(List<Bazaar.Flip> flips, Ref ref, boolean plan) {
        Config c = Config.get();
        List<Row> rows = new ArrayList<>();
        double cost = 0, profit = 0;
        int i = 1;
        for (Bazaar.Flip f : flips) {
            if (!plan && i > 50) break;
            cost += f.qty * f.buyAt;
            profit += f.profitHour;
            String tip = "§a" + f.name + "\n§7Buy order at §f" + Bazaar.Fmt1(f.buyAt) + "\n§7Sell offer at §f" + Bazaar.Fmt1(f.sellAt)
                    + "\n§7Profit each §6" + Fmt.coins(f.profitEach) + "\n§7Volume §f" + Fmt.coins(f.hourlyVolume) + "/h"
                    + (f.warnings.isEmpty() ? "" : "\n§c⚠ " + String.join(", ", f.warnings));
            rows.add(new Row(new String[]{"§8" + i++ + ". §a" + f.name + (f.warnings.isEmpty() ? "" : " §c⚠"),
                    "§f" + Bazaar.Fmt1(f.buyAt) + " → " + Bazaar.Fmt1(f.sellAt), String.format(Locale.US, "§7%.1f%%", f.margin),
                    "§f" + Fmt.num(f.qty), "§6" + Fmt.coins(f.profitHour) + "/h"}, tip,
                    List.of(new Action("§eBazaar", "Opens " + f.name + " in the Bazaar.", () -> MenuScreen.runCommand("bz " + f.name)))));
        }
        List<String> footer = new ArrayList<>();
        footer.add("§7Budget §6" + Fmt.coins(c.bzBudget) + "  §7tax " + c.bzTax + "%  §7min volume " + Fmt.coins(c.bzMinVolume) + "/week");
        if (plan) footer.add("§7Plan uses §6" + Fmt.coins(cost) + "§7, expected profit §6" + Fmt.coins(profit) + "§7. Place the orders yourself; they're tracked from chat.");
        if (!Prices.loaded()) footer.add("§cPrices are still loading — press Refresh prices.");
        return new Page(new String[]{"Item", "Buy → sell", "Margin", "Qty", "Profit"}, new int[]{150, 120, 50, 60, 70}, rows, flipTop(ref), footer);
    }

    private static Page ordersPage(Ref ref) {
        List<Row> rows = new ArrayList<>();
        var orders = Bazaar.state().orders;
        for (int i = 0; i < orders.size(); i++) {
            var o = orders.get(i);
            final int idx = i;
            rows.add(new Row(new String[]{Bazaar.orderLine(o).trim()}, null, List.of(
                    new Action("§eBazaar", "Opens " + o.name + " in the Bazaar.", () -> MenuScreen.runCommand("bz " + o.name)),
                    new Action("§cRemove", "Removes this entry from the list (if it's stale). Doesn't touch the real order.", () -> {
                        if (idx < Bazaar.state().orders.size()) Bazaar.state().orders.remove(idx);
                        Bazaar.save();
                        ref.screen.refresh();
                    }))));
        }
        return new Page(new String[]{"Order"}, new int[]{360}, rows, flipTop(ref),
                List.of("§8Orders appear automatically when you place them in the Bazaar.", "§7Coins in orders today: §6" + Fmt.coins(Bazaar.tradedToday())));
    }

    private static Page logPage() {
        List<Row> rows = new ArrayList<>();
        for (var e : Bazaar.state().log) {
            rows.add(new Row("§8" + DATE.format(new Date(e.time)), "§f" + e.qty + "x " + e.name, "§7" + Bazaar.Fmt1(e.buy) + " → " + Bazaar.Fmt1(e.sell),
                    (e.profit >= 0 ? "§a+" : "§c") + Fmt.coins(e.profit)));
        }
        return new Page(new String[]{"When", "Item", "Bought → sold", "Profit"}, new int[]{70, 170, 120, 80}, rows, List.of(),
                List.of("§7Today §6" + Fmt.coins(Bazaar.profitSince(Bazaar.startOfDay())) + "  §7All time §6" + Fmt.coins(Bazaar.profitSince(0))));
    }

    private ProfitMenus() {}
}
