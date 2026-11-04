package my.celium.org.util;

import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The shared message kit with a mod's own tag, so every MyCelium mod reads the same:
 * {@code Pets » Rex came running.} The tag is bold bright red and the {@code »} grey. Warnings
 * are amber text and errors light red text; everything else is ivory. Get one with {@link TextUtil#voice(String)} and keep it in a constant.
 *
 * <p>Message parts are joined in order: strings become body text, components are appended as
 * they are (so names, values and buttons keep their style).
 */
public final class ChatVoice {
    private static final String CHEVRON = " \u00BB ";

    private final String tag;

    ChatVoice(String tag) {
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    /**
     * The tag text, e.g. {@code "Pets"}.
     *
     * @return the tag
     */
    public String tag() {
        return tag;
    }

    /**
     * The start of a line: bold red tag and a grey {@code »}.
     *
     * @return a new prefix component
     */
    public MutableComponent prefix() {
        return TextUtil.empty()
                .append(TextUtil.hex(tag, TextUtil.PRIMARY).withStyle(style -> style.withBold(true)))
                .append(TextUtil.hex(CHEVRON, TextUtil.FAINT));
    }

    private MutableComponent tinted(int color, Object... parts) {
        MutableComponent text = TextUtil.empty();
        for (Object part : parts) {
            text.append(part instanceof Component component ? component : TextUtil.hex(String.valueOf(part), color));
        }
        return TextUtil.empty().append(prefix()).append(text);
    }

    /**
     * Prepends the tag to {@code message}.
     *
     * @param message the text after the tag
     * @return the tagged line
     */
    public MutableComponent prefixed(Component message) {
        return TextUtil.empty().append(prefix()).append(message);
    }

    /**
     * Info line: tag, {@code »}, message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent note(Object... parts) {
        return prefixed(TextUtil.msg(parts));
    }

    /**
     * Success line: tag, {@code »}, message (reads like an info line; the words say it worked).
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent ok(Object... parts) {
        return prefixed(TextUtil.msg(parts));
    }

    /**
     * Error line: tag, {@code »}, message in light red.
     *
     * @param parts strings (light red text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent fail(Object... parts) {
        return tinted(TextUtil.ERROR_TEXT, parts);
    }

    /**
     * Warning line: tag, {@code »}, message in amber.
     *
     * @param parts strings (amber text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent caution(Object... parts) {
        return tinted(TextUtil.WARN, parts);
    }

    /**
     * Header line opening a multi-line reply: tag, {@code »}, bold title. When the title already
     * starts with the tag (like {@code "Mycel 1.6.0"}) it becomes the tag plus the rest, with no
     * {@code »}: {@code Mycel 1.6.0}.
     *
     * @param title the title
     * @return the header line
     */
    public MutableComponent header(String title) {
        if (title.startsWith(tag + " ")) {
            return TextUtil.empty()
                    .append(TextUtil.hex(tag, TextUtil.PRIMARY).withStyle(style -> style.withBold(true)))
                    .append(TextUtil.hex(title.substring(tag.length()), TextUtil.BRIGHT).withStyle(style -> style.withBold(true)));
        }
        return prefix().append(TextUtil.hex(title, TextUtil.BRIGHT).withStyle(style -> style.withBold(true)));
    }

    /**
     * Header line with grey text after the title, e.g. {@code header("Your pets", "3")}.
     *
     * @param title  the title
     * @param detail the grey text after it
     * @return the header line
     */
    public MutableComponent header(String title, String detail) {
        return header(title).append(TextUtil.muted("  " + detail));
    }
}
