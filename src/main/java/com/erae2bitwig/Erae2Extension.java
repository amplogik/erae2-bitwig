package com.erae2bitwig;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.core.TouchEvent;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.layer.PageManager;
import com.erae2bitwig.sysex.FingerStreamParser;
import com.erae2bitwig.sysex.LowLevelApi;

public class Erae2Extension extends ControllerExtension
{
   /** Layout slot to force on startup (0-indexed; 0 = Erae layout 1). */
   private static final int STARTUP_LAYOUT_INDEX = 0;

   /**
    * If true, run zone-calibration mode on startup: fill each of the 8 API
    * zones with a distinct color so the user can identify which zone ID
    * corresponds to which physical Erae layout. Set to false for normal use.
    */
   private static final boolean CALIBRATION_MODE = false;

   /** Distinct colors for each zone in calibration mode (R, G, B; 0..127). */
   private static final int[][] CALIBRATION_COLORS = {
      {127,   0,   0}, // 0: red
      {  0, 127,   0}, // 1: green
      {  0,   0, 127}, // 2: blue
      {127, 127,   0}, // 3: yellow
      {  0, 127, 127}, // 4: cyan
      {127,   0, 127}, // 5: magenta
      {127, 127, 127}, // 6: white
      {127,  60,   0}, // 7: orange
   };

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

      // Register short-MIDI handler for layout-change notifications from the Erae
      midiPorts.setMainMidiCallback(this::handleMainMidi);

      // Create Bitwig model (track bank, transport, etc.)
      model = new BitwigModel(host);

      // Set up page manager and all pages
      pageManager = new PageManager(host, midiPorts, api, model);

      // Force a known startup layout. The Erae is expected to have an API Zone
      // configured on every layout slot (1..8), each mapping to one in-extension PageId.
      host.println("Erae Touch 2: Switching to startup layout " + (STARTUP_LAYOUT_INDEX + 1) + "...");
      api.sendLayoutSwitch(STARTUP_LAYOUT_INDEX);

      // Schedule API mode enable slightly after layout switch
      host.scheduleTask(() ->
      {
         host.println("Erae Touch 2: Enabling API mode...");
         api.enableApiMode();

         if (CALIBRATION_MODE)
         {
            runCalibration();
            return;
         }

         // Initialize every page — each draws to its own zone simultaneously.
         // The user picks which page they see by switching layouts on the Erae.
         pageManager.initializeAllPages();
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

   /**
    * Calibration mode: fill each of the 8 API zones with a distinct color
    * in immediate succession (no layout switching needed). After this runs,
    * the user cycles through Erae layouts 1..8 and confirms each layout
    * displays its expected color.
    */
   private void runCalibration()
   {
      getHost().println("Erae Touch 2: CALIBRATION MODE — drawing all 8 zones");
      getHost().println("  Layout 1 (zone 0) = RED");
      getHost().println("  Layout 2 (zone 1) = GREEN");
      getHost().println("  Layout 3 (zone 2) = BLUE");
      getHost().println("  Layout 4 (zone 3) = YELLOW");
      getHost().println("  Layout 5 (zone 4) = CYAN");
      getHost().println("  Layout 6 (zone 5) = MAGENTA");
      getHost().println("  Layout 7 (zone 6) = WHITE");
      getHost().println("  Layout 8 (zone 7) = ORANGE");

      for (int z = 0; z < 8; z++)
      {
         api.clearZone(z);
         final int[] c = CALIBRATION_COLORS[z];
         api.drawRectangle(z, 0, 0, 42, 24, c[0], c[1], c[2]);
      }
   }

   /**
    * Handle short MIDI messages on the main port. The Erae hardware does not
    * notify us when the user switches layouts (confirmed by experiment), so
    * we no longer try to act on Program Change messages here. The handler
    * stays installed for future use and any unexpected traffic is logged.
    */
   private void handleMainMidi(final int status, final int data1, final int data2)
   {
      getHost().println(String.format("Erae Touch 2: Main MIDI %02X %02X %02X",
         status, data1, data2));
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
         for (int z = 0; z < 8; z++)
         {
            api.clearZone(z);
         }
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
