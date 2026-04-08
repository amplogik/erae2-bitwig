package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableIntegerValue;
import com.bitwig.extension.controller.api.TimeSignatureValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.TouchZone;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/**
 * Page 1: DAW Control — general mixer + transport, modeled on Bitwig's basic mixer display.
 *
 * Layout (42×24 surface, origin bottom-left). A 1-LED border is left blank
 * around the edges so touches don't run off the device's display area.
 *
 *   y=23     : top border (blank)
 *   y=20..22 : transport row
 *     x=1..23  : transport buttons (Play, Stop, Record, AutoWrite, Fill, Takes)
 *     x=25..32 : BPM drag zone (8 wide)
 *     x=34..35 : Numerator -
 *     x=36..37 : Numerator +
 *     x=38..40 : Denominator cycle (2/4/8/16)
 *   y=19     : margin (blank) — buffer against transport row
 *   y=11..18 : mixer fader / VU meter (8 tall)
 *   y=10     : gap (blank)
 *   y=8..9   : Rec Arm row (one button per strip)
 *   y=6..7   : Solo row
 *   y=4..5   : Mute row
 *   y=3      : gap (blank)
 *   y=1..2   : Scroll buttons
 *   y=0      : bottom border (blank)
 *
 *   x=0      : left border
 *   x=1,5,9,13,17,21,25,29 : 8 scrollable track strips (3 wide each)
 *   x=34..36 : FX1 (fixed)
 *   x=38..40 : Master (fixed)
 *   x=41     : right border
 */
public class DawControlPage extends EraePage
{
   // --- Top row (transport) ---
   private static final int ROW_Y = 20;
   private static final int BTN_W = 3;
   private static final int BTN_H = 3;
   private static final int BTN_STRIDE = 4; // 3px button + 1px gap
   private static final int X_OFFSET = 1;   // left border

   private TouchZone playZone;
   private TouchZone stopZone;
   private TouchZone recordZone;
   private TouchZone autoWriteZone;
   private TouchZone fillZone;
   private TouchZone takesZone;

   // --- Status zones (BPM, time sig) ---
   private TouchZone bpmZone;
   private TouchZone numDownZone;
   private TouchZone numUpZone;
   private TouchZone denomZone;
   private float bpmDragLastX = 0f;
   private static final int[] DENOM_CYCLE = {2, 4, 8, 16};

   // --- Mixer strips ---
   private static final int FADER_Y = 11;
   private static final int FADER_H = 8;
   private static final int MUTE_Y = 4;
   private static final int SOLO_Y = 6;
   private static final int ARM_Y = 8;
   private static final int STRIP_BTN_H = 2;
   private static final int MIXER_BANK_SIZE = 8;
   private static final int STRIP_STRIDE = 4; // 3w strip + 1px gap
   private static final int FX1_X = 34;
   private static final int MASTER_X = 38;

   private final MixerStrip[] trackStrips = new MixerStrip[MIXER_BANK_SIZE];
   private MixerStrip fx1Strip;
   private MixerStrip masterStrip;
   private TouchZone scrollLeftZone;
   private TouchZone scrollRightZone;

   public DawControlPage(final ControllerHost host,
                         final LowLevelApi api,
                         final BitwigModel model)
   {
      super(PageId.DAW_CONTROL, host, api, model);
   }

   @Override
   public void setupBindings()
   {
      setupTransportRow();
      setupStatusZones();
      setupMixerStrips();
      setupScrollButtons();
   }

   private void setupMixerStrips()
   {
      final TrackBank mixerBank = model.getMixerTrackBank();
      for (int i = 0; i < MIXER_BANK_SIZE; i++)
      {
         final Track t = mixerBank.getItemAt(i);
         final int sx = X_OFFSET + i * STRIP_STRIDE;
         trackStrips[i] = new MixerStrip(t, api, pageId.getZoneId(), "Strip" + i,
            sx, FADER_Y, FADER_H, MUTE_Y, SOLO_Y, ARM_Y, STRIP_BTN_H);
         zones.addAll(trackStrips[i].getTouchZones());
      }

      final Track fx1 = model.getEffectTrackBank().getItemAt(0);
      fx1Strip = new MixerStrip(fx1, api, pageId.getZoneId(), "FX1",
         FX1_X, FADER_Y, FADER_H, MUTE_Y, SOLO_Y, ARM_Y, STRIP_BTN_H);
      zones.addAll(fx1Strip.getTouchZones());

      final MasterTrack master = model.getMasterTrack();
      masterStrip = new MixerStrip(master, api, pageId.getZoneId(), "Master",
         MASTER_X, FADER_Y, FADER_H, MUTE_Y, SOLO_Y, ARM_Y, STRIP_BTN_H);
      zones.addAll(masterStrip.getTouchZones());
   }

