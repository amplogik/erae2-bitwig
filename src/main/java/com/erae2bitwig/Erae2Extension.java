package com.erae2bitwig;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.core.TouchEvent;
import com.erae2bitwig.layer.PageManager;
import com.erae2bitwig.sysex.FingerStreamParser;
import com.erae2bitwig.sysex.LowLevelApi;

public class Erae2Extension extends ControllerExtension
{
   /** Layout slot on the Erae where the API Zone layout is stored (0-indexed). */
   private static final int API_LAYOUT_INDEX = 7; // Layout 8

   private Erae2MidiPorts midiPorts;
   private LowLevelApi api;
   private FingerStreamParser fingerStreamParser;
   private BitwigModel model;
   private PageManager pageManager;

   protected Erae2Extension(final Erae2ExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      host.println("Erae Touch 2: Initializing...");

      // Set up MIDI ports
      midiPorts = new Erae2MidiPorts(host);

      // Set up Low-Level API
      api = new LowLevelApi(midiPorts);

      // Set up FingerStream parser
      fingerStreamParser = new FingerStreamParser();
      fingerStreamParser.setLogger(host::println);
      fingerStreamParser.setTouchHandler(this::handleTouch);
      fingerStreamParser.setZoneBoundaryHandler((zoneId, width, height) ->
         host.println("Erae Touch 2: Zone " + zoneId + " boundary: " + width + "x" + height));

      // Register SysEx handler for incoming API messages
      midiPorts.setScriptSysExHandler(this::handleSysEx);

      // Create Bitwig model (track bank, transport, etc.)
      model = new BitwigModel(host);

      // Set up page manager and all pages
      pageManager = new PageManager(host, midiPorts, api, model);

      // Switch to the API layout and enable API mode
      host.println("Erae Touch 2: Switching to API layout " + (API_LAYOUT_INDEX + 1) + "...");
      api.sendLayoutSwitch(API_LAYOUT_INDEX);

      // Schedule API mode enable slightly after layout switch
      host.scheduleTask(() ->
      {
         host.println("Erae Touch 2: Enabling API mode...");
         api.enableApiMode();

         // Request zone boundary to confirm connectivity
         api.requestZoneBoundary(0);

         // Activate the default page
         pageManager.activateDefaultPage();
      }, 500);

      host.println("Erae Touch 2: Initialization complete.");
      host.showPopupNotification("Erae Touch 2: Extension loaded");
   }

   private void handleSysEx(final String data)
   {
      if (!fingerStreamParser.parse(data))
      {
         getHost().println("Erae Touch 2: Unknown SysEx [" + data.length() + " chars]: " +
            (data.length() > 60 ? data.substring(0, 60) + "..." : data));
      }
   }

   private void handleTouch(final TouchEvent event)
   {
      // Log press/release events (skip move to avoid flooding)
      if (!event.isMove())
      {
         getHost().println("Erae Touch 2: " + event);
      }

      // Route to active page
      pageManager.handleTouchEvent(event);
   }

   @Override
   public void exit()
   {
      if (api != null)
      {
         api.clearZone(0);
         api.disableApiMode();
      }
      getHost().println("Erae Touch 2: Extension exited.");
   }

   @Override
   public void flush()
   {
      // No hardware surface to flush in API mode
   }
}
