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
 * Owns all pages, initializes them on startup, and routes incoming touch
 * events to the page whose Erae API zone the touch came from.
 *
 * Each page draws to its own dedicated zone (one per Erae layout); the
 * physical Erae layout the user has selected on the hardware decides which
 * page they see. We have no way to detect hardware layout switches, so all
 * pages stay live simultaneously.
 */
public class PageManager
{
   private final ControllerHost host;
   private final LowLevelApi api;
   private final Map<PageId, EraePage> pages = new EnumMap<>(PageId.class);

   public PageManager(final ControllerHost host,
                      final Erae2MidiPorts midiPorts,
                      final LowLevelApi api,
                      final BitwigModel model)
   {
      this.host = host;
      this.api = api;

      // Create all pages. Layouts 6, 7, 8 (zones 5, 6, 7) are user-defined and
      // intentionally have no extension-managed page.
      pages.put(PageId.DAW_CONTROL, new DawControlPage(host, api, model));
      pages.put(PageId.LARGE_CLIP_LAUNCHER, new LargeClipLauncherPage(host, api, model));
      pages.put(PageId.INSTRUMENT_CONTROL, new InstrumentPage(host, api, model, midiPorts));
      pages.put(PageId.DRUM_PADS, new DrumPadPage(host, api, model, midiPorts));
      pages.put(PageId.MPE_PLAY, new MpePage(host, api, model, midiPorts));

      // Set up bindings for all pages
      for (final EraePage page : pages.values())
      {
         page.setupBindings();
      }
   }

   /**
    * Activate every page so each draws to its dedicated zone. We stagger
    * the activations so each page's burst of draw SysEx has time to flush
    * to the device before the next page starts — sending all pages back-to-back
    * overflows the USB MIDI buffer and later pages end up partially or fully
    * dropped.
    */
   public void initializeAllPages()
   {
      int delayMs = 0;
      for (final PageId pageId : PageId.values())
      {
         final EraePage page = pages.get(pageId);
         if (page == null) continue;
         final int d = delayMs;
         host.scheduleTask(() ->
         {
            host.println("Erae Touch 2: Initializing page " + pageId.name() +
               " on zone " + pageId.getZoneId());
            page.onActivate();
         }, d);
         delayMs += 250;
      }
   }

   /** Route a touch event to the page whose zone it came from. */
   public void handleTouchEvent(final TouchEvent event)
   {
      final PageId pageId = PageId.fromZoneId(event.getZoneId());
      if (pageId == null) return;
      final EraePage page = pages.get(pageId);
      if (page != null)
      {
         page.handleTouchEvent(event);
      }
   }
}
