package com.erae2bitwig.hardware;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;

import com.erae2bitwig.sysex.LedState;
import com.erae2bitwig.sysex.ScriptProtocol;

/**
 * A button on the Erae Touch 2 that communicates via SysEx.
 * Input is dispatched via the SysEx callback handler, not Bitwig's MIDI matchers.
 * LED output is sent directly via ScriptProtocol.
 */
public class EraeButton
{
   private final MultiStateHardwareLight light;
   private final int buttonType;
   private final int buttonId;
   private final ScriptProtocol protocol;

   private LedState currentState = LedState.OFF;
   private Runnable pressHandler;
   private Runnable releaseHandler;

   public EraeButton(final ControllerHost host,
                     final HardwareSurface surface,
                     final ScriptProtocol protocol,
                     final int buttonType,
                     final int buttonId,
                     final String name)
   {
      this.buttonType = buttonType;
      this.buttonId = buttonId;
      this.protocol = protocol;

      light = surface.createMultiStateHardwareLight(name + "_light");

      // LED output: when Bitwig wants to update the light, send SysEx
      light.state().onUpdateHardware((InternalHardwareLightState state) ->
      {
         if (state instanceof LedState ledState)
         {
            updateLed(ledState);
         }
      });
   }

   /** Register a callback for button press events. */
   public void onPressed(final Runnable handler)
   {
      this.pressHandler = handler;
   }

   /** Register a callback for button release events. */
   public void onReleased(final Runnable handler)
   {
      this.releaseHandler = handler;
   }

   /** Called by the SysEx dispatcher when this button is pressed. */
   public void handlePress()
   {
      if (pressHandler != null)
      {
         pressHandler.run();
      }
   }

   /** Called by the SysEx dispatcher when this button is released. */
   public void handleRelease()
   {
      if (releaseHandler != null)
      {
         releaseHandler.run();
      }
   }

   private void updateLed(final LedState state)
   {
      if (state != null && !state.equals(currentState))
      {
         currentState = state;
         protocol.sendButtonLed(buttonType, buttonId, state.getColorIndex(), state.getLedStyle());
      }
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

   /** Set the LED state directly. */
   public void setLedState(final LedState state)
   {
      light.state().setValue(state);
   }
}
