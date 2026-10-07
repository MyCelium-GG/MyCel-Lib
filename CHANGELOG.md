# Changelog

All notable changes to Mycel are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Mycel is versioned semantically (`MAJOR.MINOR.PATCH`); breaking API changes
bump major. It ships as one universal jar for Minecraft 26.3
(Fabric + Forge + NeoForge) through JitPack as
`com.github.MyCelium-GG:MyCel-Lib`.

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
