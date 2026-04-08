package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.core.TouchEvent;
import com.erae2bitwig.core.TouchZone;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Page 3: Instrument Control — keyboard + macro sliders + XY pad.
 *
 * Layout (42×24):
 *   y=23     : top border
 *   y=20..22 : control row (Octave -/+, Page -/+, Device -/+, Device on/off)
 *   y=19     : margin (blank) — buffer against the control row above
 *   y=11..18 : sliders + XY pad (8 tall)
 *     x=1..31  : 8 macro sliders (Group A), 3 wide each with 1-LED gaps
 *     x=32     : gap
 *     x=33..40 : XY pad (X=pitch bend, Y=mod wheel)
 *   y=10     : margin (blank) — buffer between slider area and keyboard
 *   y=1..9   : KEYBOARD (full width x=0..41, no side borders here)
 *     y=7..9   : black-key row (3 tall × 3 wide outlines)
 *     y=1..6   : white-key row (6 tall × 3 wide outlines)
 *   y=0      : bottom border
 *
 * 14 white keys × 3 LEDs = exactly 42 columns, giving 2 octaves of standard
 * piano layout. Default leftmost C = MIDI 60 (middle C / C3). Octave shift
 * transposes the entire keyboard. Polyphonic (single channel), velocity from
 * initial pressure, channel pressure (0xD) sent during touch for aftertouch.
 *
 * Group A sliders bind directly to cursor device's 8 remote controls (macros).
 * Page -/+ navigates the device's macro pages. The XY pad sends pitch bend on
 * X (sprung — returns to center on release) and mod wheel CC1 on Y (sticky).
 */
public class InstrumentPage extends EraePage
{
   // === Layout constants ===
   private static final int CTRL_Y = 20;
   private static final int CTRL_H = 3;
   private static final int BTN_W = 3;
   private static final int BTN_STRIDE = 4;

   private static final int SLIDER_Y = 11;
   private static final int SLIDER_H = 8;
   private static final int GROUP_A_X = 1;
   private static final int SLIDER_W = 3;
   private static final int SLIDER_STRIDE = 4;  // 3 wide + 1 LED gap
   private static final int NUM_MACROS = 8;

   private static final int XY_X = 33;
   private static final int XY_Y = 11;
   private static final int XY_W = 8;
   private static final int XY_H = 8;

   private static final int KB_WHITE_Y = 1;
   private static final int KB_WHITE_H = 6;
   private static final int KB_BLACK_Y = 7;
   private static final int KB_BLACK_H = 3;
   private static final int KB_BOTTOM = 1;
   private static final int KB_TOP = 9;
   private static final int WHITE_W = 3;
   private static final int BLACK_W = 3;
   private static final int NUM_WHITES = 14;
   private static final int OCTAVE_LED_WIDTH = 21; // 7 white keys × 3 LEDs

   private static final int[] WHITE_INTERVALS = {0, 2, 4, 5, 7, 9, 11};

   /** Black key offsets from C in semitones, in playing order (slot in octave). */
   private static final int[] BLACK_SEMITONE = {1, 3, 6, 8, 10};
   /** Black key x-offsets within an octave (LED-relative to that octave's leftmost C). */
   private static final int[] BLACK_X_IN_OCT = {2, 5, 11, 14, 17};

   // === Colors ===
   private static final int[] WHITE_OUTLINE  = {110, 110, 110};
   private static final int[] BLACK_OUTLINE  = {70, 0, 110};
   private static final int[] WHITE_PRESSED  = {127, 110, 0};   // bright yellow
   private static final int[] BLACK_PRESSED  = {127, 60, 0};    // bright orange
   /** Per-slider colors so the 8 macros are visually distinguishable. */
   private static final int[][] SLIDER_BG_COLORS = {
      {40,  0,  0}, // 1 red
      {40, 20,  0}, // 2 orange
      {40, 40,  0}, // 3 yellow
      { 0, 40,  0}, // 4 green
      { 0, 40, 40}, // 5 cyan
      { 0,  0, 40}, // 6 blue
      {20,  0, 40}, // 7 purple
      {40,  0, 40}, // 8 magenta
   };
   private static final int[][] SLIDER_FG_COLORS = {
      {127,   0,   0}, // 1 red
      {127,  60,   0}, // 2 orange
      {127, 110,   0}, // 3 yellow
      {  0, 127,   0}, // 4 green
      {  0, 127, 127}, // 5 cyan
      {  0,  60, 127}, // 6 blue
      { 80,   0, 127}, // 7 purple
      {127,   0, 127}, // 8 magenta
   };
   private static final int[] XY_BG          = {0, 20, 30};
   private static final int[] XY_CURSOR      = {127, 127, 127};

