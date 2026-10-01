package dev.farmprofit;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** All the live tracking logic for farming and mining. */
public final class Tracker {
    public static final String FARMING = "Farming";
    public static final String DWARVEN = "Dwarven Mines";
    public static final String HOLLOWS = "Crystal Hollows";
    public static final String MINESHAFT = "Glacite Mineshaft";
    public static final String FORAGING = "Foraging";

    private static final Pattern SACK_LINE = Pattern.compile("^\\s*([+-][\\d,]+) (.+?) \\(.*\\)\\s*$");
    private static final Pattern TAB_STAT = Pattern.compile("^\\s*([^:]{2,40}?):\\s*(.+?)\\s*$");
    private static final Pattern COMMISSION = Pattern.compile("^\\s*(.+?):\\s*(\\d+(?:\\.\\d+)?%|DONE)\\s*$");
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");
    private static final Pattern PEST_KILL = Pattern.compile("^You received (\\d+)x (.+?) for killing an? (.+?)!$");
    private static final Pattern PRISTINE = Pattern.compile("^PRISTINE! You found .*?x(\\d+)!$");
    private static final Pattern RARE_DROP = Pattern.compile("^((?:[A-Z]+ )*(?:DROP|CROP))! (.+)$");
    private static final Pattern AMOUNT_SUFFIX = Pattern.compile("^(.+?) x(\\d+)$");
    private static final Pattern AMOUNT_PREFIX = Pattern.compile("^(\\d+)x (.+)$");
    private static final Pattern SHARD = Pattern.compile("(?:(\\d+)x\\s+)?([A-Z][A-Za-z'\\-]*(?: [A-Z][A-Za-z'\\-]*)*) Shards?\\b(?:\\s*x(\\d+))?");
    private static final Pattern PLAYER_CHAT = Pattern.compile("^(?:[A-Za-z]+ > )?(?:\\[[^\\]]*\\]\\s*)*[A-Za-z0-9_]{3,16}(?:\\s*\\[[^\\]]*\\])?: .*");
    private static final String[] SHARD_IGNORE = {"fus", "syphon", "sold", "bought", "bazaar", "sell", "buy", "convert", "transfer"};
    private static final Set<String> SHARD_STOPWORDS = Set.of("You", "Caught", "Gained", "Obtained", "Found", "Received",
            "Got", "A", "An", "The", "Your", "Added", "Sent", "Rare", "Drop", "Tree", "Gift", "Hunting", "Box");
    private static String lastShardKey = "";
    private static long lastShardTime;
    private static final long INVENTORY_WINDOW_MS = 10_000;
    private static final String[] POWDERS = {"Mithril", "Gemstone", "Glacite", "Forest Whispers"};
    private static final Pattern TREE_GIFT = Pattern.compile("(?i)^\\W*tree gift\\b.*");

    /** One open session per activity type, so leaving the Mines for a mineshaft doesn't reset anything. */
    public static final Map<String, Session> sessions = new LinkedHashMap<>();
    /** Every "Key: Value" line from the tab list. */
    public static final Map<String, String> tab = new HashMap<>();
    public static final List<String> commissions = new ArrayList<>();
    /** DWARVEN, HOLLOWS, MINESHAFT, FORAGING, or null (farming / anywhere else). */
    public static String area;
    /** Raw "Area:" name from the tab list, e.g. "Galatea". */
    public static String areaName;

    private static Map<String, Integer> lastInventory = new HashMap<>();
    private static boolean haveSnapshot;
    private static Object lastPlayer;
    private static long lastTabCheck;
    private static final Map<String, Long> lastPowder = new HashMap<>();

    /** Blocks we've started mining and are waiting for the server to break. */
    private static final List<Target> pending = new ArrayList<>();

    private static final class Target {
        final BlockPos pos; final BlockState state; final String ore; int age;
        Target(BlockPos pos, BlockState state, String ore) { this.pos = pos; this.state = state; this.ore = ore; }
    }

    // ---------- sessions ----------

    private static Session activity(String type) {
        long now = System.currentTimeMillis();
        Session s = sessions.computeIfAbsent(type, t -> new Session(t, now));
        s.lastActivity = now;
        return s;
    }

    public static void onCropBroken(String crop) { activity(FARMING).addBreak(crop); }

    public static void onAttack() {
        String type = area != null ? area : FARMING;
        if (sessions.containsKey(type)) activity(type).pestActions++;
    }

