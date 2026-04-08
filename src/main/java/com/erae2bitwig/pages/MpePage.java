package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.NoteInput;

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
 * Page 2: MPE Play — LinnStrument-style isomorphic playing surface.
 *
 * Layout (42×24, 1-LED border):
 *   y=23     : top border (blank)
 *   y=20..22 : control row
 *     x=1..3   : Row offset −
 *     x=5..7   : Row offset +
 *     x=9..11  : Octave −
 *     x=13..15 : Octave +
 *     x=17..20 : Scale cycle
 *     x=22..25 : Root cycle
 *     x=27..32 : FX send pad (cursor track send 0)
 *   y=19     : margin (blank)
 *   y=1..18  : playing surface (20 cols × 9 rows of 2×2 cells)
 *   y=0      : bottom border (blank)
 *
 * Each cell = 2×2 LEDs. Tonic notes are bright magenta, in-scale notes are
 * medium cyan, chromatic fillers are very dim grey. Touches generate MPE
 * notes via Bitwig's NoteInput in MPE Lower mode (master ch 0, note ch 1..15,
 * pitch bend range 48). Per-finger expression: pitch bend = horizontal slide
 * from initial touch (1 cell = 1 semitone), CC74 = vertical slide, channel
 * pressure = Z, velocity = Z at touch-on. A pressure-driven filled circle is
 * drawn around each touch as visual feedback.
 */
public class MpePage extends EraePage
{
   // --- Playing surface geometry ---
   private static final int GRID_X = 1;     // left of grid
   private static final int GRID_Y = 1;     // bottom of grid
   private static final int CELL_W = 2;
   private static final int CELL_H = 2;
   private static final int GRID_COLS = 20;
   private static final int GRID_ROWS = 9;
   private static final int GRID_RIGHT = GRID_X + GRID_COLS * CELL_W; // exclusive
   private static final int GRID_TOP   = GRID_Y + GRID_ROWS * CELL_H; // exclusive

   // --- Control row ---
   private static final int CTRL_Y = 20;
   private static final int CTRL_H = 3;

   // --- MPE / pitch bend ---
   /** MPE pitch bend range, must match setUseExpressiveMidi argument. */
   private static final int PITCH_BEND_RANGE_SEMITONES = 48;
   /** Lowest note channel (MPE Lower mode reserves channel 0 as master). */
   private static final int FIRST_NOTE_CHANNEL = 1;
   private static final int LAST_NOTE_CHANNEL  = 15;

   // --- Configuration (mutable, default to LinnStrument 4ths + Pent Min in C) ---
   private int rowOffset = 5;       // semitones per row (LinnStrument default)
   private int baseNote  = 36;      // bottom-left cell MIDI note (C2)
   private int rootNote  = 0;       // 0=C, 1=C#, ..., 11=B
   private Scale scale   = Scale.PENT_MIN;

   // --- Per-finger touch state ---
   private final Map<Integer, FingerState> fingers = new HashMap<>();
   private final boolean[] channelInUse = new boolean[16];

   // --- Bitwig MPE injection ---
   private final Erae2MidiPorts midiPorts;
   private NoteInput noteInput;

   // --- Control row touch zones ---
   private TouchZone rowOffDownZone;
   private TouchZone rowOffUpZone;
   private TouchZone octDownZone;
   private TouchZone octUpZone;
   private TouchZone scaleCycleZone;
   private TouchZone rootCycleZone;
   private TouchZone fxSendZone;

   // --- Note name table ---
   private static final String[] NOTE_NAMES = {
      "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
   };

   public MpePage(final ControllerHost host,
                  final LowLevelApi api,
                  final BitwigModel model,
                  final Erae2MidiPorts midiPorts)
   {
      super(PageId.MPE_PLAY, host, api, model);
      this.midiPorts = midiPorts;
   }

   @Override
   public void setupBindings()
   {
      // Create the MPE NoteInput on the MPE port. With no masks, no MIDI
      // from the actual port flows through; we use sendRawMidiEvent() to inject.
      noteInput = midiPorts.getMpeIn().createNoteInput("Erae MPE");
      noteInput.setUseExpressiveMidi(true, 0, PITCH_BEND_RANGE_SEMITONES);

      setupControlRow();
   }

