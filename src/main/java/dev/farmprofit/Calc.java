package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Calculator: "/calc 64x8", and in sign inputs (Bazaar / AH amounts) type a sum ending in "=",
 * e.g. "64x8=" or "10m/3=", and it's replaced by the result. Understands k, m, b and x for multiply.
 */
public final class Calc {

    /** Evaluates "64x8", "10m/3", "(2.5k+500)*4". NaN if it isn't a sum. */
    public static double eval(String text) {
        String s = text.toLowerCase(Locale.ROOT).replace(",", "").replace(" ", "").replace('x', '*').replace('×', '*');
        if (s.isEmpty()) return Double.NaN;
        try {
            Parser p = new Parser(s);
            double v = p.expr();
            return p.pos == s.length() ? v : Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public static String format(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.format(Locale.US, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static final class Parser {
        final String s;
        int pos;
        Parser(String s) { this.s = s; }

        double expr() {
            double v = term();
            while (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                char op = s.charAt(pos++);
                double r = term();
                v = op == '+' ? v + r : v - r;
            }
            return v;
        }

        double term() {
            double v = factor();
            while (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/')) {
                char op = s.charAt(pos++);
                double r = factor();
                v = op == '*' ? v * r : v / r;
            }
            return v;
        }

        double factor() {
            if (pos < s.length() && s.charAt(pos) == '-') { pos++; return -factor(); }
            if (pos < s.length() && s.charAt(pos) == '(') {
                pos++;
                double v = expr();
                if (pos < s.length() && s.charAt(pos) == ')') pos++;
                return v;
            }
            int start = pos;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (start == pos) throw new IllegalArgumentException();
            double v = Double.parseDouble(s.substring(start, pos));
            if (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == 'k') { v *= 1e3; pos++; }
                else if (c == 'm') { v *= 1e6; pos++; }
                else if (c == 'b') { v *= 1e9; pos++; }
            }
            return v;
        }
    }

    // ---------------- sign inputs ----------------

    private static String lastPreview = "";

    /** Every tick while a sign editor is open: "64x8=" becomes "512"; otherwise show a preview. */
    public static void tick(Minecraft mc) {
        if (!Config.get().signCalculator) return;
        Object screen = Compat.screen(mc);
        if (screen == null || !screen.getClass().getName().contains("SignEditScreen") && !hasField(screen.getClass(), "messages")) return;
        try {
            Field mf = findField(screen.getClass(), "messages");
            Field lf = findField(screen.getClass(), "line");
            if (mf == null || lf == null) return;
            String[] messages = (String[]) mf.get(screen);
            int line = lf.getInt(screen);
            String text = messages[line];
            if (text == null) return;
            boolean apply = text.endsWith("=");
            String sum = apply ? text.substring(0, text.length() - 1) : text;
            if (!sum.matches(".*[0-9].*") || !sum.matches(".*[-+*/xX()kKmMbB].*")) return;
            double v = eval(sum);
            if (Double.isNaN(v)) return;
            String result = format(Math.floor(v * 10) / 10.0);   // Bazaar amounts / prices: at most one decimal
            if (apply) {
                Method set = findMethod(screen.getClass(), "setMessage", String.class);
                if (set != null) set.invoke(screen, result); else messages[line] = result;
                Object helper = findField(screen.getClass(), "signField") != null ? findField(screen.getClass(), "signField").get(screen) : null;
                Reflect.call(helper, "setCursorToEnd");
                lastPreview = "";
            } else if (!result.equals(lastPreview)) {
                lastPreview = result;
                actionBar(mc, "§7" + sum + " §f= §a" + result + " §8(type = to use it)");
            }
        } catch (Throwable ignored) {}
    }

    private static void actionBar(Minecraft mc, String text) {
        Component c = Component.literal(text);
        if (Reflect.call(mc.player, "displayClientMessage", c, true) != Reflect.FAIL) return;
        Object gui = Reflect.field(mc, "gui");
        Reflect.call(gui, "setOverlayMessage", c, false);
    }

    private static boolean hasField(Class<?> c, String name) { return findField(c, name) != null; }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { Field f = k.getDeclaredField(name); f.setAccessible(true); return f; } catch (Exception ignored) {}
        }
        return null;
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... params) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { Method m = k.getDeclaredMethod(name, params); m.setAccessible(true); return m; } catch (Exception ignored) {}
        }
        return null;
    }

    private Calc() {}
}
