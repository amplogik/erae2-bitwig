# Erae Touch 2 Bitwig Controller Extension

A Bitwig Studio controller extension for the [Embodme Erae Touch 2](https://www.music-group.com/embodme), providing multi-page control with MPE support, DAW control, instrument editing, drum pads, and more.

## Requirements

- Java 21+
- Maven 3.8+
- Bitwig Studio (extension API version 18+)

## Build

```sh
mvn package
```

This produces `target/Erae2Bitwig.bwextension`.

## Install

```sh
mvn install
```

This builds the extension and copies it to `~/Bitwig Studio/Extensions/`.

Alternatively, copy the `.bwextension` file manually:

```sh
cp target/Erae2Bitwig.bwextension ~/Bitwig\ Studio/Extensions/
```

## Setup in Bitwig

1. Open Bitwig Studio
2. Go to **Settings > Controllers**
3. Click **Add Controller**
4. Select **Embodme > Erae Touch 2**
   - If the Erae Touch 2 is connected, it may be auto-detected
5. Assign MIDI ports:
   - **MIDI In 1 / Out 1**: `Erae II MIDI 1` (MPE / note data)
   - **MIDI In 2 / Out 2**: `Erae II MIDI 2` (control / SysEx)

## Pages

The extension provides 8 pages of functionality:

| Page | Description |
|------|-------------|
| MPE Play + XY | MPE keyboard with X/Y/pressure expression and XY+Z pad |
| DAW Control | Transport, clip launcher grid, track levels, navigation |
| Instrument Control | Filters, oscillators, envelopes for the selected device |
| Drum Pads | Standard MIDI velocity drum pads (non-MPE) |
| Mixer Detail | Per-track sends, pan, EQ |
| Step Sequencer | Grid pattern programming |
| XY Performance | Full-surface XY pad for macro/FX control |
| Large Clip Launcher | Maximized clip launcher grid with scroll controls |

## Development

Build and install in one step during development:

```sh
mvn install
```

Bitwig will reload the extension automatically when the `.bwextension` file is updated.

## License

MIT