   private void setupControlRow()
   {
      rowOffDownZone = controlButton("RowOff-",  1, 80, 0, 80, e ->
      {
         if (rowOffset > 1)
         {
            rowOffset--;
            host.showPopupNotification("Row offset: " + rowOffset + " semitones");
            redrawGrid();
         }
      });
      rowOffUpZone   = controlButton("RowOff+",  5, 100, 0, 100, e ->
      {
         if (rowOffset < 12)
         {
            rowOffset++;
            host.showPopupNotification("Row offset: " + rowOffset + " semitones");
            redrawGrid();
         }
      });

      octDownZone    = controlButton("Oct-",     9, 0, 60, 80, e ->
      {
         if (baseNote >= 12)
         {
            baseNote -= 12;
            host.showPopupNotification("Base note: " + noteName(baseNote));
            redrawGrid();
         }
      });
      octUpZone      = controlButton("Oct+",    13, 0, 80, 100, e ->
      {
         if (baseNote <= 96)
         {
            baseNote += 12;
            host.showPopupNotification("Base note: " + noteName(baseNote));
            redrawGrid();
         }
      });

      scaleCycleZone = controlButton4("Scale",  17, 30, 100, 30, e ->
      {
         scale = scale.next();
         host.showPopupNotification("Scale: " + scale.displayName);
         redrawGrid();
      });
      rootCycleZone  = controlButton4("Root",   22, 60, 100, 30, e ->
      {
         rootNote = (rootNote + 1) % 12;
         host.showPopupNotification("Root: " + NOTE_NAMES[rootNote]);
         redrawGrid();
      });

      // FX send pressure pad (6 wide). Pressure from touch z drives the
      // cursor track's send 0 level. Released → send returns to whatever
      // Bitwig had (we don't try to restore the previous value here; the
      // user can ride the pad to set the level momentarily).
      fxSendZone = new TouchZone("FXSend", 27, CTRL_Y, 6, CTRL_H);
      fxSendZone.setColor(60, 0, 80);
      fxSendZone.onPress(e -> sendFxPressure(e.getZ()));
      fxSendZone.onMove(e -> sendFxPressure(e.getZ()));
      fxSendZone.onRelease(e -> sendFxPressure(0f));
      zones.add(fxSendZone);
   }

   private void sendFxPressure(final float z)
   {
      final var sendBank = model.getCursorTrack().sendBank();
      if (sendBank.getSizeOfBank() == 0) return;
      final double v = Math.max(0.0, Math.min(1.0, z));
      sendBank.getItemAt(0).value().set(v);
   }

   private TouchZone controlButton(final String name, final int x,
                                   final int r, final int g, final int b,
                                   final java.util.function.Consumer<TouchEvent> onPress)
   {
      final TouchZone z = new TouchZone(name, x, CTRL_Y, 3, CTRL_H);
      z.setColor(r, g, b);
      z.onPress(onPress);
      zones.add(z);
      return z;
   }

   private TouchZone controlButton4(final String name, final int x,
                                    final int r, final int g, final int b,
                                    final java.util.function.Consumer<TouchEvent> onPress)
   {
      final TouchZone z = new TouchZone(name, x, CTRL_Y, 4, CTRL_H);
      z.setColor(r, g, b);
      z.onPress(onPress);
      zones.add(z);
      return z;
   }

   @Override
   public void onActivate()
   {
      super.onActivate();
      redrawGrid();
   }

   @Override
   public void handleTouchEvent(final TouchEvent event)
   {
      final float x = event.getX();
      final float y = event.getY();

      // Route playing-surface touches to the grid handler; everything else
      // (control row buttons) falls through to the base TouchZone dispatch.
      if (x >= GRID_X && x < GRID_RIGHT && y >= GRID_Y && y < GRID_TOP)
      {
         handleGridTouch(event);
         return;
      }
      super.handleTouchEvent(event);
   }

   // ============================================================
   // Grid touch handling
   // ============================================================

   private void handleGridTouch(final TouchEvent event)
   {
      final int finger = event.getFingerIndex();
      if (event.isPress())
      {
         onFingerDown(finger, event);
      }
      else if (event.isMove())
      {
         onFingerMove(finger, event);
      }
      else if (event.isRelease())
      {
         onFingerUp(finger);
      }
   }

