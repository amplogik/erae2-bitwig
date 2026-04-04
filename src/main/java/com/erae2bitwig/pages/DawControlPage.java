package com.erae2bitwig.pages;

import com.bitwig.extension.controller.api.*;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.TouchZone;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/**
 * Page 2: DAW Control
 * Provides transport, clip launcher grid, and mixer controls
 * rendered on the 42x24 Erae Touch 2 surface via FingerStream + draw API.
 *
 * Phase 1: Basic transport controls and visual test.
 */
public class DawControlPage extends EraePage
{
   public DawControlPage(final ControllerHost host,
                         final LowLevelApi api,
                         final BitwigModel model)
   {
      super(PageId.DAW_CONTROL, host, api, model);
   }

   @Override
   public void setupBindings()
   {
      final Transport transport = model.getTransport();

      // Play button: green, top-left
      final TouchZone playZone = new TouchZone("Play", 0, 0, 5, 4);
      playZone.setColor(0, 80, 0);
      playZone.onPress(e ->
      {
         transport.play();
         host.println("Erae Touch 2: PLAY pressed");
      });
      zones.add(playZone);

      // Stop button: red, next to play
      final TouchZone stopZone = new TouchZone("Stop", 6, 0, 5, 4);
      stopZone.setColor(80, 0, 0);
      stopZone.onPress(e ->
      {
         transport.stop();
         host.println("Erae Touch 2: STOP pressed");
      });
      zones.add(stopZone);

      // Navigation: scroll tracks/scenes
      final TrackBank trackBank = model.getTrackBank();
      final SceneBank sceneBank = model.getSceneBank();

      final TouchZone leftZone = new TouchZone("Left", 12, 0, 4, 4);
      leftZone.setColor(40, 40, 40);
      leftZone.onPress(e -> trackBank.scrollBackwards());
      zones.add(leftZone);

      final TouchZone rightZone = new TouchZone("Right", 17, 0, 4, 4);
      rightZone.setColor(40, 40, 40);
      rightZone.onPress(e -> trackBank.scrollForwards());
      zones.add(rightZone);

      final TouchZone upZone = new TouchZone("Up", 22, 0, 4, 4);
      upZone.setColor(40, 40, 40);
      upZone.onPress(e -> sceneBank.scrollBackwards());
      zones.add(upZone);

      final TouchZone downZone = new TouchZone("Down", 27, 0, 4, 4);
      downZone.setColor(40, 40, 40);
      downZone.onPress(e -> sceneBank.scrollForwards());
      zones.add(downZone);

      // 4x4 Clip Launcher Grid (rows 5-20, columns 0-41)
      for (int col = 0; col < 4; col++)
      {
         final Track track = trackBank.getItemAt(col);
         final ClipLauncherSlotBank clipSlots = track.clipLauncherSlotBank();

         for (int row = 0; row < 4; row++)
         {
            final int px = col * 10 + 1;
            final int py = 5 + row * 4;
            final TouchZone clipZone = new TouchZone(
               "Clip_" + col + "_" + row, px, py, 9, 3);
            clipZone.setColor(20, 20, 20); // dim default

            final ClipLauncherSlot slot = clipSlots.getItemAt(row);
            final int c = col;
            final int r = row;

            clipZone.onPress(e ->
            {
               slot.launch();
               host.println("Erae Touch 2: Launch clip " + c + "," + r);
            });

            // Observe clip state for color updates
            slot.hasContent().addValueObserver(has -> updateClipZoneColor(clipZone, slot));
            slot.isPlaying().addValueObserver(playing -> updateClipZoneColor(clipZone, slot));
            slot.color().addValueObserver((red, green, blue) -> updateClipZoneColor(clipZone, slot));

            zones.add(clipZone);
         }
      }

      // Transport LED feedback
      transport.isPlaying().addValueObserver(playing ->
      {
         if (!isActive) return;
         playZone.setColor(0, playing ? 127 : 50, 0);
         redrawZone(playZone);
         stopZone.setColor(playing ? 40 : 100, 0, 0);
         redrawZone(stopZone);
      });
   }

   private void updateClipZoneColor(final TouchZone zone, final ClipLauncherSlot slot)
   {
      if (!isActive) return;

      if (slot.isPlaying().get())
      {
         zone.setColor(0, 100, 0);
      }
      else if (slot.hasContent().get())
      {
         // Use clip color, scaled to 7-bit
         final com.bitwig.extension.api.Color color = slot.color().get();
         zone.setColor(
            (int) (color.getRed() * 127),
            (int) (color.getGreen() * 127),
            (int) (color.getBlue() * 127));
      }
      else
      {
         zone.setColor(10, 10, 10);
      }
      redrawZone(zone);
   }
}
