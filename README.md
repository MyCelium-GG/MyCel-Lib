# Mycel — shared core for the MyCelium ecosystem

[![Release](https://jitpack.io/v/MyCelium-GG/MyCel-Lib.svg)](https://jitpack.io/#MyCelium-GG/MyCel-Lib)

Mycel is the common infrastructure library for **MyCelium** quality-of-life Minecraft mods.
It plays the same architectural role for MyCelium that Collective plays for Serilum's mods
(Common/Fabric/Forge/NeoForge source sets, loader-independent APIs), but it is an
independent, original implementation: no Collective code, names, or assets are used.

> Tiny QoL mod → **Mycel** → Minecraft. No framework gymnastics.

| | |
|---|---|
| Package | `my.celium.org` |
| Maven | `my.celium.org:mycel-<loader>-<mc>:1.0.1` |
| Minecraft | **26.3** |
| Java | **25** (Gradle daemon and compiler toolchain) |
| Loaders | Fabric, Forge, NeoForge (all first-class, one unified `./gradlew build`) |
| Toolchains | Fabric Loom 1.18 · ForgeGradle 7 · NeoForge ModDev 2.0 |
| Mappings | Official Mojang mappings everywhere |
| License | MIT |

## What Mycel gives a dependent mod

- **Bootstrap** — `Mycel.initialize()` (idempotent), `Mycel.registerMod(metadata)`
- **Configuration** — typed values, ranges, categories, JSON files, migration, change
  listeners, reload — in ~10 lines
- **Config screen** — reusable client UI with validation, reset, category tabs
- **Utilities** — cohesive `BlockUtil`, `ItemUtil`, `InventoryUtil`, `PlayerUtil`,
  `EntityUtil`, `WorldUtil`, `TextUtil` (no giant `Utils` class)
- **Entity replacement** — `EntityReplacer` with position/equipment/health/passengers/leash
  preservation plus an event hook
- **Events** — join, leave, server lifecycle, entity-replaced (native loader buses
  for everything else, by design)
- **Networking** — vanilla-payload packets, one API for all loaders, game-thread handlers
- **Translations** — namespaced keys, never-crash fallback
- **Update checker** — opt-in, async, cached, HTTPS-only, silent on failure
- **Platform abstraction** — loader/version/mod-detection/config-dir behind one service
- **Scheduling** — server-thread `runLater`/`runEvery`, background `runAsync`
- **Registries** — deferred item/block/entity-type registration on every loader
- **Commands** — Brigadier helpers plus a built-in `/mycel` diagnostics command
- **Diagnostics** — versions, platform, registered mods, config state

Gameplay features do **not** belong here — they live in the small mods.

## Depending on Mycel

Mycel is distributed through [JitPack](https://jitpack.io/#MyCelium-GG/MyCel-Lib).
Use a release tag (e.g. `1.0.0`) — or any commit hash / `main-SNAPSHOT` for
bleeding edge:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Fabric (loom)
    modImplementation 'com.github.MyCelium-GG.MyCel-Lib:mycel-fabric-26.3:1.0.1'
    // Forge (ForgeGradle userdev)
    // implementation 'com.github.MyCelium-GG.MyCel-Lib:mycel-forge-26.3:1.0.1'
    // NeoForge (ModDev)
    // implementation 'com.github.MyCelium-GG.MyCel-Lib:mycel-neoforge-26.3:1.0.1'
}
```

For local development, `./gradlew publishToMavenLocal` installs
`my.celium.org:mycel-<loader>-26.3:1.0.1` into `~/.m2`.

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
Reload live with `CONFIG.reload()` or `/mycel reload mymod`.

### Networking

```java
public record PingPacket(String text) implements CustomPacketPayload {
    public static final Type<PingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mymod", "ping"));
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

Registered automatically on every loader: `info`, `mods`, `reload <modid>`.

## Project layout

```text
common/          loader-independent API + implementations (no loader imports)
fabric/          Fabric entrypoints, events, networking, platform, registry
forge/           Forge entrypoint, events, networking, platform, registry
neoforge/        NeoForge entrypoint, events, networking, platform, registry
example-*/       MycelExample: join-greeting mod proving the whole API
```

`common` compiles against vanilla Minecraft only (via NeoForm). Loader seams are
`PlatformService`, `RegistryBridge` (both `ServiceLoader`-discovered) and the
loader event/payload wiring, which iterates common registries. Client-only code
lives in `my.celium.org.client` and is only ever touched from client entrypoints
or through a reflection-guarded proxy — dedicated servers never load it.

## Building

Requires JDK 25 (the Gradle daemon, Loom's 26.x handling and all toolchains
run on it):

```sh
./gradlew build            # all loaders + example + tests (single invocation)
./gradlew :common:test     # unit tests only
```

Run configs (`:example-fabric:runClient`, `:example-neoforge:runServer`,
`:example-forge:runClient`, …) are generated by Loom / ModDev / ForgeGradle
for client and dedicated-server smoke tests. Accept the Minecraft EULA in the
respective `runs/*/eula.txt` first.

First builds download and process Minecraft (~1GB caches, decompile takes
several minutes) and peak around 6–8GB RAM; later builds are incremental.

## Deliberate non-goals

- No dependency injection, event-bus-everything, or abstraction layers for one-liners.
- No HTTP/JSON/GUI/logging frameworks beyond what Minecraft ships (Gson, SLF4J).
- No remote translation downloading (documented; would be optional/async/cached if added).
- No gameplay features. No giant `Utils` class. No per-tick scans, no sync HTTP,
  no reflection in hot paths, no required background threads.

## Versioning

Semantic (`MAJOR.MINOR.PATCH`); breaking API changes bump major. Artifacts carry the
Minecraft version (`mycel-fabric-26.3`). `Built-On-Minecraft` is in every manifest.
