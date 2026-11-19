package my.celium.org.translation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import my.celium.org.logging.MycelLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A mod's messages in every player's own language, also on dedicated servers.
 *
 * <p>A dedicated server only has English loaded, so {@code Component.translatable}
 * from server code reaches players untranslated (or as raw keys on vanilla
 * clients). {@code ModLang} reads the mod's own {@code assets/<modid>/lang/<code>.json}
 * files on the server and resolves each message in the language the player's
 * client uses, falling back to the same language family ({@code de_at} reads
 * {@code de_de}), then to {@code en_us}, then to the key itself. Client code
 * gets the language the game is set to.
 *
 * <pre>{@code
 * public static final ModLang LANG = ModLang.of("mymod", MyMod.class);
 *
 * // assets/mymod/lang/en_us.json: { "mymod.refill.ran_out": "Ran out of %s" }
 * player.sendSystemMessage(TextUtil.caution(LANG.get(player, "refill.ran_out", blockName)));
 * }</pre>
 *
 * <p>Keys are given without the mod id: {@code "refill.ran_out"} reads
 * {@code "mymod.refill.ran_out"}. Messages use Minecraft's {@code %s} and
 * {@code %1$s} placeholders and {@code %%} for a percent sign, so the same files
 * serve {@code Component.translatable} on the client.
 */
public final class ModLang {
    /** The language every message falls back to. */
    public static final String DEFAULT = "en_us";

    private static final Map<String, ModLang> MODS = new ConcurrentHashMap<>();

    private final String modId;
    private final Class<?> anchor;
    private final Map<String, Map<String, String>> files = new ConcurrentHashMap<>();

    private ModLang(String modId, Class<?> anchor) {
        this.modId = modId;
        this.anchor = anchor;
    }

    /**
     * The translations of {@code modId}.
     *
     * @param modId  the mod id: the lang folder and the key prefix
     * @param anchor any class from the mod's jar; its class loader finds the files
     * @return the mod's translations, shared per mod id
     */
    public static ModLang of(String modId, Class<?> anchor) {
        return MODS.computeIfAbsent(modId, id -> new ModLang(id, anchor));
    }

    /**
     * The mod id this instance translates for.
     *
     * @return the mod id
     */
    public String modId() {
        return modId;
    }

    /**
     * The full key for a path: {@code "refill.ran_out"} becomes {@code "<modid>.refill.ran_out"}.
     *
     * @param path the key without the mod id
     * @return the full key
     */
    public String key(String path) {
        return modId + "." + path;
    }

    /**
     * A message as text, in the language {@code player} reads.
     *
     * @param player who reads it; {@code null} means the server console (English)
     * @param path   the key without the mod id
     * @param args   values for {@code %s}; components contribute their plain text
     * @return the message
     */
    public String get(Player player, String path, Object... args) {
        return format(template(languageOf(player), path), args);
    }

    /**
     * A message for whoever ran a command: a player in their language, anyone else in English.
     *
     * @param source the command source
     * @param path   the key without the mod id
     * @param args   values for {@code %s}
     * @return the message
     */
    public String get(CommandSourceStack source, String path, Object... args) {
        return get(source == null ? null : source.getPlayer(), path, args);
    }

    /**
     * A message in a given language.
     *
     * @param language a language code such as {@code de_de}
     * @param path     the key without the mod id
     * @param args     values for {@code %s}
     * @return the message
     */
    public String in(String language, String path, Object... args) {
        return format(template(language, path), args);
    }

    /**
     * A message as a component in the language {@code player} reads. Component
     * arguments keep their style.
     *
     * @param player who reads it; {@code null} means English
     * @param path   the key without the mod id
     * @param args   values for {@code %s}
     * @return the message
     */
    public MutableComponent text(Player player, String path, Object... args) {
        return component(template(languageOf(player), path), args);
    }

    /**
     * {@link #text(Player, String, Object...)} for a command source.
     *
     * @param source the command source
     * @param path   the key without the mod id
     * @param args   values for {@code %s}
     * @return the message
     */
    public MutableComponent text(CommandSourceStack source, String path, Object... args) {
        return text(source == null ? null : source.getPlayer(), path, args);
    }

    /**
     * Whether the mod ships {@code path} in English.
     *
     * @param path the key without the mod id
     * @return {@code true} when the key exists
     */
    public boolean has(String path) {
        return file(DEFAULT).containsKey(key(path));
    }

    /**
     * The language a player reads: their client's language on the server, the
     * game's language on the client, English for the console.
     *
     * @param player the player, or {@code null}
     * @return a lower-case language code
     */
    public static String languageOf(Player player) {
        if (player instanceof ServerPlayer server) {
            try {
                String code = server.clientInformation().language();
                if (code != null && !code.isBlank()) {
                    return code.toLowerCase(Locale.ROOT);
                }
            } catch (RuntimeException ignored) {
                // A fake player without client information reads English.
            }
            return DEFAULT;
        }
        if (player != null) {
            return ClientLanguage.selected();
        }
        return DEFAULT;
    }

    private String template(String language, String path) {
        String key = key(path);
        String code = language == null ? DEFAULT : language.toLowerCase(Locale.ROOT);
        String value = file(code).get(key);
        if (value == null) {
            String family = familyOf(code);
            if (family != null) {
                value = file(family).get(key);
            }
        }
        if (value == null) {
            value = file(DEFAULT).get(key);
        }
        if (value == null) {
            // On a client, resource packs may still provide it.
            try {
                Language loaded = Language.getInstance();
                if (loaded.has(key)) {
                    value = loaded.getOrDefault(key);
                }
            } catch (RuntimeException ignored) {
                // No language loaded (tests, early start).
            }
        }
        if (value == null) {
            warnMissing(key);
            return key;
        }
        return value;
    }

