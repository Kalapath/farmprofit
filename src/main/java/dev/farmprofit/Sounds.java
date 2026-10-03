package dev.farmprofit;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Mutes server sounds you choose (by name), plus quick switches for the usual annoying ones. */
public final class Sounds {
    public static volatile boolean hooked;
    private static final Set<String> logged = new HashSet<>();

    /** The sound's name, e.g. "entity.generic.explode". */
    private static String id(Object packet) {
        Object holder = Reflect.call(packet, new String[]{"getSound", "sound"});
        Object event = Reflect.call(holder, new String[]{"value", "get"});
        if (event == Reflect.FAIL || event == null) event = holder;
        Object loc = Reflect.call(event, new String[]{"location", "getLocation"});
        String s = loc == Reflect.FAIL || loc == null ? String.valueOf(event) : String.valueOf(loc);
        return s.replace("minecraft:", "");
    }

    public static boolean shouldMute(Object packet) {
        hooked = true;
        Config c = Config.get();
        if (!c.muteExplosions && c.mutedSounds.isEmpty() && !c.logSounds) return false;
        String id;
        try { id = id(packet).toLowerCase(Locale.ROOT); } catch (Throwable t) { return false; }
        if (c.logSounds && logged.add(id)) Tracker.say("§8[Sound] §7" + id);
        if (c.muteExplosions && id.contains("explode")) return true;
        for (String m : c.mutedSounds) if (!m.isBlank() && id.contains(m.trim().toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private Sounds() {}
}
