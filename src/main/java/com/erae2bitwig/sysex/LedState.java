package com.erae2bitwig.sysex;

import com.bitwig.extension.controller.api.InternalHardwareLightState;

import java.util.Objects;

/**
 * Represents the visual state of an LED on the Erae Touch 2.
 * Combines a color palette index with a display style (solid/blink/pulse).
 */
public class LedState extends InternalHardwareLightState
{
   private final int colorIndex;
   private final int ledStyle;

   public static final LedState OFF = new LedState(ColorPalette.BLACK, SysExConstants.LED_SOLID);

   public LedState(final int colorIndex, final int ledStyle)
   {
      this.colorIndex = colorIndex;
      this.ledStyle = ledStyle;
   }

   public static LedState solid(final int colorIndex)
   {
      return new LedState(colorIndex, SysExConstants.LED_SOLID);
   }

   public static LedState blink(final int colorIndex)
   {
      return new LedState(colorIndex, SysExConstants.LED_BLINK);
   }

   public static LedState pulse(final int colorIndex)
   {
      return new LedState(colorIndex, SysExConstants.LED_PULSE);
   }

   public int getColorIndex()
   {
      return colorIndex;
   }

   public int getLedStyle()
   {
      return ledStyle;
   }

   @Override
   public com.bitwig.extension.controller.api.HardwareLightVisualState getVisualState()
   {
      return null;
   }

   @Override
   public boolean equals(final Object o)
   {
      if (this == o) return true;
      if (!(o instanceof LedState other)) return false;
      return colorIndex == other.colorIndex && ledStyle == other.ledStyle;
   }

   @Override
   public int hashCode()
   {
      return Objects.hash(colorIndex, ledStyle);
   }
}
