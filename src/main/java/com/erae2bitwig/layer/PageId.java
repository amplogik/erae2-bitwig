package com.erae2bitwig.layer;

import com.erae2bitwig.sysex.SysExConstants;

/**
 * Identifies each of the 8 Erae Touch 2 pages.
 * Each page maps to a CC number (102-109) on MIDI channel 16
 * for page switching.
 */
public enum PageId
{
   MPE_PLAY(0, SysExConstants.PAGE_CC_BASE),
   DAW_CONTROL(1, SysExConstants.PAGE_CC_BASE + 1),
   INSTRUMENT_CONTROL(2, SysExConstants.PAGE_CC_BASE + 2),
   DRUM_PADS(3, SysExConstants.PAGE_CC_BASE + 3),
   MIXER_DETAIL(4, SysExConstants.PAGE_CC_BASE + 4),
   STEP_SEQUENCER(5, SysExConstants.PAGE_CC_BASE + 5),
   XY_PERFORMANCE(6, SysExConstants.PAGE_CC_BASE + 6),
   LARGE_CLIP_LAUNCHER(7, SysExConstants.PAGE_CC_BASE + 7);

   private final int index;
   private final int cc;

   PageId(final int index, final int cc)
   {
      this.index = index;
      this.cc = cc;
   }

   public int getIndex() { return index; }
   public int getCc() { return cc; }

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
}