   private void onFingerDown(final int finger, final TouchEvent event)
   {
      final float x = event.getX();
      final float y = event.getY();
      final float z = event.getZ();

      final int cellCol = Math.max(0, Math.min(GRID_COLS - 1, (int) ((x - GRID_X) / CELL_W)));
      final int cellRow = Math.max(0, Math.min(GRID_ROWS - 1, (int) ((y - GRID_Y) / CELL_H)));

      final int note = baseNote + cellCol + cellRow * rowOffset;
      if (note < 0 || note > 127) return;

      final int channel = allocateChannel();
      if (channel < 0) return;

      final int velocity = Math.max(1, Math.min(127, (int) (z * 127)));

      // Initialize per-channel expression at neutral, then noteOn.
      sendPitchBendRaw(channel, 8192);
      sendCC74(channel, 64);
      sendChannelPressure(channel, (int) (z * 127));
      sendNoteOn(channel, note, velocity);

      final FingerState s = new FingerState();
      s.channel = channel;
      s.note = note;
      s.cellCol = cellCol;
      s.cellRow = cellRow;
      s.initialX = x;
      s.initialY = y;
      s.touchX = x;
      s.touchY = y;
      s.touchZ = z;
      fingers.put(finger, s);

      drawCircle(s);
   }

   private void onFingerMove(final int finger, final TouchEvent event)
   {
      final FingerState s = fingers.get(finger);
      if (s == null) return;

      final float x = event.getX();
      final float y = event.getY();
      final float z = event.getZ();

      // Pitch bend: horizontal delta from initial touch, in cells, mapped to
      // ± PITCH_BEND_RANGE_SEMITONES (each cell = 1 semitone).
      final float deltaCells = (x - s.initialX) / (float) CELL_W;
      final float bendNorm = deltaCells / PITCH_BEND_RANGE_SEMITONES; // -1..+1
      final int bendValue = clamp14(8192 + (int) (bendNorm * 8192));
      sendPitchBendRaw(s.channel, bendValue);

      // CC74: vertical delta scaled so one cell = ±32 around 64.
      final float deltaY = (y - s.initialY) / (float) CELL_H;
      final int cc74 = Math.max(0, Math.min(127, 64 + (int) (deltaY * 32)));
      sendCC74(s.channel, cc74);

      // Channel pressure: live z.
      sendChannelPressure(s.channel, Math.max(0, Math.min(127, (int) (z * 127))));

      s.touchX = x;
      s.touchY = y;
      s.touchZ = z;

      drawCircle(s);
   }

   private void onFingerUp(final int finger)
   {
      final FingerState s = fingers.remove(finger);
      if (s == null) return;
      sendNoteOff(s.channel, s.note);
      freeChannel(s.channel);
      eraseCircle(s);
   }

   // ============================================================
   // MPE channel allocation
   // ============================================================

   private int allocateChannel()
   {
      for (int c = FIRST_NOTE_CHANNEL; c <= LAST_NOTE_CHANNEL; c++)
      {
         if (!channelInUse[c])
         {
            channelInUse[c] = true;
            return c;
         }
      }
      return -1;
   }

   private void freeChannel(final int channel)
   {
      if (channel >= FIRST_NOTE_CHANNEL && channel <= LAST_NOTE_CHANNEL)
      {
         channelInUse[channel] = false;
      }
   }

   // ============================================================
   // MIDI sending (via NoteInput.sendRawMidiEvent)
   // ============================================================

   private void sendNoteOn(final int channel, final int note, final int velocity)
   {
      noteInput.sendRawMidiEvent(0x90 | channel, note, velocity);
   }

   private void sendNoteOff(final int channel, final int note)
   {
      noteInput.sendRawMidiEvent(0x80 | channel, note, 0);
   }

   private void sendPitchBendRaw(final int channel, final int bendValue)
   {
      final int v = clamp14(bendValue);
      noteInput.sendRawMidiEvent(0xE0 | channel, v & 0x7F, (v >> 7) & 0x7F);
   }

   private void sendCC74(final int channel, final int value)
   {
      noteInput.sendRawMidiEvent(0xB0 | channel, 74, value & 0x7F);
   }

   private void sendChannelPressure(final int channel, final int value)
   {
      noteInput.sendRawMidiEvent(0xD0 | channel, value & 0x7F, 0);
   }

