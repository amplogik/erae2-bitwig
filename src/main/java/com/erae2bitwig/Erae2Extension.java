package com.erae2bitwig;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.PageManager;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;
import com.erae2bitwig.sysex.SysExConstants;

public class Erae2Extension extends ControllerExtension
{
   private Erae2MidiPorts midiPorts;
   private ScriptProtocol scriptProtocol;
   private BitwigModel model;
   private HardwareElements hardwareElements;
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

      // Set up SysEx protocol handler
      scriptProtocol = new ScriptProtocol(midiPorts);

      // Create Bitwig model (track bank, transport, etc.)
      model = new BitwigModel(host);

      // Create hardware control elements
      hardwareElements = new HardwareElements(host, midiPorts, scriptProtocol);

      // Set up page manager and all pages
      pageManager = new PageManager(host, midiPorts, scriptProtocol, model, hardwareElements);

      // Register SysEx handler for incoming messages
      midiPorts.setScriptSysExHandler((data) -> handleSysEx(data));

      // Start handshake
      scriptProtocol.sendIdentityRequest();

      host.println("Erae Touch 2: Initialization complete.");
   }

   private void handleSysEx(final String data)
   {
      final ControllerHost host = getHost();
      final int[] bytes = sysExStringToBytes(data);

      if (isWindowSize(bytes))
      {
         final int width = bytes[10];
         final int height = bytes[11];
         host.println("Erae Touch 2: Window size " + width + "x" + height);
         model.setSessionSize(width, height);
         pageManager.activatePage(PageId.DAW_CONTROL);
         pageManager.refreshActivePage();
      }
      else if (isUpdate(bytes))
      {
         host.println("Erae Touch 2: Update requested");
         pageManager.refreshActivePage();
      }
      else if (isFaderValue(bytes))
      {
         final int target = bytes[10];
         final int value = bytes[11];
         pageManager.handleFaderValue(target, value);
      }
   }

   private boolean isWindowSize(final int[] bytes)
   {
      return bytes.length == 13 && matchesPrefix(bytes, SysExConstants.SCRIPT_SYSEX_PREFIX, SysExConstants.WINDOW_BYTE);
   }

   private boolean isUpdate(final int[] bytes)
   {
      return bytes.length == 11 && matchesPrefix(bytes, SysExConstants.SCRIPT_SYSEX_PREFIX, SysExConstants.UPDATE_BYTE);
   }

   private boolean isFaderValue(final int[] bytes)
   {
      return bytes.length == 13 && matchesPrefix(bytes, SysExConstants.SCRIPT_SYSEX_PREFIX, SysExConstants.SLIDER_PREFIX);
   }

   private boolean matchesPrefix(final int[] bytes, final int[] prefix, final int typeByte)
   {
      if (bytes.length < prefix.length + 1)
         return false;
      for (int i = 0; i < prefix.length; i++)
      {
         if (bytes[i] != prefix[i])
            return false;
      }
      return bytes[prefix.length] == typeByte;
   }

   private int[] sysExStringToBytes(final String hex)
   {
      final String clean = hex.replace(" ", "");
      final int len = clean.length() / 2;
      final int[] result = new int[len];
      for (int i = 0; i < len; i++)
      {
         result[i] = Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
      }
      return result;
   }

   @Override
   public void exit()
   {
      scriptProtocol.sendQuitMessage();
      getHost().println("Erae Touch 2: Extension exited.");
   }

   @Override
   public void flush()
   {
      if (hardwareElements != null)
      {
         hardwareElements.flush();
      }
   }
}
