package my.celium.org.util;

import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The shared message kit with a mod's own tag, so every MyCelium mod reads
 * the same: {@code Pets › ✔ Rex came running.} Get one with
 * {@link TextUtil#voice(String)} and keep it in a constant.
 *
 * <p>Message parts are joined in order: strings become body text, components
 * are appended as they are (so names, values and buttons keep their style).
 */
public final class ChatVoice {
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
     * {@code Tag › } prefix: bold wiki-red tag + faint chevron.
     *
     * @return a new prefix component
     */
    public MutableComponent prefix() {
        return TextUtil.empty()
                .append(TextUtil.primary(tag).withStyle(style -> style.withBold(true)))
                .append(TextUtil.faint(" › "));
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
     * Info line: tag + message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent note(Object... parts) {
        return prefixed(TextUtil.msg(parts));
    }

    /**
     * Success line: tag + green check + message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent ok(Object... parts) {
        return prefixed(TextUtil.empty().append(TextUtil.success("✔ ")).append(TextUtil.msg(parts)));
    }

    /**
     * Error line: tag + red cross + message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent fail(Object... parts) {
        return prefixed(TextUtil.empty().append(TextUtil.error("✘ ")).append(TextUtil.msg(parts)));
    }

    /**
     * Warning line: tag + orange bang + message.
     *
     * @param parts strings (body text) and components (kept as they are), in order
     * @return the line
     */
    public MutableComponent caution(Object... parts) {
        return prefixed(TextUtil.empty().append(TextUtil.warn("! ")).append(TextUtil.msg(parts)));
    }

    /**
     * Header line opening a multi-line reply: tag + bright bold title.
     *
     * @param title the title
     * @return the header line
     */
    public MutableComponent header(String title) {
        return prefixed(TextUtil.body(title).withStyle(style -> style.withBold(true)));
    }

    /**
     * Header line with a muted suffix, e.g. {@code header("Your pets", "3")}.
     *
     * @param title  the title
     * @param detail the muted text after it
     * @return the header line
     */
    public MutableComponent header(String title, String detail) {
        return header(title).append(TextUtil.muted("  " + detail));
    }
}
