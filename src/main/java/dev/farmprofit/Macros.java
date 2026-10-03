package dev.farmprofit;

import net.minecraft.client.Minecraft;

/** Keybind macros: a key you press runs a command (like Skytils' keybinds). */
public final class Macros {
    static void run(int n) {
        String text;
        try { text = (String) Config.class.getField("macro" + n).get(Config.get()); } catch (Exception e) { return; }
        if (text == null || text.isBlank()) return;
        Object conn = Minecraft.getInstance().getConnection();
        String t = text.trim();
        if (t.startsWith("/")) Reflect.call(conn, new String[]{"sendCommand"}, t.substring(1));
        else Reflect.call(conn, new String[]{"sendChat"}, t);
    }

    private Macros() {}
}
