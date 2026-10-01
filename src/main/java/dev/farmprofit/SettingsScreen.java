package dev.farmprofit;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Settings menu (/profit settings). Built automatically from the fields in Config:
 * anything marked with @Setting appears in its category, anything else under "Other",
 * so new features show up here without extra work.
 */
public final class SettingsScreen extends Screen {
    private static final String OTHER = "Other";
    /** Tab order. Categories not listed here come after these (and before "Other"). */
    private static final List<String> ORDER = List.of("General", "HUD", "Farming", "Mining", "Foraging", "Fishing",
            "Combat & Slayers", "Dungeons", "Kuudra", "Diana", "Bazaar flipping", "Items & areas");
    private static String category;
    private int page;
    private final List<Runnable> pending = new ArrayList<>();   // text boxes are applied on page change / close

    public SettingsScreen() {
        super(Component.literal("Profit Counter Settings"));
    }

    // ---------------- which fields go where ----------------

    private static Map<String, List<Field>> categories() {
        Map<String, List<Field>> map = new LinkedHashMap<>();
        List<Field> other = new ArrayList<>();
        for (Field f : Config.class.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || !Modifier.isPublic(mod)) continue;
            Setting s = f.getAnnotation(Setting.class);
            if (s == null) other.add(f);
            else map.computeIfAbsent(s.category(), k -> new ArrayList<>()).add(f);
        }
        Map<String, List<Field>> sorted = new LinkedHashMap<>();
        for (String c : ORDER) if (map.containsKey(c)) sorted.put(c, map.get(c));
        map.forEach((c, f) -> sorted.putIfAbsent(c, f));
        if (!other.isEmpty()) sorted.put(OTHER, other);
        return sorted;
    }

    private static String label(Field f) {
        Setting s = f.getAnnotation(Setting.class);
        if (s != null) return s.label();
        String n = f.getName().replaceAll("([a-z])([A-Z])", "$1 $2");   // camelCase -> words
        return Character.toUpperCase(n.charAt(0)) + n.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String desc(Field f) {
        Setting s = f.getAnnotation(Setting.class);
        return s != null && !s.desc().isEmpty() ? s.desc() : "Setting \"" + f.getName() + "\" in config.json";
    }

    // ---------------- layout ----------------

    @Override
    protected void init() {
        pending.clear();
        Map<String, List<Field>> cats = categories();
        if (category == null || !cats.containsKey(category)) category = cats.keySet().iterator().next();

        // category tabs (wrap onto more rows if needed)
        int x = 10, y = 8;
        for (String cat : cats.keySet()) {
            int w = font.width(cat) + 16;
            if (x + w > width - 10) { x = 10; y += 22; }
            final String c = cat;
            addRenderableWidget(Button.builder(Component.literal(cat.equals(category) ? "§e§l" + cat : cat), b -> {
                applyPending();
                category = c;
                page = 0;
                rebuildWidgets();
            }).bounds(x, y, w, 20).build());
            x += w + 4;
        }

        // rows of settings
        int top = y + 30, rowH = 24, bottom = height - 34;
        int perPage = Math.max(1, (bottom - top) / rowH);
        List<Field> fields = cats.get(category);
        int pages = (fields.size() + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));

        int labelW = 170, controlW = 180;
        int left = Math.max(10, width / 2 - (labelW + controlW + 10) / 2);
        int row = 0;
        for (int i = page * perPage; i < Math.min(fields.size(), (page + 1) * perPage); i++, row++) {
            Field f = fields.get(i);
            int ry = top + row * rowH;
            StringWidget lbl = new StringWidget(left, ry + 6, labelW, 10, Component.literal(label(f)), font);
            lbl.setTooltip(Tooltip.create(Component.literal(desc(f))));
            addRenderableWidget(lbl);
            addControl(f, left + labelW + 10, ry, controlW);
        }

        // bottom bar
        int by = height - 28;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> { applyPending(); page--; rebuildWidgets(); })
                    .bounds(width / 2 - 154, by, 70, 20).build()).active = page > 0;
            addRenderableWidget(new StringWidget(width / 2 - 80, by + 6, 60, 10,
                    Component.literal("§7" + (page + 1) + " / " + pages), font));
            addRenderableWidget(Button.builder(Component.literal("Next >"), b -> { applyPending(); page++; rebuildWidgets(); })
                    .bounds(width / 2 - 20, by, 70, 20).build()).active = page < pages - 1;
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 + 60, by, 90, 20).build());
    }

    /** The right control for the field's type. */
    private void addControl(Field f, int x, int y, int w) {
        Setting s = f.getAnnotation(Setting.class);
        Tooltip tip = Tooltip.create(Component.literal(desc(f)));
        Config cfg = Config.get();
        try {
            Class<?> t = f.getType();
            if (t == boolean.class) {
                Button b = Button.builder(onOff(f.getBoolean(cfg)), btn -> {
                    try {
                        f.setBoolean(cfg, !f.getBoolean(cfg));
                        btn.setMessage(onOff(f.getBoolean(cfg)));
                        Config.save();
                    } catch (Exception ignored) {}
                }).bounds(x, y, w, 20).build();
                b.setTooltip(tip);
                addRenderableWidget(b);
                return;
            }
            if (t == String.class && s != null && s.options().length > 0) {
                Button b = Button.builder(Component.literal(String.valueOf(f.get(cfg))), btn -> {
                    try {
                        List<String> opts = Arrays.asList(s.options());
                        int i = opts.indexOf(String.valueOf(f.get(cfg)));
                        String next = opts.get((i + 1) % opts.size());
                        f.set(cfg, next);
                        btn.setMessage(Component.literal(next));
                        Config.save();
                    } catch (Exception ignored) {}
                }).bounds(x, y, w, 20).build();
                b.setTooltip(tip);
                addRenderableWidget(b);
                return;
            }
            // everything else: a text box
            EditBox box = new EditBox(font, x, y, w, 20, Component.literal(label(f)));
            box.setMaxLength(2000);
            box.setValue(toText(f.get(cfg)));
            box.setTooltip(tip);
            addRenderableWidget(box);
            pending.add(() -> fromText(f, box.getValue()));
        } catch (Exception e) {
            addRenderableWidget(new StringWidget(x, y + 6, w, 10, Component.literal("§8(edit in config.json)"), font));
        }
    }

    private static Component onOff(boolean v) { return Component.literal(v ? "§aON" : "§cOFF"); }

    // ---------------- value <-> text ----------------

    private static String toText(Object v) {
        if (v == null) return "";
        if (v instanceof Double d) return d == Math.rint(d) && Math.abs(d) < 1e15 ? String.valueOf(d.longValue()) : String.valueOf(d);
        if (v instanceof List<?> l) return String.join(", ", l.stream().map(String::valueOf).toList());
        if (v instanceof Map<?, ?> m) {
            List<String> parts = new ArrayList<>();
            m.forEach((k, val) -> parts.add(k + "=" + val));
            return String.join(", ", parts);
        }
        return String.valueOf(v);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void fromText(Field f, String text) {
        Config cfg = Config.get();
        Setting s = f.getAnnotation(Setting.class);
        try {
            Class<?> t = f.getType();
            if (t == int.class || t == long.class || t == double.class || t == float.class) {
                double v = FlipsCommand.parseAmount(text);   // accepts 10m, 500k, 1.5b
                if (Double.isNaN(v)) return;                 // invalid: keep the old value
                if (s != null) v = Math.max(s.min(), Math.min(s.max(), v));
                if (t == int.class) f.setInt(cfg, (int) Math.round(v));
                else if (t == long.class) f.setLong(cfg, Math.round(v));
                else if (t == float.class) f.setFloat(cfg, (float) v);
                else f.setDouble(cfg, v);
            } else if (t == String.class) {
                f.set(cfg, text.trim());
            } else if (List.class.isAssignableFrom(t)) {
                List<String> list = new ArrayList<>();
                for (String part : text.split(",")) if (!part.isBlank()) list.add(part.trim());
                f.set(cfg, list);
            } else if (Map.class.isAssignableFrom(t)) {
                Map map = new java.util.HashMap<String, String>();
                for (String part : text.split(",")) {
                    int eq = part.indexOf('=');
                    if (eq > 0) map.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
                }
                f.set(cfg, map);
            }
        } catch (Exception ignored) {}
    }

    private void applyPending() {
        for (Runnable r : pending) r.run();
        Config.save();
    }

    @Override
    public void onClose() {
        applyPending();
        super.onClose();
    }

    // ---------------- opening ----------------

    private static boolean openNextTick;

    /** Called from a command: opens next tick, after the chat screen has closed. */
    public static void requestOpen() { openNextTick = true; }

    public static void requestOpen(String tab) { category = tab; openNextTick = true; }

    static void tick(net.minecraft.client.Minecraft mc) {
        if (openNextTick && mc.screen == null) {
            openNextTick = false;
            mc.setScreen(new SettingsScreen());
        }
    }
}
