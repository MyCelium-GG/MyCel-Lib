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
 * <p>The palette below is the shared Mycel look, all custom hex colors on chat's dark backdrop:
 * a bold bright red tag and a grey {@code »} ({@code Mycel » }), off-white text, names, values and
 * buttons, grey for details. Green text means it worked, amber means careful, rose
 * means failed. No legacy {@code §} colors. Legacy helpers ({@link #green(String)},
 * {@link #red(String)}, {@link #gray(String)}, {@link #gold(String)}) keep their signatures and
 * render through the matching tone.
 *
 * <p>Most mods only need a {@link ChatVoice}, so every reply carries the mod's tag:
 * <pre>{@code
 * static final ChatVoice CHAT = TextUtil.voice("Pets");
 *
 * PlayerUtil.sendMessage(player, CHAT.ok(TextUtil.accent(pet.name()), " came running."));
 * PlayerUtil.sendMessage(player, CHAT.fail("That pet is too far away."));
 *
 * // A multi-line reply: one tagged header, untagged rows under it.
 * PlayerUtil.sendMessage(player, CHAT.header("Your pets", String.valueOf(pets.size())));
 * for (Pet pet : pets) {
 *     PlayerUtil.sendMessage(player, TextUtil.item(TextUtil.keyValue(pet.name(), pet.where())));
 * }
 * }</pre>
 */
public final class TextUtil {
    private TextUtil() {
    }

    // ------------------------------------------------------------ palette
    /** Bright red: the mod's tag at the start of every message, and headers. */
    public static final int PRIMARY = 0xFF3B3B;
    /** White: names, values and anything else the reader looks for inside a line. */
    public static final int ACCENT = 0xF4F4F5;
    /** Green: success lines and good news, like an available update. */
    public static final int SUCCESS = 0x4ADE80;
    /** Rose: the text of error lines. */
    public static final int ERROR = 0xF87171;
    /** Amber: the text of warning lines, things that are off. */
    public static final int WARN = 0xFBBF24;
    /** Values and commands: white, like names. */
    public static final int INFO = 0xF4F4F5;
    /** Grey: details, versions, hints, keys. */
    public static final int MUTED = 0xA1A1AA;
    /** The quietest text: the {@code »}, ranges, separators. */
    public static final int FAINT = 0x71717A;
    /** Off-white body text. */
    public static final int BODY = 0xF4F4F5;
    /** Titles: white, underlined. */
    public static final int BRIGHT = 0xFFFFFF;
    /** Error text (same as {@link #ERROR}). */
    static final int ERROR_TEXT = ERROR;
    /** Clickable labels: underlined off-white. */
    static final int LINK = 0xF4F4F5;

    /**
     * Shorthand for {@link Component#literal(String)}.
     *
     * @param text the text
     * @return a new unstyled literal
     */
    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    /**
     * Shorthand for {@link Component#translatable(String, Object...)}.
     *
     * @param key  the translation key
     * @param args values for the key's {@code %s} slots
     * @return a new translatable component
     */
    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /**
     * Shorthand for {@link Component#empty()}, the usual start for {@code append} chains.
     *
     * @return a new empty component
     */
    public static MutableComponent empty() {
        return Component.empty();
    }

    /**
     * Applies {@code formatting} to a copy of {@code text}. Legacy colors are
     * swapped for the nearest wiki tone, so old callers never show {@code §}
     * colors; bold, italic and the other decorations pass through unchanged.
     *
     * @param text       the text to style
     * @param formatting the legacy color or decoration
     * @return a styled copy
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
     *
     * @param legacy the legacy formatting
     * @return its {@code 0xRRGGBB} tone, or {@code -1}
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

    /**
     * Applies a raw RGB color ({@code 0xRRGGBB}) to a copy of {@code text}.
     *
     * @param text the text to color
     * @param rgb  the color; any alpha bits are ignored
     * @return a colored copy
     */
    public static MutableComponent coloredHex(Component text, int rgb) {
        return text.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }

    /**
     * Parses {@code "#RRGGBB"} or {@code "RRGGBB"} into {@code 0xRRGGBB}.
     *
     * @param hexCode the color text, with or without {@code #}
     * @return the color as {@code 0xRRGGBB}
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

    /**
     * Hex-colored literal, e.g. {@code hex("hi", "#EF5350")}.
     *
     * @param text    the text
     * @param hexCode the color, {@code "#RRGGBB"} or {@code "RRGGBB"}
     * @return a new colored literal
     * @throws IllegalArgumentException when {@code hexCode} is not a hex color
     */
    public static MutableComponent hex(String text, String hexCode) {
        return hex(text, parseHex(hexCode));
    }

    /**
     * Hex-colored literal from a raw RGB int ({@code 0xRRGGBB}).
     *
     * @param text the text
     * @param rgb  the color; any alpha bits are ignored
     * @return a new colored literal
     */
    public static MutableComponent hex(String text, int rgb) {
        return literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
    }

    // ------------------------------------------------------------ tones
    /**
     * Brand red text ({@link #PRIMARY}), for headers and tags.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent primary(String text) {
        return hex(text, PRIMARY);
    }

    /**
     * Blue text ({@link #ACCENT}), for mod names and config keys.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent accent(String text) {
        return hex(text, ACCENT);
    }

    /**
     * Green success text ({@link #SUCCESS}).
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent success(String text) {
        return hex(text, SUCCESS);
    }

    /**
     * Rose error text ({@link #ERROR}).
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent error(String text) {
        return hex(text, ERROR);
    }

    /**
     * Orange warning text ({@link #WARN}).
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent warn(String text) {
        return hex(text, WARN);
    }

    /**
     * White text ({@link #INFO}), for values and commands.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent infoText(String text) {
        return hex(text, INFO);
    }

    /**
     * Grey secondary text ({@link #MUTED}).
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent muted(String text) {
        return hex(text, MUTED);
    }

    /**
     * Faint grey text ({@link #FAINT}), for brackets, bullets and dividers.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent faint(String text) {
        return hex(text, FAINT);
    }

    /**
     * Body text ({@link #BODY}).
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent body(String text) {
        return hex(text, BODY);
    }

    // ------------------------------------------------------------ legacy
    // Same signatures as before; now backed by the softer hex tones.

    /**
     * Same as {@link #success(String)}; kept for older callers.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent green(String text) {
        return success(text);
    }

    /**
     * Same as {@link #error(String)}; kept for older callers.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent red(String text) {
        return error(text);
    }

    /**
     * Same as {@link #muted(String)}; kept for older callers.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent gray(String text) {
        return muted(text);
    }

    /**
     * Same as {@link #warn(String)}; kept for older callers.
     *
     * @param text the text
     * @return a new literal in that color
     */
    public static MutableComponent gold(String text) {
        return warn(text);
    }

    // ------------------------------------------------------------ message kit
    // One voice for every reply. Single lines carry the "Tag ›" prefix;
    // multi-line replies tag only their header and indent the rows below it.
    // The static helpers speak as Mycel; mods get their own tag from voice().

    private static final ChatVoice MYCEL = new ChatVoice("Mycel");

    /**
     * Message kit tagged with {@code tag}, e.g. {@code voice("Pets").ok(name, " came running.")}.
     *
     * @param tag the tag shown before every line, usually the mod's short name
     * @return a new message kit; keep it in a constant
     */
    public static ChatVoice voice(String tag) {
        return new ChatVoice(tag);
    }

    /**
     * A message from parts: strings become body text, components are appended
     * as they are. {@code msg(name, " came running.")}
     *
     * @param parts strings and components, in order
     * @return the joined message
     */
    public static MutableComponent msg(Object... parts) {
        MutableComponent out = empty();
        for (Object part : parts) {
            out.append(part instanceof Component component ? component : body(String.valueOf(part)));
        }
        return out;
    }

    /**
     * {@code Mycel › } tag: bold wiki-red brand + faint chevron.
     *
     * @return a new prefix component
     */
    public static MutableComponent prefix() {
        return MYCEL.prefix();
    }

    /**
     * Prepends the {@code Mycel ›} tag to {@code message}.
     *
     * @param message the text after the tag
     * @return the tagged line
     */
    public static MutableComponent prefixed(Component message) {
        return MYCEL.prefixed(message);
    }

    /**
     * Success line: green check + message.
     *
     * @param text the message
     * @return the line
     */
    public static MutableComponent ok(String text) {
        return MYCEL.ok(text);
    }

    /**
     * Error line: red cross + message.
     *
     * @param text the message
     * @return the line
     */
    public static MutableComponent fail(String text) {
        return MYCEL.fail(text);
    }

    /**
     * Info line: plain message after the tag.
     *
     * @param text the message
     * @return the line
     */
    public static MutableComponent note(String text) {
        return MYCEL.note(text);
    }

    /**
     * Warning line: tag + message in amber.
     *
     * @param text the message
     * @return the line
     */
    public static MutableComponent caution(String text) {
        return MYCEL.caution(text);
    }

    /**
     * Success line built from parts, like {@link ChatVoice#ok(Object...)} with the Mycel tag.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public static MutableComponent ok(Object... parts) {
        return MYCEL.ok(parts);
    }

    /**
     * Info line built from parts, like {@link ChatVoice#note(Object...)} with the Mycel tag.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public static MutableComponent note(Object... parts) {
        return MYCEL.note(parts);
    }

    /**
     * Header line opening a multi-line reply: tag + bright title.
     *
     * @param title the title
     * @return the header line
     */
    public static MutableComponent header(String title) {
        return MYCEL.header(title);
    }

    /**
     * Header line with a muted suffix, e.g. {@code header("Mods", "3")}.
     *
     * @param title  the title
     * @param detail the muted text after it
     * @return the header line
     */
    public static MutableComponent header(String title, String detail) {
        return MYCEL.header(title, detail);
    }

    /**
     * Row under a {@link #header}: a grey {@code ›} and the content, no indent.
     *
     * @param content the row's content
     * @return the row
     */
    public static MutableComponent item(Component content) {
        return empty().append(faint(ROW_MARKER)).append(content);
    }

    /** Marker in front of rows under a header. */
    static final String ROW_MARKER = "\u203A ";

    /**
     * Grey hint line (descriptions, hints) under a list of {@link #item}s.
     *
     * @param text the muted text
     * @return the line
     */
    public static MutableComponent detail(String text) {
        return muted(text);
    }

    /**
     * Faint divider used between sections.
     *
     * @return a new divider line
     */
    public static MutableComponent divider() {
        return empty().append(faint("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
    }

    /**
     * {@code key  value} pair: grey key, white value.
     *
     * @param key   the key
     * @param value the value
     * @return the pair
     */
    public static MutableComponent keyValue(String key, String value) {
        return empty().append(muted(key + "  ")).append(literal(value).withStyle(style -> style.withColor(ACCENT)));
    }

    /**
     * {@code key}: value pair with a pre-built value component.
     *
     * @param key   the key
     * @param value the value, kept as it is
     * @return the pair
     */
    public static MutableComponent keyValue(String key, Component value) {
        return empty().append(muted(key + "  ")).append(value);
    }

    // ------------------------------------------------------------ buttons

    /**
     * A clickable label that runs a command: underlined off-white, with a hover hint.
     *
     * @param label   the label, e.g. {@code "Upload"}
     * @param command the command to run, with its leading slash
     * @param hover   what the hover hint says
     * @return the button
     */
    public static MutableComponent button(String label, String command, String hover) {
        return clickable(label, new net.minecraft.network.chat.ClickEvent.RunCommand(command), hover);
    }

    /**
     * A clickable label that fills the chat box with a command, for commands that need more typing.
     *
     * @param label   the label
     * @param command the command to put in the chat box
     * @param hover   what the hover hint says
     * @return the button
     */
    public static MutableComponent suggest(String label, String command, String hover) {
        return clickable(label, new net.minecraft.network.chat.ClickEvent.SuggestCommand(command), hover);
    }

    /**
     * A clickable label that opens a web page.
     *
     * @param label the label, e.g. {@code "Open"}
     * @param url   the https address
     * @return the link
     */
    public static MutableComponent link(String label, String url) {
        return clickable(label, new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)), url);
    }

    /**
     * A clickable label that copies text to the clipboard.
     *
     * @param label the label, e.g. {@code "Copy link"}
     * @param text  what gets copied
     * @return the button
     */
    public static MutableComponent copy(String label, String text) {
        return clickable(label, new net.minecraft.network.chat.ClickEvent.CopyToClipboard(text), "Copy to clipboard");
    }

    private static MutableComponent clickable(String label, net.minecraft.network.chat.ClickEvent click, String hover) {
        return literal(label).withStyle(style -> style
                .withColor(LINK)
                .withUnderlined(true)
                .withClickEvent(click)
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(muted(hover))));
    }

    /**
     * Joins components with a literal separator.
     *
     * @param separator the text between parts
     * @param parts     the components, in order
     * @return the joined component (empty for no parts)
     */
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

    /**
     * Plain (unformatted) string content.
     *
     * @param component the component
     * @return its text without styling
     */
    public static String plain(Component component) {
        return component.getString();
    }
}
