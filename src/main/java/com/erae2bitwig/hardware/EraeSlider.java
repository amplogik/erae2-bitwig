package com.erae2bitwig.hardware;

import com.erae2bitwig.sysex.ScriptProtocol;

/**
 * A slider/fader on the Erae Touch 2 that communicates via SysEx.
 * Input is dispatched via the SysEx callback handler (handleFaderValue).
 * Outbound value sync is sent directly via ScriptProtocol.
 */
public class EraeSlider
{
   private final int sliderId;
   private final ScriptProtocol protocol;
   private int lastSentValue = -1;

   public EraeSlider(final ScriptProtocol protocol,
                     final int sliderId)
   {
      this.sliderId = sliderId;
      this.protocol = protocol;
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