   // === State ===
   private int baseMidi = 60;                       // leftmost white = middle C
   private final int[] noteRefCount = new int[128]; // refcount for poly suppression
   private final Map<Integer, Integer> fingerToNote = new HashMap<>();

   private final TouchZone[] sliderZones = new TouchZone[NUM_MACROS];
   private final double[] sliderValues  = new double[NUM_MACROS];

   private TouchZone xyPadZone;
   private int xyPrevCx = -1;
   private int xyPrevCy = -1;

   private TouchZone octDownZone;
   private TouchZone octUpZone;
   private TouchZone pagePrevZone;
   private TouchZone pageNextZone;
   private TouchZone devicePrevZone;
   private TouchZone deviceNextZone;
   private TouchZone deviceOnOffZone;
   private TouchZone trackPrevZone;
   private TouchZone trackNextZone;

   /** Cache: which absolute track index is an instrument-style track. */
   private final boolean[] isInstrumentTrack = new boolean[128];

   // === Octave indicator (6-LED horizontal bar in the control row gap) ===
   private static final int OCT_IND_X = 28;
   private static final int OCT_IND_W = 6;
   /** LED 0 of the indicator represents this octave number (C3 convention). */
   private static final int OCT_IND_FIRST_OCTAVE = 1;

   /** Bright per-octave colors for the indicator (octaves 1..6, rainbow). */
   private static final int[][] OCT_COLORS_BRIGHT = {
      {127,   0,   0}, // 1 red
      {127,  60,   0}, // 2 orange
      {127, 110,   0}, // 3 yellow
      {  0, 127,   0}, // 4 green
      {  0, 127, 127}, // 5 cyan
      {  0,  60, 127}, // 6 blue
   };
   private static final int[] OCT_COLOR_INACTIVE = {6, 6, 6};

   private final Erae2MidiPorts midiPorts;
   private NoteInput keyboardNoteInput;

   private static final String[] NOTE_NAMES = {
      "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
   };

   public InstrumentPage(final ControllerHost host,
                         final LowLevelApi api,
                         final BitwigModel model,
                         final Erae2MidiPorts midiPorts)
   {
      super(PageId.INSTRUMENT_CONTROL, host, api, model);
      this.midiPorts = midiPorts;
   }

   @Override
   public void setupBindings()
   {
      keyboardNoteInput = midiPorts.getMpeIn().createNoteInput("Erae Keyboard");
      // Standard polyphonic mode — no MPE setup.

      setupControlRow();
      setupSliders();
      setupXYPad();
      // Keyboard touches are routed by handleTouchEvent override (no per-key TouchZones).
   }

   // ============================================================
   // Setup
   // ============================================================

