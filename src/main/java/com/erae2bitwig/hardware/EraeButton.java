package com.erae2bitwig.hardware;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

import com.erae2bitwig.sysex.LedState;
import com.erae2bitwig.sysex.ScriptProtocol;
import com.erae2bitwig.sysex.SysExConstants;

/**
 * A button on the Erae Touch 2 that communicates via SysEx.
 * Wraps a Bitwig HardwareButton with SysEx input matching and LED output.
 */
public class EraeButton
{
   private final HardwareButton button;
   private final MultiStateHardwareLight light;
   private final int buttonType;
   private final int buttonId;
   private final ScriptProtocol protocol;

   private LedState currentState = LedState.OFF;

   public EraeButton(final ControllerHost host,
                     final HardwareSurface surface,
                     final MidiIn midiIn,
                     final ScriptProtocol protocol,
                     final int buttonType,
                     final int buttonId,
                     final String name)
   {
      this.buttonType = buttonType;
      this.buttonId = buttonId;
      this.protocol = protocol;

      button = surface.createHardwareButton(name);
      light = surface.createMultiStateHardwareLight(name + "_light");

      // Match incoming SysEx for button press/release using Bitwig's expression matcher.
      // Format: "F0 <prefix bytes> <type> <id> <value> F7"
      // createActionMatcher takes a MIDI expression string.
      final String pressPattern = buildSysExExpression(1);
      final String releasePattern = buildSysExExpression(0);

      button.pressedAction().setActionMatcher(midiIn.createActionMatcher(pressPattern));
      button.releasedAction().setActionMatcher(midiIn.createActionMatcher(releasePattern));

      // LED output: when Bitwig wants to update the light, send SysEx
      light.state().onUpdateHardware((InternalHardwareLightState state) ->
      {
         if (state instanceof LedState ledState)
         {
            updateLed(ledState);
         }
      });

      button.setBackgroundLight(light);
   }

   /**
    * Build a SysEx MIDI expression for action matching.
    * Bitwig's createActionMatcher expects a hex string like "F0 00 21 50 ... F7"
    */
   private String buildSysExExpression(final int value)
   {
      final StringBuilder sb = new StringBuilder();
      for (final int b : SysExConstants.SCRIPT_SYSEX_PREFIX)
      {
         if (sb.length() > 0) sb.append(' ');
         sb.append(String.format("%02X", b));
      }
      sb.append(String.format(" %02X", buttonType));
      sb.append(String.format(" %02X", buttonId));
      sb.append(String.format(" %02X", value));
      sb.append(" F7");
      return sb.toString();
   }

   private void updateLed(final LedState state)
   {
      if (state != null && !state.equals(currentState))
      {
         currentState = state;
         protocol.sendButtonLed(buttonType, buttonId, state.getColorIndex(), state.getLedStyle());
      }
   }

   public HardwareButton getButton()
   {
      return button;
   }

   public MultiStateHardwareLight getLight()
   {
      return light;
   }

   public int getButtonType()
   {
      return buttonType;
   }

   public int getButtonId()
   {
      return buttonId;
   }

   /** Set the LED state directly (for use outside of binding system). */
   public void setLedState(final LedState state)
   {
      light.state().setValue(state);
   }
}