    public static void onVacuum() { activity(FARMING).pestActions++; }

    public static String shownType() { return area != null ? area : FARMING; }

    public static Session shown() { return sessions.get(shownType()); }

    private static Session mostRecent() {
        Session best = null;
        for (Session s : sessions.values()) if (best == null || s.lastActivity > best.lastActivity) best = s;
        return best;
    }

    public static long idleMs(Session s) { return s == null ? 0 : System.currentTimeMillis() - s.lastActivity; }

    public static long resetMs() { return Config.get().resetMinutes * 60_000L; }

    public static void endSession(Session s, boolean announce) {
        if (s == null) return;
        sessions.remove(s.type);
        if (s.totalBreaks() == 0 && s.items.isEmpty()) return;
        s.fortune = statsSummary(s.type, s.mainCrop());
        s.finish();
        History.add(s);
        if (announce) {
            say("§6[" + s.type + "] §7Session ended: §f" + Fmt.duration(s.durationMs(0)) + " §7of §a" + s.mainCrop
                    + "§7, profit §6" + Fmt.coins(s.profit) + " §7(§6" + Fmt.coins(s.profitPerHour) + "/h§7). Saved to history.");
        }
    }

    public static void endAll(boolean announce) {
        for (Session s : new ArrayList<>(sessions.values())) endSession(s, announce);
    }

    // ---------- every tick ----------

    public static void tick(Minecraft mc) {
        Prices.tick();
        long now = System.currentTimeMillis();
        for (Session s : new ArrayList<>(sessions.values())) {
            if (idleMs(s) > resetMs()) endSession(s, true);
        }

        if (mc.player == null || mc.level == null) {
            haveSnapshot = false; lastPlayer = null; pending.clear(); return;
        }
        if (mc.player != lastPlayer) { haveSnapshot = false; lastPlayer = mc.player; pending.clear(); }

        if (now - lastTabCheck > 1000) {
            lastTabCheck = now;
            readTab(mc);
        }

        trackMining(mc);

        // Inventory changes go to whichever session was active in the last few seconds
        Map<String, Integer> inv = scanInventory(mc);
        Session target = mostRecent();
        boolean counting = haveSnapshot && target != null && mc.screen == null && idleMs(target) < INVENTORY_WINDOW_MS;
        if (counting) {
            Set<String> keys = new HashSet<>(inv.keySet());
            keys.addAll(lastInventory.keySet());
            for (String k : keys) {
                int delta = inv.getOrDefault(k, 0) - lastInventory.getOrDefault(k, 0);
                if (delta != 0) target.addItem(k, delta);
            }
        }
        lastInventory = inv;
        haveSnapshot = true;
    }

