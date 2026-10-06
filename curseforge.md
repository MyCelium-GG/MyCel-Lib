# Mycel

**Required library for all MyCelium mods.**

Mycel is the shared core behind the MyCelium mod family. It provides
configuration, utilities, events, networking, translations, update checking
and more, so each MyCelium mod stays small. It adds no gameplay on its own.
Install it alongside any MyCelium mod that needs it.

- Minecraft 26.3 on Fabric, Forge and NeoForge (one universal jar)
- Java 25
- MIT license

## Developers

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    modImplementation 'com.github.MyCelium-GG:MyCel-Lib:1.2.1'
}
```

```java
Mycel.initialize();
Mycel.registerMod(META);
ConfigBuilder builder = Mycel.config(META);
ConfigValue<Boolean> enabled = builder.booleanValue("enabled", true, "Whether the feature runs.");
MycelConfig config = builder.build();
```

## Links

- Sources: <https://github.com/MyCelium-GG/MyCel-Lib>
- Issues: GitHub Issues on the repository above