   private void setupScrollButtons()
   {
      final TrackBank mixerBank = model.getMixerTrackBank();
      mixerBank.canScrollBackwards().markInterested();
      mixerBank.canScrollForwards().markInterested();

      scrollLeftZone = new TouchZone("MixScrollL", 1, 1, 3, 2);
      scrollLeftZone.setColor(20, 20, 40);
      scrollLeftZone.onPress(e -> mixerBank.scrollBackwards());
      zones.add(scrollLeftZone);

      scrollRightZone = new TouchZone("MixScrollR", 29, 1, 3, 2);
      scrollRightZone.setColor(20, 20, 40);
      scrollRightZone.onPress(e -> mixerBank.scrollForwards());
      zones.add(scrollRightZone);

      mixerBank.canScrollBackwards().addValueObserver(can ->
      {
         if (!isActive) return;
         scrollLeftZone.setColor(can ? 60 : 15, can ? 60 : 15, can ? 110 : 25);
         redrawZone(scrollLeftZone);
      });
      mixerBank.canScrollForwards().addValueObserver(can ->
      {
         if (!isActive) return;
         scrollRightZone.setColor(can ? 60 : 15, can ? 60 : 15, can ? 110 : 25);
         redrawZone(scrollRightZone);
      });
   }

   @Override
   public void onActivate()
   {
      super.onActivate();
      // Mark strips active and draw their custom rendering on top of the
      // base TouchZone solid fills (which super.drawSurface() already drew).
      for (final MixerStrip s : trackStrips)
      {
         s.setActive(true);
         s.drawAll();
      }
      fx1Strip.setActive(true);
      fx1Strip.drawAll();
      masterStrip.setActive(true);
      masterStrip.drawAll();
   }

   @Override
   public void onDeactivate()
   {
      super.onDeactivate();
      for (final MixerStrip s : trackStrips) s.setActive(false);
      if (fx1Strip != null) fx1Strip.setActive(false);
      if (masterStrip != null) masterStrip.setActive(false);
   }

   private void setupTransportRow()
   {
      final Transport transport = model.getTransport();

      // Make sure all values we observe are marked interested
      transport.isPlaying().markInterested();
      transport.isArrangerRecordEnabled().markInterested();
      transport.isArrangerAutomationWriteEnabled().markInterested();
      transport.isFillModeActive().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();

      int x = X_OFFSET;

      // Play
      playZone = button("Play", x, transport.isPlaying(),
         () -> transport.play(),
         /*onR*/  0, /*onG*/ 127, /*onB*/   0,
         /*offR*/ 0, /*offG*/ 40, /*offB*/  0);
      transport.isPlaying().addValueObserver(v -> refreshButton(playZone, v,
         0, 127, 0, 0, 40, 0));
      x += BTN_STRIDE;

      // Stop — momentary; bright red, dim red between presses
      stopZone = new TouchZone("Stop", x, ROW_Y, BTN_W, BTN_H);
      stopZone.setColor(80, 0, 0);
      stopZone.onPress(e -> transport.stop());
      zones.add(stopZone);
      x += BTN_STRIDE;

      // Record (arranger record enable)
      recordZone = button("Record", x, transport.isArrangerRecordEnabled(),
         () -> transport.isArrangerRecordEnabled().toggle(),
         127, 0, 0,
          40, 0, 0);
      transport.isArrangerRecordEnabled().addValueObserver(v -> refreshButton(recordZone, v,
         127, 0, 0, 40, 0, 0));
      x += BTN_STRIDE;

      // Write Automation toggle (arranger automation write)
      autoWriteZone = button("AutoW", x, transport.isArrangerAutomationWriteEnabled(),
         () -> transport.isArrangerAutomationWriteEnabled().toggle(),
         127, 80, 0,
          40, 25, 0);
      transport.isArrangerAutomationWriteEnabled().addValueObserver(v -> refreshButton(autoWriteZone, v,
         127, 80, 0, 40, 25, 0));
      x += BTN_STRIDE;

      // Fill
      fillZone = button("Fill", x, transport.isFillModeActive(),
         () -> transport.isFillModeActive().toggle(),
           0, 100, 127,
           0,  30,  40);
      transport.isFillModeActive().addValueObserver(v -> refreshButton(fillZone, v,
         0, 100, 127, 0, 30, 40));
      x += BTN_STRIDE;

      // "Record as Takes" — clip launcher overdub OFF means new recordings become takes (new clips).
      // Lit when in "takes" mode (overdub disabled), dim when in overdub mode.
      takesZone = new TouchZone("Takes", x, ROW_Y, BTN_W, BTN_H);
      final boolean overdub0 = transport.isClipLauncherOverdubEnabled().get();
      setTakesColor(overdub0);
      takesZone.onPress(e -> transport.isClipLauncherOverdubEnabled().toggle());
      transport.isClipLauncherOverdubEnabled().addValueObserver(overdub ->
      {
         setTakesColor(overdub);
         redrawZone(takesZone);
      });
      zones.add(takesZone);
      x += BTN_STRIDE;
   }