   private void setupControlRow()
   {
      final CursorRemoteControlsPage rc = model.getRemoteControls();
      final PinnableCursorDevice dev = model.getCursorDevice();

      octDownZone = controlButton("Oct-", BTN_X(0), 60, 0, 80, e ->
      {
         if (baseMidi >= 12)
         {
            baseMidi -= 12;
            host.showPopupNotification("Keyboard base: " + noteName(baseMidi));
            redrawKeyboard();
            drawOctaveIndicator();
         }
      });
      octUpZone = controlButton("Oct+", BTN_X(1), 100, 0, 127, e ->
      {
         if (baseMidi <= 96)
         {
            baseMidi += 12;
            host.showPopupNotification("Keyboard base: " + noteName(baseMidi));
            redrawKeyboard();
            drawOctaveIndicator();
         }
      });

      pagePrevZone = controlButton("Page-", BTN_X(2), 0, 60, 80, e ->
      {
         rc.selectPrevious();
      });
      pageNextZone = controlButton("Page+", BTN_X(3), 0, 100, 127, e ->
      {
         rc.selectNext();
      });

      devicePrevZone = controlButton("Dev-", BTN_X(4), 60, 60, 0, e ->
      {
         dev.selectPrevious();
      });
      deviceNextZone = controlButton("Dev+", BTN_X(5), 100, 100, 0, e ->
      {
         dev.selectNext();
      });

      deviceOnOffZone = controlButton("DevOn", BTN_X(6), 0, 100, 0, e ->
      {
         dev.isEnabled().toggle();
      });

      dev.isEnabled().addValueObserver(enabled ->
      {
         if (!isActive) return;
         deviceOnOffZone.setColor(0, enabled ? 110 : 30, 0);
         redrawZone(deviceOnOffZone);
      });

      // Right-aligned: prev/next instrument track. Track + sits just inside the
      // right border (x=38..40), Track − is one stride to its left (x=34..36).
      trackPrevZone = controlButton("Trk-", 34, 60, 30, 80, e -> jumpToInstrumentTrack(-1));
      trackNextZone = controlButton("Trk+", 38, 100, 60, 127, e -> jumpToInstrumentTrack(+1));

      // Subscribe to each instrument-nav-bank slot's trackType to maintain
      // the isInstrumentTrack cache. Updates fire whenever Bitwig populates
      // or rearranges the bank.
      final TrackBank navBank = model.getInstrumentNavBank();
      final int navSize = model.getInstrumentNavBankSize();
      for (int i = 0; i < navSize; i++)
      {
         final int idx = i;
         navBank.getItemAt(i).trackType().addValueObserver(type ->
         {
            isInstrumentTrack[idx] = "Instrument".equals(type) || "Hybrid".equals(type);
         });
      }
   }

   private void jumpToInstrumentTrack(final int direction)
   {
      final TrackBank navBank = model.getInstrumentNavBank();
      final int navSize = model.getInstrumentNavBankSize();
      final int currentPos = model.getCursorTrack().position().get();

      int target = -1;
      if (direction > 0)
      {
         for (int i = currentPos + 1; i < navSize; i++)
         {
            if (isInstrumentTrack[i]) { target = i; break; }
         }
      }
      else
      {
         for (int i = Math.min(currentPos - 1, navSize - 1); i >= 0; i--)
         {
            if (isInstrumentTrack[i]) { target = i; break; }
         }
      }

      if (target >= 0)
      {
         final Track t = navBank.getItemAt(target);
         t.selectInMixer();
         host.showPopupNotification("Instrument track " + (target + 1));
      }
      else
      {
         host.showPopupNotification(direction > 0
            ? "No more instrument tracks"
            : "No previous instrument tracks");
      }
   }

   private static int BTN_X(final int idx) { return 1 + idx * BTN_STRIDE; }

   private TouchZone controlButton(final String name, final int x,
                                   final int r, final int g, final int b,
                                   final java.util.function.Consumer<TouchEvent> onPress)
   {
      final TouchZone z = new TouchZone(name, x, CTRL_Y, BTN_W, CTRL_H);
      z.setColor(r, g, b);
      z.onPress(onPress);
      zones.add(z);
      return z;
   }

