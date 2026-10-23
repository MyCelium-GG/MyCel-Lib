package my.celium.org.client;

import my.celium.org.util.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.MutableComponent;

/**
 * The MyCelium wiki dark theme for screens and HUD text: flat neutrals, one
 * red accent, 2px borders, hard offset card shadows. Colors are ARGB for
 * drawing; text tones match {@link TextUtil}'s chat palette. Client only.
 */
public final class MycelTheme {
    private MycelTheme() {
    }

    private static final int OPAQUE = 0xFF000000;

    // ------------------------------------------------------------ surfaces
    /** Dimmed world behind a full screen. */
    public static final int SHADE = 0xD8171717;
    /** Wiki {@code --bg}. */
    public static final int BG = 0xFF171717;
    /** Wiki {@code --code-bg}: rows, inputs, chips. */
    public static final int SURFACE = 0xFF262626;
    /** Wiki {@code --border}. */
    public static final int BORDER = 0xFF383838;
    /** Hard offset card shadow. */
    public static final int SHADOW = 0x66000000;
    /** Bottom edge under the header bar. */
    public static final int HEADER_EDGE = 0x40000000;
    /** Wiki {@code --note-bg}. */
    public static final int NOTE = 0xFF2B1A1A;
    /** Every other list row. */
    public static final int ZEBRA = 0x08FFFFFF;

    // ------------------------------------------------------------ accents
    /** Wiki {@code --accent}. */
    public static final int ACCENT = OPAQUE | TextUtil.PRIMARY;
    /** Wiki {@code --accent-dark}: header bar, active tab. */
    public static final int ACCENT_DARK = 0xFFB71C1C;

    // ------------------------------------------------------------ text
    /** Titles. */
    public static final int HEADING = OPAQUE | TextUtil.BRIGHT;
    /** Wiki {@code --text}. */
    public static final int TEXT = OPAQUE | TextUtil.BODY;
    /** Wiki {@code --muted}. */
    public static final int DIM = OPAQUE | TextUtil.MUTED;
    /** Wiki comment grey: hints, separators. */
    public static final int FAINT = OPAQUE | TextUtil.FAINT;
    /** Disabled text. */
    public static final int OFF = 0xFF5F5F5F;
    /** Wiki blue: names, keys, links. */
    public static final int LINK = OPAQUE | TextUtil.ACCENT;
    /** Wiki purple: values. */
    public static final int VALUE = OPAQUE | TextUtil.INFO;
    /** Wiki green: have, cheap, done. */
    public static final int GOOD = OPAQUE | TextUtil.SUCCESS;
    /** Wiki orange: pricey, maxed, attention. */
    public static final int WARN = OPAQUE | TextUtil.WARN;
    /** Missing, too expensive, errors. */
    public static final int BAD = ACCENT;

    /** A wiki card: hard offset shadow, 2px border, flat body. */
    public static void panel(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1) {
        gfx.fill(x0 + 3, y0 + 3, x1 + 3, y1 + 3, SHADOW);
        gfx.fill(x0, y0, x1, y1, BORDER);
        gfx.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, BG);
    }

    /** Text in an ARGB theme color (alpha is ignored). */
    public static MutableComponent colored(String text, int argb) {
        return TextUtil.hex(text, argb & 0xFFFFFF);
    }

    /** {@code text}, cut with an ellipsis so it fits {@code maxWidth} pixels. */
    public static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
    }
}
