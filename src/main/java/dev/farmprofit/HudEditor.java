package dev.farmprofit;

import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * While chat is open: drag any HUD panel with the left mouse button (each keeps its own position),
 * right-click an item line to hide it.
 */
final class HudEditor {
    /** One panel as drawn this frame. x/y/w/h are in (scaled) drawing coordinates. */
    record Box(String id, Hud.Lines lines, int x, int y, int w, int h) {}

    private static boolean prevLeft, prevRight;
    private static String dragging;
    private static double offX, offY;

    /** Returns {panel index, line index} under the mouse, or null. */
    static int[] update(Minecraft mc, List<Box> boxes, float scale) {
        if (!Mouse.update(mc)) { Debug.mouseWorks = false; return null; }
        Debug.mouseWorks = true;
        Config cfg = Config.get();
        double mx = Mouse.x / scale, my = Mouse.y / scale;
        int[] hovered = null;
        for (int b = boxes.size() - 1; b >= 0 && hovered == null; b--) {
            Box box = boxes.get(b);
            if (mx >= box.x() - 3 && mx <= box.x() + box.w() + 3 && my >= box.y() - 3 && my <= box.y() + box.h() + 1) {
                int i = (int) Math.floor((my - box.y()) / 10.0);
                hovered = new int[]{b, Math.max(-1, Math.min(i, box.lines().size() - 1))};
            }
        }

        if (Mouse.left && !prevLeft && hovered != null) {
            Box box = boxes.get(hovered[0]);
            dragging = box.id();
            double px = box.x() * scale, py = box.y() * scale;
            offX = Mouse.x - px;
            offY = Mouse.y - py;
        }
        if (dragging != null) {
            if (Mouse.left) {
                int nx = (int) Math.max(0, Mouse.x - offX), ny = (int) Math.max(0, Mouse.y - offY);
                if (dragging.equals("bazaar")) { cfg.bazaarHudX = nx; cfg.bazaarHudY = ny; }
                else { cfg.hudX = nx; cfg.hudY = ny; }
            } else {
                dragging = null;
                Config.save();
            }
        }

        if (Mouse.right && !prevRight && hovered != null && hovered[1] >= 0) {
            Hud.HudLine line = boxes.get(hovered[0]).lines().get(hovered[1]);
            String item = line.item();
            if (item != null && !cfg.ignoredItems.contains(item)) {
                cfg.ignoredItems.add(item);
                Config.save();
                Tracker.say("§6[Profit] §7Hidden §f" + item + "§7 (not counted). Undo: §f/profit unignore " + item);
            }
        }

        prevLeft = Mouse.left;
        prevRight = Mouse.right;
        return dragging != null ? null : hovered;
    }

    static void reset() {
        if (dragging != null) Config.save();
        dragging = null;
        prevLeft = prevRight = false;
    }

    private HudEditor() {}
}
