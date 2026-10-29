# Mycel: shared core for the MyCelium ecosystem

[![Release](https://jitpack.io/v/MyCelium-GG/MyCel-Lib.svg)](https://jitpack.io/#MyCelium-GG/MyCel-Lib)
[![CI](https://github.com/MyCelium-GG/MyCel-Lib/actions/workflows/build.yml/badge.svg)](https://github.com/MyCelium-GG/MyCel-Lib/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/MyCelium-GG/MyCel-Lib)](LICENSE)
[![Last commit](https://img.shields.io/github/last-commit/MyCelium-GG/MyCel-Lib)](https://github.com/MyCelium-GG/MyCel-Lib/commits/main)

Mycel is the common infrastructure library for **MyCelium** quality-of-life Minecraft mods.
It plays the same architectural role for MyCelium that Collective plays for Serilum's mods
(Common/Fabric/Forge/NeoForge source sets, loader-independent APIs), but it is an
independent, original implementation: no Collective code, names, or assets are used.

> Tiny QoL mod → **Mycel** → Minecraft. No framework gymnastics.

| | |
|---|---|
| Package | `my.celium.org` |
| Maven | `com.github.MyCelium-GG:MyCel-Lib:1.5.1` (universal, via JitPack; `1.5.1-26.2` / `1.5.1-26.1` for older Minecraft) |
| API docs | [api.mycelium-gg.org](https://api.mycelium-gg.org) (Javadoc; `./gradlew :common:javadoc` builds them locally) |
| Minecraft | **26.3**, **26.2** and **26.1** (26.1, 26.1.1, 26.1.2) |
| Java | **25** (Gradle daemon and compiler toolchain) |
| Loaders | Fabric, Forge, NeoForge (all first-class, one unified `./gradlew build`) |
| Toolchains | Fabric Loom 1.18 · ForgeGradle 7 · NeoForge ModDev 2.0 |
| Mappings | Official Mojang mappings everywhere |
| License | MIT |

## What Mycel gives a dependent mod

- **Bootstrap**: `Mycel.initialize()` (idempotent), `Mycel.registerMod(metadata)`
- **Configuration**: typed values, ranges, categories, JSON files, migration, change
  listeners, reload. All in about ten lines.
- **Config screen**: reusable client UI with validation, reset, category tabs
- **Utilities**: cohesive `BlockUtil`, `ItemUtil`, `InventoryUtil`, `PlayerUtil`,
  `EntityUtil`, `WorldUtil`, `TextUtil` (no giant `Utils` class)
- **Entity replacement**: `EntityReplacer` preserving position, equipment,
  health, passengers, leash and more, plus an event hook
- **Events**: join, leave, server lifecycle, entity-replaced (native loader buses
  for everything else, by design)
- **Networking**: vanilla-payload packets, one API for all loaders, game-thread handlers
- **Translations**: namespaced keys, never-crash fallback
- **Update checker**: opt-in, async, cached, HTTPS-only, silent on failure
- **Platform abstraction**: loader, version, mod detection and config dir behind one service
- **Scheduling**: server-thread `runLater`/`runEvery`, background `runAsync`
- **Registries**: deferred item/block/entity-type registration on every loader
- **Commands**: Brigadier helpers plus a built-in `/mycel` diagnostics command
- **Diagnostics**: versions, platform, registered mods, config state, and
  shareable `/mycel dump` reports
- **Look**: `TextUtil` chat palette + `ChatVoice` message kit, `MycelTheme`
  for screens, all in the MyCelium wiki colors

Gameplay features do **not** belong here. They live in the small mods.

## Depending on Mycel

Mycel ships as **one universal jar** that loads on Fabric, Forge and
NeoForge alike (like Collective's `fabric+forge+neo` bundle): one download
per Minecraft version, no per-loader artifacts to choose between. It is
distributed through [JitPack](https://jitpack.io/#MyCelium-GG/MyCel-Lib).
Use a release tag (e.g. `1.5.1`), a commit hash, or `main-SNAPSHOT` for bleeding edge:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Fabric (loom)
    modImplementation 'com.github.MyCelium-GG:MyCel-Lib:1.5.1'
    // Forge (ForgeGradle userdev)
    // implementation 'com.github.MyCelium-GG:MyCel-Lib:1.5.1'
    // NeoForge (ModDev)
    // implementation 'com.github.MyCelium-GG:MyCel-Lib:1.5.1'
}
```

Each release is also built for older Minecraft lines: append the line to the
tag (`com.github.MyCelium-GG:MyCel-Lib:1.5.1-26.2`, `...:1.5.1-26.1`). The
release workflow pushes those tags; JitPack builds them with `-Pmc=<line>`.
The 26.1 jar is built against 26.1.2 and loads on 26.1, 26.1.1 and 26.1.2.

For local development, `./gradlew :universal:publishToMavenLocal` installs
`my.celium.org:mycel-26.3:1.5.1` into `~/.m2` (add `-Pmc=26.2` or `-Pmc=26.1`
for `mycel-26.2` / `mycel-26.1`).

## Minimal integration

```java
public static final ModMetadata META =
        ModMetadata.builder("mymod", "My Mod", "1.0.0").build();

public static final ConfigValue<Boolean> ENABLED;
public static final MycelConfig CONFIG;

public static void init() {           // called from each loader entrypoint
    Mycel.initialize();
    Mycel.registerMod(META);

    ConfigBuilder builder = Mycel.config(META);
    ENABLED = builder.booleanValue("enabled", true, "Whether the feature runs.");
    CONFIG = builder.build();         // loads config/mymod.json

    MycelEvents.onPlayerJoin(event -> {
        if (Mycel.isEnabled("mymod") && ENABLED.get()) {
            PlayerUtil.sendMessage(event.player(), Component.literal("Hi!"));
        }
    });
}
```

More values: `intValue(key, def, min, max, desc)`, `longValue`, `doubleValue`,
`stringValue`, `enumValue(key, Type.class, def, desc)`, `stringListValue`.
Group with `builder.category("tweaks")` / `builder.root()`.
Version configs with `builder.schemaVersion(2)` + `builder.migrate(1, json -> …)`.
Reload live with `CONFIG.reload()`, `/mycel reload mymod`, or edit live with
`/mycel set mymod <key> <value>` / `/mycel get mymod [key]` / `/mycel reset mymod [key]`.

### Networking

```java
public record PingPacket(String text) implements CustomPacketPayload {
    public static final Type<PingPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("mymod", "ping"));
    public static final StreamCodec<FriendlyByteBuf, PingPacket> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PingPacket::text, PingPacket::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

PacketDefinition<PingPacket> PING = PacketDefinition.toClient(
        PingPacket.TYPE, PingPacket.CODEC, (packet, ctx) -> { /* client game thread */ });
MycelNetwork.register(META, PING);
MycelNetwork.sendToPlayer(player, PING, new PingPacket("hi"));
```

On Fabric, if you register packets after Mycel's entrypoint ran, call
`FabricNetworking.flush()` once from your entrypoint. On Forge/NeoForge the payload
event always fires after every mod constructor, so nothing extra is needed.

### Config screen

```java
// Server-safe (returns null on dedicated servers):
Screen screen = Mycel.openConfigScreen(parentScreen, "mymod");
```

- Forge/NeoForge: register it for your mod-list entry with
  `ConfigScreenFactory` / `IConfigScreenFactory` (see `mycelexample`).
- Fabric: add a one-line ModMenu integration calling the same method
  (ModMenu stays an optional dependency; Mycel does not require it).

### `/mycel` command

Registered automatically on every loader. All output uses the shared hex
palette in `TextUtil`, built only from the MyCelium wiki's dark-theme colors
(wiki red brand, blue keys, green/orange/red status marks, no legacy `§` colors). Single replies start with a bold
`Mycel ›` tag; lists tag only their header and indent the rows below it:

- `help` — usage overview
- `info` — library, loader and MC versions
- `mods` — every mod registered with Mycel
- `get <modid> [key]` — print one or all config values (anyone)
- `set <modid> <key> <value>` — change + save a config value live (op level 2).
  Validated exactly like the config screen; booleans accept `true/false`
  (`1/0`, `yes/no`, `on/off`), enums are case-insensitive, lists are
  comma-separated. Keys are `key` or `category.key`.
- `reset <modid> [key]` — restore one or all values to defaults (op level 2)
- `dump` — support report; `dump confirm` uploads it to pastes.dev and replies
  with a `dumps.mycelium-gg.org` link (op level 2 or singleplayer host;
  `allowDumpUploads: false` in `mycel.json` keeps it local)
- `reload <modid>` — re-read the config file from disk (op level 2)

Tab-completion covers mod ids, config keys and (for booleans/enums) values.

## Project layout

```text
common/          loader-independent API + implementations (no loader imports)
fabric/          Fabric entrypoints, events, networking, platform, registry
forge/           Forge entrypoint, events, networking, platform, registry
neoforge/        NeoForge entrypoint, events, networking, platform, registry
example-*/       MycelExample: join-greeting mod proving the whole API
mc/              per-Minecraft-line overrides of gradle.properties (-Pmc=26.2)
common/src/mc/   version-specific code, one folder per Minecraft line
```

`common` compiles against vanilla Minecraft only (via NeoForm). Loader seams are
`PlatformService`, `RegistryBridge` (both `ServiceLoader`-discovered) and the
loader event/payload wiring, which iterates common registries. Client-only code
lives in `my.celium.org.client` and is only ever touched from client entrypoints
or through a reflection-guarded proxy. Dedicated servers never load client code.

## Building

Requires JDK 25 (the Gradle daemon, Loom's 26.x handling and all toolchains
run on it):

```sh
./gradlew build            # all loaders + example + tests (single invocation)
./gradlew :common:test     # unit tests only
./gradlew build -Pmc=26.2  # the same for Minecraft 26.2 (or -Pmc=26.1)
```

### Other Minecraft versions

`gradle.properties` targets the newest Minecraft (26.3). `-Pmc=<line>`
loads `mc/<line>.properties` over it (Minecraft, NeoForm, Fabric API,
Forge and NeoForge versions), and `common` compiles
`common/src/mc/<line>/java` next to its own sources. `minecraft_line` names
the jar and those folders; `minecraft_version` is the exact version built
against, and `minecraft_versions` (optional) lists the game versions the jar
is published for. That folder holds
`internal/MinecraftCompat`, which wraps every vanilla call whose signature
differs between the versions; code in `common/src/main` calls it instead of
vanilla. Every version's `MinecraftCompat` has the same methods, so a change
that needs one adds it to all of them. CI builds each version.

Run configs (`:example-fabric:runClient`, `:example-neoforge:runServer`,
`:example-forge:runClient`, …) are generated by Loom / ModDev / ForgeGradle
for client and dedicated-server smoke tests. Accept the Minecraft EULA in the
respective `runs/*/eula.txt` first.

First builds download and process Minecraft (~1GB caches, decompile takes
several minutes) and peak around 6-8GB RAM; later builds are incremental.

## Releasing

Push a version tag (`git tag 1.3.2 && git push origin 1.3.2`). The `release`
workflow builds the universal jar for every line (26.3, 26.2, 26.1), creates
the GitHub Release with all of them, pushes a `<tag>-<line>` tag per older
line for JitPack and uploads each
jar to CurseForge for its Minecraft version, with the matching
`CHANGELOG.md` section as its changelog.
To publish a tag that already exists, run the workflow by hand (Actions →
release → Run workflow) and enter the tag.

The CurseForge step is the shared action in
`.github/actions/curseforge-upload`, which every MyCelium mod uses too
(`MyCelium-GG/MyCel-Lib/.github/actions/curseforge-upload@main`). Mods set
`curseforge_project_id` and `curseforge_environment` (`client`, `server` or
`both`) in `gradle.properties`; an empty id skips CurseForge. The API token
lives in each repo's `CURSEFORGE_TOKEN` secret. Game versions are looked up by
name, and only the jar named for the tagged version is uploaded. Both upload
actions take an optional `minecraft-version` input, one or more game versions
separated by spaces (default: `minecraft_versions`, else `minecraft_version`,
in `gradle.properties`).

## Deliberate non-goals

- No dependency injection, event-bus-everything, or abstraction layers for one-liners.
- No HTTP/JSON/GUI/logging frameworks beyond what Minecraft ships (Gson, SLF4J).
- No remote translation downloading (documented; would be optional/async/cached if added).
- No gameplay features. No giant `Utils` class. No per-tick scans, no sync HTTP,
  no reflection in hot paths, no required background threads.

## Versioning

Semantic (`MAJOR.MINOR.PATCH`); breaking API changes bump major. The universal
artifact carries the Minecraft version (`mycel-26.3`, `mycel-26.2`, `mycel-26.1`). `Built-On-Minecraft`
is in every manifest.