   private void setupSliders()
   {
      final CursorRemoteControlsPage rc = model.getRemoteControls();
      for (int i = 0; i < NUM_MACROS; i++)
      {
         final int idx = i;
         final int sx = GROUP_A_X + i * SLIDER_STRIDE;
         final TouchZone z = new TouchZone("Macro" + i, sx, SLIDER_Y, SLIDER_W, SLIDER_H);
         z.setColor(0, 0, 0);

         final Parameter param = rc.getParameter(i);
         param.value().addValueObserver(v ->
         {
            sliderValues[idx] = v;
            drawSlider(idx);
         });

         z.onPress(e ->
         {
            final double v = clamp01((e.getY() - SLIDER_Y) / (double) SLIDER_H);
            param.set(v);
            // Update the cache + redraw immediately so the visual responds
            // even if Bitwig hasn't fired the value observer yet (which can
            // happen for macros that haven't been touched in this session).
            sliderValues[idx] = v;
            drawSlider(idx);
            host.showPopupNotification("Macro " + (idx + 1) + ": " + String.format("%.2f", v));
         });
         z.onMove(e ->
         {
            final double v = clamp01((e.getY() - SLIDER_Y) / (double) SLIDER_H);
            param.set(v);
            sliderValues[idx] = v;
            drawSlider(idx);
         });

         sliderZones[idx] = z;
         zones.add(z);
      }
   }

   private void setupXYPad()
   {
      xyPadZone = new TouchZone("XYPad", XY_X, XY_Y, XY_W, XY_H);
      xyPadZone.setColor(0, 0, 0);
      xyPadZone.onPress(e -> handleXYTouch(e));
      xyPadZone.onMove(e -> handleXYTouch(e));
      xyPadZone.onRelease(e ->
      {
         eraseXYCursor();
         sendPitchBend(8192); // sprung return to center
         // Mod wheel stays at last value (sticky)
      });
      zones.add(xyPadZone);
   }

   // ============================================================
   // Touch event routing (override to handle keyboard area)
   // ============================================================

   @Override
   public void handleTouchEvent(final TouchEvent event)
   {
      final float y = event.getY();
      if (y >= KB_BOTTOM && y < KB_TOP + 1)
      {
         handleKeyboardTouch(event);
         return;
      }
      super.handleTouchEvent(event);
   }

   // ============================================================
   // Keyboard handling
   // ============================================================

   private void handleKeyboardTouch(final TouchEvent event)
   {
      final int finger = event.getFingerIndex();
      final float fx = event.getX();
      final float fy = event.getY();
      final float fz = event.getZ();

      if (event.isPress())
      {
         final int note = noteForTouch(fx, fy);
         if (note < 0 || note > 127) return;
         final int velocity = Math.max(1, Math.min(127, (int) (fz * 127)));
         pressNote(note, velocity);
         fingerToNote.put(finger, note);
      }
      else if (event.isMove())
      {
         // Channel pressure tracks the latest finger's pressure.
         sendChannelPressure(Math.max(0, Math.min(127, (int) (fz * 127))));
      }
      else if (event.isRelease())
      {
         final Integer note = fingerToNote.remove(finger);
         if (note != null) releaseNote(note);
      }
   }

   /** Map a touch (x, y) within the keyboard area to a MIDI note. */
   private int noteForTouch(final float fx, final float fy)
   {
      final int x = (int) fx;
      final int y = (int) fy;
      if (x < 0 || x >= 42) return -1;

      final int octave = x / OCTAVE_LED_WIDTH;
      final int xInOct = x % OCTAVE_LED_WIDTH;
      final int baseInOct = baseMidi + octave * 12;

      // If the touch is in the black-key row, check for a black key first.
      if (y >= KB_BLACK_Y && y <= KB_BLACK_Y + KB_BLACK_H - 1)
      {
         final int blackOff = blackOffsetInOctave(xInOct);
         if (blackOff >= 0) return baseInOct + blackOff;
         // No black at this column → fall through to the white key beneath.
      }

      // White key — column / 3 picks the white-key index in the octave.
      final int wIdxInOct = xInOct / WHITE_W;
      if (wIdxInOct < 0 || wIdxInOct > 6) return -1;
      return baseInOct + WHITE_INTERVALS[wIdxInOct];
   }

