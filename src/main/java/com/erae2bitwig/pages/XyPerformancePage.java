package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;

/** Page 7: XY Performance - full-surface XY pad for macro/FX control. Stub. */
public class XyPerformancePage extends EraePage
{
   public XyPerformancePage(final ControllerHost host, final ScriptProtocol protocol,
                            final BitwigModel model, final HardwareElements hardware)
   {
      super(PageId.XY_PERFORMANCE, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      // TODO: Full-surface XY pad mapped to macro controls
   }
}
