package dev.farmprofit;

import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Editing panels while chat is open (or in /profit gui):
 * left-drag = move, middle-click = change size, right-click the title = hide/show,
 * click the main title = switch the all-time line, right-click an item = stop counting it.
 */
final class HudEditor {
    /** One panel as placed this frame. x/y are screen coordinates; w/h are before scaling. */
    record Box(String id, String key, Hud.Lines lines, int x, int y, int w, int h, int lineH, float scale, boolean hidden) {
        int left() { return x - Math.round(3 * scale); }
        int top() { return y - Math.round(3 * scale); }
        int right() { return x + Math.round((w + 3) * scale); }
        int bottom() { return y + Math.round((h + 1) * scale); }
    }

    private static final double[] SIZES = {0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0};
    private static boolean prevLeft, prevRight, prevMiddle, moved;
    private static Box dragging;
    private static double offX, offY, pressX, pressY;

    /** Returns {box index, line index} under the mouse, or null. */
    static int[] update(Minecraft mc, List<Box> boxes) {
        if (!Mouse.update(mc)) { Debug.mouseWorks = false; return null; }
        Debug.mouseWorks = true;
        int[] hovered = null;
        for (int b = boxes.size() - 1; b >= 0 && hovered == null; b--) {
            Box box = boxes.get(b);
            if (Mouse.x >= box.left() && Mouse.x <= box.right() && Mouse.y >= box.top() && Mouse.y <= box.bottom()) {
                int i = (int) Math.floor((Mouse.y - box.y()) / (box.lineH() * box.scale()));
                hovered = new int[]{b, Math.max(0, Math.min(i, box.lines().size() - 1))};
            }
        }

        // left button: drag to move, plain click on the main title switches the all-time line
        if (Mouse.left && !prevLeft && hovered != null) {
            dragging = boxes.get(hovered[0]);
            offX = Mouse.x - dragging.x();
            offY = Mouse.y - dragging.y();
            pressX = Mouse.x;
            pressY = Mouse.y;
            moved = false;
        }
        if (dragging != null) {
            if (Mouse.left) {
                if (Math.abs(Mouse.x - pressX) > 3 || Math.abs(Mouse.y - pressY) > 3) moved = true;
                if (moved) {
                    Panels.Pos p = Panels.get(dragging.key());
                    p.x = (int) Math.max(0, Mouse.x - offX);
                    p.y = (int) Math.max(0, Mouse.y - offY);
                }
            } else {
                if (!moved && dragging.id().equals("main") && hovered != null && hovered[1] == 0) {
                    Config.get().hudShowTotal = !Config.get().hudShowTotal;
                    Config.save();
                    Tracker.say("§6[SkyAssist] §7All-time line " + (Config.get().hudShowTotal ? "§aon" : "§coff"));
                }
                dragging = null;
                Panels.save();
            }
        }

        // middle button: cycle the panel's size
        if (Mouse.middle && !prevMiddle && hovered != null) {
            Panels.Pos p = Panels.get(boxes.get(hovered[0]).key());
            int i = 0;
            while (i < SIZES.length - 1 && SIZES[i] < p.scale - 0.01) i++;
            p.scale = SIZES[(i + 1) % SIZES.length];
            Panels.save();
        }

        // right button: item line = stop counting it, title line = hide/show the panel
        if (Mouse.right && !prevRight && hovered != null) {
            Box box = boxes.get(hovered[0]);
            String item = box.lines().get(hovered[1]).item();
            Config cfg = Config.get();
            if (item != null) {
                if (!cfg.ignoredItems.contains(item)) {
                    cfg.ignoredItems.add(item);
                    Config.save();
                    Tracker.say("§6[SkyAssist] §7Hidden §f" + item + "§7 (not counted). Undo in Settings → Hidden items.");
                }
            } else if (hovered[1] == 0) {
                Panels.Pos p = Panels.get(box.key());
                p.hidden = !p.hidden;
                Panels.save();
                Tracker.say("§6[SkyAssist] §f" + Panels.title(box.id()) + " §7" + (p.hidden ? "hidden (/profit gui to show it again)" : "shown"));
            }
        }

        prevLeft = Mouse.left;
        prevRight = Mouse.right;
        prevMiddle = Mouse.middle;
        return dragging != null && moved ? null : hovered;
    }

    static void reset() {
        if (dragging != null) Panels.save();
        dragging = null;
        prevLeft = prevRight = prevMiddle = false;
    }

    private HudEditor() {}
}
