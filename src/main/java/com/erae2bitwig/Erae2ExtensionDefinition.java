package com.erae2bitwig;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class Erae2ExtensionDefinition extends ControllerExtensionDefinition
{
   private static final UUID ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

   @Override
   public String getName()
   {
      return "Erae Touch 2";
   }

   @Override
   public String getVersion()
   {
      return "1.0";
   }

   @Override
   public String getAuthor()
   {
      return "Kim";
   }

   @Override
   public UUID getId()
   {
      return ID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 18;
   }

   @Override
   public String getHardwareVendor()
   {
      return "Embodme";
   }

   @Override
   public String getHardwareModel()
   {
      return "Erae Touch 2";
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 2;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 2;
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list,
      final PlatformType platformType)
   {
      switch (platformType)
      {
         case LINUX:
            list.add(
               new String[]{"Erae 2 MIDI", "Erae 2 MIDI (MPE)"},
               new String[]{"Erae 2 MIDI", "Erae 2 MIDI (MPE)"});
            break;
         case WINDOWS:
         case MAC:
            list.add(
               new String[]{"Erae 2 MIDI", "Erae 2 MIDI (MPE)"},
               new String[]{"Erae 2 MIDI", "Erae 2 MIDI (MPE)"});
            break;
      }
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new Erae2Extension(this, host);
   }
}
