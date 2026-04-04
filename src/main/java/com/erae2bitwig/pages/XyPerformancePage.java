package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/** Page 7: XY Performance. Stub. */
public class XyPerformancePage extends EraePage
{
   public XyPerformancePage(final ControllerHost host, final LowLevelApi api,
                                   final BitwigModel model)
   {
      super(PageId.XY_PERFORMANCE, host, api, model);
   }

   @Override
   public void setupBindings() {}
}
