package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/** Page 6: Step Sequencer. Stub. */
public class StepSequencerPage extends EraePage
{
   public StepSequencerPage(final ControllerHost host, final LowLevelApi api,
                                   final BitwigModel model)
   {
      super(PageId.STEP_SEQUENCER, host, api, model);
   }

   @Override
   public void setupBindings() {}
}
