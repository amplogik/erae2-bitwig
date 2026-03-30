package com.erae2bitwig.hardware;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;

import com.erae2bitwig.sysex.ScriptProtocol;
import com.erae2bitwig.sysex.SysExConstants;

/**
 * A slider/fader on the Erae Touch 2 that communicates via SysEx.
 * Wraps a Bitwig HardwareSlider with SysEx input matching.
 */
public class EraeSlider
{
   private final HardwareSlider slider;
   private final int sliderId;
   private final ScriptProtocol protocol;
   private int lastSentValue = -1;

   public EraeSlider(final ControllerHost host,
                     final HardwareSurface surface,
                     final MidiIn midiIn,
                     final ScriptProtocol protocol,
                     final int sliderId,
                     final String name)
   {
      this.sliderId = sliderId;
      this.protocol = protocol;

      slider = surface.createHardwareSlider(name);

      // Build a SysEx expression with a value wildcard for the absolute value matcher.
      // Format: SCRIPT_SYSEX_PREFIX + SLIDER_PREFIX + sliderId + ?? (value) + F7
      // createAbsoluteValueMatcher(matchExpression, valueExpression, valueRange)
      // The match expression uses hex with ?? for wildcards.
      final String matchExpr = buildMatchExpression();
      final String valueExpr = buildValueExpression();

      slider.setAdjustValueMatcher(midiIn.createAbsoluteValueMatcher(matchExpr, valueExpr, 128));
   }

   /**
    * Build SysEx match expression for incoming slider messages.
    * Uses Bitwig's MIDI expression syntax with ?? for the value byte.
    */
   private String buildMatchExpression()
   {
      final StringBuilder sb = new StringBuilder();
      for (final int b : SysExConstants.SCRIPT_SYSEX_PREFIX)
      {
         if (sb.length() > 0) sb.append(' ');
         sb.append(String.format("%02X", b));
      }
      sb.append(String.format(" %02X", SysExConstants.SLIDER_PREFIX));
      sb.append(String.format(" %02X", sliderId));
      sb.append(" ??"); // value wildcard
      sb.append(" F7");
      return sb.toString();
   }

   /**
    * Build value extraction expression.
    * References the wildcard byte position in the match expression.
    */
   private String buildValueExpression()
   {
      // The value is at the position of the ?? wildcard.
      // In Bitwig's MIDI expression syntax, this is referenced as the first matched group.
      return buildMatchExpression();
   }

   public HardwareSlider getSlider()
   {
      return slider;
   }

   public int getSliderId()
   {
      return sliderId;
   }

   /**
    * Send the current value to the device (for bidirectional sync).
    * Guards against feedback loops.
    * @param value 0-127
    */
   public void sendValue(final int value)
   {
      if (value != lastSentValue)
      {
         lastSentValue = value;
         protocol.sendSliderValue(sliderId, value);
      }
   }

   /** Reset the feedback guard (call when receiving value from device). */
   public void resetLastSent()
   {
      lastSentValue = -1;
   }
}
