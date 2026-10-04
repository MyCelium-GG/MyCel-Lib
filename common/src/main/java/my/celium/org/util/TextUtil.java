package my.celium.org.util;

import java.util.Collection;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Small helpers around {@link Component}. Mycel does not rebuild the text
 * system - these just remove qualifier noise.
 */
public final class TextUtil {
    private TextUtil() {
    }

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

    public static MutableComponent green(String text) {
        return literal(text).withStyle(ChatFormatting.GREEN);
    }

    public static MutableComponent red(String text) {
        return literal(text).withStyle(ChatFormatting.RED);
    }

    public static MutableComponent gray(String text) {
        return literal(text).withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent gold(String text) {
        return literal(text).withStyle(ChatFormatting.GOLD);
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
