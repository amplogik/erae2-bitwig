package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;

/** Page 6: Step Sequencer - grid pattern programming. Stub. */
public class StepSequencerPage extends EraePage
{
   public StepSequencerPage(final ControllerHost host, final ScriptProtocol protocol,
                            final BitwigModel model, final HardwareElements hardware)
   {
      super(PageId.STEP_SEQUENCER, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      // TODO: Step sequencer grid, note entry, pattern editing
   }
}
