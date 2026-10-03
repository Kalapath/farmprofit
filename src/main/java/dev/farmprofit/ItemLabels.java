package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small labels on items in menus: pet level, minion tier, enchanted book level. */
public final class ItemLabels {
    private static final Pattern PET = Pattern.compile("^\\[Lvl (\\d+)\\]");
    private static final Pattern MINION = Pattern.compile("Minion ([IVX]+)$");
    private static final Pattern ENCHANT = Pattern.compile("^[A-Z][A-Za-z' -]+ ([IVX]+)$");

    static String label(ItemStack stack) {
        String name = Tracker.strip(stack.getHoverName().getString()).trim();
        Matcher m = PET.matcher(name);
        if (m.find()) return "§6" + m.group(1);
        m = MINION.matcher(name);
        if (m.find()) return "§b" + m.group(1);
        if (name.equals("Enchanted Book")) {
            List<String> lore = ItemIds.lore(stack);
            if (!lore.isEmpty()) { m = ENCHANT.matcher(lore.get(0).trim()); if (m.matches()) return "§d" + m.group(1); }
        }
        return null;
    }

    private static int intField(Object o, String name) {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try { Field f = k.getDeclaredField(name); f.setAccessible(true); return f.getInt(o); } catch (Exception ignored) {}
        }
        return Integer.MIN_VALUE;
    }

    static void draw(Object screen, Object g) {
        if (!Config.get().itemLabels) return;
        int left = intField(screen, "leftPos"), top = intField(screen, "topPos");
        if (left == Integer.MIN_VALUE) return;
        Object slots = Reflect.field(Reflect.call(screen, "getMenu"), "slots");
        if (!(slots instanceof List<?> list)) return;
        Object font = Minecraft.getInstance().font;
        for (Object slot : list) {
            Object st = Reflect.call(slot, "getItem");
            if (!(st instanceof ItemStack stack) || stack.isEmpty()) continue;
            String l = label(stack);
            if (l == null) continue;
            int sx = intField(slot, "x"), sy = intField(slot, "y");
            if (sx == Integer.MIN_VALUE) continue;
            Reflect.call(g, new String[]{"text", "drawString"}, font, l, left + sx, top + sy, 0xFFFFFFFF, true);
        }
    }

    private ItemLabels() {}
}