   /** Returns the semitone offset of a black key at this column, or -1. */
   private static int blackOffsetInOctave(final int xInOct)
   {
      // Black key column ranges (each black is 3 wide centered at a white-white boundary).
      if (xInOct >= 2  && xInOct <= 4)  return 1;  // C#
      if (xInOct >= 5  && xInOct <= 7)  return 3;  // D#
      if (xInOct >= 11 && xInOct <= 13) return 6;  // F#
      if (xInOct >= 14 && xInOct <= 16) return 8;  // G#
      if (xInOct >= 17 && xInOct <= 19) return 10; // A#
      return -1;
   }

   private void pressNote(final int note, final int velocity)
   {
      noteRefCount[note]++;
      if (noteRefCount[note] == 1)
      {
         keyboardNoteInput.sendRawMidiEvent(0x90, note, velocity);
         drawKeyForNote(note, true);
      }
   }

   private void releaseNote(final int note)
   {
      if (noteRefCount[note] <= 0) return;
      noteRefCount[note]--;
      if (noteRefCount[note] == 0)
      {
         keyboardNoteInput.sendRawMidiEvent(0x80, note, 0);
         drawKeyForNote(note, false);
      }
   }

   // ============================================================
   // MIDI sending helpers
   // ============================================================

   private void sendChannelPressure(final int value)
   {
      keyboardNoteInput.sendRawMidiEvent(0xD0, value & 0x7F, 0);
   }

   private void sendPitchBend(final int bendValue)
   {
      final int v = Math.max(0, Math.min(16383, bendValue));
      keyboardNoteInput.sendRawMidiEvent(0xE0, v & 0x7F, (v >> 7) & 0x7F);
   }

   private void sendModWheel(final int value)
   {
      keyboardNoteInput.sendRawMidiEvent(0xB0, 1, value & 0x7F);
   }

   // ============================================================
   // Drawing — onActivate, full redraw
   // ============================================================

   @Override
   public void onActivate()
   {
      super.onActivate();
      drawXYBackground();
      for (int i = 0; i < NUM_MACROS; i++) drawSlider(i);
      redrawKeyboard();
      drawOctaveIndicator();
   }

   /**
    * Draw the 6-LED octave indicator bar in the control-row gap (x=28..33).
    * The two octaves currently covered by the keyboard light up bright in
    * their octave's color; all others are very dim.
    */
   private void drawOctaveIndicator()
   {
      if (!isActive) return;
      final int z = pageId.getZoneId();
      // C3 convention: MIDI 60 → octave 3, so octave = (midi/12) - 2.
      final int lowerOct = (baseMidi / 12) - 2;
      final int upperOct = lowerOct + 1;

      for (int led = 0; led < OCT_IND_W; led++)
      {
         final int octave = OCT_IND_FIRST_OCTAVE + led;
         final boolean active = (octave == lowerOct || octave == upperOct);
         final int[] c = active ? OCT_COLORS_BRIGHT[led] : OCT_COLOR_INACTIVE;
         api.drawRectangle(z, OCT_IND_X + led, CTRL_Y, 1, CTRL_H, c[0], c[1], c[2]);
      }
   }

   /** Draw all 14 white outlines and 10 black outlines from scratch. */
   private void redrawKeyboard()
   {
      if (!isActive) return;
      final int z = pageId.getZoneId();
      // Clear the entire keyboard area first.
      api.drawRectangle(z, 0, KB_WHITE_Y, 42, KB_WHITE_H + KB_BLACK_H, 0, 0, 0);
      // Whites
      for (int w = 0; w < NUM_WHITES; w++)
      {
         drawWhiteOutlineAt(w * WHITE_W);
      }
      // Blacks: each octave has 5 blacks; we have 2 octaves.
      for (int oct = 0; oct < 2; oct++)
      {
         final int xBase = oct * OCTAVE_LED_WIDTH;
         for (final int blackXOff : BLACK_X_IN_OCT)
         {
            final int bx = xBase + blackXOff;
            if (bx + BLACK_W > 42) continue;
            drawBlackOutlineAt(bx);
         }
      }
   }

