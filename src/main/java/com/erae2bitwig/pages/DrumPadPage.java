package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/** Page 4: Drum Pads. Stub. */
public class DrumPadPage extends EraePage
{
   public DrumPadPage(final ControllerHost host, final LowLevelApi api,
                             final BitwigModel model)
   {
      super(PageId.DRUM_PADS, host, api, model);
   }

   @Override
   public void setupBindings() {}
}
