package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;

/** Page 4: Drum Pads - standard MIDI velocity triggers, non-MPE. Stub. */
public class DrumPadPage extends EraePage
{
   public DrumPadPage(final ControllerHost host, final ScriptProtocol protocol,
                      final BitwigModel model, final HardwareElements hardware)
   {
      super(PageId.DRUM_PADS, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      // TODO: NoteInput with drum mapping, velocity-sensitive triggers
   }
}
