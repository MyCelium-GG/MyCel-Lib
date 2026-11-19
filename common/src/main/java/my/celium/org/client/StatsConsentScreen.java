package my.celium.org.client;

import my.celium.org.internal.stats.MycelStats;
import my.celium.org.util.TextUtil;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Asks once whether Mycel may send anonymous stats. Escape leaves it unanswered, so it asks
 * again next start; nothing is sent until the player picks a sharing button.
 *
 * @hidden
 */
public final class StatsConsentScreen extends Screen {
    /** Paragraphs of the explanation, as keys with their English text; they wrap to the screen. */
    private static final String[][] BODY = {
        {"mycel.stats.body.what", "Mycel can send anonymous stats to the MyCelium team through FastStats: which MyCelium mods you use, their versions and settings, your Minecraft, loader and Java versions, and errors from MyCelium mods."},
        {"mycel.stats.body.never", "No player names, chat, worlds or servers. Nothing is sent unless you agree."},
        {"mycel.stats.body.change", "Change it any time with shareStats in config/mycel.json."},
    };

    private final Screen parent;

    StatsConsentScreen(Screen parent) {
        super(Component.translatableWithFallback("mycel.stats.title", "Help improve MyCelium mods"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int maxWidth = Math.min(360, this.width - 40);
        int buttonsWidth = 120;
        for (String[] label : new String[][] {
                {"mycel.stats.share_all", "Share stats and errors"}, {"mycel.stats.share_stats", "Stats only"},
                {"mycel.stats.share_none", "Don't share"}}) {
            buttonsWidth = Math.max(buttonsWidth,
                    this.font.width(Component.translatableWithFallback(label[0], label[1])) + 20);
        }
        int textHeight = 0;
        for (String[] paragraph : BODY) {
            textHeight += this.font.split(Component.translatableWithFallback(paragraph[0], paragraph[1]), maxWidth).size()
                    * this.font.lineHeight + 6;
        }
        int buttonsHeight = 3 * buttonsWidth + 12 <= this.width - 20 ? 20 : 3 * 20 + 2 * 4;
        int total = 22 + textHeight + 26 + buttonsHeight;
        int y = Math.max(8, (this.height - total) / 2);
        add(this.title.copy().withStyle(style -> style.withBold(true).withColor(TextUtil.BRIGHT)), y);
        y += 22;
        for (String[] paragraph : BODY) {
            MultiLineTextWidget text = new MultiLineTextWidget(Component.translatableWithFallback(paragraph[0], paragraph[1])
                    .withStyle(style -> style.withColor(TextUtil.BODY)), this.font)
                    .setMaxWidth(maxWidth).setCentered(true);
            text.setX(this.width / 2 - text.getWidth() / 2);
            text.setY(y);
            addRenderableWidget(text);
            y += text.getHeight() + 6;
        }
        add(Component.translatableWithFallback("mycel.stats.privacy", "What is sent: %s", "wiki.mycelium-gg.org/privacy")
                .withStyle(style -> style.withColor(TextUtil.MUTED)), y);
        y += 26;

        Component[] labels = {
            Component.translatableWithFallback("mycel.stats.share_all", "Share stats and errors"),
            Component.translatableWithFallback("mycel.stats.share_stats", "Stats only"),
            Component.translatableWithFallback("mycel.stats.share_none", "Don't share"),
        };
        Runnable[] actions = {() -> choose(true, true), () -> choose(true, false), () -> choose(false, false)};
        int gap = 6;
        int width = 120;
        for (Component label : labels) {
            width = Math.max(width, this.font.width(label) + 20);
        }
        boolean row = 3 * width + 2 * gap <= this.width - 20;
        width = row ? width : Math.min(Math.max(width, 200), this.width - 20);
        int left = row ? this.width / 2 - (width * 3 + gap * 2) / 2 : this.width / 2 - width / 2;
        for (int i = 0; i < labels.length; i++) {
            Runnable action = actions[i];
            int x = row ? left + i * (width + gap) : left;
            int by = row ? y : y + i * (20 + 4);
            addRenderableWidget(Button.builder(labels[i], button -> action.run()).bounds(x, by, width, 20).build());
        }
    }

    private void add(Component text, int y) {
        int w = Math.min(this.width - 20, this.font.width(text) + 4);
        addRenderableWidget(new StringWidget(this.width / 2 - w / 2, y, w, 10, text, this.font));
    }

    private void choose(boolean share, boolean errors) {
        MycelStats.answer(share, errors);
        MycelScreens.open(parent);
    }

    @Override
    public void onClose() {
        MycelScreens.open(parent);
    }
}
