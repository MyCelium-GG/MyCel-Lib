package my.celium.org.util;

import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The shared message kit with a mod's own tag, so every MyCelium mod reads
 * the same: a slim colored bar, the mod's name in grey, then the message
 * ({@code ▌ Pets  Rex came running.}). The bar's color says what kind of line it
 * is: brand red for information, green for done, amber for careful, red for
 * failed (failed lines also get light red text). Get one with
 * {@link TextUtil#voice(String)} and keep it in a constant.
 *
 * <p>Message parts are joined in order: strings become body text, components
 * are appended as they are (so names, values and buttons keep their style).
 */
public final class ChatVoice {
    private static final String BAR = "\u258C";

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
     * The start of a line: brand red bar, then the tag in grey.
     *
     * @return a new prefix component
     */
    public MutableComponent prefix() {
        return start(TextUtil.PRIMARY);
    }

    private MutableComponent start(int barColor) {
        return TextUtil.empty()
                .append(TextUtil.hex(BAR, barColor))
                .append(TextUtil.muted(" " + tag + "  "));
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
     * Info line: brand bar, tag, message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent note(Object... parts) {
        return prefixed(TextUtil.msg(parts));
    }

    /**
     * Success line: green bar, tag, message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent ok(Object... parts) {
        return TextUtil.empty().append(start(TextUtil.SUCCESS)).append(TextUtil.msg(parts));
    }

    /**
     * Error line: red bar, tag, message in light red.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent fail(Object... parts) {
        MutableComponent text = TextUtil.empty();
        for (Object part : parts) {
            text.append(part instanceof Component component ? component : TextUtil.hex(String.valueOf(part), TextUtil.ERROR_TEXT));
        }
        return TextUtil.empty().append(start(TextUtil.ERROR)).append(text);
    }

    /**
     * Warning line: amber bar, tag, message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent caution(Object... parts) {
        return TextUtil.empty().append(start(TextUtil.WARN)).append(TextUtil.msg(parts));
    }

    /**
     * Header line opening a multi-line reply: brand bar, tag, bold title. When the
     * title already starts with the tag (like {@code "Mycel 1.6.0"}) the tag is left out.
     *
     * @param title the title
     * @return the header line
     */
    public MutableComponent header(String title) {
        MutableComponent start = title.startsWith(tag)
                ? TextUtil.empty().append(TextUtil.hex(BAR, TextUtil.PRIMARY)).append(TextUtil.literal(" "))
                : prefix();
        return start.append(TextUtil.hex(title, TextUtil.BRIGHT).withStyle(style -> style.withBold(true)));
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
