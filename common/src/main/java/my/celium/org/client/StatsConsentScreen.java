package my.celium.org.client;

import my.celium.org.internal.stats.MycelStats;
import my.celium.org.util.TextUtil;
import net.minecraft.client.gui.components.Button;
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
    private static final String[] BODY = {
        "Mycel can send anonymous stats to the MyCelium team through FastStats:",
        "which MyCelium mods you use, their versions and settings, your",
        "Minecraft, loader and Java versions, and errors from MyCelium mods.",
        "",
        "No player names, chat, worlds or servers. Nothing is sent unless you agree.",
        "Change it any time with shareStats in config/mycel.json.",
    };

    private final Screen parent;

    StatsConsentScreen(Screen parent) {
        super(Component.literal("Help improve MyCelium mods"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = Math.max(20, this.height / 2 - 90);
        add(this.title.copy().withStyle(style -> style.withBold(true).withColor(TextUtil.BRIGHT)), y);
        y += 22;
        for (String line : BODY) {
            if (!line.isEmpty()) {
                add(TextUtil.hex(line, TextUtil.BODY), y);
            }
            y += 11;
        }
        add(TextUtil.hex("What is sent: wiki.mycelium-gg.org/privacy", TextUtil.MUTED), y + 4);
        y += 28;

        int gap = 6;
        int width = Math.min(150, (this.width - 20 - 2 * gap) / 3);
        int left = this.width / 2 - (width * 3 + gap * 2) / 2;
        addRenderableWidget(Button.builder(Component.literal("Share stats and errors"), button -> choose(true, true))
                .bounds(left, y, width, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Stats only"), button -> choose(true, false))
                .bounds(left + width + gap, y, width, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Don't share"), button -> choose(false, false))
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
