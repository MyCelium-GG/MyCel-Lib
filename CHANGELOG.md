# Changelog

All notable changes to Mycel are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Mycel is versioned semantically (`MAJOR.MINOR.PATCH`); breaking API changes
bump major. It ships as one universal jar per Minecraft version (26.3, 26.2, 26.1;
Fabric + Forge + NeoForge) through JitPack as
`com.github.MyCelium-GG:MyCel-Lib`.

## [1.5.1] - 2026-10-28

### Fixed

- JitPack builds for 26.2 and 26.1 (`<version>-26.2`, `<version>-26.1`)
  contained the 26.3 jar. The CurseForge, Modrinth and GitHub downloads of
  1.5.0 were right; only JitPack (for mod developers) was affected, so use
  1.5.1 there.

## [1.5.0] - 2026-10-28

### Added

- Minecraft 26.2 and 26.1 support (26.1, 26.1.1 and 26.1.2). Every release now
  ships one universal jar (Fabric + Forge + NeoForge) per Minecraft line:
  `mycel-26.3`, `mycel-26.2` and `mycel-26.1`. On JitPack the older lines are
  `com.github.MyCelium-GG:MyCel-Lib:<version>-26.2` and `...:<version>-26.1`.
  The API is the same on every version.
- `MycelScreens.open(screen)` / `MycelScreens.current()`: switch and read the
  current screen on every supported version (26.2 moved this from `Minecraft`
  to `Minecraft.gui`). Mods that use them instead of vanilla build for 26.1
  too.
- `./gradlew build -Pmc=26.2` (or `-Pmc=26.1`) builds for an older line (see
  "Other Minecraft versions" in the README).

### Changed

- CurseForge and Modrinth file names now include the Minecraft version, e.g.
  "Mycel 1.5.0 (Fabric, Forge, NeoForge, 26.2)".

## [1.4.1] - 2026-10-26

### Changed

- The API docs at https://api.mycelium-gg.org now describe every public
  class and method, with every parameter, return value and exception, plus
  examples for networking, events, the scheduler, commands, chat messages
  and translations.
- No behavior changes: mods built against 1.4.0 work unchanged.

## [1.4.0] - 2026-10-25

### Added

- API docs at https://api.mycelium-gg.org (standard Javadoc, rebuilt for
  every release). Mycel now promises semantic versioning for everything
  shown there; `my.celium.org.internal` stays private.
- `MycelDataFile`: a mod's own JSON data file with atomic writes and a size
  cap. A file that cannot be read is kept as `<name>.broken` instead of being
  overwritten by the next save. `inConfigDir`, `inWorld` (saved with the world)
  and `at`; typed `read(type, fallback)` / `write(value)` through Gson.
- `MycelTesting.useConfigDir(dir)` for headless unit tests, replacing the
  hand-written `PlatformService` stubs and `internal.PlatformHolder` calls.
- `MycelNetwork.canSend(player, packet)` and, on the client,
  `canSendToServer(packet)`: whether the other side has the channel. All
  senders, `sendToServer` included, now skip a peer without it instead of
  sending a packet NeoForge would reject.
- `MycelRegistry.registerWithKey(name, key -> ...)` and `key(name)`, for
  items and blocks, whose properties need `setId(key)` on Minecraft 26.x.
- Update notices in game: `/mycel mods` marks mods with an update (`↑ 1.2.0`),
  and operators (or the singleplayer host) get one chat line per session
  listing them. New `notifyOpsOfUpdates` flag in `mycel.json` (default `true`).

### Fixed

- Fabric: a serverbound packet registered through `MycelNetwork` crashed the
  client with "already registered" once both `FabricNetworking.flush()` and
  `flushClient()` ran.
- NeoForge: packet channels were registered as required, so a player with a
  Mycel mod could not join a server without it (and the other way round).
  They are optional now, and senders skip players without the channel.