   private void setupStatusZones()
   {
      final Transport transport = model.getTransport();
      final Parameter tempo = transport.tempo();
      final TimeSignatureValue timeSig = transport.timeSignature();
      final SettableIntegerValue numerator = timeSig.numerator();
      final SettableIntegerValue denominator = timeSig.denominator();

      tempo.markInterested();
      numerator.markInterested();
      denominator.markInterested();

      // BPM drag zone: x=25..32, w=8 (trimmed from 10 to make room for borders)
      bpmZone = new TouchZone("BPM", 25, ROW_Y, 8, BTN_H);
      bpmZone.setColor(0, 40, 80);
      bpmZone.onPress(e ->
      {
         bpmDragLastX = e.getX();
         bpmZone.setColor(0, 80, 127);
         redrawZone(bpmZone);
         host.showPopupNotification("Tempo: " + (int) tempo.getRaw() + " BPM");
      });
      bpmZone.onMove(e ->
      {
         final float dx = e.getX() - bpmDragLastX;
         if (Math.abs(dx) >= 1f)
         {
            // 1 pixel = 1 BPM
            tempo.incRaw((int) dx);
            bpmDragLastX = e.getX();
            host.showPopupNotification("Tempo: " + (int) tempo.getRaw() + " BPM");
         }
      });
      bpmZone.onRelease(e ->
      {
         bpmZone.setColor(0, 40, 80);
         redrawZone(bpmZone);
      });
      zones.add(bpmZone);

      // Numerator -
      numDownZone = new TouchZone("Num-", 34, ROW_Y, 2, BTN_H);
      numDownZone.setColor(60, 30, 0);
      numDownZone.onPress(e ->
      {
         final int n = Math.max(1, numerator.get() - 1);
         numerator.set(n);
         host.showPopupNotification("Time sig: " + n + "/" + denominator.get());
      });
      zones.add(numDownZone);

      // Numerator +
      numUpZone = new TouchZone("Num+", 36, ROW_Y, 2, BTN_H);
      numUpZone.setColor(100, 50, 0);
      numUpZone.onPress(e ->
      {
         final int n = Math.min(16, numerator.get() + 1);
         numerator.set(n);
         host.showPopupNotification("Time sig: " + n + "/" + denominator.get());
      });
      zones.add(numUpZone);

      // Denominator cycle
      denomZone = new TouchZone("Denom", 38, ROW_Y, 3, BTN_H);
      denomZone.setColor(80, 40, 0);
      denomZone.onPress(e ->
      {
         final int current = denominator.get();
         int idx = 0;
         for (int i = 0; i < DENOM_CYCLE.length; i++)
         {
            if (DENOM_CYCLE[i] == current) { idx = i; break; }
         }
         final int next = DENOM_CYCLE[(idx + 1) % DENOM_CYCLE.length];
         denominator.set(next);
         host.showPopupNotification("Time sig: " + numerator.get() + "/" + next);
      });
      zones.add(denomZone);
   }

   private void setTakesColor(final boolean overdubEnabled)
   {
      // Lit purple when in "takes" mode (overdub OFF), dim when overdub ON
      if (overdubEnabled)
      {
         takesZone.setColor(40, 0, 50);
      }
      else
      {
         takesZone.setColor(127, 0, 127);
      }
   }

   /** Create a toggle-style button bound to a SettableBooleanValue. */
   private TouchZone button(final String name, final int x,
                            final SettableBooleanValue value,
                            final Runnable pressAction,
                            final int onR, final int onG, final int onB,
                            final int offR, final int offG, final int offB)
   {
      final TouchZone zone = new TouchZone(name, x, ROW_Y, BTN_W, BTN_H);
      final boolean current = value.get();
      zone.setColor(current ? onR : offR, current ? onG : offG, current ? onB : offB);
      zone.onPress(e -> pressAction.run());
      zones.add(zone);
      return zone;
   }

   private void refreshButton(final TouchZone zone, final boolean on,
                              final int onR, final int onG, final int onB,
                              final int offR, final int offG, final int offB)
   {
      if (!isActive) return;
      zone.setColor(on ? onR : offR, on ? onG : offG, on ? onB : offB);
      redrawZone(zone);
   }
}
