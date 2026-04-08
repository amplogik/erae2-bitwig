package com.erae2bitwig.pages;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.Track;

import com.erae2bitwig.core.TouchZone;
import com.erae2bitwig.sysex.LowLevelApi;

import java.util.ArrayList;
import java.util.List;

/**
 * One vertical mixer strip: a 2-pixel wide volume slider plus a 1-pixel wide
 * VU meter to its right, with three stacked mute / solo / record-arm buttons
 * underneath. Bound to a Bitwig Channel (Track / EffectTrack / MasterTrack).
 *
 * Layout (per strip, all using the page's API zone):
 *   slider columns: x .. x+1   (2 wide)
 *   meter column:   x+2        (1 wide)
 *   button row:     x .. x+2   (3 wide each, stacked vertically)
 *
 * Touching the slider region maps the touch's y-coordinate directly to the
 * volume value (absolute, not relative). Touching mute/solo/arm toggles the
 * corresponding state.
 *
 * Channels that aren't Tracks (i.e., MasterTrack) skip the arm binding —
 * the arm button is still drawn for layout consistency but doesn't respond.
 */
public class MixerStrip
{
   public static final int STRIP_WIDTH = 3; // 2 slider + 1 meter

   private static final int SLIDER_W = 2;
   private static final int METER_W  = 1;

   // Meter color thresholds (pixel index, inclusive)
   private final int greenMaxPx;
   private final int yellowMaxPx;

   private final Channel channel;
   private final LowLevelApi api;
   private final int apiZoneId;
   private final int x;
   private final int faderY;
   private final int faderH;

   private final TouchZone sliderZone;
   private final TouchZone muteZone;
   private final TouchZone soloZone;
   private final TouchZone armZone;

   // Cached state used for drawing
   private float trackR = 0.4f;
   private float trackG = 0.4f;
   private float trackB = 0.4f;
   private double volumeNormalized = 0.0;
   private int meterLevel = 0;
   private boolean exists = true;

   private boolean active = false;

   public MixerStrip(final Channel channel,
                     final LowLevelApi api,
                     final int apiZoneId,
                     final String name,
                     final int x,
                     final int faderY,
                     final int faderH,
                     final int muteY,
                     final int soloY,
                     final int armY,
                     final int btnH)
   {
      this.channel = channel;
      this.api = api;
      this.apiZoneId = apiZoneId;
      this.x = x;
      this.faderY = faderY;
      this.faderH = faderH;

      this.greenMaxPx  = (int) Math.round(faderH * 0.72) - 1;
      this.yellowMaxPx = (int) Math.round(faderH * 0.89) - 1;

      // Slider touch zone (slider columns only — meter is read-only)
      sliderZone = new TouchZone(name + "_slider", x, faderY, SLIDER_W, faderH);
      sliderZone.onPress(e -> setVolumeFromTouch(e.getY()));
      sliderZone.onMove(e -> setVolumeFromTouch(e.getY()));

      // Mute / Solo / Arm buttons — full strip width, stacked
      muteZone = new TouchZone(name + "_mute", x, muteY, STRIP_WIDTH, btnH);
      muteZone.setColor(15, 30, 50); // dim sky blue (off)
      muteZone.onPress(e -> channel.mute().toggle());

      soloZone = new TouchZone(name + "_solo", x, soloY, STRIP_WIDTH, btnH);
      soloZone.setColor(35, 30, 0); // dim yellow (off)
      soloZone.onPress(e -> channel.solo().toggle());

      armZone = new TouchZone(name + "_arm", x, armY, STRIP_WIDTH, btnH);
      armZone.setColor(40, 0, 0); // dim red (off)
      // Arm only works on Tracks (MasterTrack is a Channel but not a Track).
      if (channel instanceof Track)
      {
         final Track track = (Track) channel;
         armZone.onPress(e -> track.arm().toggle());
         track.arm().addValueObserver(armed ->
         {
            armZone.setColor(armed ? 127 : 40, 0, 0);
            redrawTouchZone(armZone);
         });
      }

      // Subscribe to Bitwig values
      channel.color().addValueObserver((r, g, b) ->
      {
         trackR = r;
         trackG = g;
         trackB = b;
         redrawSlider();
      });

      channel.volume().value().addValueObserver(v ->
      {
         volumeNormalized = v;
         redrawSlider();
      });

      // VU meter: combined channel (-1), peak, range = fader pixel height
      channel.addVuMeterObserver(faderH, -1, true, level ->
      {
         meterLevel = Math.max(0, Math.min(faderH, level));
         redrawMeter();
      });

      channel.mute().addValueObserver(muted ->
      {
         muteZone.setColor(muted ? 60 : 15, muted ? 110 : 30, muted ? 127 : 50);
         redrawTouchZone(muteZone);
      });

      channel.solo().addValueObserver(soloed ->
      {
         soloZone.setColor(soloed ? 127 : 35, soloed ? 110 : 30, 0);
         redrawTouchZone(soloZone);
      });

      // Track existence (only present on Tracks; Master always exists)
      try
      {
         channel.exists().addValueObserver(e ->
         {
            exists = e;
            redrawAll();
         });
      }
      catch (final Exception ignored)
      {
         exists = true;
      }
   }

