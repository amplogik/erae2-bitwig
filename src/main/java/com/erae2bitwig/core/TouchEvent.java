package com.erae2bitwig.core;

/**
 * Represents a touch event from the Erae Touch 2 FingerStream.
 * Coordinates are normalized 0.0-1.0, origin bottom-left.
 */
public class TouchEvent
{
   public static final int ACTION_PRESS = 0;
   public static final int ACTION_MOVE = 1;
   public static final int ACTION_RELEASE = 2;

   private final int fingerIndex;
   private final int zoneId;
   private final int action;
   private final float x;
   private final float y;
   private final float z;

   public TouchEvent(final int fingerIndex, final int zoneId, final int action,
                     final float x, final float y, final float z)
   {
      this.fingerIndex = fingerIndex;
      this.zoneId = zoneId;
      this.action = action;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public int getFingerIndex() { return fingerIndex; }
   public int getZoneId() { return zoneId; }
   public int getAction() { return action; }
   public float getX() { return x; }
   public float getY() { return y; }
   public float getZ() { return z; }

   public boolean isPress() { return action == ACTION_PRESS; }
   public boolean isMove() { return action == ACTION_MOVE; }
   public boolean isRelease() { return action == ACTION_RELEASE; }

   /** X coordinate as pixel column (0-41). Coordinates arrive in zone pixel units. */
   public int pixelX() { return Math.min(41, Math.max(0, (int) x)); }

   /** Y coordinate as pixel row (0-23). Coordinates arrive in zone pixel units. */
   public int pixelY() { return Math.min(23, Math.max(0, (int) y)); }

   @Override
   public String toString()
   {
      final String actionStr = switch (action)
      {
         case ACTION_PRESS -> "PRESS";
         case ACTION_MOVE -> "MOVE";
         case ACTION_RELEASE -> "RELEASE";
         default -> "UNKNOWN(" + action + ")";
      };
      return String.format("Touch[finger=%d zone=%d %s x=%.3f y=%.3f z=%.3f px=(%d,%d)]",
         fingerIndex, zoneId, actionStr, x, y, z, pixelX(), pixelY());
   }
}
