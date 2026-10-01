package dev.farmprofit;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** All the live tracking logic. */
public final class Tracker {
    private static final Pattern SACK_LINE = Pattern.compile("^\\s*([+-][\\d,]+) (.+?) \\(.*\\)\\s*$");
    private static final Pattern FORTUNE = Pattern.compile("^\\s*(.+?) Fortune:\\s*\\S*?([\\d,.]+)\\s*$");
    private static final long INVENTORY_WINDOW_MS = 10_000;

    public static Session current;
    private static Map<String, Integer> lastInventory = new HashMap<>();
    private static boolean haveSnapshot;
    private static Object lastPlayer;
    private static long lastFortuneCheck;
    public static String farmingFortune = null;
    public static final Map<String, String> cropFortunes = new HashMap<>();

    // ---------- activity ----------

    private static void activity() {
        long now = System.currentTimeMillis();
        if (current == null) {
            current = new Session(now);
            haveSnapshot = false; // don't count what's already in the inventory
        }
        current.lastActivity = now;
    }

    public static void onCropBroken(String crop) {
        activity();
        current.addBreak(crop);
    }

    public static void onPestAction() {
        activity();
        current.pestActions++;
    }

    public static long idleMs() {
        return current == null ? 0 : System.currentTimeMillis() - current.lastActivity;
    }

    public static long resetMs() { return Config.get().resetMinutes * 60_000L; }

    public static void endSession(boolean announce) {
        if (current == null) return;
        Session s = current;
        current = null;
        if (s.totalBreaks() == 0 && s.items.isEmpty()) return;
        s.fortune = fortuneText(s.mainCrop());
        s.finish();
        History.add(s);
        if (announce) {
            say("§6[FarmProfit] §7Session ended: §f" + Fmt.duration(s.durationMs(0)) + " §7of §a" + s.mainCrop
                    + "§7, profit §6" + Fmt.coins(s.profit) + " §7(§6" + Fmt.coins(s.profitPerHour) + "/h§7). Saved to history.");
        }
    }

    // ---------- every tick ----------

    public static void tick(Minecraft mc) {
        Prices.tick();
        if (current != null && idleMs() > resetMs()) endSession(true);

        if (mc.player == null) { haveSnapshot = false; lastPlayer = null; return; }
        if (mc.player != lastPlayer) { haveSnapshot = false; lastPlayer = mc.player; }

        Map<String, Integer> inv = scanInventory(mc);
        boolean counting = haveSnapshot && current != null && mc.screen == null
                && idleMs() < INVENTORY_WINDOW_MS;
        if (counting) {
            Set<String> keys = new HashSet<>(inv.keySet());
            keys.addAll(lastInventory.keySet());
            for (String k : keys) {
                int delta = inv.getOrDefault(k, 0) - lastInventory.getOrDefault(k, 0);
                if (delta != 0) current.addItem(k, delta);
            }
        }
        lastInventory = inv;
        haveSnapshot = true;

        long now = System.currentTimeMillis();
        if (now - lastFortuneCheck > 2000) {
            lastFortuneCheck = now;
            readFortuneFromTab(mc);
        }
    }

    private static Map<String, Integer> scanInventory(Minecraft mc) {
        Map<String, Integer> out = new HashMap<>();
        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            var stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            String name = strip(stack.getHoverName().getString());
            if (Items.isFarmingItem(name)) out.merge(name, stack.getCount(), Integer::sum);
        }
        return out;
    }

    // ---------- fortune from the tab list ----------

    private static void readFortuneFromTab(Minecraft mc) {
        var conn = mc.getConnection();
        if (conn == null) return;
        String farming = null;
        Map<String, String> crops = new HashMap<>();
        for (var info : conn.getListedOnlinePlayers()) {
            Component dn = info.getTabListDisplayName();
            if (dn == null) continue;
            Matcher m = FORTUNE.matcher(strip(dn.getString()));
            if (!m.matches()) continue;
            String stat = m.group(1).trim();
            if (stat.equalsIgnoreCase("Farming")) farming = m.group(2);
            else crops.put(stat, m.group(2));
        }
        farmingFortune = farming;
        cropFortunes.clear();
        cropFortunes.putAll(crops);
    }

    public static String fortuneText(String crop) {
        String cropF = crop == null ? null : cropFortunes.get(crop);
        if (farmingFortune == null && cropF == null) return null;
        StringBuilder sb = new StringBuilder();
        if (farmingFortune != null) sb.append("☘").append(farmingFortune);
        if (cropF != null) sb.append(sb.length() > 0 ? " + " : "").append("☘").append(cropF).append(" ").append(crop);
        return sb.toString();
    }

    // ---------- chat (sacks) ----------

    public static void onChat(Component message) {
        if (current == null) return;
        String plain = strip(message.getString());
        if (!plain.startsWith("[Sacks]")) return;
        List<Component> hovers = new ArrayList<>();
        collectHovers(message, hovers);
        for (Component hover : hovers) {
            for (String line : strip(hover.getString()).split("\n")) {
                Matcher m = SACK_LINE.matcher(line);
                if (!m.matches()) continue;
                long amount = Long.parseLong(m.group(1).replace(",", "").replace("+", ""));
                current.addItem(m.group(2).trim(), amount);
            }
        }
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
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(msg), false);
    }

    private Tracker() {}
}