    private static Map<String, Integer> scanInventory(Minecraft mc) {
        Map<String, Integer> out = new HashMap<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            String name = strip(stack.getHoverName().getString());
            if (Items.isTracked(name)) out.merge(name, stack.getCount(), Integer::sum);
        }
        return out;
    }

    // ---------- mining detection ----------
    // Hypixel breaks blocks on the server, so we remember which ore you were hitting
    // and count it when that block changes.

    private static void trackMining(Minecraft mc) {
        if (area == null) { pending.clear(); return; }

        for (Iterator<Target> it = pending.iterator(); it.hasNext(); ) {
            Target t = it.next();
            if (mc.level.getBlockState(t.pos) != t.state) {
                activity(area).addBreak(t.ore);
                it.remove();
            } else if (++t.age > 40) {
                it.remove();
            }
        }

        if (mc.screen != null || !mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        for (Target t : pending) {
            if (t.pos.equals(pos)) { t.age = 0; return; }
        }
        BlockState state = mc.level.getBlockState(pos);
        String ore = Items.blockFor(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath(), area);
        if (ore != null && pending.size() < 32) pending.add(new Target(pos.immutable(), state, ore));
    }

    // ---------- tab list (area, stats, powder, commissions) ----------

    private static void readTab(Minecraft mc) {
        var conn = mc.getConnection();
        if (conn == null) return;
        tab.clear();
        commissions.clear();
        for (var info : conn.getListedOnlinePlayers()) {
            Component dn = info.getTabListDisplayName();
            if (dn == null) continue;
            String line = strip(dn.getString());
            Matcher c = COMMISSION.matcher(line);
            if (c.matches()) commissions.add(c.group(1).trim() + ": " + c.group(2));
            Matcher m = TAB_STAT.matcher(line);
            if (m.matches()) tab.putIfAbsent(m.group(1).trim(), m.group(2).trim());
        }

        String a = tab.get("Area");
        areaName = a;
        boolean foraging = false;
        if (a != null) for (String f : Config.get().foragingAreas) if (a.contains(f)) foraging = true;
        if (a == null) area = null;
        else if (foraging) area = FORAGING;
        else if (a.contains("Dwarven")) area = DWARVEN;
        else if (a.contains("Crystal Hollows")) area = HOLLOWS;
        else if (a.contains("Mineshaft")) area = MINESHAFT;
        else area = null;

        // Powder: count increases while a mining session in this area is running
        Session s = area != null ? sessions.get(area) : null;
        for (String p : POWDERS) {
            String v = tab.containsKey(p + " Powder") ? tab.get(p + " Powder") : tab.get(p);
            long val = number(v);
            if (val < 0) continue;
            Long last = lastPowder.put(p, val);
            if (s != null && last != null && val > last) s.addPowder(p, val - last);
        }
    }

    public static long number(String s) {
        if (s == null) return -1;
        Matcher m = NUMBER.matcher(s);
        if (!m.find()) return -1;
        try { return (long) Double.parseDouble(m.group().replace(",", "")); } catch (Exception e) { return -1; }
    }

    /** Which tab stats to show for each HUD. */
    public static String[] statKeys(String type) {
        return switch (type) {
            case DWARVEN -> new String[]{"Mining Speed", "Mining Fortune", "Ore Fortune", "Block Fortune", "Dwarven Metal Fortune"};
            case HOLLOWS -> new String[]{"Mining Speed", "Mining Fortune", "Gemstone Fortune", "Ore Fortune"};
            case MINESHAFT -> new String[]{"Mining Speed", "Mining Fortune", "Gemstone Fortune", "Ore Fortune", "Block Fortune"};
            case FORAGING -> new String[]{"Sweep", "Foraging Fortune", "Fig Fortune", "Mangrove Fortune"};
            default -> new String[]{"Farming Fortune"};
        };
    }

    public static String[] powderKeys(String type) {
        return switch (type) {
            case DWARVEN -> new String[]{"Mithril"};
            case HOLLOWS -> new String[]{"Gemstone", "Mithril"};
            case MINESHAFT -> new String[]{"Glacite"};
            case FORAGING -> new String[]{"Forest Whispers"};
            default -> new String[0];
        };
    }

    /** Short text saved into history, e.g. "Mining Fortune ☘850, Mining Speed ⸕4200". */
    public static String statsSummary(String type, String main) {
        List<String> parts = new ArrayList<>();
        for (String k : statKeys(type)) if (tab.containsKey(k)) parts.add(k + " " + tab.get(k));
        if (FARMING.equals(type) && main != null && tab.containsKey(main + " Fortune"))
            parts.add(main + " Fortune " + tab.get(main + " Fortune"));
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    // ---------- chat (sacks) ----------

    public static void onChat(Component message) {
        String plain = strip(message.getString()).trim();
        if (PLAYER_CHAT.matcher(plain).matches()) return;   // ignore what players type

        // Attribute shards (can be part of Tree Gift / rare drop messages, so check every line first)
        scanShards(plain);

        // Pest kill: "You received 7x Enchanted Potato for killing a Locust!"
        Matcher pk = PEST_KILL.matcher(plain);
        if (pk.matches()) {
            Session s = activity(FARMING);
            s.pests.merge(pk.group(3).trim(), 1, Integer::sum);
            s.pestActions++;
            return;
        }

        // Tree Gift (Galatea): one per tree cut down
        if (TREE_GIFT.matcher(plain).matches() && !plain.contains(": ")) {
            Session s = sessions.get(FORAGING);
            if (s != null || FORAGING.equals(area)) activity(FORAGING).treeGifts++;
            return;
        }

        // Pristine proc (Crystal Hollows / Mineshaft gemstones)
        if (PRISTINE.matcher(plain).matches()) {
            Session s = area != null ? sessions.get(area) : null;
            if (s != null) s.pristine++;
            return;
        }

        // RARE DROP! / VERY RARE DROP! / CRAZY RARE DROP! / PET DROP! / RARE CROP! ...
        Matcher rd = RARE_DROP.matcher(plain);
        if (rd.matches()) {
            Session s = sessions.get(area != null ? area : FARMING);
            if (s == null) return;
            String item = rd.group(2).replaceAll("^[^A-Za-z0-9\\[]+", "").trim();
            while (item.matches(".*\\([^()]*\\)\\s*$") && !item.startsWith("Enchanted Book (")) {
                item = item.replaceAll("\\s*\\([^()]*\\)\\s*$", "");
            }
            if (item.startsWith("Enchanted Book (") && item.indexOf(')') < item.length() - 1) {
                item = item.substring(0, item.indexOf(')') + 1);
            }
            int count = 1;
            Matcher a = AMOUNT_SUFFIX.matcher(item);
            if (a.matches()) { item = a.group(1).trim(); count = Integer.parseInt(a.group(2)); }
            Matcher b = AMOUNT_PREFIX.matcher(item);
            if (b.matches()) { item = b.group(2).trim(); count = Integer.parseInt(b.group(1)); }
            if (!item.isEmpty() && !item.endsWith(" Shard")) s.rareDrops.merge(item, count, Integer::sum);
            return;
        }

        Session target = mostRecent();
        if (target == null) return;
        if (!plain.startsWith("[Sacks]")) return;
        List<Component> hovers = new ArrayList<>();
        collectHovers(message, hovers);
        for (Component hover : hovers) {
            for (String line : strip(hover.getString()).split("\n")) {
                Matcher m = SACK_LINE.matcher(line);
                if (!m.matches()) continue;
                long amount = Long.parseLong(m.group(1).replace(",", "").replace("+", ""));
                target.addItem(m.group(2).trim(), amount);
            }
        }
    }

    private static void scanShards(String text) {
        String lower = text.toLowerCase();
        for (String bad : SHARD_IGNORE) if (lower.contains(bad)) return;
        if (!text.contains("Shard")) return;

        Session s = FORAGING.equals(area) ? activity(FORAGING) : shown();
        if (s == null) return;

        for (String line : text.split("\n")) {
            Matcher m = SHARD.matcher(line);
            while (m.find()) {
                String name = cleanShardName(m.group(2));
                if (name == null) continue;
                int count = m.group(1) != null ? Integer.parseInt(m.group(1))
                        : m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;
                // the same shard is sometimes announced twice in a row
                long now = System.currentTimeMillis();
                String key = name + "#" + count;
                if (key.equals(lastShardKey) && now - lastShardTime < 1500) continue;
                lastShardKey = key;
                lastShardTime = now;
                s.shards.merge(name + " Shard", count, Integer::sum);
            }
        }
    }

    /** "You caught a Sparrow" -> "Sparrow"; prefers a name the Bazaar knows. */
    private static String cleanShardName(String raw) {
        String[] words = raw.trim().split(" ");
        for (int i = 0; i < words.length; i++) {
            String candidate = String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length));
            if (Prices.knowsShard(candidate + " Shard")) return candidate;
        }
        int start = 0;
        while (start < words.length - 1 && SHARD_STOPWORDS.contains(words[start])) start++;
        if (SHARD_STOPWORDS.contains(words[start])) return null;
        return String.join(" ", java.util.Arrays.copyOfRange(words, start, words.length));
    }

    private static void collectHovers(Component c, List<Component> out) {
        if (c.getStyle().getHoverEvent() instanceof HoverEvent.ShowText st) out.add(st.value());
        for (Component sibling : c.getSiblings()) collectHovers(sibling, out);
    }

    // ---------- helpers ----------

    public static String strip(String s) {
        String r = ChatFormatting.stripFormatting(s);
        return r == null ? "" : r;
    }

    public static void say(String msg) {
        Minecraft mc = Minecraft.getInstance();
        Component c = Component.literal(msg);
        if (mc.player == null) return;
        // Minecraft keeps renaming its chat methods, so find one at runtime.
        if (call(mc.player, "sendSystemMessage", c)) return;
        if (call(mc.player, "displayClientMessage", c, false)) return;
        try {
            Object listener = mc.getClass().getMethod("getChatListener").invoke(mc);
            if (call(listener, "handleSystemMessage", c, false)) return;
        } catch (Exception ignored) {}
        FarmProfitClient.LOG.info(msg);
    }

    private static boolean call(Object target, String name, Object... args) {
        for (java.lang.reflect.Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            try { m.invoke(target, args); return true; } catch (Exception ignored) {}
        }
        return false;
    }

    private Tracker() {}
}
