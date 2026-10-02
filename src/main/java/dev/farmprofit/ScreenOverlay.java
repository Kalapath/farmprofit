package dev.farmprofit;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

/**
 * Draws on top of menus (used by the terminal solvers). Fabric's "after a screen is drawn" event got renamed
 * in 26.x, so it's looked up at runtime: if it can't be found, the overlay simply doesn't show.
 */
final class ScreenOverlay {
    static Boolean works;

    static void register(Object screen) {
        try {
            Class<?> events = Class.forName("net.fabricmc.fabric.api.client.screen.v1.ScreenEvents");
            for (Method m : events.getMethods()) {
                String n = m.getName();
                if (m.getParameterCount() != 1 || !(n.equals("afterRender") || n.equals("afterExtract") || n.equals("afterExtractRenderState"))) continue;
                Object event = m.invoke(null, screen);
                Class<?> listener = listenerType(m.getGenericReturnType());
                if (listener == null) continue;
                Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[]{listener}, (p, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "hashCode" -> System.identityHashCode(p);
                            case "equals" -> p == args[0];
                            default -> "SkyAssistOverlay";
                        };
                    }
                    if (args != null && args.length >= 2) {
                        try { Terminals.draw(args[0], args[1]); } catch (Throwable ignored) {}
                        try { InvSearch.draw(args[0], args[1]); } catch (Throwable ignored) {}
                    }
                    return null;
                });
                event.getClass().getMethod("register", Object.class).invoke(event, proxy);
                works = true;
                return;
            }
            works = false;
        } catch (Throwable t) {
            works = false;
        }
    }

    private static Class<?> listenerType(Type t) {
        if (t instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0] instanceof Class<?> c) return c;
        return null;
    }

    private ScreenOverlay() {}
}
