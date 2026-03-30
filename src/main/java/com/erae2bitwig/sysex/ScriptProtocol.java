package com.erae2bitwig.sysex;

import com.erae2bitwig.core.Erae2MidiPorts;

/**
 * Protocol A: Script Protocol for button/slider/LED communication.
 * All messages use the ERAE_SYSEX_PREFIX for outbound and
 * SCRIPT_SYSEX_PREFIX for inbound.
 */
public class ScriptProtocol
{
   private final Erae2MidiPorts ports;

   public ScriptProtocol(final Erae2MidiPorts ports)
   {
      this.ports = ports;
   }

   /** Send universal MIDI identity request to initiate handshake. */
   public void sendIdentityRequest()
   {
      ports.sendMainSysEx(SysExConstants.toHexString(SysExConstants.IDENTITY_REQUEST));
   }

   /** Request device state after identification. */
   public void sendRequestState()
   {
      final int[] msg = appendToPrefix(SysExConstants.REQUEST_STATE_BYTE);
      ports.sendMainSysEx(SysExConstants.toHexString(msg));
   }

   /** Send quit/disconnect message. */
   public void sendQuitMessage()
   {
      final int[] msg = appendToPrefix(SysExConstants.QUIT_BYTE);
      ports.sendMainSysEx(SysExConstants.toHexString(msg));
   }

   /**
    * Send LED color update for a button.
    * @param buttonType ACTION_BUTTON_PREFIX (1) or MATRIX_BUTTON_PREFIX (2)
    * @param buttonId the button identifier
    * @param colorIndex color palette index (0-127)
    * @param ledStyle LED_SOLID (0), LED_BLINK (1), or LED_PULSE (2)
    */
   public void sendButtonLed(final int buttonType, final int buttonId,
                             final int colorIndex, final int ledStyle)
   {
      final int[] msg;
      if (ledStyle == SysExConstants.LED_SOLID)
      {
         msg = new int[]{
            0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x02,
            buttonType, buttonId, colorIndex, 0xF7
         };
      }
      else
      {
         msg = new int[]{
            0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x02,
            buttonType, buttonId, ledStyle, colorIndex, 0xF7
         };
      }
      ports.sendMainSysEx(SysExConstants.toHexString(msg));
   }

   /**
    * Send slider/fader position value.
    * @param sliderId 0-11 volume, 12-23 send1, 24-35 send2
    * @param value 0-127
    */
   public void sendSliderValue(final int sliderId, final int value)
   {
      final int[] msg = {
         0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x02,
         SysExConstants.SLIDER_PREFIX, sliderId, value, 0xF7
      };
      ports.sendMainSysEx(SysExConstants.toHexString(msg));
   }

   private int[] appendToPrefix(final int... extraBytes)
   {
      final int[] prefix = SysExConstants.ERAE_SYSEX_PREFIX;
      final int[] result = new int[prefix.length + extraBytes.length + 1];
      System.arraycopy(prefix, 0, result, 0, prefix.length);
      System.arraycopy(extraBytes, 0, result, prefix.length, extraBytes.length);
      result[result.length - 1] = SysExConstants.SYSEX_END;
      return result;
   }
}
