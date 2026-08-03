# At Hand
At Hand is a calm desktop companion for weekly Stoic practice.

This repository currently contains **Milestone 5**: the Compose Desktop shell, bundled weekly practice content, deterministic current week/day selection, local persistence, top-level navigation, tray behavior, and reminder settings.

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

## Data persistence

- Reflections and completion state are saved locally in the user app-data directory.
- On Linux this defaults to `~/.local/share/at-hand/`.
- Files:
  - `reflections.json`
  - `progress.json`
  - `preferences.json`
- Save is explicit from the Today screen (`Save` button).
- Corrupt JSON is preserved as `*.corrupt-<timestamp>` and the app resets that document to defaults.

## Tray and reminders (Milestone 5)

- Closing the main window hides the app to the system tray.
- Tray menu actions: `Show`, `Hide`, `Open Today`, `Open Settings`, `Quit`.
- Reminder preferences are configured in `Settings` and persisted locally.
- Reminder time format is `HH:mm`.
- `Quit` from the tray stops reminder scheduling and exits the process.

## Compose desktop lifecycle used here

- The app starts in `main` via `application { ... }`.
- A single `Window` is created with compact default dimensions and remains resizable.
- UI state (`ThemeMode`) is remembered in-process.
- Today content is loaded from `src/main/resources/practices/practice-weeks.json` and validated at startup.
- Week progression is deterministic from a Monday-based sequence anchor date (`2026-01-05`) and cycles through bundled practices.
- Reflection text and completion for the current practice day are loaded at startup and can be saved explicitly.
- A tray is created at startup; the app keeps running when the window is hidden.
- Closing the window hides to tray; process exit is done via tray `Quit`.
