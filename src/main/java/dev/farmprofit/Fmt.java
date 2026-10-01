package dev.farmprofit;

import java.util.Locale;

public final class Fmt {
    public static String coins(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format(Locale.US, "%.2fB", v / 1e9);
        if (a >= 1e6) return String.format(Locale.US, "%.2fM", v / 1e6);
        if (a >= 1e3) return String.format(Locale.US, "%.1fk", v / 1e3);
        return String.format(Locale.US, "%.0f", v);
    }

    public static String num(long v) { return String.format(Locale.US, "%,d", v); }

    public static String duration(long ms) {
        long s = ms / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return String.format(Locale.US, "%dh %02dm %02ds", h, m, sec);
        return String.format(Locale.US, "%dm %02ds", m, sec);
    }

    public static String clock(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private Fmt() {}
}