   private static int clamp14(final int v)
   {
      if (v < 0) return 0;
      if (v > 16383) return 16383;
      return v;
   }

   // ============================================================
   // Grid drawing
   // ============================================================

   /** Draw all 180 cells with their tonic / scale / non-scale colors. */
   private void redrawGrid()
   {
      if (!isActive) return;
      for (int col = 0; col < GRID_COLS; col++)
      {
         for (int row = 0; row < GRID_ROWS; row++)
         {
            drawCell(col, row);
         }
      }
   }

   private void drawCell(final int col, final int row)
   {
      final int x = GRID_X + col * CELL_W;
      final int y = GRID_Y + row * CELL_H;
      final int note = baseNote + col + row * rowOffset;
      final int[] c = colorForNote(note);
      api.drawRectangle(pageId.getZoneId(), x, y, CELL_W, CELL_H, c[0], c[1], c[2]);
   }

   private int[] colorForNote(final int midiNote)
   {
      final int relToRoot = ((midiNote - rootNote) % 12 + 12) % 12;
      if (relToRoot == 0)
      {
         // Tonic — bright magenta
         return new int[]{100, 0, 100};
      }
      if (scale.contains(relToRoot))
      {
         // In-scale — medium cyan
         return new int[]{0, 50, 70};
      }
      // Out-of-scale chromatic filler — very dim grey
      return new int[]{4, 4, 4};
   }

   // ============================================================
   // Pressure ring animation
   // ============================================================

   /**
    * Map touch pressure to ring diameter. We use odd diameters for symmetric
    * rings (the center sits on an integer pixel). D=2 is the minimum and is
    * a 2×2 filled square — the "tap indicator" for the lightest touches.
    */
   private static int diameterFromPressure(final float z)
   {
      if (z < 0.10f) return 2;
      if (z < 0.25f) return 3;
      if (z < 0.45f) return 5;
      if (z < 0.65f) return 7;
      if (z < 0.85f) return 9;
      return 11;
   }

   private void drawCircle(final FingerState s)
   {
      if (!isActive) return;

      final int cx = clampToGridX(Math.round(s.touchX));
      final int cy = clampToGridY(Math.round(s.touchY));
      final int diameter = diameterFromPressure(s.touchZ);

      // Restore the area under the previous ring before drawing the new one.
      if (s.prevDiameter > 0)
      {
         restoreArea(s.prevCx, s.prevCy, s.prevDiameter);
      }

      drawRing(cx, cy, diameter, 127, 127, 127);

      s.prevCx = cx;
      s.prevCy = cy;
      s.prevDiameter = diameter;
   }

   private void eraseCircle(final FingerState s)
   {
      if (!isActive) return;
      if (s.prevDiameter <= 0) return;
      restoreArea(s.prevCx, s.prevCy, s.prevDiameter);
      s.prevDiameter = -1;
   }

