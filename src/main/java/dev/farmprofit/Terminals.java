package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Floor 7 / Master 7 terminal solvers: highlights the slots to click (display only, you click).
 * Correct all the panes, Click in order, What starts with, Select all the [color] items, Change all to same color.
 */
public final class Terminals {
    private static final Pattern STARTS = Pattern.compile("What starts with: '(.)'\\?");
    private static final Pattern COLOR = Pattern.compile("Select all the (.+?) items!?");
    private static final String[] CYCLE = {"red", "orange", "yellow", "green", "blue"};

    private record SlotInfo(int x, int y, ItemStack stack, String path, String name) {}

    private static String title(Object screen) {
        Object t = Reflect.call(screen, "getTitle");
        return t instanceof Component c ? Tracker.strip(c.getString()) : "";
    }

    private static int intField(Object o, String name) {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try { Field f = k.getDeclaredField(name); f.setAccessible(true); return f.getInt(o); } catch (Exception ignored) {}
        }
        return Integer.MIN_VALUE;
    }

    /** The menu's own slots (not your inventory), with their positions on screen. */
    private static List<SlotInfo> slots(Object screen) {
        List<SlotInfo> out = new ArrayList<>();
        int left = intField(screen, "leftPos"), top = intField(screen, "topPos");
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) return out;
        Object menu = Reflect.call(screen, "getMenu");
        Object slots = Reflect.field(menu, "slots");
        if (!(slots instanceof List<?> list)) return out;
        int own = Math.max(0, list.size() - 36);
        for (int i = 0; i < own; i++) {
            Object slot = list.get(i);
            Object st = Reflect.call(slot, "getItem");
            int sx = intField(slot, "x"), sy = intField(slot, "y");
            if (!(st instanceof ItemStack stack) || stack.isEmpty() || sx == Integer.MIN_VALUE) continue;
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            out.add(new SlotInfo(left + sx, top + sy, stack, path, Tracker.strip(stack.getHoverName().getString())));
        }
        return out;
    }

    private static void box(Object g, SlotInfo s, int color) {
        Reflect.call(g, "fill", s.x(), s.y(), s.x() + 16, s.y() + 16, color);
    }

    private static void label(Object g, SlotInfo s, String text, int color) {
        Object font = Minecraft.getInstance().font;
        Reflect.call(g, new String[]{"text", "drawString"}, font, text, s.x() + 5, s.y() + 4, color, true);
    }

    /** Called after a menu is drawn. */
    static void draw(Object screen, Object g) {
        if (!Config.get().solveTerminals || !Tracker.DUNGEONS.equals(Tracker.area)) return;
        String title = title(screen);
        if (title.isEmpty()) return;
        int good = 0x8055FF55, next = 0x80FFFF55, later = 0x60FFAA00;

        if (title.startsWith("Correct all the panes")) {
            for (SlotInfo s : slots(screen)) if (s.path().equals("red_stained_glass_pane")) box(g, s, good);
            return;
        }
        if (title.startsWith("Click in order")) {
            List<SlotInfo> todo = new ArrayList<>();
            for (SlotInfo s : slots(screen)) if (s.path().equals("red_stained_glass_pane")) todo.add(s);
            todo.sort((a, b) -> Integer.compare(a.stack().getCount(), b.stack().getCount()));
            for (int i = 0; i < Math.min(3, todo.size()); i++) box(g, todo.get(i), i == 0 ? good : i == 1 ? next : later);
            return;
        }
        Matcher sw = STARTS.matcher(title);
        if (sw.find()) {
            String letter = sw.group(1).toLowerCase(Locale.ROOT);
            for (SlotInfo s : slots(screen)) {
                if (s.path().endsWith("glass_pane") || s.stack().hasFoil()) continue;
                if (s.name().toLowerCase(Locale.ROOT).startsWith(letter)) box(g, s, good);
            }
            return;
        }
        Matcher col = COLOR.matcher(title);
        if (col.find()) {
            String color = col.group(1).toLowerCase(Locale.ROOT).replace(' ', '_').replace("silver", "light_gray");
            for (SlotInfo s : slots(screen)) {
                if (s.path().endsWith("glass_pane") && !s.path().startsWith(color)) continue;
                if (s.stack().hasFoil()) continue;
                if (matchesColor(s.path(), color)) box(g, s, good);
            }
            return;
        }
        if (title.startsWith("Click the button on time") && Config.get().solveMelody) {
            List<SlotInfo> all = slots(screen);
            if (all.isEmpty()) return;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            for (SlotInfo s : all) { minX = Math.min(minX, s.x()); minY = Math.min(minY, s.y()); }
            // the magenta pane in the top row marks the column the lime pane has to be in
            int target = -1;
            for (SlotInfo s : all) if (s.path().equals("magenta_stained_glass_pane") && s.y() == minY) target = (s.x() - minX) / 18;
            if (target < 0) return;
            for (SlotInfo lime : all) {
                if (!lime.path().equals("lime_stained_glass_pane")) continue;
                int row = (lime.y() - minY) / 18, limeCol = (lime.x() - minX) / 18;
                for (SlotInfo b : all) {
                    if (!b.path().endsWith("terracotta") || (b.y() - minY) / 18 != row) continue;
                    box(g, b, limeCol == target ? 0xC055FF55 : 0x60FF5555);      // green = click now
                }
            }
            return;
        }
        if (title.startsWith("Change all to same color")) {
            List<SlotInfo> panes = new ArrayList<>();
            List<Integer> idx = new ArrayList<>();
            for (SlotInfo s : slots(screen)) {
                for (int c = 0; c < CYCLE.length; c++) {
                    if (s.path().equals(CYCLE[c] + "_stained_glass_pane")) { panes.add(s); idx.add(c); }
                }
            }
            if (panes.isEmpty()) return;
            int best = 0, bestClicks = Integer.MAX_VALUE;
            for (int t = 0; t < CYCLE.length; t++) {
                int clicks = 0;
                for (int i : idx) clicks += Math.min(((t - i) % 5 + 5) % 5, ((i - t) % 5 + 5) % 5);
                if (clicks < bestClicks) { bestClicks = clicks; best = t; }
            }
            for (int p = 0; p < panes.size(); p++) {
                int i = idx.get(p);
                int fwd = ((best - i) % 5 + 5) % 5, back = ((i - best) % 5 + 5) % 5;
                if (fwd == 0) continue;
                if (fwd <= back) label(g, panes.get(p), String.valueOf(fwd), 0xFF55FF55);       // left-click this many times
                else label(g, panes.get(p), String.valueOf(-back), 0xFFFF5555);                // right-click this many times
            }
        }
    }

    private static boolean matchesColor(String path, String color) {
        if (path.startsWith(color + "_")) return true;
        return switch (color) {
            case "blue" -> path.equals("lapis_lazuli");
            case "brown" -> path.equals("cocoa_beans");
            case "white" -> path.equals("bone_meal");
            case "black" -> path.equals("ink_sac");
            default -> path.equals(color + "_dye");
        };
    }

    private Terminals() {}
}
