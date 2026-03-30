package com.erae2bitwig.sysex;

/**
 * All SysEx protocol constants for Erae Touch 2 communication.
 * Derived from the Ableton script consts.py and Erae 2 API docs.
 */
public final class SysExConstants
{
   private SysExConstants() {}

   // --- Protocol A: Script Protocol ---

   /** Outbound prefix: extension -> device UI */
   public static final int[] ERAE_SYSEX_PREFIX = {
      0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x02
   };

   /** Inbound prefix: device UI -> extension */
   public static final int[] SCRIPT_SYSEX_PREFIX = {
      0xF0, 0x00, 0x21, 0x50, 0x00, 0x03, 0x00, 0x01, 0x20
   };

   /** Universal MIDI identity request */
   public static final int[] IDENTITY_REQUEST = {
      0xF0, 0x7E, 0x7F, 0x06, 0x01, 0xF7
   };

   /** Product ID bytes for Erae 2 identification */
   public static final int[] PRODUCT_ID_BYTES = {
      0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02
   };

   // --- Message type bytes (appended after prefix) ---
   public static final int ACTION_BUTTON_PREFIX = 1;
   public static final int MATRIX_BUTTON_PREFIX = 2;
   public static final int SLIDER_PREFIX = 15;
   public static final int WINDOW_BYTE = 38;
   public static final int UPDATE_BYTE = 44;
   public static final int ZOOM_BYTE = 34;
   public static final int MODE_BYTE = 35;
   public static final int POSITION_BYTE = 36;
   public static final int STATE_BYTE = 37;
   public static final int REQUEST_STATE_BYTE = 3;
   public static final int QUIT_BYTE = 64;

   // --- LED styles ---
   public static final int LED_SOLID = 0;
   public static final int LED_BLINK = 1;
   public static final int LED_PULSE = 2;

   // --- MIDI channels used by the Erae script protocol ---
   public static final int OTHER_BUTTON_CHANNEL = 10;
   public static final int SCENE_MATRIX_CHANNEL = 13;

   // --- Default session dimensions ---
   public static final int DEFAULT_SESSION_WIDTH = 12;
   public static final int DEFAULT_SESSION_HEIGHT = 10;
   public static final int MATRIX_COLUMN_STRIDE = 12;

   // --- Button IDs (matching Ableton script) ---
   public static final int BUTTON_UP = 100;
   public static final int BUTTON_DOWN = 101;
   public static final int BUTTON_LEFT = 102;
   public static final int BUTTON_RIGHT = 103;
   public static final int BUTTON_STOP = 90;
   public static final int BUTTON_PLAY = 91;
   public static final int BUTTON_STOP_ALL = 63;

   public static final int MUTE_OFFSET = 21;
   public static final int SOLO_OFFSET = 9;
   public static final int ARM_OFFSET = 33;

   // --- Protocol B: Low-Level API (Lab port) ---

   /** API header for low-level commands */
   public static final int[] API_HEADER = {
      0xF0, 0x00, 0x21, 0x50, 0x00, 0x01, 0x00, 0x02, 0x01, 0x01, 0x04
   };

   public static final int API_MODE_ENABLE = 0x01;
   public static final int API_MODE_DISABLE = 0x02;
   public static final int ZONE_BOUNDARY_REQUEST = 0x10;
   public static final int CLEAR_ZONE = 0x20;
   public static final int DRAW_PIXEL = 0x21;
   public static final int DRAW_RECTANGLE = 0x22;
   public static final int DRAW_IMAGE = 0x23;
   public static final int API_VERSION_REQUEST = 0x7F;

   /** Receiver prefix for API replies */
   public static final int[] API_RECEIVER_PREFIX = {0x01, 0x02, 0x03};

   // --- Page switching ---
   /** CC numbers for page switching (CC 102-109 on ch16) */
   public static final int PAGE_CC_BASE = 102;
   public static final int PAGE_SWITCH_CHANNEL = 15; // 0-indexed, so ch16 = 15

   // --- SysEx terminator ---
   public static final int SYSEX_END = 0xF7;

   // --- Utility: build hex string for SysEx sending ---
   public static String toHexString(final int[] bytes)
   {
      final StringBuilder sb = new StringBuilder();
      for (final int b : bytes)
      {
         if (sb.length() > 0) sb.append(' ');
         sb.append(String.format("%02X", b & 0xFF));
      }
      return sb.toString();
   }
}
