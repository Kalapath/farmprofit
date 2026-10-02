package dev.farmprofit;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/**
 * Things Minecraft changed between 26.1 and 26.2, looked up at runtime so one source works on both.
 * (26.2: Minecraft.screen moved to Minecraft.gui.screen(), setScreen became setScreenAndShow.)
 */
final class Compat {
    private static Field screenField;
    private static boolean screenFieldChecked;

    /** The open screen (menu, chat...) or null. */
    static Object screen(Minecraft mc) {
        if (!screenFieldChecked) {
            screenFieldChecked = true;
            try { screenField = Minecraft.class.getField("screen"); } catch (Exception ignored) {}
        }
        try {
            if (screenField != null) return screenField.get(mc);
        } catch (Exception ignored) {}
        Object gui = Reflect.field(mc, "gui");
        Object s = Reflect.call(gui, new String[]{"screen", "getScreen"});
        return s == Reflect.FAIL ? null : s;
    }

    static boolean noScreen(Minecraft mc) { return screen(mc) == null; }

    /** Opens a screen (or closes it with null). */
    static void setScreen(Minecraft mc, Object screen) {
        for (var m : Minecraft.class.getMethods()) {
            if ((m.getName().equals("setScreen") || m.getName().equals("setScreenAndShow")) && m.getParameterCount() == 1) {
                try { m.invoke(mc, screen); return; } catch (Exception ignored) {}
            }
        }
    }

    private Compat() {}
}
