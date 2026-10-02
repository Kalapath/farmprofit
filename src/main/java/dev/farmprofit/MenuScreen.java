package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shared menu layout: tabs, buttons at the top, a table (columns + a button per row), pages, footer lines.
 * Every menu in the mod (history, flips, session, suggestions...) is built from this.
 */
public final class MenuScreen extends Screen {
    public record Action(String label, String tooltip, Runnable run) {}
    public record Row(String[] cells, String tooltip, List<Action> buttons) {
        public Row(String... cells) { this(cells, null, List.of()); }
    }
    public record Page(String[] columns, int[] widths, List<Row> rows, List<Action> top, List<String> footer) {}
    public record Tab(String name, Supplier<Page> page) {}

    private final String heading;
    private final List<Tab> tabs;
    private final Screen parent;
    private int tab, page;

    public MenuScreen(String heading, List<Tab> tabs, int startTab, Screen parent) {
        super(Component.literal(heading));
        this.heading = heading;
        this.tabs = tabs;
        this.parent = parent;
        this.tab = Math.max(0, Math.min(startTab, tabs.size() - 1));
    }

    @Override
    protected void init() {
        int left = Math.max(10, width / 2 - 230), right = Math.min(width - 10, width / 2 + 230);
        addRenderableWidget(new StringWidget(left, 8, right - left, 10, Component.literal("§6§l" + heading), font));

        // tabs
        int x = left, y = 22;
        if (tabs.size() > 1) {
            for (int i = 0; i < tabs.size(); i++) {
                String n = tabs.get(i).name();
                int w = font.width(n) + 12;
                if (x + w > right) { x = left; y += 20; }
                final int t = i;
                addRenderableWidget(Button.builder(Component.literal(i == tab ? "§e§l" + n : n), b -> { tab = t; page = 0; rebuildWidgets(); })
                        .bounds(x, y, w, 18).build());
                x += w + 3;
            }
            y += 22;
        }

        Page p;
        try { p = tabs.isEmpty() ? null : tabs.get(tab).page().get(); }
        catch (Exception e) { p = new Page(new String[]{"§cCouldn't build this page: " + e.getMessage()}, new int[]{400}, List.of(), List.of(), List.of()); }
        if (p == null) { closeButton(right); return; }

        // buttons at the top
        x = left;
        if (!p.top().isEmpty()) {
            for (Action a : p.top()) {
                int w = font.width(a.label()) + 14;
                if (x + w > right) { x = left; y += 20; }
                Button b = Button.builder(Component.literal(a.label()), btn -> a.run().run()).bounds(x, y, w, 18).build();
                if (a.tooltip() != null) b.setTooltip(Tooltip.create(Component.literal(a.tooltip())));
                addRenderableWidget(b);
                x += w + 3;
            }
            y += 22;
        }

        // column titles
        int[] colX = new int[p.widths().length];
        int cx = left;
        for (int i = 0; i < p.widths().length; i++) { colX[i] = cx; cx += p.widths()[i]; }
        for (int i = 0; i < p.columns().length && i < colX.length; i++) {
            addRenderableWidget(new StringWidget(colX[i], y, p.widths()[i], 10, Component.literal("§7§n" + p.columns()[i]), font));
        }
        y += 14;

        // rows
        int footerH = p.footer().size() * 11 + 30;
        int rowH = 20, perPage = Math.max(1, (height - footerH - y) / rowH);
        int pages = Math.max(1, (p.rows().size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        if (p.rows().isEmpty()) addRenderableWidget(new StringWidget(left, y + 4, right - left, 10, Component.literal("§8Nothing here yet."), font));
        int r = 0;
        for (int i = page * perPage; i < Math.min(p.rows().size(), (page + 1) * perPage); i++, r++) {
            Row row = p.rows().get(i);
            int ry = y + r * rowH;
            for (int c = 0; c < row.cells().length && c < colX.length; c++) {
                StringWidget w = new StringWidget(colX[c], ry + 5, p.widths()[c] - 4, 10, Component.literal(row.cells()[c]), font);
                if (row.tooltip() != null) w.setTooltip(Tooltip.create(Component.literal(row.tooltip())));
                addRenderableWidget(w);
            }
            int bx = right;
            for (int b = row.buttons().size() - 1; b >= 0; b--) {
                Action a = row.buttons().get(b);
                int w = font.width(a.label()) + 12;
                bx -= w;
                Button btn = Button.builder(Component.literal(a.label()), x2 -> a.run().run()).bounds(bx, ry, w, 18).build();
                if (a.tooltip() != null) btn.setTooltip(Tooltip.create(Component.literal(a.tooltip())));
                addRenderableWidget(btn);
                bx -= 3;
            }
        }

        // footer
        int fy = height - footerH + 6;
        for (String f : p.footer()) {
            addRenderableWidget(new StringWidget(left, fy, right - left - 90, 10, Component.literal(f), font));
            fy += 11;
        }
        if (pages > 1) {
            int py = height - 26;
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); }).bounds(right - 150, py, 20, 20).build()).active = page > 0;
            addRenderableWidget(new StringWidget(right - 126, py + 6, 34, 10, Component.literal("§7" + (page + 1) + "/" + pages), font));
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); }).bounds(right - 90, py, 20, 20).build()).active = page < pages - 1;
        }
        closeButton(right);
    }

    private void closeButton(int right) {
        addRenderableWidget(Button.builder(Component.literal(parent != null ? "Back" : "Done"), b -> onClose())
                .bounds(right - 60, height - 26, 60, 20).build());
    }

    /** Rebuild after something changed (e.g. a row was removed). */
    public void refresh() { rebuildWidgets(); }

    @Override
    public void onClose() { Compat.setScreen(Minecraft.getInstance(), parent); }

    // ---------------- opening ----------------

    private static Supplier<Screen> pending;

    /** Open a menu from a command (next tick, once chat has closed). */
    public static void open(Supplier<Screen> screen) { pending = screen; }

    static void tick(Minecraft mc) {
        if (pending != null && Compat.noScreen(mc)) {
            Supplier<Screen> s = pending;
            pending = null;
            Compat.setScreen(mc, s.get());
        }
    }

    /** Closes the menu and runs a Hypixel command (because you clicked it). */
    public static void runCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        Compat.setScreen(mc, null);
        if (Reflect.call(mc.getConnection(), "sendCommand", command) == Reflect.FAIL) Tracker.say("§6[SkyAssist] §7Type §f/" + command);
    }

    static List<Row> rows() { return new ArrayList<>(); }
}
