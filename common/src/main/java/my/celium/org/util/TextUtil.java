package my.celium.org.util;

import java.util.Collection;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Small helpers around {@link Component}. Mycel does not rebuild the text
 * system - these just remove qualifier noise.
 *
 * <p>The palette below is the shared Mycel look and uses only colors from the
 * MyCelium wiki's dark theme (chat is always drawn on a dark backdrop): the
 * page tokens ({@code --accent}, {@code --text}, {@code --muted}) plus the
 * dark code-highlight tokens ({@code .tok-*}). No legacy {@code §} colors.
 * Legacy helpers ({@link #green(String)}, {@link #red(String)},
 * {@link #gray(String)}, {@link #gold(String)}) keep their signatures and
 * render through the matching tone.
 */
public final class TextUtil {
    private TextUtil() {
    }

    // ------------------------------------------------------------ palette
    // Wiki dark-theme tokens (style.css). Nothing here exists outside the wiki.
    /** Wiki red ({@code --accent}) - brand, headers. */
    public static final int PRIMARY = 0xEF5350;
    /** Wiki blue ({@code .tok-k}) - mod names, config keys. */
    public static final int ACCENT = 0x79B8FF;
    /** Wiki green ({@code .tok-s}) - success. */
    public static final int SUCCESS = 0x7EE787;
    /** Wiki red ({@code --accent}) - errors. Always paired with a cross so it never reads as the brand. */
    public static final int ERROR = 0xEF5350;
    /** Wiki orange ({@code .tok-a}) - warnings / gold. */
    public static final int WARN = 0xFFA657;
    /** Wiki purple ({@code .tok-n}) - values, commands. */
    public static final int INFO = 0xD2A8FF;
    /** Wiki {@code --muted} - secondary text. */
    public static final int MUTED = 0xA3A3A3;
    /** Wiki comment grey ({@code .tok-c}) - brackets, bullets, dividers. */
    public static final int FAINT = 0x8B949E;
    /** Wiki {@code --text} - body text. */
    public static final int BODY = 0xE6E6E6;
    /** White - names and titles that should stand out from body text. */
    public static final int BRIGHT = 0xFFFFFF;

    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static MutableComponent empty() {
        return Component.empty();
    }

    /**
     * Applies {@code formatting} to a copy of {@code text}. Legacy colors are
     * swapped for the nearest wiki tone, so old callers never show {@code §}
     * colors; bold, italic and the other decorations pass through unchanged.
     *
     * @deprecated use the tone helpers ({@link #success(String)}, ...) or
     *             {@link #coloredHex(Component, int)}
     */
    @Deprecated
    public static MutableComponent colored(Component text, ChatFormatting formatting) {
        int tone = tone(formatting);
        return tone < 0 ? text.copy().withStyle(formatting) : coloredHex(text, tone);
    }

    /**
     * Wiki tone standing in for a legacy {@link ChatFormatting} color, or
     * {@code -1} for decorations (bold, italic, ...) and reset.
     */
    public static int tone(ChatFormatting legacy) {
        return switch (legacy) {
            case GREEN, DARK_GREEN -> SUCCESS;
            case RED, DARK_RED -> ERROR;
            case GOLD, YELLOW -> WARN;
            case AQUA, DARK_AQUA, BLUE, DARK_BLUE -> ACCENT;
            case LIGHT_PURPLE, DARK_PURPLE -> INFO;
            case GRAY -> MUTED;
            case DARK_GRAY, BLACK -> FAINT;
            case WHITE -> BRIGHT;
            default -> -1;
        };
    }

    /** Applies a raw RGB color ({@code 0xRRGGBB}) to a copy of {@code text}. */
    public static MutableComponent coloredHex(Component text, int rgb) {
        return text.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }

    /**
     * Parses {@code "#RRGGBB"} or {@code "RRGGBB"} into {@code 0xRRGGBB}.
     *
     * @throws IllegalArgumentException when the text is not a hex color
     */
    public static int parseHex(String hexCode) {
        if (hexCode == null) {
            throw new IllegalArgumentException("Hex color must not be null");
        }
        String clean = hexCode.trim();
        if (clean.startsWith("#")) {
            clean = clean.substring(1);
        }
        if (clean.length() != 6 || !clean.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Expected hex color like #EF5350, got '" + hexCode + "'");
        }
        return Integer.parseInt(clean, 16);
    }

    /** Hex-colored literal, e.g. {@code hex("hi", "#EF5350")}. */
    public static MutableComponent hex(String text, String hexCode) {
        return hex(text, parseHex(hexCode));
    }

    /** Hex-colored literal from a raw RGB int ({@code 0xRRGGBB}). */
    public static MutableComponent hex(String text, int rgb) {
        return literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }

    // ------------------------------------------------------------ tones
    public static MutableComponent primary(String text) {
        return hex(text, PRIMARY);
    }

    public static MutableComponent accent(String text) {
        return hex(text, ACCENT);
    }

    public static MutableComponent success(String text) {
        return hex(text, SUCCESS);
    }

    public static MutableComponent error(String text) {
        return hex(text, ERROR);
    }

    public static MutableComponent warn(String text) {
        return hex(text, WARN);
    }

    public static MutableComponent infoText(String text) {
        return hex(text, INFO);
    }

    public static MutableComponent muted(String text) {
        return hex(text, MUTED);
    }

    public static MutableComponent faint(String text) {
        return hex(text, FAINT);
    }

    public static MutableComponent body(String text) {
        return hex(text, BODY);
    }

    // ------------------------------------------------------------ legacy
    // Same signatures as before; now backed by the softer hex tones.

    public static MutableComponent green(String text) {
        return success(text);
    }

    public static MutableComponent red(String text) {
        return error(text);
    }

    public static MutableComponent gray(String text) {
        return muted(text);
    }

    public static MutableComponent gold(String text) {
        return warn(text);
    }

    // ------------------------------------------------------------ message kit
    // One voice for every reply. Single lines carry the "Tag ›" prefix;
    // multi-line replies tag only their header and indent the rows below it.
    // The static helpers speak as Mycel; mods get their own tag from voice().

    private static final ChatVoice MYCEL = new ChatVoice("Mycel");

    /** Message kit tagged with {@code tag}, e.g. {@code voice("Pets").ok(name, " came running.")}. */
    public static ChatVoice voice(String tag) {
        return new ChatVoice(tag);
    }

    /**
     * A message from parts: strings become body text, components are appended
     * as they are. {@code msg(name, " came running.")}
     */
    public static MutableComponent msg(Object... parts) {
        MutableComponent out = empty();
        for (Object part : parts) {
            out.append(part instanceof Component component ? component : body(String.valueOf(part)));
        }
        return out;
    }

    /** {@code Mycel › } tag: bold wiki-red brand + faint chevron. */
    public static MutableComponent prefix() {
        return MYCEL.prefix();
    }

    /** Prepends the {@code Mycel ›} tag to {@code message}. */
    public static MutableComponent prefixed(Component message) {
        return MYCEL.prefixed(message);
    }

    /** Success line: green check + message. */
    public static MutableComponent ok(String text) {
        return MYCEL.ok(text);
    }

    /** Error line: red cross + message. */
    public static MutableComponent fail(String text) {
        return MYCEL.fail(text);
    }

    /** Info line: plain message after the tag. */
    public static MutableComponent note(String text) {
        return MYCEL.note(text);
    }

    /** Warning line: orange bang + message. */
    public static MutableComponent caution(String text) {
        return MYCEL.caution(text);
    }

    /** Header line opening a multi-line reply: tag + bright title. */
    public static MutableComponent header(String title) {
        return MYCEL.header(title);
    }

    /** Header line with a muted suffix, e.g. {@code header("Mods", "3")}. */
    public static MutableComponent header(String title, String detail) {
        return MYCEL.header(title, detail);
    }

    /** Indented row under a {@link #header}: faint bullet + content, no tag. */
    public static MutableComponent item(Component content) {
        return empty().append(faint("  \u2022 ")).append(content);
    }

    /** Indented continuation line (descriptions, hints) under an {@link #item}. */
    public static MutableComponent detail(String text) {
        return empty().append(faint("     ")).append(muted(text));
    }

    /** Faint divider used between sections. */
    public static MutableComponent divider() {
        return faint("  \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
    }

    /** {@code key}: value pair with accent key and bright value. */
    public static MutableComponent keyValue(String key, String value) {
        return empty().append(accent(key)).append(muted(": ")).append(body(value));
    }

    /** {@code key}: value pair with a pre-built value component. */
    public static MutableComponent keyValue(String key, Component value) {
        return empty().append(accent(key)).append(muted(": ")).append(value);
    }

    /** Joins components with a literal separator. */
    public static MutableComponent join(String separator, Collection<? extends Component> parts) {
        MutableComponent out = empty();
        boolean first = true;
        for (Component part : parts) {
            if (!first) {
                out.append(separator);
            }
            out.append(part);
            first = false;
        }
        return out;
    }

    /** Plain (unformatted) string content. */
    public static String plain(Component component) {
        return component.getString();
    }
}
