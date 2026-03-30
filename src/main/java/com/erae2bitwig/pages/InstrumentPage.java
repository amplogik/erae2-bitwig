package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;

/** Page 3: Instrument Control - filters, oscillators, envelopes via CursorDevice. Stub. */
public class InstrumentPage extends EraePage
{
   public InstrumentPage(final ControllerHost host, final ScriptProtocol protocol,
                         final BitwigModel model, final HardwareElements hardware)
   {
      super(PageId.INSTRUMENT_CONTROL, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      // TODO: CursorDevice remote controls, parameter page navigation
   }
}
