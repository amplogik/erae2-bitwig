package com.erae2bitwig.layer;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.core.TouchEvent;
import com.erae2bitwig.pages.*;
import com.erae2bitwig.sysex.LowLevelApi;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages page switching and routes touch events to the active page.
 */
public class PageManager
{
   private final ControllerHost host;
   private final LowLevelApi api;
   private final Map<PageId, EraePage> pages = new EnumMap<>(PageId.class);
   private PageId activePageId = null;

   public PageManager(final ControllerHost host,
                      final Erae2MidiPorts midiPorts,
                      final LowLevelApi api,
                      final BitwigModel model)
   {
      this.host = host;
      this.api = api;

      // Create all pages
      pages.put(PageId.DAW_CONTROL, new DawControlPage(host, api, model));
      pages.put(PageId.MPE_PLAY, new MpePage(host, api, model));
      pages.put(PageId.INSTRUMENT_CONTROL, new InstrumentPage(host, api, model));
      pages.put(PageId.DRUM_PADS, new DrumPadPage(host, api, model));
      pages.put(PageId.MIXER_DETAIL, new MixerDetailPage(host, api, model));
      pages.put(PageId.STEP_SEQUENCER, new StepSequencerPage(host, api, model));
      pages.put(PageId.XY_PERFORMANCE, new XyPerformancePage(host, api, model));
      pages.put(PageId.LARGE_CLIP_LAUNCHER, new LargeClipLauncherPage(host, api, model));

      // Set up bindings for all pages
      for (final EraePage page : pages.values())
      {
         page.setupBindings();
      }
   }

   /** Activate the default page (DAW Control). */
   public void activateDefaultPage()
   {
      activatePage(PageId.DAW_CONTROL);
   }

   /** Activate a specific page, deactivating the current one. */
   public void activatePage(final PageId pageId)
   {
      if (pageId == activePageId) return;

      if (activePageId != null)
      {
         final EraePage currentPage = pages.get(activePageId);
         if (currentPage != null)
         {
            currentPage.onDeactivate();
         }
      }

      activePageId = pageId;
      final EraePage newPage = pages.get(pageId);
      if (newPage != null)
      {
         newPage.onActivate();
         host.println("Erae Touch 2: Switched to page " + pageId.name());
      }
   }

   /** Route a touch event to the active page. */
   public void handleTouchEvent(final TouchEvent event)
   {
      if (activePageId != null)
      {
         final EraePage page = pages.get(activePageId);
         if (page != null)
         {
            page.handleTouchEvent(event);
         }
      }
   }

   /** Refresh the currently active page. */
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

   public PageId getActivePageId() { return activePageId; }
}
