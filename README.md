# At Hand
At Hand is a calm desktop companion for weekly Stoic practice.

This repository currently contains **Milestone 2**: the Compose Desktop shell plus bundled weekly practice content, deterministic current week/day selection, and unit tests for week-boundary behavior.

## Requirements

- JDK 21 (or newer, with Gradle toolchain support for Java 21)
- Linux desktop environment with standard GUI libraries

For Ubuntu packaging work (`.deb`) later in the roadmap, these packages are typically required:

- `fakeroot`
- `dpkg-dev`
- `libgtk-3-dev`
- `libxext-dev`
- `libxrender-dev`
- `libxtst-dev`

## Run

```bash
./gradlew run
```

If the Gradle wrapper is not present yet, generate it once:

```bash
gradle wrapper --gradle-version 9.1.0
```

For Java 25, Gradle 9.1.0+ is required.

## Test

```bash
./gradlew test
```

## Build

```bash
./gradlew build
```

## Compose desktop lifecycle used here

- The app starts in `main` via `application { ... }`.
- A single `Window` is created with compact default dimensions and remains resizable.
- UI state (`ThemeMode`) is remembered in-process.
- Today content is loaded from `src/main/resources/practices/practice-weeks.json` and validated at startup.
- Week progression is deterministic from a Monday-based sequence anchor date (`2026-01-05`) and cycles through bundled practices.
- Closing the window calls `exitApplication()`, which cleanly terminates the process.
