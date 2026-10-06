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
 * <p>The palette below is the shared Mycel look: soft hex tones instead of the
 * harsh vanilla {@link ChatFormatting} primaries. Legacy helpers
 * ({@link #green(String)}, {@link #red(String)}, {@link #gray(String)},
 * {@link #gold(String)}) keep their signatures for compatibility but now render
 * through the matching hex tone, so existing callers get the cleaner look
 * without changing code.
 */
public final class TextUtil {
    private TextUtil() {
    }

    // ------------------------------------------------------------ palette
    // Mycelium-inspired, high-contrast-on-dark-chat hex palette.
    /** Teal - primary brand / headers. */
    public static final int PRIMARY = 0x5EEAD4;
    /** Violet - accent / highlights, mod names. */
    public static final int ACCENT = 0xA78BFA;
    /** Soft green - success. */
    public static final int SUCCESS = 0x4ADE80;
    /** Soft red - errors. */
    public static final int ERROR = 0xF87171;
    /** Amber - warnings / gold. */
    public static final int WARN = 0xFBBF24;
    /** Light blue - informational values. */
    public static final int INFO = 0x93C5FD;
    /** Muted grey - secondary text. */
    public static final int MUTED = 0x9CA3AF;
    /** Dark grey - brackets, dividers. */
    public static final int FAINT = 0x4B5563;
    /** Near-white - body text. */
    public static final int BODY = 0xE5E7EB;

    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static MutableComponent empty() {
        return Component.empty();
    }

    public static MutableComponent colored(Component text, ChatFormatting formatting) {
        return text.copy().withStyle(formatting);
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
            throw new IllegalArgumentException("Expected hex color like #5EEAD4, got '" + hexCode + "'");
        }
        return Integer.parseInt(clean, 16);
    }

    /** Hex-colored literal, e.g. {@code hex("hi", "#5EEAD4")}. */
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
    // One consistent voice for every /mycel line: "[Mycel] message".

    /** {@code [Mycel]} prefix: faint brackets + primary brand name. */
    public static MutableComponent prefix() {
        return empty()
                .append(faint("["))
                .append(primary("Mycel"))
                .append(faint("] "));
    }

    /** Prepends the {@code [Mycel]} prefix to {@code message}. */
    public static MutableComponent prefixed(Component message) {
        return empty().append(prefix()).append(message);
    }

    /** Success line: green check + message. */
    public static MutableComponent ok(String text) {
        return prefixed(empty().append(success("\u2714 ")).append(body(text)));
    }

    /** Error line: red cross + message. */
    public static MutableComponent fail(String text) {
        return prefixed(empty().append(error("\u2718 ")).append(body(text)));
    }

    /** Info line: muted bullet + message. */
    public static MutableComponent note(String text) {
        return prefixed(empty().append(faint("\u2022 ")).append(body(text)));
    }

    /** Warning line: amber bang + message. */
    public static MutableComponent caution(String text) {
        return prefixed(empty().append(warn("! ")).append(body(text)));
    }

    /** Header line: primary title centred between faint rules. */
    public static MutableComponent header(String title) {
        return prefixed(empty()
                .append(faint("\u2014 "))
                .append(primary(title))
                .append(faint(" \u2014")));
    }

    /** Faint divider used between sections. */
    public static MutableComponent divider() {
        return prefixed(faint("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
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
