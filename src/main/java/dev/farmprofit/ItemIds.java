package dev.farmprofit;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reads Hypixel's SkyBlock item ID (e.g. ENCHANTED_WHEAT) and lore from an item. */
public final class ItemIds {
    /** Display name -> real SkyBlock ID, learned from items you've held. Used to price sack items etc. */
    public static final Map<String, String> LEARNED = new ConcurrentHashMap<>();

    /** The SkyBlock ID stored on the item, or null. */
    public static String of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            var data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) return null;
            Object tag = Reflect.call(data, new String[]{"copyTag", "getUnsafe", "tag"});
            if (tag == Reflect.FAIL || tag == null) return null;
            Object id = Reflect.call(tag, "getStringOr", "id", "");
            if (id == Reflect.FAIL) id = Reflect.call(tag, "getString", "id");
            if (id instanceof java.util.Optional<?> opt) id = opt.orElse(null);
            return id instanceof String s && !s.isEmpty() ? s : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Remembers name -> ID (skips generic IDs shared by many different items). */
    public static void learn(String name, ItemStack stack) {
        if (LEARNED.containsKey(name)) return;
        String id = of(stack);
        if (id == null || id.equals("PET") || id.equals("ENCHANTED_BOOK") || id.equals("POTION") || id.equals("RUNE")) return;
        LEARNED.put(name, id);
    }

    /** Plain-text lore lines of an item. */
    public static List<String> lore(ItemStack stack) {
        List<String> out = new ArrayList<>();
        try {
            var lore = stack.get(DataComponents.LORE);
            if (lore == null) return out;
            for (Component c : lore.lines()) out.add(Tracker.strip(c.getString()));
        } catch (Throwable ignored) {}
        return out;
    }

    private ItemIds() {}
}
