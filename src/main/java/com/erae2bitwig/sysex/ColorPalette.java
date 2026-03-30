package com.erae2bitwig.sysex;

/**
 * 128-color palette matching the Erae Touch 2 firmware.
 * Ported from the Ableton script Colors.py RGB_COLOR_TABLE.
 */
public final class ColorPalette
{
   private ColorPalette() {}

   /** Named color indices for common use. */
   public static final int BLACK = 0;
   public static final int DARK_GREY = 1;
   public static final int GREY = 2;
   public static final int WHITE = 3;
   public static final int RED = 5;
   public static final int DARK_RED = 6;
   public static final int ORANGE = 9;
   public static final int YELLOW = 13;
   public static final int GREEN = 21;
   public static final int DARK_GREEN = 18;
   public static final int CYAN = 33;
   public static final int BLUE = 45;
   public static final int PURPLE = 49;
   public static final int MAGENTA = 53;
   public static final int RED_HALF = 7;
   public static final int GREEN_HALF = 23;
   public static final int BLUE_HALF = 47;
   public static final int YELLOW_HALF = 15;
   public static final int DARK_ORANGE_2 = 84;

   /**
    * RGB color table: index -> 0xRRGGBB.
    * All 128 entries from the Erae firmware palette.
    */
   public static final int[] RGB_TABLE = {
      0x000000, // 0: Black
      0x1E0E3E, // 1: Dark Purple
      0x7F7F7F, // 2: Gray
      0xFFFFFF, // 3: White
      0xFEBEDC, // 4: Pink
      0xFF0000, // 5: Red
      0x590000, // 6: Dark Red
      0x190000, // 7: Very Dark Red
      0xFFE5AC, // 8: Peach
      0xFFCC00, // 9: Orange
      0x590800, // 10: Dark Orange
      0x271800, // 11: Very Dark Orange
      0xFFFFEC, // 12: Light Yellow
      0xFFFF00, // 13: Yellow
      0x591F00, // 14: Dark Yellow
      0x191000, // 15: Very Dark Yellow
      0x88E18C, // 16: Light Green
      0x550000, // 17
      0x1D5900, // 18: Green
      0x145000, // 19: Dark Green
      0x4D4D3C, // 20: Olive
      0x00FF00, // 21: Green
      0x005900, // 22: Dark Green
      0x001900, // 23: Very Dark Green
      0x4D4D2E, // 24: Dark Olive
      0x00FF49, // 25: Green-Cyan
      0x005928, // 26
      0x001912, // 27
      0x4D4D48, // 28: Gray-Green
      0x00FF95, // 29: Mint
      0x00592D, // 30
      0x001F1A, // 31
      0x4D4D67, // 32: Gray-Blue
      0x00FFD9, // 33: Cyan
      0x005955, // 34
      0x001918, // 35
      0x4D42FF, // 36: Light Blue
      0x00A9FF, // 37: Light Blue
      0x004176, // 38
      0x001029, // 39
      0x4D1AFF, // 40
      0x0056FF, // 41: Bright Blue
      0x001D75, // 42
      0x00081D, // 43
      0x4D004F, // 44
      0x0000FF, // 45: Blue
      0x000059, // 46: Dark Blue
      0x000019, // 47: Very Dark Blue
      0x8767FF, // 48: Purple-Blue
      0x5400FF, // 49: Purple
      0x190064, // 50
      0x0F0030, // 51
      0xFEBED7, // 52: Light Pink
      0xFF00FF, // 53: Magenta
      0x590059, // 54: Dark Magenta
      0x190019, // 55
      0xFEBEA7, // 56
      0xFF00F4, // 57: Magenta-Red
      0x59004D, // 58
      0x220073, // 59
      0xFF0080, // 60: Red-Magenta
      0x990000, // 61
      0x790000, // 62
      0x430000, // 63
      0x033C00, // 64
      0x00FF47, // 65: Green
      0x0050FF, // 66: Blue
      0x0000FF, // 67: Bright Blue
      0x451F00, // 68: Brown
      0x250AAC, // 69: Blue-Purple
      0x7F7F7F, // 70: Gray
      0x20AA00, // 71: Green
      0xFF0000, // 72: Red
      0xBEAA6D, // 73: Tan
      0xB0A686, // 74
      0x650919, // 75
      0x108080, // 76: Teal
      0xFFE7F7, // 77: Light Pink
      0x00A9FF, // 78: Light Blue
      0x002B87, // 79
      0x3DFEFF, // 80: Light Cyan
      0x79FFFF, // 81: Bright Cyan
      0xB2310D, // 82
      0x400800, // 83
      0xFF8800, // 84: Orange
      0x88B866, // 85
      0x73D046, // 86: Green-Yellow
      0x00FF00, // 87: Green
      0x3C3A26, // 88
      0x59B391, // 89
      0x38E2CC, // 90: Bright Cyan
      0x5BDFFF, // 91: Light Cyan
      0x313906, // 92
      0x876D99, // 93
      0xD31AFF, // 94: Magenta
      0xFF010D, // 95: Red
      0xFF8000, // 96: Orange
      0xB9A000, // 97: Gold
      0x914900, // 98: Brown
      0x835017, // 99
      0x392300, // 100
      0x145190, // 101: Blue
      0x0D4830, // 102
      0x151D8A, // 103
      0x161A4A, // 104
      0x68D28C, // 105: Light Green
      0xA7D85A, // 106: Yellow-Green
      0xDEE9CD, // 107
      0xD8B76C, // 108: Tan
      0x10100D, // 109
      0x9EB896, // 110
      0x679F8F, // 111: Teal
      0x1E0F10, // 112
      0xDC6A5B, // 113: Rust
      0x810C4D, // 114
      0x9A915F, // 115
      0x8E445F, // 116
      0x408000, // 117
      0x756365, // 118
      0xE0BBFF, // 119: Light Purple
      0xA00000, // 120: Dark Red
      0x348000, // 121
      0x1A5800, // 122
      0x073E00, // 123
      0xB9A000, // 124: Gold
      0x3F3A00, // 125
      0xB32C00, // 126
      0x4B0A2E, // 127
   };

   /**
    * Find the nearest palette color index for an RGB value.
    * Uses Euclidean distance in RGB space.
    */
   public static int findNearestColor(final int r, final int g, final int b)
   {
      int bestIndex = 0;
      int bestDist = Integer.MAX_VALUE;

      for (int i = 0; i < RGB_TABLE.length; i++)
      {
         final int pr = (RGB_TABLE[i] >> 16) & 0xFF;
         final int pg = (RGB_TABLE[i] >> 8) & 0xFF;
         final int pb = RGB_TABLE[i] & 0xFF;

         final int dr = r - pr;
         final int dg = g - pg;
         final int db = b - pb;
         final int dist = dr * dr + dg * dg + db * db;

         if (dist < bestDist)
         {
            bestDist = dist;
            bestIndex = i;
         }
      }
      return bestIndex;
   }

   /**
    * Find the nearest palette color index for a Bitwig Color.
    * @param red 0.0-1.0
    * @param green 0.0-1.0
    * @param blue 0.0-1.0
    */
   public static int findNearestColor(final double red, final double green, final double blue)
   {
      return findNearestColor(
         (int) (red * 255),
         (int) (green * 255),
         (int) (blue * 255));
   }
}
