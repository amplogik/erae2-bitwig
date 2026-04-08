# Erae Touch 2 Bitwig Controller Extension

A Bitwig Studio controller extension for the [Embodme Erae Touch 2](https://www.embodme.com/), providing five purpose-built control pages: a DAW mixer, a clip launcher, an instrument editor with piano keyboard, a drum-pad performance surface, and a LinnStrument-style MPE playing grid. Three additional layouts on the device are reserved for the user to configure freely in Erae Lab.

> ⚠️ **Linux only.** This extension is developed and tested exclusively on Linux with **PulseAudio + ALSA MIDI**. It is **not** supported on macOS or Windows — the audio/MIDI stack assumptions and the device-discovery paths are Linux-specific. If you try to use this on another platform, you will have a bad time.
>
> **Recommended environment:** Ubuntu Linux 24.04 or later (x86_64), Bitwig Studio 6, PulseAudio, ALSA MIDI.

## Requirements

- **Linux** with PulseAudio and ALSA MIDI (Ubuntu 24.04+ recommended, x86_64)
- **Bitwig Studio 6** (extension API version 18+)
- Java 21+
- Maven 3.8+
- Embodme Erae Touch 2 with Erae Lab installed

## Build & install

```sh
mvn -DskipTests clean install
```

This compiles the extension, packages it as `target/Erae2Bitwig.bwextension`, and copies it to `~/Bitwig Studio/Extensions/`. **Use `mvn install`, not `mvn package`** — the install phase is what deploys the file to Bitwig.

If you'd rather copy manually:

```sh
mvn -DskipTests clean package
cp target/Erae2Bitwig.bwextension ~/Bitwig\ Studio/Extensions/
```

## Erae Lab setup

This extension expects a specific Erae Lab project that configures one full-surface (42×24) **API Zone** on each of the device's first five layouts. The project file is included in this repo as `bitwig.erproj`.

1. Open `bitwig.erproj` in Erae Lab.
2. Push it to your Erae Touch 2.
3. Power-cycle the device to make sure the new configuration is fully active.

The shipped project gives every layout an API Zone with `zoneIndex = layout − 1`, so:

| Layout | Zone ID | Used by |
|---|---|---|
| 1 | 0 | Extension — DAW Control |
| 2 | 1 | Extension — Large Clip Launcher |
| 3 | 2 | Extension — Instrument Control |
| 4 | 3 | Extension — Drum Pads |
| 5 | 4 | Extension — MPE Play |
| 6 | 5 | **User defined** |
| 7 | 6 | **User defined** |
| 8 | 7 | **User defined** |

**Layouts 1–5 are managed by the extension. Do not modify them in Erae Lab** — replacing or reordering the API Zones on those layouts will break the extension's drawing and touch routing.

**Layouts 6, 7, and 8 are yours to do whatever you want with.** The extension does not draw to or react to those zones. You can leave the API Zones in place and add your own controls in Erae Lab, replace them with native Lab zones (MPE pads, drum grids, faders, custom keyboards, etc.), or build something entirely different. They're carved out specifically for user customisation.

## Bitwig Studio setup

1. Open Bitwig Studio.
2. Go to **Settings → Controllers**.
3. Click **Add Controller** and select **Embodme → Erae Touch 2**. (Auto-detect should pick it up if it's connected.)
4. Assign MIDI ports:
   - **MIDI In/Out 1**: `Erae 2 MIDI` (main port — used for the API SysEx and FingerStream touch data)
   - **MIDI In/Out 2**: `Erae 2 MPE` (used for the note inputs the extension creates)
5. The extension creates several **Note Inputs** that appear in Bitwig's Studio I/O panel. Route these to whichever tracks you want them to play:
   - `Erae Keyboard` — the piano keyboard on Layout 3
   - `Erae MPE` — the MPE playing surface on Layout 5 (MPE Lower mode, master channel 1, note channels 2–16, pitch-bend range 48 semitones)
   - `Erae Drum Pads` — the drum pads on Layout 4

## Layout guide

Switch between layouts using the layout selector on the Erae Touch 2 itself (the extension does not control layout switching — it draws to all five managed layouts simultaneously, and your hardware selector decides which one is visible).

---

### Layout 1 — DAW Control

A general-purpose DAW front panel: transport, BPM, time signature, and 10 mixer strips with mute/solo/record-arm.

**Top row** (transport + tempo):
- **Play / Stop / Record** — standard transport. Record toggles arranger record-enable.
- **Auto Write** — toggles arranger automation write.
- **Fill** — toggles fill mode (used by Bitwig Operators).
- **Take** — toggles clip-launcher overdub. When unlit, new clip-launcher recordings go to fresh slots (take recording).
- **BPM drag pad** — drag horizontally to change tempo. Roughly 1 BPM per LED column. Bitwig popup shows the current value.
- **Time signature** — Numerator −, Numerator +, Denominator cycle (2 / 4 / 8 / 16).

**Mixer strips** (under the transport row):
- **8 scrollable track strips** on the left + **fixed FX1 strip** + **fixed Master strip** on the right.
- Each strip is a vertical fader with a VU meter to its right (green / yellow / red).
- Slider background shows the track's color (dim); the filled portion brightens to show the volume position.
- Touch + drag the slider vertically to set volume.
- Below each strip: **Rec Arm** (red), **Solo** (yellow), **Mute** (sky blue) buttons. Tap to toggle.
- **Track scroll** buttons at the bottom move the 8-strip window through the project.

---

### Layout 2 — Large Clip Launcher

Bitwig's clip launcher with 10 scenes × 6 tracks visible at once and modifier-based clip operations.

- **Top row**: 10 scene launch buttons.
- **Clip grid**: 6 tracks × 10 scenes. Each cell shows the clip's color (or a dim track-color tint for empty slots). Playing clips are bright; queued clips are amber; recording is red.
- **Bottom transport row**:
  - **Play / Stop**
  - **Scene scroll** ◄ ►
  - **Track scroll** ▲ ▼
  - **Copy / Paste / Delete / Record** modifiers — hold one and tap a clip to apply that operation.
- Tapping an **empty** clip slot once selects it (outlined). Tapping it again arms the track and launches the slot to record into it.
- Tapping a clip with content launches it.

---

### Layout 3 — Instrument Control

A 2-octave piano keyboard with macro sliders, an XY pad, and full device navigation.

**Top control row:**
- **Octave −, Octave +** — transpose the keyboard by ±12.
- **Page −, Page +** — cycle through the cursor device's macro pages (remote control pages).
- **Device −, Device +** — walk the cursor through devices on the cursor track.
- **Device on/off** — toggle the cursor device's enabled state. Lights green when on, dim green when off.
- **Octave indicator** (6 LEDs, rainbow) — red, orange, yellow, green, cyan, blue represent octaves 1–6. The two LEDs corresponding to the keyboard's currently visible octaves light bright in their octave's color.
- **Track −, Track +** (right-aligned, violet) — jump the cursor to the previous/next track whose type is `Instrument` or `Hybrid` (skipping audio, FX, group, master).

**Slider area:**
- **8 macro sliders**, color-coded (red, orange, yellow, green, cyan, blue, purple, magenta), bound to the 8 remote controls of the cursor device. Touch + vertical drag to set value. Popup shows the value.
- **XY pad** (right side, 8×8): X axis = pitch bend (sprung — returns to 0 on release), Y axis = mod wheel (CC 1, sticky). A bright cursor follows your finger.

**Keyboard** (bottom, full width):
- 14 white keys + 10 black keys = 2 octaves. Default leftmost = **C3 (MIDI 60)**.
- White keys outlined in white, black keys outlined in purple, hollow interior.
- Press → note on with velocity from initial pressure. Hold → channel pressure (0xD) tracks live pressure for aftertouch. Release → note off.
- Pressed keys fill bright (yellow for white, orange for black).
- Polyphonic, single MIDI channel via the `Erae Keyboard` note input. Per-note refcount so two fingers on the same key don't double-trigger.

---

### Layout 4 — Drum Pads

4×4 grid of large pressure-sensitive drum pads bound to the cursor device's `DrumPadBank`.

**Pad grid:**
- 16 pads, each 7 LEDs wide × 4 LEDs tall.
- Pad index 0 (bottom-left) → MIDI note `baseNote + 0`. Pad 15 (top-right) → `baseNote + 15`. Default base = **MIDI 36 (C2)**.
- **Empty pads**: dim grey, no outline.
- **Populated pads**: drum's color (dim fill + brighter outline).
- **Triggered pads** (Bitwig is sequencing them): brighten in their drum color.
- **Soloed pads**: yellow outline.
- **Muted pads**: blue outline.

**Pad behavior:**
- Press → note on, **velocity from initial pressure**.
- Hold → **channel pressure (aftertouch)** tracks live pressure.
- A **pressure ring animation** is drawn centered on your touch — small at light pressure, larger at heavier pressure, clipped to the pad's bounds.
- Per-pad refcount: two fingers on the same pad send only one note-on/off pair.
- Notes are sent via the `Erae Drum Pads` note input.

**Control column** (right side, x=34..40), aligned with the pad rows:
- **Bank Down** (bottom) — scroll the pad bank back by 16 (one full bank).
- **Bank Up** — scroll forward by 16.
- **Solo modifier** — hold and tap a pad to toggle its solo state. Stays brightly lit while held.
- **Mute modifier** (top) — hold and tap a pad to toggle its mute state. Stays brightly lit while held.

---

### Layout 5 — MPE Play

LinnStrument-style isomorphic playing surface with full MPE expression.

**Top control row:**
- **Row offset −, Row offset +** — semitones per row. Default 5 (LinnStrument 4ths tuning). Range 1..12.
- **Octave −, Octave +** — shift the base note by ±12.
- **Scale cycle** — cycle through 12 scales: Chromatic, Major, Natural Minor, Pent Maj, Pent Min, Blues, Dorian, Mixolydian, Lydian, Phrygian, Harm Min, Melo Min.
- **Root cycle** — cycle root note C through B.
- **FX send pad** — pressure-sensitive momentary pad. While held, your finger's pressure (z) drives send slot 0 of the cursor track.

**Playing surface** (20 columns × 9 rows of 2×2 LED cells, 180 cells total):
- **Tonics** (root note in any octave): bright magenta.
- **In-scale notes**: medium cyan.
- **Out-of-scale chromatic fillers**: very dim grey.
- Default base = MIDI 36 (C2 by C3 convention).

**MPE expression per finger:**
- **Velocity** = initial pressure.
- **Pitch bend** = horizontal slide from the initial touch (1 cell = 1 semitone).
- **CC 74 (Timbre)** = vertical slide from the initial touch (1 cell ≈ ±32 around 64).
- **Channel pressure** = live z.
- **Pressure ring** drawn centred on each finger, growing and shrinking with pressure (white, hollow).

**Bitwig routing:** the extension creates a `Erae MPE` note input on the MPE port and configures it for **MPE Lower Mode** with pitch-bend range 48 semitones (master channel 1, note channels 2–16). Route this to any track that supports MPE.

---

### Layouts 6, 7, 8 — User defined

These three layouts are reserved for your own use. The extension does not draw to them, does not react to touches on them, and does not require any specific configuration on them.

You can:
- Leave the API Zones in place and add your own static visuals via Erae Lab.
- Delete the API Zones and replace them with **native Lab zones** — MPE pads, drum grids, sliders, custom keyboards, footswitches — anything Erae Lab supports.
- Mix and match: e.g., a small API zone in one corner alongside native zones.

Whatever you build on these layouts is independent of this extension and won't affect Bitwig unless you wire it up via your own MIDI routing.

## Note inputs created by the extension

After the extension loads, three note inputs appear in Bitwig's Studio I/O panel under your Erae Touch 2 controller:

| Note input | Source layout | Notes |
|---|---|---|
| `Erae Keyboard` | Layout 3 | Single MIDI channel, polyphonic, channel pressure for aftertouch |
| `Erae MPE` | Layout 5 | MPE Lower mode (master ch 1, notes 2–16, pitch bend ±48) |
| `Erae Drum Pads` | Layout 4 | Single MIDI channel, polyphonic, channel pressure for aftertouch |

Route each to the track you want it to play. The Layout 1 mixer and Layout 2 clip launcher control Bitwig directly via the extension API and don't need note input routing.

## Development

Build and install in one step:

```sh
mvn -DskipTests clean install
```

Bitwig will reload the extension automatically when the `.bwextension` file is updated. If your changes don't seem to take effect, double-check that you ran `install` and not just `package`.

A built-in **calibration mode** (set `CALIBRATION_MODE = true` in `Erae2Extension.java`) fills each of the 8 API zones with a distinct color so you can verify the layout-to-zone mapping after editing your Erae Lab project. Use it whenever the device starts behaving oddly.

## Architecture notes

- The extension manages 5 pages, each bound to its own Erae API zone (zone IDs 0–4). All five pages stay drawn to their respective zones simultaneously — the user's hardware layout switch decides which one is visible.
- Touch events from the FingerStream protocol carry their source zone ID, and the `PageManager` routes each touch to the matching page (or drops it for zones 5–7).
- The Erae Touch 2 does not notify the host via MIDI when the user switches layouts on the device, so we cannot react to layout changes — keeping all pages live is the only viable architecture.

## License

MIT
