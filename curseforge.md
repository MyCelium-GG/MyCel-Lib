# Mycel

The required library for all MyCelium mods. Mycel is the shared core behind the MyCelium mods: configuration, utilities, events, networking, translations and update checking, so each mod stays small. It adds no gameplay on its own, so install it alongside any MyCelium mod that needs it.

One download per Minecraft version covers Fabric, Forge and NeoForge. Minecraft 26.1, 26.2 and 26.3, Java 25, MIT license.

## Mods that use Mycel

- [Better Pets](https://www.curseforge.com/minecraft/mc-mods/better-pets): list, find, call and rename your pets, with a full menu
- [Essentials GG](https://www.curseforge.com/minecraft/mc-mods/essentials-gg): homes, warps, kits, economy and moderation for servers
- [AutoBlockRefiller](https://www.curseforge.com/minecraft/mc-mods/auto-block-refiller): empty hotbar slots refill while you build
- [BedrockPlacing](https://www.curseforge.com/minecraft/mc-mods/bedrock-placing): place blocks by clicking the air, like on Bedrock
- [InstantXP](https://www.curseforge.com/minecraft/mc-mods/instant-xp-pickup): XP orbs are picked up instantly
- [Better Librarians](https://www.curseforge.com/minecraft/mc-mods/better-librarians): see a librarian's books without opening the trade screen
- [ItemHighlighter](https://www.curseforge.com/minecraft/mc-mods/item-highlighter-mod): give any item a color and spot it everywhere
- [Brewing+](https://www.curseforge.com/minecraft/mc-mods/brewing-plus): recipes and next steps right at the brewing stand
- [Anvil Cost Calculator](https://www.curseforge.com/minecraft/mc-mods/anvil-cost-calculator): the cheapest order to apply your enchantments
- [More Beacon Colors](https://www.curseforge.com/minecraft/mc-mods/more-beacon-colors): any color for beacon beams
- [Colored Nether Portals](https://www.curseforge.com/minecraft/mc-mods/colored-nether-portal): any color for Nether portals
- [Better Server List](https://www.curseforge.com/minecraft/mc-mods/better-server-list): lists, categories and search for the multiplayer screen

## Developers

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.MyCelium-GG:MyCel-Lib:1.5.1'
}
```

Use `1.5.1-26.2` or `1.5.1-26.1` for older Minecraft.

```java
Mycel.initialize();
Mycel.registerMod(META);
ConfigBuilder builder = Mycel.config(META);
ConfigValue<Boolean> enabled = builder.booleanValue("enabled", true, "Whether the feature runs.");
MycelConfig config = builder.build();
```

- API docs: <https://api.mycelium-gg.org>
- Wiki: <https://wiki.mycelium-gg.org/mycel>
- Sources and issues: <https://github.com/MyCelium-GG/MyCel-Lib>
