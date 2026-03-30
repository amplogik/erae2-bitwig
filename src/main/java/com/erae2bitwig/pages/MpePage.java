package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ScriptProtocol;

/** Page 1: MPE Playing Surface + XY Controller. Stub for future implementation. */
public class MpePage extends EraePage
{
   public MpePage(final ControllerHost host, final ScriptProtocol protocol,
                  final BitwigModel model, final HardwareElements hardware)
   {
      super(PageId.MPE_PLAY, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      // TODO: MPE NoteInput with setUseExpressiveMidi, XY pad mode
   }
}
