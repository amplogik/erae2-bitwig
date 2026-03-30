package com.erae2bitwig.layer;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.pages.*;
import com.erae2bitwig.sysex.ScriptProtocol;
import com.erae2bitwig.sysex.SysExConstants;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages page switching and routes events to the active page.
 * Listens for CC 102-109 on channel 16 for page switch triggers.
 */
public class PageManager
{
   private final ControllerHost host;
   private final Map<PageId, EraePage> pages = new EnumMap<>(PageId.class);
   private PageId activePageId = null;

   public PageManager(final ControllerHost host,
                      final Erae2MidiPorts midiPorts,
                      final ScriptProtocol protocol,
                      final BitwigModel model,
                      final HardwareElements hardware)
   {
      this.host = host;

      // Create all pages
      pages.put(PageId.MPE_PLAY, new MpePage(host, protocol, model, hardware));
      pages.put(PageId.DAW_CONTROL, new DawControlPage(host, protocol, model, hardware));
      pages.put(PageId.INSTRUMENT_CONTROL, new InstrumentPage(host, protocol, model, hardware));
      pages.put(PageId.DRUM_PADS, new DrumPadPage(host, protocol, model, hardware));
      pages.put(PageId.MIXER_DETAIL, new MixerDetailPage(host, protocol, model, hardware));
      pages.put(PageId.STEP_SEQUENCER, new StepSequencerPage(host, protocol, model, hardware));
      pages.put(PageId.XY_PERFORMANCE, new XyPerformancePage(host, protocol, model, hardware));
      pages.put(PageId.LARGE_CLIP_LAUNCHER, new LargeClipLauncherPage(host, protocol, model, hardware));

      // Set up bindings for all pages
      for (final EraePage page : pages.values())
      {
         page.setupBindings();
      }

      // Listen for page-switch CC on Main MIDI port (CC 102-109, ch16)
      midiPorts.setMainMidiCallback((status, data1, data2) ->
      {
         final int channel = status & 0x0F;
         final int messageType = status & 0xF0;

         // CC on channel 16 (0-indexed = 15)
         if (messageType == 0xB0 && channel == SysExConstants.PAGE_SWITCH_CHANNEL && data2 == 127)
         {
            final PageId targetPage = PageId.fromCc(data1);
            if (targetPage != null)
            {
               activatePage(targetPage);
            }
         }
      });
   }

   /** Activate a specific page, deactivating the current one. */
   public void activatePage(final PageId pageId)
   {
      if (pageId == activePageId) return;

      // Deactivate current page
      if (activePageId != null)
      {
         final EraePage currentPage = pages.get(activePageId);
         if (currentPage != null)
         {
            currentPage.onDeactivate();
         }
      }

      // Activate new page
      activePageId = pageId;
      final EraePage newPage = pages.get(pageId);
      if (newPage != null)
      {
         newPage.onActivate();
         host.println("Erae Touch 2: Switched to page " + pageId.name());
      }
   }

   /** Refresh the currently active page (full LED state resend). */
   public void refreshActivePage()
   {
      if (activePageId != null)
      {
         final EraePage page = pages.get(activePageId);
         if (page != null)
         {
            page.refresh();
         }
      }
   }

   /** Route a fader value to the active page. */
   public void handleFaderValue(final int target, final int value)
   {
      if (activePageId != null)
      {
         final EraePage page = pages.get(activePageId);
         if (page != null)
         {
            page.handleFaderValue(target, value);
         }
      }
   }

   public PageId getActivePageId() { return activePageId; }
}
