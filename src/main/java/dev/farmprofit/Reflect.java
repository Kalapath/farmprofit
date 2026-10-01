package dev.farmprofit;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calls Minecraft methods by name at runtime. Used for things whose names Mojang keeps changing,
 * so the mod still compiles (and the feature just switches off) if a name is different.
 */
final class Reflect {
    static final Object FAIL = new Object();
    private static final Map<String, Method> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> MISSING = new ConcurrentHashMap<>();

    /** Tries each name in turn; returns the result, or FAIL if none worked. */
    static Object call(Object target, String[] names, Object... args) {
        if (target == null) return FAIL;
        Class<?> cls = target.getClass();
        for (String name : names) {
            String key = cls.getName() + "#" + name + "#" + args.length;
            if (MISSING.containsKey(key)) continue;
            Method cached = CACHE.get(key);
            if (cached != null) {
                try { return cached.invoke(target, args); } catch (Exception ignored) {}
            }
            boolean found = false;
            for (Method m : cls.getMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
                found = true;
                try {
                    Object r = m.invoke(target, args);
                    CACHE.put(key, m);
                    return r;
                } catch (Exception ignored) {}
            }
            if (!found) MISSING.put(key, true);
        }
        return FAIL;
    }

    static Object call(Object target, String name, Object... args) {
        return call(target, new String[]{name}, args);
    }

    static Object field(Object target, String name) {
        try { return target.getClass().getField(name).get(target); } catch (Exception e) { return FAIL; }
    }

    static double num(Object o, double fallback) {
        return o instanceof Number n ? n.doubleValue() : fallback;
    }

    private Reflect() {}
}
