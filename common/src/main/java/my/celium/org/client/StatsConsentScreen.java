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
        int y = Math.max(20, this.height / 2 - 90);
        add(this.title.copy().withStyle(style -> style.withBold(true).withColor(TextUtil.BRIGHT)), y);
        y += 22;
        int maxWidth = Math.min(360, this.width - 40);
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
                .withStyle(style -> style.withColor(TextUtil.MUTED)), y + 2);
        y += 28;

        int gap = 6;
        int width = Math.min(150, (this.width - 20 - 2 * gap) / 3);
        int left = this.width / 2 - (width * 3 + gap * 2) / 2;
        addRenderableWidget(Button.builder(Component.translatableWithFallback("mycel.stats.share_all", "Share stats and errors"), button -> choose(true, true))
                .bounds(left, y, width, 20).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback("mycel.stats.share_stats", "Stats only"), button -> choose(true, false))
                .bounds(left + width + gap, y, width, 20).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback("mycel.stats.share_none", "Don't share"), button -> choose(false, false))
                .bounds(left + 2 * (width + gap), y, width, 20).build());
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
