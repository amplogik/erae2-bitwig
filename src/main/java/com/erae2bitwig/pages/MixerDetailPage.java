package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;

/** Page 5: Mixer Detail - per-track sends, pan, EQ. Stub. */
public class MixerDetailPage extends EraePage
{
   public MixerDetailPage(final ControllerHost host, final ScriptProtocol protocol,
                          final BitwigModel model, final HardwareElements hardware)
   {
      super(PageId.MIXER_DETAIL, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      // TODO: Detailed mixer with sends, pan, EQ
   }
}