   /** Draw a single key based on its MIDI note (used on press / release). */
   private void drawKeyForNote(final int midi, final boolean pressed)
   {
      if (!isActive) return;

      // Find which visible key (if any) this note corresponds to.
      final int relMidi = midi - baseMidi;
      if (relMidi < 0 || relMidi >= 24) return; // outside the visible 2-octave range
      final int octave = relMidi / 12;
      final int semitone = relMidi % 12;

      // White?
      for (int w = 0; w < 7; w++)
      {
         if (WHITE_INTERVALS[w] == semitone)
         {
            final int absoluteWhiteIdx = octave * 7 + w;
            if (absoluteWhiteIdx < 0 || absoluteWhiteIdx >= NUM_WHITES) return;
            final int x = absoluteWhiteIdx * WHITE_W;
            if (pressed)
            {
               fillWhiteAt(x);
            }
            else
            {
               clearWhiteAt(x);
               drawWhiteOutlineAt(x);
            }
            return;
         }
      }
      // Black?
      for (int i = 0; i < BLACK_SEMITONE.length; i++)
      {
         if (BLACK_SEMITONE[i] == semitone)
         {
            final int x = octave * OCTAVE_LED_WIDTH + BLACK_X_IN_OCT[i];
            if (x + BLACK_W > 42) return;
            if (pressed)
            {
               fillBlackAt(x);
            }
            else
            {
               clearBlackAt(x);
               drawBlackOutlineAt(x);
            }
            return;
         }
      }
   }

   private void drawWhiteOutlineAt(final int x)
   {
      final int z = pageId.getZoneId();
      // Top edge
      api.drawRectangle(z, x, KB_WHITE_Y + KB_WHITE_H - 1, WHITE_W, 1,
         WHITE_OUTLINE[0], WHITE_OUTLINE[1], WHITE_OUTLINE[2]);
      // Bottom edge
      api.drawRectangle(z, x, KB_WHITE_Y, WHITE_W, 1,
         WHITE_OUTLINE[0], WHITE_OUTLINE[1], WHITE_OUTLINE[2]);
      // Left col
      api.drawRectangle(z, x, KB_WHITE_Y, 1, KB_WHITE_H,
         WHITE_OUTLINE[0], WHITE_OUTLINE[1], WHITE_OUTLINE[2]);
      // Right col
      api.drawRectangle(z, x + WHITE_W - 1, KB_WHITE_Y, 1, KB_WHITE_H,
         WHITE_OUTLINE[0], WHITE_OUTLINE[1], WHITE_OUTLINE[2]);
   }

   private void clearWhiteAt(final int x)
   {
      api.drawRectangle(pageId.getZoneId(), x, KB_WHITE_Y, WHITE_W, KB_WHITE_H, 0, 0, 0);
   }

   private void fillWhiteAt(final int x)
   {
      api.drawRectangle(pageId.getZoneId(), x, KB_WHITE_Y, WHITE_W, KB_WHITE_H,
         WHITE_PRESSED[0], WHITE_PRESSED[1], WHITE_PRESSED[2]);
   }

   private void drawBlackOutlineAt(final int x)
   {
      final int z = pageId.getZoneId();
      // Top
      api.drawRectangle(z, x, KB_BLACK_Y + KB_BLACK_H - 1, BLACK_W, 1,
         BLACK_OUTLINE[0], BLACK_OUTLINE[1], BLACK_OUTLINE[2]);
      // Bottom
      api.drawRectangle(z, x, KB_BLACK_Y, BLACK_W, 1,
         BLACK_OUTLINE[0], BLACK_OUTLINE[1], BLACK_OUTLINE[2]);
      // Left
      api.drawRectangle(z, x, KB_BLACK_Y, 1, KB_BLACK_H,
         BLACK_OUTLINE[0], BLACK_OUTLINE[1], BLACK_OUTLINE[2]);
      // Right
      api.drawRectangle(z, x + BLACK_W - 1, KB_BLACK_Y, 1, KB_BLACK_H,
         BLACK_OUTLINE[0], BLACK_OUTLINE[1], BLACK_OUTLINE[2]);
   }

