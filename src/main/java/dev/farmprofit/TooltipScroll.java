package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Long item descriptions that don't fit on screen can be scrolled with the mouse wheel.
 * The item name stays at the top; "▲ / ▼ N more" shows what's hidden.
 */
public final class TooltipScroll {
    private static int offset;
    private static int lastKey;
    private static long lastShown;
    private static boolean scrollable;

    /** Called for every tooltip (last, after enchant colors and prices were added). */
    public static void apply(List<Component> lines) {
        Config c = Config.get();
        if (!c.tooltipScroll || lines.size() < 3) return;
        int key = lines.get(0).getString().hashCode() * 31 + lines.size();
        if (key != lastKey) { lastKey = key; offset = 0; }           // new item: start at the top
        lastShown = System.currentTimeMillis();

        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int fit = Math.max(4, (screenH - 16) / 10);                   // ~10 px per tooltip line
        scrollable = lines.size() > fit;
        if (!scrollable) { offset = 0; return; }

        // line 0 (the name) is pinned; the body scrolls, with one line at each end for the "more" hints
        List<Component> body = new ArrayList<>(lines.subList(1, lines.size()));
        int window = fit - 3;
        int maxOffset = Math.max(0, body.size() - window);
        offset = Math.max(0, Math.min(offset, maxOffset));

        List<Component> out = new ArrayList<>();
        out.add(lines.get(0));
        out.add(Component.literal(offset > 0 ? "§8▲ " + offset + " more (scroll up)" : "§8 "));
        out.addAll(body.subList(offset, Math.min(body.size(), offset + window)));
        int below = body.size() - (offset + window);
        out.add(Component.literal(below > 0 ? "§8▼ " + below + " more (scroll down)" : "§8 "));
        lines.clear();
        lines.addAll(out);
    }

    /** Mouse wheel in a menu. Returns false (= we used it) when a long tooltip is showing. */
    public static boolean onScroll(double amount) {
        if (!Config.get().tooltipScroll || !scrollable || System.currentTimeMillis() - lastShown > 150 || amount == 0) return true;
        int step = Math.max(1, Config.get().tooltipScrollSpeed);
        offset += amount > 0 ? -step : step;
        if (offset < 0) offset = 0;
        return false;
    }

    private TooltipScroll() {}
}
