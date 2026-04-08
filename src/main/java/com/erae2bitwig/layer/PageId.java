package com.erae2bitwig.layer;

import com.erae2bitwig.sysex.SysExConstants;

/**
 * Identifies each Erae Touch 2 page that the extension manages. Each page
 * draws to its own Erae API zone (zone IDs 0..4), and the user switches
 * between them by changing layouts on the Erae hardware. The mapping is:
 *
 *   PageId index = Erae API zone ID = Erae hardware layout (1-indexed minus 1)
 *
 * Layouts 6, 7, and 8 (zones 5, 6, 7) are USER DEFINED — the extension does
 * not draw to or react to them; users configure those layouts themselves in
 * Erae Lab.
 */
public enum PageId
{
   DAW_CONTROL(0, SysExConstants.PAGE_CC_BASE),
   LARGE_CLIP_LAUNCHER(1, SysExConstants.PAGE_CC_BASE + 1),
   INSTRUMENT_CONTROL(2, SysExConstants.PAGE_CC_BASE + 2),
   DRUM_PADS(3, SysExConstants.PAGE_CC_BASE + 3),
   MPE_PLAY(4, SysExConstants.PAGE_CC_BASE + 4);

   private final int index;
   private final int cc;

   PageId(final int index, final int cc)
   {
      this.index = index;
      this.cc = cc;
   }

   public int getIndex() { return index; }
   public int getCc() { return cc; }
   /** The Erae API zone ID this page draws to. Equal to index. */
   public int getZoneId() { return index; }

   /** Find a PageId by CC number, or null if no match. */
   public static PageId fromCc(final int cc)
   {
      for (final PageId page : values())
      {
         if (page.cc == cc) return page;
      }
      return null;
   }

   /** Find a PageId by index (0-7), or null if out of range. */
   public static PageId fromIndex(final int index)
   {
      for (final PageId page : values())
      {
         if (page.index == index) return page;
      }
      return null;
   }

   /** Find a PageId by Erae API zone ID, or null if no match. */
   public static PageId fromZoneId(final int zoneId)
   {
      return fromIndex(zoneId);
   }
}
