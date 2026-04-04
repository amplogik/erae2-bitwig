package com.erae2bitwig.core;

import java.util.function.Consumer;

/**
 * A rectangular touch region on the Erae Touch 2 surface.
 * Defined in both normalized (0.0-1.0) and pixel (0-41, 0-23) coordinates.
 */
public class TouchZone
{
   public static final int SURFACE_WIDTH = 42;
   public static final int SURFACE_HEIGHT = 24;

   private final String name;
   private final int pixelX;
   private final int pixelY;
   private final int pixelW;
   private final int pixelH;

   // Current display color (7-bit RGB, 0-127)
   private int colorR;
   private int colorG;
   private int colorB;

   // Touch callbacks
   private Consumer<TouchEvent> onPress;
   private Consumer<TouchEvent> onMove;
   private Consumer<TouchEvent> onRelease;

   public TouchZone(final String name, final int pixelX, final int pixelY,
                    final int pixelW, final int pixelH)
   {
      this.name = name;
      this.pixelX = pixelX;
      this.pixelY = pixelY;
      this.pixelW = pixelW;
      this.pixelH = pixelH;
   }

   /** Check if a coordinate (in zone pixel units) falls within this zone. */
   public boolean contains(final float x, final float y)
   {
      return x >= pixelX && x < (pixelX + pixelW) &&
             y >= pixelY && y < (pixelY + pixelH);
   }

   public TouchZone onPress(final Consumer<TouchEvent> handler)
   {
      this.onPress = handler;
      return this;
   }

   public TouchZone onMove(final Consumer<TouchEvent> handler)
   {
      this.onMove = handler;
      return this;
   }

   public TouchZone onRelease(final Consumer<TouchEvent> handler)
   {
      this.onRelease = handler;
      return this;
   }

   /** Dispatch a touch event to the appropriate handler. */
   public void handleTouch(final TouchEvent event)
   {
      switch (event.getAction())
      {
         case TouchEvent.ACTION_PRESS:
            if (onPress != null) onPress.accept(event);
            break;
         case TouchEvent.ACTION_MOVE:
            if (onMove != null) onMove.accept(event);
            break;
         case TouchEvent.ACTION_RELEASE:
            if (onRelease != null) onRelease.accept(event);
            break;
      }
   }

   public void setColor(final int r, final int g, final int b)
   {
      this.colorR = r;
      this.colorG = g;
      this.colorB = b;
   }

   public String getName() { return name; }
   public int getPixelX() { return pixelX; }
   public int getPixelY() { return pixelY; }
   public int getPixelW() { return pixelW; }
   public int getPixelH() { return pixelH; }
   public int getColorR() { return colorR; }
   public int getColorG() { return colorG; }
   public int getColorB() { return colorB; }
}
