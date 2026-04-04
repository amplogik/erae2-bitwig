package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/** Page 5: Mixer Detail. Stub. */
public class MixerDetailPage extends EraePage
{
   public MixerDetailPage(final ControllerHost host, final LowLevelApi api,
                                 final BitwigModel model)
   {
      super(PageId.MIXER_DETAIL, host, api, model);
   }

   @Override
   public void setupBindings() {}
}
