package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/** Page 1: MPE Playing Surface + XY Controller. Stub. */
public class MpePage extends EraePage
{
   public MpePage(final ControllerHost host, final LowLevelApi api,
                         final BitwigModel model)
   {
      super(PageId.MPE_PLAY, host, api, model);
   }

   @Override
   public void setupBindings() {}
}
