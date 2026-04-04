package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/** Page 3: Instrument Control. Stub. */
public class InstrumentPage extends EraePage
{
   public InstrumentPage(final ControllerHost host, final LowLevelApi api,
                                final BitwigModel model)
   {
      super(PageId.INSTRUMENT_CONTROL, host, api, model);
   }

   @Override
   public void setupBindings() {}
}
