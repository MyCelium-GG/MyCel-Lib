package my.celium.org.client;

import java.util.ArrayList;
import java.util.List;

import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.config.ValueKind;
import my.celium.org.util.TextUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Reusable config screen: one tab per category, one row per value, with
 * inline validation, reset-all and save-on-done.
 *
 * <p>Built only from stable vanilla widgets ({@link Button}, {@link EditBox},
 * {@link StringWidget}) composed in {@link #init()} - no custom rendering, no
 * third-party GUI library. Values are applied to the live config only when
 * every edit is valid and the player presses Done.
 */
public final class MycelConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int TOP_ROWS = 64;
    private static final int BOTTOM_ROWS = 36;

    private final Screen parent;
    private final MycelConfig config;
    private String category;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private Button doneButton;
    private StringWidget scrollHint;

    /**
     * Creates the screen; {@code ClientHooks.createConfigScreen} is the usual way in.
     *
     * @param parent the screen to return to when it closes, or {@code null}
     * @param config the config to edit
     */
    public MycelConfigScreen(Screen parent, MycelConfig config) {
        super(Component.translatableWithFallback("mycel.config.title", "%s settings", config.metadata().name()));
        this.parent = parent;
        this.config = config;
        List<String> categories = config.categories();
        this.category = categories.isEmpty() ? "" : categories.get(0);
    }

    @Override
    protected void init() {
        clearWidgets();
        rows.clear();
        scroll = 0;

        String titleText = this.title.getString();
        int titleWidth = Math.min(this.width - 20, this.font.width(titleText) + 8);
        addRenderableWidget(new StringWidget(this.width / 2 - titleWidth / 2, 10, titleWidth, 12,
                this.title, this.font));

        List<String> categories = config.categories();
        if (categories.size() > 1) {
            int tabWidth = Math.min(120, (this.width - 20) / categories.size());
            int x = (this.width - tabWidth * categories.size()) / 2;
            for (String tab : categories) {
                Component label = tab.isEmpty()
                        ? Component.translatableWithFallback("mycel.config.general", "General")
                        : Component.translatableWithFallback(config.metadata().id() + ".config.category." + tab, humanize(tab));
                Button tabButton = Button.builder(label, button -> {
                    category = tab;
                    init();
                }).bounds(x, 28, tabWidth - 4, 20).build();
                tabButton.active = !tab.equals(category);
                addRenderableWidget(tabButton);
                x += tabWidth;
            }
        }

        int index = 0;
        for (ConfigValue<?> value : config.valuesIn(category)) {
            rows.add(Row.create(this, value, TOP_ROWS + index * ROW_HEIGHT));
            index++;
        }

        int bottomY = this.height - 28;
        doneButton = Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(this.width / 2 - 105, bottomY, 100, 20).build();
        addRenderableWidget(doneButton);
        addRenderableWidget(Button.builder(Component.translatableWithFallback("mycel.config.reset_all", "Reset all"), button -> {
            config.resetAll();
            init();
        }).bounds(this.width / 2 + 5, bottomY, 100, 20).build());

        scrollHint = new StringWidget(this.width - 110, this.height - 44, 100, 10,
                Component.translatableWithFallback("mycel.config.scroll", "scroll for more"), this.font);
        scrollHint.visible = contentHeight() > visibleHeight();
        addRenderableWidget(scrollHint);

        repositionRows();
        refreshDoneState();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (contentHeight() > visibleHeight()) {
            scroll = Math.max(0, Math.min(contentHeight() - visibleHeight(),
                    scroll - (int) (scrollY * ROW_HEIGHT)));
            repositionRows();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(parent);
        }
    }

    private int contentHeight() {
        return rows.size() * ROW_HEIGHT;
    }

    private int visibleHeight() {
        return this.height - TOP_ROWS - BOTTOM_ROWS;
    }

    private void repositionRows() {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = TOP_ROWS + i * ROW_HEIGHT - scroll;
            boolean visible = y >= TOP_ROWS - ROW_HEIGHT && y <= this.height - BOTTOM_ROWS - ROW_HEIGHT + 8;
            row.setY(y, visible);
        }
        if (scrollHint != null) {
            scrollHint.visible = contentHeight() > visibleHeight();
        }
    }

    void refreshDoneState() {
        if (doneButton == null) {
            return;
        }
        for (Row row : rows) {
            if (!row.valid()) {
                doneButton.active = false;
                return;
            }
        }
        doneButton.active = true;
    }

    private void saveAndClose() {
        for (Row row : rows) {
            row.apply();
        }
        config.save();
        onClose();
    }

    // ------------------------------------------------------------- one row

    /** {@code ballSizeLimit} or {@code ball_size_limit} becomes "Ball size limit". */
    static String humanize(String key) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
                out.append(' ');
            } else if (Character.isUpperCase(c) && i > 0 && !Character.isUpperCase(key.charAt(i - 1))) {
                out.append(' ').append(Character.toLowerCase(c));
            } else {
                out.append(out.length() == 0 ? Character.toUpperCase(c) : c);
            }
        }
        return out.toString().replaceAll(" +", " ").trim();
    }

    private abstract static class Row {
        final MycelConfigScreen screen;
        final ConfigValue<?> value;
        final StringWidget label;
        final AbstractWidget editor;
        final StringWidget error;
        boolean valid = true;

        Row(MycelConfigScreen screen, ConfigValue<?> value, int y) {
            this.screen = screen;
            this.value = value;
            int labelWidth = screen.width / 2 - 48;
            this.label = new StringWidget(16, y + 6, Math.max(40, labelWidth), 12,
                    Component.literal(shortLabel(screen, value)), screen.font);
            Component description = describe(screen, value);
            if (description != null) {
                this.label.setTooltip(net.minecraft.client.gui.components.Tooltip.create(description));
            }
            this.editor = createEditor(y);
            this.error = new StringWidget(screen.width / 2 + 8, y + 6, 170, 12,
                    Component.empty(), screen.font);
            this.error.visible = false;
            screen.addRenderableWidget(label);
            screen.addRenderableWidget(editor);
            screen.addRenderableWidget(error);
        }

        static Row create(MycelConfigScreen screen, ConfigValue<?> value, int y) {
            ValueKind kind = value.kind();
            if (kind == ValueKind.BOOLEAN && value instanceof ConfigValue.BooleanValue typed) {
                return new ToggleRow(screen, typed, y);
            }
            if (kind == ValueKind.ENUM) {
                return new EnumRow(screen, value, y);
            }
            if (kind == ValueKind.STRING_LIST && value instanceof ConfigValue.StringListValue typed) {
                return new ListRow(screen, typed, y);
            }
            if (value instanceof ConfigValue.IntegerValue
                    || value instanceof ConfigValue.LongValue
                    || value instanceof ConfigValue.DoubleValue
                    || value instanceof ConfigValue.StringValue) {
                return new TextRow(screen, value, y);
            }
            // Unknown future kinds render read-only rather than crashing.
            return new ReadOnlyRow(screen, value, y);
        }

        abstract AbstractWidget createEditor(int y);

        int widgetX() {
            return screen.width / 2 + 8;
        }

        int widgetWidth() {
            return Math.min(180, screen.width / 2 - 32);
        }

        void setY(int y, boolean visible) {
            label.setY(y + 6);
            editor.setY(y + 2);
            error.setY(y + 6);
            label.visible = visible;
            editor.visible = visible;
            updateErrorVisibility(visible);
        }

        void updateErrorVisibility(boolean rowVisible) {
            error.visible = rowVisible && !valid;
        }

        void mark(boolean valid, String errorText) {
            this.valid = valid;
            error.setMessage(Component.literal(errorText == null ? "" : "! " + errorText));
            updateErrorVisibility(label.visible);
            if (valid) {
                label.setMessage(Component.literal(shortLabel(screen, value)));
            } else {
                label.setMessage(Component.literal("! " + shortLabel(screen, value)));
            }
            screen.refreshDoneState();
        }

        boolean valid() {
            return valid;
        }

        /** The setting's name: translated when the mod ships {@code <modid>.config.<key>}, else from the key. */
        private static String shortLabel(MycelConfigScreen screen, ConfigValue<?> value) {
            String key = screen.config.metadata().id() + ".config." + value.key();
            String text = net.minecraft.locale.Language.getInstance().has(key)
                    ? net.minecraft.locale.Language.getInstance().getOrDefault(key)
                    : humanize(value.key());
            int max = Math.max(12, (screen.width / 2 - 48) / 6);
            return text.length() > max ? text.substring(0, max - 1) + "…" : text;
        }

        /** The description, shown when hovering the name: {@code <modid>.config.<key>.desc}, else the English one. */
        private static Component describe(MycelConfigScreen screen, ConfigValue<?> value) {
            String key = screen.config.metadata().id() + ".config." + value.key() + ".desc";
            if (net.minecraft.locale.Language.getInstance().has(key)) {
                return Component.translatable(key);
            }
            return value.description().isEmpty() ? null : Component.literal(value.description());
        }

        abstract void apply();
    }

    /** Boolean toggle button. */
    private static final class ToggleRow extends Row {
        private final ConfigValue<Boolean> typed;
        private boolean pending;

        ToggleRow(MycelConfigScreen screen, ConfigValue<Boolean> value, int y) {
            super(screen, value, y);
            this.typed = value;
            this.pending = value.get();
            // The button was made in Row's constructor, before pending had a value.
            editor.setMessage(labelFor(pending));
        }

        @Override
        AbstractWidget createEditor(int y) {
            return Button.builder(labelFor(pending), pressed -> {
                pending = !pending;
                pressed.setMessage(labelFor(pending));
            }).bounds(widgetX(), y + 2, widgetWidth(), 20).build();
        }

        private static Component labelFor(boolean current) {
            return current ? net.minecraft.network.chat.CommonComponents.OPTION_ON
                    : net.minecraft.network.chat.CommonComponents.OPTION_OFF;
        }

        @Override
        void apply() {
            typed.set(pending);
        }
    }

    /** Enum cycler button. */
    private static final class EnumRow extends Row {
        // Filled on first use: Row's constructor calls createEditor before this
        // class's own field initializers run, so neither may have one.
        private List<String> names;
        private int index;

        EnumRow(MycelConfigScreen screen, ConfigValue<?> value, int y) {
            super(screen, value, y);
        }

        private List<String> names() {
            if (names == null) {
                names = new ArrayList<>();
                for (Object constant : enumConstants(value)) {
                    names.add(constant.toString());
                }
                index = Math.max(0, names.indexOf(String.valueOf(value.get())));
            }
            return names;
        }

        @Override
        AbstractWidget createEditor(int y) {
            return Button.builder(Component.literal(currentName()), pressed -> {
                if (!names().isEmpty()) {
                    index = (index + 1) % names().size();
                    pressed.setMessage(Component.literal(currentName()));
                }
            }).bounds(widgetX(), y + 2, widgetWidth(), 20).build();
        }

        private String currentName() {
            return names().isEmpty() ? "?" : names().get(index);
        }

        private static Object[] enumConstants(ConfigValue<?> value) {
            if (value instanceof ConfigValue.EnumValue<?> enumValue) {
                return enumValue.type().getEnumConstants();
            }
            return new Object[0];
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        void apply() {
            if (value instanceof ConfigValue.EnumValue enumValue && !names().isEmpty()) {
                Object[] constants = enumValue.type().getEnumConstants();
                for (Object constant : constants) {
                    if (constant.toString().equals(names().get(index))) {
                        ((ConfigValue) value).set(constant);
                        return;
                    }
                }
            }
        }
    }

    /** Free-text value with parsing + range validation. */
    private static final class TextRow extends Row {
        private final EditBox box;
        private Object parsed;

        TextRow(MycelConfigScreen screen, ConfigValue<?> value, int y) {
            super(screen, value, y);
            this.box = (EditBox) editor;
            this.parsed = value.get();
            box.setValue(displayOf(value.get()));
            onText(box.getValue());
        }

        @Override
        AbstractWidget createEditor(int y) {
            EditBox created = new EditBox(screen.font, widgetX(), y + 2, widgetWidth(), 20,
                    Component.literal(value.key()));
            created.setMaxLength(512);
            created.setResponder(this::onText);
            return created;
        }

        private static String displayOf(Object current) {
            return String.valueOf(current);
        }

        private void onText(String text) {
            try {
                parsed = parse(value, text.trim());
                box.setTextColor(0xFF000000 | TextUtil.BODY);
                mark(true, null);
            } catch (IllegalArgumentException e) {
                box.setTextColor(0xFF000000 | TextUtil.ERROR);
                String message = e.getMessage();
                mark(false, message == null || message.length() > 40 ? "invalid value" : message);
            }
        }

        private static Object parse(ConfigValue<?> value, String text) {
            if (value instanceof ConfigValue.IntegerValue v) {
                return v.validate(Integer.parseInt(text));
            }
            if (value instanceof ConfigValue.LongValue v) {
                return v.validate(Long.parseLong(text));
            }
            if (value instanceof ConfigValue.DoubleValue v) {
                return v.validate(Double.parseDouble(text));
            }
            if (value instanceof ConfigValue.StringValue v) {
                return v.validate(text);
            }
            throw new IllegalArgumentException("unsupported value");
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        void apply() {
            if (valid) {
                ((ConfigValue) value).set(parsed);
            }
        }
    }

    /** Comma-separated string list editor. */
    private static final class ListRow extends Row {
        private final ConfigValue<java.util.List<String>> typed;
        private final EditBox box;

        ListRow(MycelConfigScreen screen, ConfigValue<java.util.List<String>> value, int y) {
            super(screen, value, y);
            this.typed = value;
            this.box = (EditBox) editor;
            box.setValue(String.join(", ", value.get()));
        }

        @Override
        AbstractWidget createEditor(int y) {
            EditBox created = new EditBox(screen.font, widgetX(), y + 2, widgetWidth(), 20,
                    Component.literal(value.key()));
            created.setMaxLength(2048);
            return created;
        }

        @Override
        void apply() {
            List<String> entries = new ArrayList<>();
            for (String part : box.getValue().split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    entries.add(trimmed);
                }
            }
            typed.set(List.copyOf(entries));
        }
    }

    /** Fallback for unknown future kinds: visible but inert. */
    private static final class ReadOnlyRow extends Row {
        ReadOnlyRow(MycelConfigScreen screen, ConfigValue<?> value, int y) {
            super(screen, value, y);
        }

        @Override
        AbstractWidget createEditor(int y) {
            Button button = Button.builder(Component.literal(String.valueOf(value.get())), pressed -> {
            }).bounds(widgetX(), y + 2, widgetWidth(), 20).build();
            button.active = false;
            return button;
        }

        @Override
        void apply() {
        }
    }
}
