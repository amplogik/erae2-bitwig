package com.erae2bitwig.layer;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.sysex.ScriptProtocol;

/**
 * Abstract base class for all 8 Erae Touch 2 pages.
 * Each page manages its own control bindings and LED state.
 */
public abstract class EraePage
{
   protected final PageId pageId;
   protected final ControllerHost host;
   protected final ScriptProtocol protocol;
   protected final BitwigModel model;
   protected final HardwareElements hardware;
   protected boolean isActive = false;

   protected EraePage(final PageId pageId,
                      final ControllerHost host,
                      final ScriptProtocol protocol,
                      final BitwigModel model,
                      final HardwareElements hardware)
   {
      this.pageId = pageId;
      this.host = host;
      this.protocol = protocol;
      this.model = model;
      this.hardware = hardware;
   }

   /** Called once during init to set up all control bindings. */
   public abstract void setupBindings();

   /** Called when this page becomes the active page. */
   public void onActivate()
   {
      isActive = true;
      host.println("Erae Touch 2: Page activated - " + pageId.name());
   }

   /** Called when switching away from this page. */
   public void onDeactivate()
   {
      isActive = false;
   }

   /** Called when the device requests a full state refresh. */
   public void refresh()
   {
      // Override in subclasses to send full LED state
   }

   /** Handle an incoming fader value from the device. */
   public void handleFaderValue(final int target, final int value)
   {
      // Override in subclasses that use sliders
   }

   public PageId getPageId() { return pageId; }
   public boolean isActive() { return isActive; }
}
