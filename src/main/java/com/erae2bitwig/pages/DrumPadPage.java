package com.erae2bitwig.pages;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.core.TouchEvent;
import com.erae2bitwig.core.TouchZone;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Page 4: Drum Pads — 4×4 large pressure pads bound to the cursor device's
 * DrumPadBank, with a 4-button control column on the right.
 *
 * Layout (42×24):
 *   y=23    : top border
 *   Pad rows (bottom to top, each 4 tall, 2-LED gap between):
 *     y=1..4   : pads 0..3   (lowest notes)
 *     y=7..10  : pads 4..7
 *     y=13..16 : pads 8..11
 *     y=19..22 : pads 12..15 (highest notes)
 *   y=0     : bottom border
 *
 *   x=0     : left border
 *   Pad cols (left to right, each 7 wide, 1-LED gap between):
 *     x=1..7   : col 0
 *     x=9..15  : col 1
 *     x=17..23 : col 2
 *     x=25..31 : col 3
 *   x=32..33: divider gap
 *   Control column at x=34..40 (7 wide), 4 buttons aligned with the pad rows:
 *     y=1..4   : Bank Down (page back through the kit)
 *     y=7..10  : Bank Up
 *     y=13..16 : Solo modifier
 *     y=19..22 : Mute modifier
 *   x=41    : right border
 *
 * Pad indexing:
 *   pad index = row * 4 + col   (0 = bottom-left, 15 = top-right)
 *   MIDI note = baseNote + pad index   (default base = 36 = C2)
 *
 * Touch handling:
 *   - Press → noteOn (velocity from initial pressure), draw white ring
 *     centred on the touch with diameter scaled by pressure (clipped to the
 *     pad's bounds so the ring stays inside the pad)
 *   - Move → channel pressure (live z), redraw ring with new diameter
 *   - Release → noteOff, redraw the pad to clear the ring
 *
 * Modifiers (Mute / Solo) — held while tapping a pad → toggles that pad's
 * mute / solo state instead of triggering the note. Modifier buttons stay
 * brightly lit while held.
 *
 * Triggered animation: cursor track's playingNotes() observer fires when
 * Bitwig is sequencing notes that match the visible pads. Triggered pads
 * brighten in their drum colour and revert when the note stops.
 */
public class DrumPadPage extends EraePage
{
   // === Layout ===
   private static final int PAD_W = 7;
   private static final int PAD_H = 4;
   private static final int NUM_COLS = 4;
   private static final int NUM_ROWS = 4;
   private static final int NUM_PADS = NUM_COLS * NUM_ROWS;
   private static final int PAD_X_START = 1;
   private static final int PAD_X_STRIDE = 8;  // 7 wide + 1 LED gap
   private static final int PAD_Y_START = 1;
   private static final int PAD_Y_STRIDE = 6;  // 4 tall + 2 LED gap

   private static final int CTRL_X = 34;
   private static final int CTRL_W = 7;

   // === MIDI ===
   private static final int DEFAULT_BASE_NOTE = 36; // C2 — Bitwig drum machine default

   // === Modifier button colours ===
   private static final int[] MUTE_DIM    = {15, 30, 50};
   private static final int[] MUTE_BRIGHT = {60, 110, 127};
   private static final int[] SOLO_DIM    = {35, 30, 0};
   private static final int[] SOLO_BRIGHT = {127, 110, 0};
   private static final int[] BANK_COLOR  = {0, 60, 90};

   // === Ring colour ===
   private static final int[] RING_COLOR  = {127, 127, 127};

   // === State ===
   private final Erae2MidiPorts midiPorts;
   private NoteInput drumNoteInput;
   private DrumPadBank drumPadBank;

   private int baseNote = DEFAULT_BASE_NOTE;
   private final int[] padPressCount = new int[NUM_PADS];
   private final boolean[] padTriggered = new boolean[NUM_PADS];
   private final Set<Integer> playingNotes = new HashSet<>();

   private boolean muteModifier = false;
   private boolean soloModifier = false;

   private final Map<Integer, FingerState> fingers = new HashMap<>();

   private TouchZone bankDownZone;
   private TouchZone bankUpZone;
   private TouchZone soloModZone;
   private TouchZone muteModZone;

   public DrumPadPage(final ControllerHost host,
                      final LowLevelApi api,
                      final BitwigModel model,
                      final Erae2MidiPorts midiPorts)
   {
      super(PageId.DRUM_PADS, host, api, model);
      this.midiPorts = midiPorts;
   }

   @Override
   public void setupBindings()
   {
      drumNoteInput = midiPorts.getMpeIn().createNoteInput("Erae Drum Pads");
      // Standard polyphonic, no MPE setup.

      drumPadBank = model.getCursorDrumPadBank();

      setupControlColumn();
      setupPadObservers();
      setupPlayingNotesObserver();
   }

   // ============================================================
   // Control column
   // ============================================================

   private void setupControlColumn()
   {
      // Bank Down at the bottom (y=1..4, aligned with pad row 0)
      bankDownZone = new TouchZone("BankDown", CTRL_X, padRowY(0), CTRL_W, PAD_H);
      bankDownZone.setColor(BANK_COLOR[0], BANK_COLOR[1], BANK_COLOR[2]);
      bankDownZone.onPress(e ->
      {
         if (baseNote >= NUM_PADS)
         {
            baseNote -= NUM_PADS;
            drumPadBank.scrollPageBackwards();
            host.showPopupNotification("Drum bank base: " + baseNote);
            // Pad observers will repaint as colours update; force an immediate
            // repaint too in case the bank state hasn't propagated yet.
            redrawAllPads();
         }
      });
      zones.add(bankDownZone);

      // Bank Up (row 1)
      bankUpZone = new TouchZone("BankUp", CTRL_X, padRowY(1), CTRL_W, PAD_H);
      bankUpZone.setColor(BANK_COLOR[0], BANK_COLOR[1], BANK_COLOR[2]);
      bankUpZone.onPress(e ->
      {
         if (baseNote + NUM_PADS + (NUM_PADS - 1) <= 127)
         {
            baseNote += NUM_PADS;
            drumPadBank.scrollPageForwards();
            host.showPopupNotification("Drum bank base: " + baseNote);
            redrawAllPads();
         }
      });
      zones.add(bankUpZone);

      // Solo modifier (row 2)
      soloModZone = new TouchZone("SoloMod", CTRL_X, padRowY(2), CTRL_W, PAD_H);
      soloModZone.setColor(SOLO_DIM[0], SOLO_DIM[1], SOLO_DIM[2]);
      soloModZone.onPress(e ->
      {
         soloModifier = true;
         soloModZone.setColor(SOLO_BRIGHT[0], SOLO_BRIGHT[1], SOLO_BRIGHT[2]);
         redrawZone(soloModZone);
      });
      soloModZone.onRelease(e ->
      {
         soloModifier = false;
         soloModZone.setColor(SOLO_DIM[0], SOLO_DIM[1], SOLO_DIM[2]);
         redrawZone(soloModZone);
      });
      zones.add(soloModZone);

      // Mute modifier (row 3, top)
      muteModZone = new TouchZone("MuteMod", CTRL_X, padRowY(3), CTRL_W, PAD_H);
      muteModZone.setColor(MUTE_DIM[0], MUTE_DIM[1], MUTE_DIM[2]);
      muteModZone.onPress(e ->
      {
         muteModifier = true;
         muteModZone.setColor(MUTE_BRIGHT[0], MUTE_BRIGHT[1], MUTE_BRIGHT[2]);
         redrawZone(muteModZone);
      });
      muteModZone.onRelease(e ->
      {
         muteModifier = false;
         muteModZone.setColor(MUTE_DIM[0], MUTE_DIM[1], MUTE_DIM[2]);
         redrawZone(muteModZone);
      });
      zones.add(muteModZone);
   }

   // ============================================================
   // Pad observers
   // ============================================================

   private void setupPadObservers()
   {
      for (int i = 0; i < NUM_PADS; i++)
      {
         final int idx = i;
         final DrumPad pad = drumPadBank.getItemAt(i);
         pad.color().addValueObserver((r, g, b) -> drawPad(idx));
         pad.exists().addValueObserver(e -> drawPad(idx));
         pad.mute().addValueObserver(m -> drawPad(idx));
         pad.solo().addValueObserver(s -> drawPad(idx));
      }
   }

   // ============================================================
   // Playing notes observer (drives triggered animation)
   // ============================================================

   private void setupPlayingNotesObserver()
   {
      model.getCursorTrack().playingNotes().addValueObserver((PlayingNote[] notes) ->
      {
         final Set<Integer> incoming = new HashSet<>();
         for (final PlayingNote n : notes)
         {
            incoming.add(n.pitch());
         }

         // Notes that just started
         for (final Integer p : incoming)
         {
            if (!playingNotes.contains(p))
            {
               final int padIdx = p - baseNote;
               if (padIdx >= 0 && padIdx < NUM_PADS)
               {
                  padTriggered[padIdx] = true;
                  drawPad(padIdx);
               }
            }
         }
         // Notes that just stopped
         for (final Integer p : playingNotes)
         {
            if (!incoming.contains(p))
            {
               final int padIdx = p - baseNote;
               if (padIdx >= 0 && padIdx < NUM_PADS)
               {
                  padTriggered[padIdx] = false;
                  drawPad(padIdx);
               }
            }
         }

         playingNotes.clear();
         playingNotes.addAll(incoming);
      });
   }

   // ============================================================
   // Touch routing
   // ============================================================

   @Override
   public void handleTouchEvent(final TouchEvent event)
   {
      final int col = padColForX(event.getX());
      final int row = padRowForY(event.getY());
      if (col >= 0 && row >= 0)
      {
         handlePadTouch(event, col, row);
         return;
      }
      super.handleTouchEvent(event);
   }

   private static int padColForX(final float fx)
   {
      final int x = (int) fx;
      if (x < PAD_X_START) return -1;
      final int rel = x - PAD_X_START;
      final int col = rel / PAD_X_STRIDE;
      if (col < 0 || col >= NUM_COLS) return -1;
      final int withinCol = rel % PAD_X_STRIDE;
      if (withinCol >= PAD_W) return -1; // in the inter-pad gap
      return col;
   }

   private static int padRowForY(final float fy)
   {
      final int y = (int) fy;
      if (y < PAD_Y_START) return -1;
      final int rel = y - PAD_Y_START;
      final int row = rel / PAD_Y_STRIDE;
      if (row < 0 || row >= NUM_ROWS) return -1;
      final int withinRow = rel % PAD_Y_STRIDE;
      if (withinRow >= PAD_H) return -1; // in the inter-pad gap
      return row;
   }

   // ============================================================
   // Pad press / release
   // ============================================================

   private void handlePadTouch(final TouchEvent event, final int col, final int row)
   {
      final int finger = event.getFingerIndex();
      final int padIdx = row * NUM_COLS + col;
      final float fz = event.getZ();

      if (event.isPress())
      {
         // Modifier actions intercept the press: no note triggered.
         if (muteModifier)
         {
            drumPadBank.getItemAt(padIdx).mute().toggle();
            return;
         }
         if (soloModifier)
         {
            drumPadBank.getItemAt(padIdx).solo().toggle();
            return;
         }

         final int note = baseNote + padIdx;
         if (note < 0 || note > 127) return;

         final int velocity = Math.max(1, Math.min(127, (int) (fz * 127)));
         pressNote(padIdx, note, velocity);

         final FingerState s = new FingerState();
         s.padCol = col;
         s.padRow = row;
         s.padIdx = padIdx;
         s.note = note;
         s.touchX = event.getX();
         s.touchY = event.getY();
         s.touchZ = fz;
         fingers.put(finger, s);

         drawRingForFinger(s);
      }
      else if (event.isMove())
      {
         final FingerState s = fingers.get(finger);
         if (s == null) return;

         // Channel pressure tracks live z (last finger's pressure dominates)
         sendChannelPressure(Math.max(0, Math.min(127, (int) (fz * 127))));

         s.touchX = event.getX();
         s.touchY = event.getY();
         s.touchZ = fz;
         drawRingForFinger(s);
      }
      else if (event.isRelease())
      {
         final FingerState s = fingers.remove(finger);
         if (s == null) return;
         releaseNote(s.padIdx, s.note);
         // Redraw the pad to clear any ring drawn inside it.
         drawPad(s.padIdx);
      }
   }

   private void pressNote(final int padIdx, final int note, final int velocity)
   {
      padPressCount[padIdx]++;
      if (padPressCount[padIdx] == 1)
      {
         drumNoteInput.sendRawMidiEvent(0x90, note, velocity);
      }
   }

   private void releaseNote(final int padIdx, final int note)
   {
      if (padPressCount[padIdx] <= 0) return;
      padPressCount[padIdx]--;
      if (padPressCount[padIdx] == 0)
      {
         drumNoteInput.sendRawMidiEvent(0x80, note, 0);
      }
   }

   private void sendChannelPressure(final int value)
   {
      drumNoteInput.sendRawMidiEvent(0xD0, value & 0x7F, 0);
   }

   // ============================================================
   // Pad drawing
   // ============================================================

   @Override
   public void onActivate()
   {
      super.onActivate();
      redrawAllPads();
   }

   private void redrawAllPads()
   {
      if (!isActive) return;
      for (int i = 0; i < NUM_PADS; i++)
      {
         drawPad(i);
      }
   }

   private void drawPad(final int padIdx)
   {
      if (!isActive) return;
      if (padIdx < 0 || padIdx >= NUM_PADS) return;
      final int col = padIdx % NUM_COLS;
      final int row = padIdx / NUM_COLS;
      final int x = padColX(col);
      final int y = padRowY(row);
      final int z = pageId.getZoneId();

      final DrumPad pad = drumPadBank.getItemAt(padIdx);

      if (!pad.exists().get())
      {
         // Empty slot — solid dim grey, no outline.
         api.drawRectangle(z, x, y, PAD_W, PAD_H, 5, 5, 5);
         return;
      }

      // Populated pad: dimmer fill + outline whose colour reflects state.
      //   solo  → yellow outline (takes precedence over mute, since solo
      //           overrides mute in Bitwig's playback)
      //   mute  → blue outline
      //   else  → brighter version of the pad's drum colour
      final Color c = pad.color().get();
      final boolean isSolo      = pad.solo().get();
      final boolean isMute      = pad.mute().get();
      final boolean isTriggered = padTriggered[padIdx];

      final double fillScale;
      final int or, og, ob;
      if (isSolo)
      {
         or = 127; og = 110; ob = 0;            // yellow
         fillScale = isTriggered ? 80 : 35;
      }
      else if (isMute)
      {
         or = 60;  og = 110; ob = 127;          // sky blue
         fillScale = 12;
      }
      else
      {
         final double outlineScale = isTriggered ? 127 : 110;
         or = clamp7(c.getRed()   * outlineScale);
         og = clamp7(c.getGreen() * outlineScale);
         ob = clamp7(c.getBlue()  * outlineScale);
         fillScale = isTriggered ? 80 : 35;
      }

      final int fr = clamp7(c.getRed()   * fillScale);
      final int fg = clamp7(c.getGreen() * fillScale);
      final int fb = clamp7(c.getBlue()  * fillScale);

      // Fill the entire pad first.
      api.drawRectangle(z, x, y, PAD_W, PAD_H, fr, fg, fb);
      // Then overlay the outline (top, bottom, left, right edges).
      api.drawRectangle(z, x,             y + PAD_H - 1, PAD_W, 1, or, og, ob);
      api.drawRectangle(z, x,             y,             PAD_W, 1, or, og, ob);
      api.drawRectangle(z, x,             y,             1, PAD_H, or, og, ob);
      api.drawRectangle(z, x + PAD_W - 1, y,             1, PAD_H, or, og, ob);
   }

   private static int padColX(final int col) { return PAD_X_START + col * PAD_X_STRIDE; }
   private static int padRowY(final int row) { return PAD_Y_START + row * PAD_Y_STRIDE; }

   // ============================================================
   // Ring animation (clipped to pad bounds)
   // ============================================================

   private void drawRingForFinger(final FingerState s)
   {
      if (!isActive) return;
      // Clear any existing ring by repainting the pad first, then draw the new ring.
      drawPad(s.padIdx);

      final int diameter = diameterFromPressure(s.touchZ);
      final int padX = padColX(s.padCol);
      final int padY = padRowY(s.padRow);
      final int xMin = padX;
      final int xMax = padX + PAD_W - 1;
      final int yMin = padY;
      final int yMax = padY + PAD_H - 1;

      final int cx = clamp(Math.round(s.touchX), xMin, xMax);
      final int cy = clamp(Math.round(s.touchY), yMin, yMax);

      drawRing(cx, cy, diameter, xMin, xMax, yMin, yMax);
   }

   private static int diameterFromPressure(final float z)
   {
      if (z < 0.10f) return 2;
      if (z < 0.25f) return 3;
      if (z < 0.45f) return 5;
      if (z < 0.65f) return 7;
      if (z < 0.85f) return 9;
      return 11;
   }

   /**
    * Draw a hollow ring of odd diameter centered at (cx, cy), or a 2×2 filled
    * square if diameter == 2. Clipped to (xMin..xMax, yMin..yMax).
    */
   private void drawRing(final int cx, final int cy, final int diameter,
                         final int xMin, final int xMax,
                         final int yMin, final int yMax)
   {
      final int z = pageId.getZoneId();

      if (diameter <= 2)
      {
         int x = cx;
         int y = cy;
         if (x + 1 > xMax) x = xMax - 1;
         if (y + 1 > yMax) y = yMax - 1;
         if (x < xMin) x = xMin;
         if (y < yMin) y = yMin;
         final int w = Math.min(2, xMax - x + 1);
         final int h = Math.min(2, yMax - y + 1);
         if (w > 0 && h > 0)
         {
            api.drawRectangle(z, x, y, w, h, RING_COLOR[0], RING_COLOR[1], RING_COLOR[2]);
         }
         return;
      }

      final double outerR = diameter / 2.0;
      final double innerR = (diameter <= 5) ? (outerR - 1.0) : (outerR - 2.0);
      final double outerSq = outerR * outerR;
      final double innerSq = innerR * innerR;
      final int half = diameter / 2;

      for (int dy = -half; dy <= half; dy++)
      {
         final int py = cy + dy;
         if (py < yMin || py > yMax) continue;

         int segStart = Integer.MIN_VALUE;
         for (int dx = -half; dx <= half + 1; dx++)
         {
            final boolean inRing;
            if (dx > half)
            {
               inRing = false;
            }
            else
            {
               final double distSq = dx * dx + dy * dy;
               inRing = distSq > innerSq && distSq <= outerSq;
            }

            if (inRing)
            {
               if (segStart == Integer.MIN_VALUE) segStart = dx;
            }
            else if (segStart != Integer.MIN_VALUE)
            {
               int xLeft  = cx + segStart;
               int xRight = cx + dx - 1;
               if (xLeft  < xMin) xLeft  = xMin;
               if (xRight > xMax) xRight = xMax;
               final int width = xRight - xLeft + 1;
               if (width > 0)
               {
                  api.drawRectangle(z, xLeft, py, width, 1,
                     RING_COLOR[0], RING_COLOR[1], RING_COLOR[2]);
               }
               segStart = Integer.MIN_VALUE;
            }
         }
      }
   }

   // ============================================================
   // Helpers
   // ============================================================

   private static int clamp(final int v, final int lo, final int hi)
   {
      if (v < lo) return lo;
      if (v > hi) return hi;
      return v;
   }

   private static int clamp7(final double v)
   {
      if (v < 0) return 0;
      if (v > 127) return 127;
      return (int) v;
   }

   // ============================================================
   // Per-finger state
   // ============================================================

   private static final class FingerState
   {
      int padCol;
      int padRow;
      int padIdx;
      int note;
      float touchX;
      float touchY;
      float touchZ;
   }
}
