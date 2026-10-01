package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Clickable chat lines and a notification sound, built at runtime so renamed classes can't break the build. */
final class Chat {

    /** A chat line that runs a command when clicked and shows hover text. Falls back to plain text. */
    static Component clickable(String text, String command, String hover) {
        MutableComponent c = Component.literal(text);
        try {
            Object click = Class.forName("net.minecraft.network.chat.ClickEvent$RunCommand")
                    .getConstructor(String.class).newInstance(command);
            Object hov = Class.forName("net.minecraft.network.chat.HoverEvent$ShowText")
                    .getConstructor(Component.class).newInstance(Component.literal(hover));
            return c.withStyle(style -> {
                Object s1 = Reflect.call(style, "withClickEvent", click);
                Object s2 = s1 instanceof Style ? Reflect.call(s1, "withHoverEvent", hov) : Reflect.FAIL;
                if (s2 instanceof Style done) return done;
                if (s1 instanceof Style half) return half;
                return style;
            });
        } catch (Throwable t) {
            return c;
        }
    }

    /** Copies text to the clipboard. Returns false if that isn't possible. */
    static boolean copy(String text) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Object kb = mc.getClass().getField("keyboardHandler").get(mc);
            kb.getClass().getMethod("setClipboard", String.class).invoke(kb, text);
            return true;
        } catch (Throwable t) { return false; }
    }

    /** A chat line that opens a web page when clicked. Falls back to showing the address. */
    static Component link(String text, String url) {
        MutableComponent c = Component.literal(text);
        try {
            Class<?> cls = Class.forName("net.minecraft.network.chat.ClickEvent$OpenUrl");
            Object click;
            try { click = cls.getConstructor(java.net.URI.class).newInstance(java.net.URI.create(url)); }
            catch (NoSuchMethodException e) { click = cls.getConstructor(String.class).newInstance(url); }
            final Object ev = click;
            return c.withStyle(style -> Reflect.call(style, "withClickEvent", ev) instanceof Style s ? s : style);
        } catch (Throwable t) {
            return Component.literal(text + " §7" + url);
        }
    }

    /** Plays a short "ding" (note block pling). Silently does nothing if unavailable. */
    static void ping() {
        try {
            Minecraft mc = Minecraft.getInstance();
            Object sound = Class.forName("net.minecraft.sounds.SoundEvents").getField("NOTE_BLOCK_PLING").get(null);
            Class<?> ssi = Class.forName("net.minecraft.client.resources.sounds.SimpleSoundInstance");
            Object instance = null;
            for (var m : ssi.getMethods()) {
                if (!m.getName().equals("forUI") || m.getParameterCount() != 2) continue;
                try { instance = m.invoke(null, sound, 1.5f); break; } catch (Exception ignored) {}
            }
            if (instance == null) return;
            Object manager = Reflect.call(mc, "getSoundManager");
            Reflect.call(manager, "play", instance);
        } catch (Throwable ignored) {}
    }

    private Chat() {}
}
