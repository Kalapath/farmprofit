package dev.farmprofit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private int scroll, listHeight, contentHeight;
    private String query = "";
    private Page cachedPage;
    private int cachedTab = -1;
    private boolean dirty = true;
    private boolean searchable;

    /** Adds a search box at the top right that filters the rows. */
    public MenuScreen searchable() { this.searchable = true; return this; }

    private void scrollBy(int px) {
        scroll = Math.max(0, Math.min(scroll + px, Math.max(0, contentHeight - listHeight)));
        rebuildWidgets();
    }

    /** Mouse wheel scrolls the list. (No @Override on purpose: if Minecraft renames it, this just goes unused.) */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical == 0 || contentHeight <= listHeight) return false;
        scrollBy(vertical > 0 ? -30 : 30);
        return true;
    }

    /** Splits text into lines that fit the width, keeping color codes going on the next line. */
    private List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        if (font.width(text) <= width) { out.add(text); return out; }
        StringBuilder line = new StringBuilder();
        String carry = "";
        for (String word : text.split(" ")) {
            String tryLine = line.length() == 0 ? carry + word : line + " " + word;
            if (font.width(tryLine) > width && line.length() > 0) {
                out.add(line.toString());
                carry = lastColors(line.toString());
                line = new StringBuilder(carry + word);
            } else {
                line = new StringBuilder(tryLine);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    private static String lastColors(String s) {
        String color = "";
        for (int i = 0; i < s.length() - 1; i++) if (s.charAt(i) == '§') {
            char c = Character.toLowerCase(s.charAt(i + 1));
            if ("0123456789abcdef".indexOf(c) >= 0) color = "§" + c; else if (c == 'l' || c == 'o') color += "§" + c;
        }
        return color;
    }

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
        addRenderableWidget(new StringWidget(left, 8, right - left - (searchable ? 160 : 0), 10, Component.literal("§6§l" + heading), font));

        // tabs
        int x = left, y = 22;
        if (tabs.size() > 1) {
            for (int i = 0; i < tabs.size(); i++) {
                String n = tabs.get(i).name();
                int w = font.width(n) + 12;
                if (x + w > right) { x = left; y += 20; }
                final int t = i;
                addRenderableWidget(Button.builder(Component.literal(i == tab ? "§e§l" + n : n), b -> { tab = t; page = 0; scroll = 0; rebuildWidgets(); })
                        .bounds(x, y, w, 18).build());
                x += w + 3;
            }
            y += 22;
        }

        Page p;
        if (cachedPage != null && cachedTab == tab && !dirty) p = cachedPage;        // scrolling / searching: reuse
        else {
            try { p = tabs.isEmpty() ? null : tabs.get(tab).page().get(); }
            catch (Exception e) { p = new Page(new String[]{"§cCouldn't build this page: " + e.getMessage()}, new int[]{400}, List.of(), List.of(), List.of()); }
            cachedPage = p;
            cachedTab = tab;
            dirty = false;
        }
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

        // search box (filters every row of every tab by its text and hover text)
        if (searchable) {
            EditBox search = new EditBox(font, right - 150, 8, 150, 14, Component.literal("Search"));
            search.setMaxLength(40);
            search.setHint(Component.literal("§8Search..."));
            search.setValue(query);
            search.setResponder(t -> { if (!t.equals(query)) { query = t; scroll = 0; rebuildWidgets(); } });
            addRenderableWidget(search);
            setFocused(search);
        }

        // column titles
        int[] colX = new int[p.widths().length];
        int cx = left;
        for (int i = 0; i < p.widths().length; i++) { colX[i] = cx; cx += p.widths()[i]; }
        for (int i = 0; i < p.columns().length && i < colX.length; i++) {
            addRenderableWidget(new StringWidget(colX[i], y, p.widths()[i], 10, Component.literal("§7§n" + p.columns()[i]), font));
        }
        y += 14;

        // rows: long text wraps onto more lines; the list scrolls with the mouse wheel
        List<Row> rowsAll = new ArrayList<>();
        String q = query.toLowerCase(java.util.Locale.ROOT).trim();
        for (Row row : p.rows()) {
            if (q.isEmpty()) { rowsAll.add(row); continue; }
            StringBuilder hay = new StringBuilder();
            for (String c : row.cells()) hay.append(c).append(' ');
            if (row.tooltip() != null) hay.append(row.tooltip());
            if (Tracker.strip(hay.toString()).toLowerCase(java.util.Locale.ROOT).contains(q)) rowsAll.add(row);
        }
        int footerH = p.footer().size() * 11 + 30;
        int listTop = y, listBottom = height - footerH;
        listHeight = listBottom - listTop;
        if (rowsAll.isEmpty()) addRenderableWidget(new StringWidget(left, y + 4, right - left, 10,
                Component.literal(q.isEmpty() ? "§8Nothing here yet." : "§8Nothing matches \"" + query + "\"."), font));
        // lay out every row (wrapped), then show the part inside the scroll window
        int buttonsW = 0;
        for (Row row : rowsAll) {
            int bw = 0;
            for (Action a : row.buttons()) bw += font.width(a.label()) + 15;
            buttonsW = Math.max(buttonsW, bw);
        }
        int[] heights = new int[rowsAll.size()];
        List<List<List<String>>> wrapped = new ArrayList<>();
        contentHeight = 0;
        for (int i = 0; i < rowsAll.size(); i++) {
            Row row = rowsAll.get(i);
            List<List<String>> cellsLines = new ArrayList<>();
            int lines = 1;
            for (int c = 0; c < row.cells().length && c < colX.length; c++) {
                boolean last = c == Math.min(row.cells().length, colX.length) - 1;
                int width = (last ? (right - buttonsW) - colX[c] : p.widths()[c]) - 4;
                List<String> wl = wrap(row.cells()[c], Math.max(30, width));
                cellsLines.add(wl);
                lines = Math.max(lines, wl.size());
            }
            wrapped.add(cellsLines);
            heights[i] = Math.max(20, lines * 10 + 8);
            contentHeight += heights[i];
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - listHeight)));
        int ry = listTop - scroll;
        for (int i = 0; i < rowsAll.size(); i++) {
            Row row = rowsAll.get(i);
            int h = heights[i];
            if (ry + h > listTop && ry < listBottom) {
                if (ry >= listTop - 2 && ry + h <= listBottom + 2) {          // only rows fully in view (widgets can't be clipped)
                    List<List<String>> cellsLines = wrapped.get(i);
                    for (int c = 0; c < cellsLines.size(); c++) {
                        List<String> wl = cellsLines.get(c);
                        for (int li = 0; li < wl.size(); li++) {
                            StringWidget w = new StringWidget(colX[c], ry + 5 + li * 10, Math.max(30, font.width(wl.get(li)) + 2), 10, Component.literal(wl.get(li)), font);
                            if (row.tooltip() != null) w.setTooltip(Tooltip.create(Component.literal(row.tooltip())));
                            addRenderableWidget(w);
                        }
                    }
                    int bx = right;
                    for (int b = row.buttons().size() - 1; b >= 0; b--) {
                        Action a = row.buttons().get(b);
                        int w = font.width(a.label()) + 12;
                        bx -= w;
                        Button btn = Button.builder(Component.literal(a.label()), x2 -> a.run().run()).bounds(bx, ry + (h - 18) / 2 - 2, w, 18).build();
                        if (a.tooltip() != null) btn.setTooltip(Tooltip.create(Component.literal(a.tooltip())));
                        addRenderableWidget(btn);
                        bx -= 3;
                    }
                }
            }
            ry += h;
        }
        // scroll buttons (the mouse wheel does the same)
        if (contentHeight > listHeight) {
            int py = height - 26;
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollBy(-listHeight + 20)).bounds(right - 150, py, 20, 20).build()).active = scroll > 0;
            int pct = (int) Math.round(100.0 * scroll / Math.max(1, contentHeight - listHeight));
            addRenderableWidget(new StringWidget(right - 126, py + 6, 34, 10, Component.literal("§7" + pct + "%"), font));
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollBy(listHeight - 20)).bounds(right - 90, py, 20, 20).build()).active = scroll < contentHeight - listHeight;
        }

        // footer
        int fy = height - footerH + 6;
        for (String f : p.footer()) {
            addRenderableWidget(new StringWidget(left, fy, right - left - 90, 10, Component.literal(f), font));
            fy += 11;
        }
        closeButton(right);
    }

    private void closeButton(int right) {
        addRenderableWidget(Button.builder(Component.literal(parent != null ? "Back" : "Done"), b -> onClose())
                .bounds(right - 60, height - 26, 60, 20).build());
    }

    /** Rebuild after something changed (e.g. a row was removed). */
    public void refresh() { dirty = true; rebuildWidgets(); }

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