- Forge: every `MycelNetwork` send kicked the receiving player ("cannot be
  cast to ForgePayload"): payloads went out as plain vanilla packets, which
  Forge's channel refuses to encode. They now go through the channel.
- Forge: Mycel's channel is optional too. With packets in both directions the
  channel was built twice, so every packet was handled twice.
- The update checker kept using a closed thread pool after leaving a
  singleplayer world, so later checks silently failed.

### Deprecated

- `InventoryUtil.holdOne`: it only ever reported whether a match exists and
  never moved anything into the hand. Use `findSlot`. Removed in 2.0.

## [1.3.1] - 2026-10-23

### Added

- `/mycel dump` (op level 2, or the singleplayer host): one support report
  with versions, every loaded mod, each Mycel mod's config and the tail of
  `logs/latest.log`. `/mycel dump` previews what is shared; `/mycel dump
  confirm` saves it to `mycel-dumps/`, uploads it to pastes.dev and replies
  with a `https://dumps.mycelium-gg.org/<key>` link. IP addresses, tokens and
  the OS home folder are removed first. New `allowDumpUploads` flag in
  `mycel.json` (default `true`) keeps dumps local when off.
- `TextUtil.voice(tag)` returns a `ChatVoice`: the shared message kit
  (`ok`, `fail`, `note`, `caution`, `header`) with a mod's own `Tag ›` prefix.
  `TextUtil.msg(parts...)` joins strings and components.
- `MycelTheme` (client): the wiki theme as ARGB for screens and HUD text, plus
  `panel()`, `colored()` and `fit()`.
- `PlatformService.loadedMods()` / `gameDir()` and `Mycel.getLoadedMods()`.
- `TextUtil.BRIGHT` (white) and `TextUtil.tone(ChatFormatting)`.

### Changed

- Chat colors now come only from the MyCelium wiki's dark theme: `ACCENT`
  is wiki blue `#79B8FF`, `SUCCESS` `#7EE787`, `WARN` `#FFA657`, `INFO`
  `#D2A8FF`, `FAINT` `#8B949E`, `ERROR` the wiki red `#EF5350`. The made-up
  light-red/rose/amber tones are gone.
- `TextUtil.colored(Component, ChatFormatting)` is deprecated and maps legacy
  colors to the nearest wiki tone.

### Fixed

- `CommandHelper.failure()` no longer lets vanilla paint uncolored parts
  legacy red (`§c`); they use the wiki body color.
- Config screen text boxes use the wiki body/error colors instead of
  `#E0E0E0` / legacy `#FF5555`.

## [1.3.0] - 2026-10-19

### Changed

- Chat messages use the MyCelium wiki palette: wiki red (`#EF5350`) brand,
  light-red keys and names, the wiki's text/muted greys. `TextUtil` constants
  keep their names, so dependent mods pick it up without code changes.
- Cleaner `/mycel` layout: single replies start with a bold `Mycel ›` tag;
  lists tag only their header and indent the rows below (new
  `TextUtil.item()`, `TextUtil.detail()`, `TextUtil.header(title, detail)`).
  `MycelDiagnostics.modLine()`/`configLine()` now return untagged rows.
- `CommandHelper.failure()` uses vanilla's failure channel, so errors still
  show when `sendCommandFeedback` is off.
- `/mycel` output uses plain separators (`·`, `›`, `→`) instead of em-dashes.
- Mycel checks for its own updates through the MyCelium wiki.

### Fixed

- Mods registered before `Mycel.initialize()` were never update-checked.
- The update checker now refuses oversized responses while reading instead of
  after buffering the whole body, and reuses one HTTP client.
- Update-available log line no longer prints the URL twice.

## [1.2.1] - 2026-10-06

### Fixed

- JitPack now builds with an installed Gradle (via SDKMAN) instead of the
  Gradle wrapper jar, which JitPack's current image fails to open.
  No API changes since 1.2.0; depend on this version for reliable resolves.

## [1.2.0] - 2026-10-06

### Added

- Live config editing from chat: `/mycel get <mod> [key]`,
  `/mycel set <mod> <key> <value>`, `/mycel reset <mod> [key]`,
  plus `/mycel help`. Viewing works for everyone; changing needs operator
  permission (level 2). Values are validated exactly like the config screen
  (ranges, enum names, booleans, comma-separated lists) with tab-completion
  for mod ids, keys, and boolean/enum values.
- Shared hex message palette in `TextUtil` (teal headers, violet names,
  green confirmations, red errors) behind a `[Mycel]` prefix, used by all
  `/mycel` output.
- New `ConfigStrings` helper: config-value lookup by path (`key` or
  `category.key`), display strings, constraint hints, and validated
  `parseAndSet()` for every value kind; `MycelConfig.find()` delegates to it.
- Styled diagnostics in `MycelDiagnostics` (`headerLine()`, `modLine()`,
  `configLine()`); `CommandHelper` keeps component formatting and gains a
  `failure()` helper.

## [1.1.0] - 2026-10-04

### Added

- Universal jar: one download for Fabric, Forge, and NeoForge alike.
- Runtime service selection for platform specifics.
