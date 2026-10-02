package dev.farmprofit;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Mouse position (in GUI pixels) and buttons. Read straight from GLFW (the window library Minecraft uses),
 * which doesn't change between Minecraft versions; Minecraft's own mouse handler is only a fallback.
 */
final class Mouse {
    static double x, y;
    static boolean left, right, middle;

    static boolean update(Minecraft mc) {
        Object win = Reflect.call(mc, "getWindow");
        double gw = Reflect.num(Reflect.call(win, "getGuiScaledWidth"), 0);
        double gh = Reflect.num(Reflect.call(win, "getGuiScaledHeight"), 0);
        if (gw <= 0 || gh <= 0) return false;

        Object h = Reflect.call(win, new String[]{"handle", "getWindow", "getHandle"});
        if (h instanceof Long handle && handle != 0L) {
            try {
                double[] cx = new double[1], cy = new double[1];
                int[] ww = new int[1], wh = new int[1];
                GLFW.glfwGetCursorPos(handle, cx, cy);
                GLFW.glfwGetWindowSize(handle, ww, wh);
                if (ww[0] > 0 && wh[0] > 0) {
                    x = cx[0] * gw / ww[0];
                    y = cy[0] * gh / wh[0];
                    left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                    right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
                    middle = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // fallback: Minecraft's mouse handler
        Object mh = Reflect.field(mc, "mouseHandler");
        double px = Reflect.num(Reflect.call(mh, "xpos"), Double.NaN);
        double py = Reflect.num(Reflect.call(mh, "ypos"), Double.NaN);
        double sw = Reflect.num(Reflect.call(win, new String[]{"getScreenWidth", "getWidth"}), 0);
        double sh = Reflect.num(Reflect.call(win, new String[]{"getScreenHeight", "getHeight"}), 0);
        if (Double.isNaN(px) || Double.isNaN(py) || sw <= 0 || sh <= 0) return false;
        x = px * gw / sw;
        y = py * gh / sh;
        left = Reflect.call(mh, "isLeftPressed") == Boolean.TRUE;
        right = Reflect.call(mh, "isRightPressed") == Boolean.TRUE;
        middle = Reflect.call(mh, "isMiddlePressed") == Boolean.TRUE;
        return true;
    }

    private Mouse() {}
}
