package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/** Page 8: Large Clip Launcher. Stub. */
public class LargeClipLauncherPage extends EraePage
{
   public LargeClipLauncherPage(final ControllerHost host, final LowLevelApi api,
                                       final BitwigModel model)
   {
      super(PageId.LARGE_CLIP_LAUNCHER, host, api, model);
   }

   @Override
   public void setupBindings() {}
}
