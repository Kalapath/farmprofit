package dev.farmprofit;

import net.minecraft.client.Minecraft;

/** /profit gui: opens chat in edit mode so every panel shows with an outline and can be moved, resized or hidden. */
final class GuiEditor {
    private static boolean openNextTick;

    static void open() { openNextTick = true; }

    static void tick(Minecraft mc) {
        if (!openNextTick || mc.screen != null) return;
        openNextTick = false;
        Object chat = null;
        try {
            Class<?> cls = Class.forName("net.minecraft.client.gui.screens.ChatScreen");
            for (var c : cls.getConstructors()) {
                Object[] args = switch (c.getParameterCount()) {
                    case 1 -> new Object[]{""};
                    case 2 -> new Object[]{"", false};
                    default -> null;
                };
                if (args == null) continue;
                try { chat = c.newInstance(args); break; } catch (Exception ignored) {}
            }
        } catch (Throwable ignored) {}
        if (chat instanceof net.minecraft.client.gui.screens.Screen screen) {
            HudRenderer.editMode = true;
            mc.setScreen(screen);
            Tracker.say("§6[Profit] §7HUD editor: drag panels, middle-click to resize, right-click a title to hide it. Close chat when done.");
        } else {
            Tracker.say("§6[Profit] §7Open chat (§fT§7) to edit the HUD: drag panels, middle-click to resize, right-click a title to hide.");
        }
    }

    private GuiEditor() {}
}