   private void clearBlackAt(final int x)
   {
      api.drawRectangle(pageId.getZoneId(), x, KB_BLACK_Y, BLACK_W, KB_BLACK_H, 0, 0, 0);
   }

   private void fillBlackAt(final int x)
   {
      api.drawRectangle(pageId.getZoneId(), x, KB_BLACK_Y, BLACK_W, KB_BLACK_H,
         BLACK_PRESSED[0], BLACK_PRESSED[1], BLACK_PRESSED[2]);
   }

   // ============================================================
   // Sliders — drawing
   // ============================================================

   private void drawSlider(final int idx)
   {
      if (!isActive) return;
      final int z = pageId.getZoneId();
      final int x = GROUP_A_X + idx * SLIDER_STRIDE;
      final int[] bg = SLIDER_BG_COLORS[idx];
      final int[] fg = SLIDER_FG_COLORS[idx];
      api.drawRectangle(z, x, SLIDER_Y, SLIDER_W, SLIDER_H, bg[0], bg[1], bg[2]);
      final int fillH = (int) Math.round(sliderValues[idx] * SLIDER_H);
      if (fillH > 0)
      {
         api.drawRectangle(z, x, SLIDER_Y, SLIDER_W, fillH, fg[0], fg[1], fg[2]);
      }
   }

   // ============================================================
   // XY pad — drawing
   // ============================================================

   private void drawXYBackground()
   {
      if (!isActive) return;
      api.drawRectangle(pageId.getZoneId(), XY_X, XY_Y, XY_W, XY_H,
         XY_BG[0], XY_BG[1], XY_BG[2]);
      xyPrevCx = -1;
      xyPrevCy = -1;
   }

   private void handleXYTouch(final TouchEvent e)
   {
      final float fx = e.getX();
      final float fy = e.getY();

      double nx = (fx - XY_X) / (double) XY_W;
      double ny = (fy - XY_Y) / (double) XY_H;
      nx = clamp01(nx);
      ny = clamp01(ny);

      // Pitch bend: center at 0.5 → 8192, ends at 0/16383
      final int bend = 8192 + (int) ((nx - 0.5) * 16383);
      sendPitchBend(bend);

      // Mod wheel: 0..127
      final int mod = (int) (ny * 127);
      sendModWheel(mod);

      updateXYCursor((int) fx, (int) fy);
   }

   private void updateXYCursor(final int cx, final int cy)
   {
      if (!isActive) return;
      // Erase previous cursor
      if (xyPrevCx >= 0)
      {
         api.drawRectangle(pageId.getZoneId(), xyPrevCx, xyPrevCy, 2, 2,
            XY_BG[0], XY_BG[1], XY_BG[2]);
      }
      // Clamp so the 2×2 cursor stays inside the pad
      int x = cx;
      int y = cy;
      if (x < XY_X) x = XY_X;
      if (x > XY_X + XY_W - 2) x = XY_X + XY_W - 2;
      if (y < XY_Y) y = XY_Y;
      if (y > XY_Y + XY_H - 2) y = XY_Y + XY_H - 2;
      api.drawRectangle(pageId.getZoneId(), x, y, 2, 2,
         XY_CURSOR[0], XY_CURSOR[1], XY_CURSOR[2]);
      xyPrevCx = x;
      xyPrevCy = y;
   }

   private void eraseXYCursor()
   {
      if (!isActive) return;
      if (xyPrevCx < 0) return;
      api.drawRectangle(pageId.getZoneId(), xyPrevCx, xyPrevCy, 2, 2,
         XY_BG[0], XY_BG[1], XY_BG[2]);
      xyPrevCx = -1;
      xyPrevCy = -1;
   }

   // ============================================================
   // Helpers
   // ============================================================

   private static double clamp01(final double v)
   {
      if (v < 0) return 0;
      if (v > 1) return 1;
      return v;
   }

   private static String noteName(final int midi)
   {
      final int n = midi % 12;
      final int oct = midi / 12 - 1;
      return NOTE_NAMES[n] + oct;
   }
}
