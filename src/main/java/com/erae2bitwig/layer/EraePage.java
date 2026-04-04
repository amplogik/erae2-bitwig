package com.erae2bitwig.layer;

import com.bitwig.extension.controller.api.ControllerHost;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.TouchEvent;
import com.erae2bitwig.core.TouchZone;
import com.erae2bitwig.sysex.LowLevelApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all Erae Touch 2 pages.
 * Each page defines touch zones and renders its visual layout.
 */
public abstract class EraePage
{
   protected final PageId pageId;
   protected final ControllerHost host;
   protected final LowLevelApi api;
   protected final BitwigModel model;
   protected final List<TouchZone> zones = new ArrayList<>();
   protected boolean isActive = false;

   protected EraePage(final PageId pageId,
                      final ControllerHost host,
                      final LowLevelApi api,
                      final BitwigModel model)
   {
      this.pageId = pageId;
      this.host = host;
      this.api = api;
      this.model = model;
   }

   /** Called once during init to set up touch zones and Bitwig observers. */
   public abstract void setupBindings();

   /** Called when this page becomes the active page. */
   public void onActivate()
   {
      isActive = true;
      host.println("Erae Touch 2: Page activated - " + pageId.name());
      drawSurface();
   }

   /** Called when switching away from this page. */
   public void onDeactivate()
   {
      isActive = false;
   }

   /** Draw all zones to the Erae surface. */
   public void drawSurface()
   {
      api.clearZone(0);
      for (final TouchZone zone : zones)
      {
         api.drawZone(zone);
      }
   }

   /** Redraw a single zone (e.g., after color change). */
   protected void redrawZone(final TouchZone zone)
   {
      if (!isActive) return;
      api.drawZone(zone);
   }

   /** Handle a touch event by routing to the appropriate zone. */
   public void handleTouchEvent(final TouchEvent event)
   {
      for (final TouchZone zone : zones)
      {
         if (zone.contains(event.getX(), event.getY()))
         {
            zone.handleTouch(event);
            return;
         }
      }
   }

   /** Called when the device requests a full state refresh. */
   public void refresh()
   {
      if (isActive) drawSurface();
   }

   public PageId getPageId() { return pageId; }
   public boolean isActive() { return isActive; }
}
