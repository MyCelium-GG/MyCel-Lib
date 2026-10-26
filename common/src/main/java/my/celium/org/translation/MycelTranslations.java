package my.celium.org.translation;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import my.celium.org.logging.MycelLog;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Centralised translation helpers.
 *
 * <p>Keys are namespaced per mod: {@code key("mymod", "config.enabled")} gives
 * {@code "mymod.config.enabled"}. Mods ship their own {@code en_us.json} (and
 * friends) under {@code assets/<modid>/lang/} as usual; these helpers only
 * remove key-building boilerplate and guarantee a missing translation can
 * never crash the game - it falls back to readable text plus one debug line.
 *
 * <pre>{@code
 * // assets/mymod/lang/en_us.json: { "mymod.join.greeting": "Welcome back, %s!" }
 * PlayerUtil.sendMessage(player, MycelTranslations.componentOrFallback(
 *         "mymod", "join.greeting", "Welcome back, " + name + "!", name));
 * }</pre>
 * It uses the key when the language loaded where the code runs has it, and
 * the literal fallback otherwise.
 *
 * <p>Remote translation downloading is intentionally not implemented: it would
 * add network, cache-invalidation and trust concerns to a core library. If it
 * ever lands, it will be optional, asynchronous, cached, timeout-controlled,
 * validated and failure-tolerant.
 */
public final class MycelTranslations {
    private static final Set<String> WARNED_KEYS =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private MycelTranslations() {
    }

    /**
     * Builds {@code "<modId>.<path>"} translation keys.
     *
     * @param modId the mod id (the key's namespace)
     * @param path  the rest of the key, e.g. {@code "message.found"}
     * @return the full key
     * @throws IllegalArgumentException when either part is blank
     */
    public static String key(String modId, String path) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        return modId + "." + path;
    }

    /**
     * {@code Component.translatable} for a namespaced key.
     *
     * @param modId the mod id (the key's namespace)
     * @param path  the rest of the key, e.g. {@code "message.found"}
     * @param args  values for the key's {@code %s} slots
     * @return a new translatable component
     */
    public static MutableComponent component(String modId, String path, Object... args) {
        return Component.translatable(key(modId, path), args);
    }

    /**
     * Translatable component, or a literal {@code fallback} when the key is
     * missing. Never throws: any failure (including headless environments
     * without a loaded language) yields the fallback.
     *
     * @param modId the mod id (the key's namespace)
     * @param path  the rest of the key, e.g. {@code "message.found"}
     * @param fallback the English text to show when the key is missing
     * @param args  values for the key's {@code %s} slots
     * @return the translated component, or the fallback as a literal
     */
    public static Component componentOrFallback(String modId, String path, String fallback, Object... args) {
        String fullKey = key(modId, path);
        try {
            if (has(fullKey)) {
                return Component.translatable(fullKey, args);
            }
        } catch (RuntimeException e) {
            warnOnce(fullKey);
            return Component.literal(fallback);
        }
        warnOnce(fullKey);
        return Component.literal(fallback);
    }

    /**
     * Whether the currently loaded language provides {@code fullKey}.
     *
     * @param fullKey the full key, e.g. from {@link #key(String, String)}
     * @return {@code true} when the language has it
     */
    public static boolean has(String fullKey) {
        return Language.getInstance().has(fullKey);
    }

    private static void warnOnce(String fullKey) {
        if (WARNED_KEYS.add(fullKey)) {
            MycelLog.debug("Missing translation key '{}'; using fallback text", fullKey);
        }
    }
}