    /** {@code de_at} reads {@code de_de} when the mod has no {@code de_at}; null when nothing fits. */
    private String familyOf(String code) {
        int underscore = code.indexOf('_');
        if (underscore <= 0) {
            return null;
        }
        String same = code.substring(0, underscore) + "_" + code.substring(0, underscore);
        if (!same.equals(code) && !file(same).isEmpty()) {
            return same;
        }
        for (String known : Families.FALLBACKS.getOrDefault(code.substring(0, underscore), new String[0])) {
            if (!known.equals(code) && !file(known).isEmpty()) {
                return known;
            }
        }
        return null;
    }

    private Map<String, String> file(String code) {
        return files.computeIfAbsent(code, this::load);
    }

    private Map<String, String> load(String code) {
        String path = "/assets/" + modId + "/lang/" + code + ".json";
        try (InputStream in = anchor.getResourceAsStream(path)) {
            if (in == null) {
                return Map.of();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    return Map.of();
                }
                Map<String, String> out = new HashMap<>();
                JsonObject object = root.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        out.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                return Map.copyOf(out);
            }
        } catch (Exception e) {
            MycelLog.warn("Broken language file {} of {}: {}", code, modId, e.toString());
            return Map.of();
        }
    }

    private final java.util.Set<String> warned = ConcurrentHashMap.newKeySet();

    private void warnMissing(String key) {
        if (warned.add(key)) {
            MycelLog.debug("Missing translation '{}' in every language of {}", key, modId);
        }
    }

    /**
     * Fills {@code %s}, {@code %1$s} and {@code %%} like Minecraft's language files.
     * Unknown or broken placeholders are left as they are.
     *
     * @param template the message
     * @param args     the values; components contribute their plain text
     * @return the filled message
     */
    public static String format(String template, Object... args) {
        if (template.indexOf('%') < 0) {
            return template;
        }
        StringBuilder out = new StringBuilder(template.length() + 16);
        walk(template, args, out::append, arg -> out.append(plain(arg)));
        return out.toString();
    }

    /**
     * Like {@link #format} but as a component, keeping the style of component arguments.
     *
     * @param template the message
     * @param args     the values
     * @return the filled message
     */
    public static MutableComponent component(String template, Object... args) {
        MutableComponent out = Component.empty();
        StringBuilder text = new StringBuilder();
        walk(template, args, text::append, arg -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text.toString()));
                text.setLength(0);
            }
            out.append(arg instanceof Component c ? c.copy() : Component.literal(plain(arg)));
        });
        if (!text.isEmpty()) {
            out.append(Component.literal(text.toString()));
        }
        return out;
    }

    private static String plain(Object arg) {
        return arg instanceof Component c ? c.getString() : String.valueOf(arg);
    }

    private static void walk(String template, Object[] args, java.util.function.Consumer<String> text,
            java.util.function.Consumer<Object> arg) {
        int next = 0;
        int i = 0;
        int n = template.length();
        while (i < n) {
            char c = template.charAt(i);
            if (c != '%' || i + 1 >= n) {
                text.accept(String.valueOf(c));
                i++;
                continue;
            }
            char d = template.charAt(i + 1);
            if (d == '%') {
                text.accept("%");
                i += 2;
                continue;
            }
            if (d == 's' || d == 'd') {
                emit(args, next++, text, arg, template.substring(i, i + 2));
                i += 2;
                continue;
            }
            int j = i + 1;
            while (j < n && Character.isDigit(template.charAt(j))) {
                j++;
            }
            if (j > i + 1 && j + 1 < n && template.charAt(j) == '$'
                    && (template.charAt(j + 1) == 's' || template.charAt(j + 1) == 'd')) {
                int index = Integer.parseInt(template.substring(i + 1, j)) - 1;
                emit(args, index, text, arg, template.substring(i, j + 2));
                i = j + 2;
                continue;
            }
            text.accept("%");
            i++;
        }
    }

    private static void emit(Object[] args, int index, java.util.function.Consumer<String> text,
            java.util.function.Consumer<Object> arg, String raw) {
        if (args != null && index >= 0 && index < args.length) {
            arg.accept(args[index]);
        } else {
            text.accept(raw);
        }
    }

    /** Language families: which shipped language a regional variant reads. */
    private static final class Families {
        static final Map<String, String[]> FALLBACKS = Map.ofEntries(
                Map.entry("en", new String[] {"en_us"}),
                Map.entry("de", new String[] {"de_de"}),
                Map.entry("es", new String[] {"es_es", "es_mx"}),
                Map.entry("fr", new String[] {"fr_fr", "fr_ca"}),
                Map.entry("nl", new String[] {"nl_nl"}),
                Map.entry("pt", new String[] {"pt_br", "pt_pt"}),
                Map.entry("ru", new String[] {"ru_ru"}),
                Map.entry("pl", new String[] {"pl_pl"}),
                Map.entry("it", new String[] {"it_it"}),
                Map.entry("ja", new String[] {"ja_jp"}),
                Map.entry("zh", new String[] {"zh_cn", "zh_tw"}));
    }

    /** The game's language on a client, read without referencing client classes on servers. */
    private static final class ClientLanguage {
        static String selected() {
            try {
                Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
                Object instance = mc.getMethod("getInstance").invoke(null);
                Object options = mc.getField("options").get(instance);
                Object code = options.getClass().getField("languageCode").get(options);
                return code == null ? DEFAULT : code.toString().toLowerCase(Locale.ROOT);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return DEFAULT;
            }
        }
    }
}