   /** Returns all touch zones owned by this strip so the page can register them. */
   public List<TouchZone> getTouchZones()
   {
      final List<TouchZone> all = new ArrayList<>(4);
      all.add(sliderZone);
      all.add(muteZone);
      all.add(soloZone);
      all.add(armZone);
      return all;
   }

   public void setActive(final boolean a)
   {
      this.active = a;
   }

   /** Called by the page after activation, draws the full strip from cached state. */
   public void drawAll()
   {
      redrawSlider();
      redrawMeter();
      // Mute / solo / arm zones are drawn by the page's drawSurface() since
      // they're TouchZones with solid colors registered in the page's zones list.
   }

   private void setVolumeFromTouch(final float touchY)
   {
      final float pos = (touchY - faderY) / (float) faderH;
      final double clamped = Math.max(0.0, Math.min(1.0, pos));
      channel.volume().value().set(clamped);
   }

   private void redrawSlider()
   {
      if (!active) return;

      if (!exists)
      {
         api.drawRectangle(apiZoneId, x, faderY, SLIDER_W, faderH, 4, 4, 4);
         return;
      }

      final int bgR = clamp7(trackR * 18);
      final int bgG = clamp7(trackG * 18);
      final int bgB = clamp7(trackB * 18);
      api.drawRectangle(apiZoneId, x, faderY, SLIDER_W, faderH, bgR, bgG, bgB);

      final int fillH = (int) Math.round(volumeNormalized * faderH);
      if (fillH > 0)
      {
         final int fR = clamp7(trackR * 90);
         final int fG = clamp7(trackG * 90);
         final int fB = clamp7(trackB * 90);
         api.drawRectangle(apiZoneId, x, faderY, SLIDER_W, fillH, fR, fG, fB);
      }
   }

   private void redrawMeter()
   {
      if (!active) return;

      final int mx = x + SLIDER_W;

      api.drawRectangle(apiZoneId, mx, faderY, METER_W, faderH, 3, 3, 3);

      if (!exists || meterLevel <= 0) return;

      final int top = Math.min(meterLevel, faderH);

      final int greenCount = Math.min(top, greenMaxPx + 1);
      if (greenCount > 0)
      {
         api.drawRectangle(apiZoneId, mx, faderY, METER_W, greenCount, 0, 110, 0);
      }

      if (top > greenMaxPx + 1)
      {
         final int yellowStart = greenMaxPx + 1;
         final int yellowCount = Math.min(top, yellowMaxPx + 1) - yellowStart;
         if (yellowCount > 0)
         {
            api.drawRectangle(apiZoneId, mx, faderY + yellowStart, METER_W, yellowCount,
               110, 90, 0);
         }
      }

      if (top > yellowMaxPx + 1)
      {
         final int redStart = yellowMaxPx + 1;
         final int redCount = top - redStart;
         if (redCount > 0)
         {
            api.drawRectangle(apiZoneId, mx, faderY + redStart, METER_W, redCount,
               127, 0, 0);
         }
      }
   }

   private void redrawTouchZone(final TouchZone zone)
   {
      if (!active) return;
      api.drawZone(apiZoneId, zone);
   }

   private void redrawAll()
   {
      redrawSlider();
      redrawMeter();
      redrawTouchZone(muteZone);
      redrawTouchZone(soloZone);
      redrawTouchZone(armZone);
   }

   private static int clamp7(final double v)
   {
      if (v < 0) return 0;
      if (v > 127) return 127;
      return (int) v;
   }
}
