package dev.farmprofit;

import net.minecraft.client.Minecraft;

/** Mouse position (in GUI pixels) and button state, read safely at runtime. */
final class Mouse {
    static double x, y;
    static boolean left, right;

    static boolean update(Minecraft mc) {
        Object mh = Reflect.field(mc, "mouseHandler");
        Object win = Reflect.call(mc, "getWindow");
        double px = Reflect.num(Reflect.call(mh, "xpos"), Double.NaN);
        double py = Reflect.num(Reflect.call(mh, "ypos"), Double.NaN);
        double sw = Reflect.num(Reflect.call(win, new String[]{"getScreenWidth", "getWidth"}), 0);
        double sh = Reflect.num(Reflect.call(win, new String[]{"getScreenHeight", "getHeight"}), 0);
        double gw = Reflect.num(Reflect.call(win, "getGuiScaledWidth"), 0);
        double gh = Reflect.num(Reflect.call(win, "getGuiScaledHeight"), 0);
        if (Double.isNaN(px) || Double.isNaN(py) || sw <= 0 || sh <= 0) return false;
        x = px * gw / sw;
        y = py * gh / sh;
        left = Reflect.call(mh, "isLeftPressed") == Boolean.TRUE;
        right = Reflect.call(mh, "isRightPressed") == Boolean.TRUE;
        return true;
    }

    private Mouse() {}
}
