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
