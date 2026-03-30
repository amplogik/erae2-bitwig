package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;

/** Page 8: Large Clip Launcher/Arranger - maximized 4x4 grid with scroll. Stub. */
public class LargeClipLauncherPage extends EraePage
{
   public LargeClipLauncherPage(final ControllerHost host, final ScriptProtocol protocol,
                                final BitwigModel model, final HardwareElements hardware)
   {
      super(PageId.LARGE_CLIP_LAUNCHER, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      // TODO: Large 4x4 clip launcher buttons with scroll controls
   }
}
