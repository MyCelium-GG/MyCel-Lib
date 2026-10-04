# Mycel

**Mycel is the shared core library for the MyCelium mod ecosystem.**

MyCelium is a family of many small, focused, high-quality quality-of-life mods.
Mycel is the common infrastructure underneath them, so each mod stays tiny
instead of re-implementing configuration, networking, translations and the
other dozen systems every mod needs.

Players install it because MyCelium mods require it. Mod developers depend on
it to skip the boring parts.

- Minecraft **26.3** — Fabric, Forge and NeoForge (one universal jar)
- Java **25**
- License: **MIT**

## For players

Install Mycel like any other mod, alongside the MyCelium mods you want. It has
no configuration of its own that you need to touch, adds no gameplay of its
own, and does nothing unless a dependent mod uses it.

## What it provides (for developers)

- **Configuration** — typed values with ranges, categories, JSON files,
  migration, change listeners and reload, in about ten lines
- **Config screen** — reusable client UI with validation, reset and category tabs
- **Utilities** — cohesive block, item, inventory, player, entity, world and
  text helpers (no giant `Utils` class)
- **Entity replacement** — swap entities while preserving position, equipment,
  health, passengers, leash state and more, plus an event hook
- **Events** — player join/leave, server lifecycle, entity replacement
  (native loader buses for everything else, by design)
- **Networking** — vanilla-payload packets with one API for all loaders and
  game-thread handlers
- **Translations** — namespaced keys with never-crash fallback text
- **Update checker** — opt-in, asynchronous, cached, HTTPS-only, silent on failure
- **Platform abstraction** — loader, version, mod detection and config paths
  behind one tiny service
- **Scheduling** — server-thread `runLater`/`runEvery`, background `runAsync`
- **Registries** — deferred item/block/entity-type registration on every loader
- **Commands** — Brigadier helpers plus a built-in `/mycel` diagnostics command
  (`info`, `mods`, `reload <modid>`)
- **Diagnostics** — versions, platform, registered mods and config state

## Depending on Mycel

Via [JitPack](https://jitpack.io/#MyCelium-GG/MyCel-Lib) (single universal
artifact for all loaders):

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Fabric
    modImplementation 'com.github.MyCelium-GG:MyCel-Lib:1.1.0'
    // Forge
    // implementation 'com.github.MyCelium-GG:MyCel-Lib:1.1.0'
    // NeoForge
    // implementation 'com.github.MyCelium-GG:MyCel-Lib:1.1.0'
}
```

A minimal integration:

```java
public static final ModMetadata META =
        ModMetadata.builder("mymod", "My Mod", "1.0.0").build();

public static void init() {
    Mycel.initialize();
    Mycel.registerMod(META);

    ConfigBuilder builder = Mycel.config(META);
    ConfigValue<Boolean> enabled =
        builder.booleanValue("enabled", true, "Whether the feature runs.");
    MycelConfig config = builder.build();
}
```

## Links

- Sources: <https://github.com/MyCelium-GG/MyCel-Lib>
- Library reference: see the README on GitHub
- Issues and suggestions: GitHub Issues on the repository above
- Requires: Minecraft 26.3, Java 25, Fabric Loader / Forge / NeoForge

## License

MIT — free to use in any mod or modpack. See `LICENSE` in the repository.