   /**
    * Draw a hollow ring of the given odd diameter centered at (cx, cy),
    * or a 2×2 filled square if diameter == 2. Ring is computed by checking
    * each pixel against (innerR² < dist² ≤ outerR²) using half-pixel radii
    * to avoid single-pixel spikes at the extremes.
    *
    * Thickness:
    *   D≤5  → 1-pixel ring (innerR = outerR − 1)
    *   D≥7  → 2-pixel ring (innerR = outerR − 2)
    */
   private void drawRing(final int cx, final int cy, final int diameter,
                         final int r, final int g, final int b)
   {
      if (diameter <= 2)
      {
         // Min size: 2×2 filled square. Anchor so it stays inside the grid.
         int x = cx;
         int y = cy;
         if (x + 1 >= GRID_RIGHT) x = GRID_RIGHT - 2;
         if (y + 1 >= GRID_TOP)   y = GRID_TOP - 2;
         if (x < GRID_X) x = GRID_X;
         if (y < GRID_Y) y = GRID_Y;
         api.drawRectangle(pageId.getZoneId(), x, y, 2, 2, r, g, b);
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
         if (py < GRID_Y || py >= GRID_TOP) continue;

         // Walk the row, batching contiguous in-ring pixels into rect segments.
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
               drawHorizontalSegment(cx + segStart, py, dx - segStart, r, g, b);
               segStart = Integer.MIN_VALUE;
            }
         }
      }
   }

   private void drawHorizontalSegment(int x, final int y, int width,
                                      final int r, final int g, final int b)
   {
      if (x < GRID_X)
      {
         width -= (GRID_X - x);
         x = GRID_X;
      }
      if (x + width > GRID_RIGHT)
      {
         width = GRID_RIGHT - x;
      }
      if (width > 0)
      {
         api.drawRectangle(pageId.getZoneId(), x, y, width, 1, r, g, b);
      }
   }

   /** Repaint cells that overlap the bounding box of a (cx, cy, diameter) ring. */
   private void restoreArea(final int cx, final int cy, final int diameter)
   {
      final int half = diameter / 2;
      final int xMin, xMax, yMin, yMax;
      if (diameter == 2)
      {
         xMin = cx;
         xMax = cx + 1;
         yMin = cy;
         yMax = cy + 1;
      }
      else
      {
         xMin = cx - half;
         xMax = cx + half;
         yMin = cy - half;
         yMax = cy + half;
      }
      final int xMinC = Math.max(GRID_X, xMin);
      final int xMaxC = Math.min(GRID_RIGHT - 1, xMax);
      final int yMinC = Math.max(GRID_Y, yMin);
      final int yMaxC = Math.min(GRID_TOP - 1, yMax);

      final int firstCol = (xMinC - GRID_X) / CELL_W;
      final int lastCol  = (xMaxC - GRID_X) / CELL_W;
      final int firstRow = (yMinC - GRID_Y) / CELL_H;
      final int lastRow  = (yMaxC - GRID_Y) / CELL_H;

      for (int col = firstCol; col <= lastCol; col++)
      {
         for (int row = firstRow; row <= lastRow; row++)
         {
            drawCell(col, row);
         }
      }
   }

   private static int clampToGridX(final int x)
   {
      if (x < GRID_X) return GRID_X;
      if (x >= GRID_RIGHT) return GRID_RIGHT - 1;
      return x;
   }

   private static int clampToGridY(final int y)
   {
      if (y < GRID_Y) return GRID_Y;
      if (y >= GRID_TOP) return GRID_TOP - 1;
      return y;
   }

   // ============================================================
   // Helpers
   // ============================================================

   private static String noteName(final int midi)
   {
      final int n = midi % 12;
      final int oct = midi / 12 - 1;
      return NOTE_NAMES[n] + oct;
   }

   // ============================================================
   // Per-finger state
   // ============================================================

   private static final class FingerState
   {
      int channel;
      int note;
      int cellCol;
      int cellRow;
      float initialX;
      float initialY;
      float touchX;
      float touchY;
      float touchZ;
      int prevCx = 0;
      int prevCy = 0;
      int prevDiameter = -1;
   }

   // ============================================================
   // Scale enum
   // ============================================================

   private enum Scale
   {
      CHROMATIC ("Chromatic",  0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      MAJOR     ("Major",      0, 2, 4, 5, 7, 9, 11),
      MINOR     ("Nat Minor",  0, 2, 3, 5, 7, 8, 10),
      PENT_MAJ  ("Pent Maj",   0, 2, 4, 7, 9),
      PENT_MIN  ("Pent Min",   0, 3, 5, 7, 10),
      BLUES     ("Blues",      0, 3, 5, 6, 7, 10),
      DORIAN    ("Dorian",     0, 2, 3, 5, 7, 9, 10),
      MIXO      ("Mixolydian", 0, 2, 4, 5, 7, 9, 10),
      LYDIAN    ("Lydian",     0, 2, 4, 6, 7, 9, 11),
      PHRYGIAN  ("Phrygian",   0, 1, 3, 5, 7, 8, 10),
      HARM_MIN  ("Harm Min",   0, 2, 3, 5, 7, 8, 11),
      MELO_MIN  ("Melo Min",   0, 2, 3, 5, 7, 9, 11);

      final String displayName;
      private final boolean[] members = new boolean[12];

      Scale(final String displayName, final int... intervals)
      {
         this.displayName = displayName;
         for (final int i : intervals) members[i] = true;
      }

      boolean contains(final int interval)
      {
         return interval >= 0 && interval < 12 && members[interval];
      }

      Scale next()
      {
         final Scale[] all = values();
         return all[(ordinal() + 1) % all.length];
      }
   }
}
