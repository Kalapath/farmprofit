package dev.farmprofit;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Adds Bazaar / NPC / lowest BIN prices to item tooltips. */
public final class PriceTooltip {
    public static void add(ItemStack stack, List<Component> lines) {
        if (!Config.get().priceTooltips || stack == null || stack.isEmpty() || !Prices.loaded()) return;
        String name = Tracker.strip(stack.getHoverName().getString());
        String id = ItemIds.of(stack);
        if (id == null) id = Prices.idFor(name);
        if (id == null) return;
        int count = stack.getCount();
        double[] bz = Prices.bazaarRaw(id);
        double npc = Prices.npcPrice(id), bin = Prices.binPrice(id);
        if (bz == null && npc <= 0 && bin <= 0) return;
        if (bz != null) lines.add(Component.literal("§6Bazaar §7sell §f" + Fmt.coins(bz[0]) + " §7buy §f" + Fmt.coins(bz[1])
                + (count > 1 ? " §8(x" + count + ": " + Fmt.coins(bz[0] * count) + ")" : "")));
        if (bin > 0) lines.add(Component.literal("§6Lowest BIN §f" + Fmt.coins(bin)));
        if (npc > 0) lines.add(Component.literal("§6NPC §f" + Fmt.coins(npc) + (count > 1 ? " §8(x" + count + ": " + Fmt.coins(npc * count) + ")" : "")));
    }

    private PriceTooltip() {}
}
