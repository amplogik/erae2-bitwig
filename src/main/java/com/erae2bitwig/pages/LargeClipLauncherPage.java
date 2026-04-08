package com.erae2bitwig.pages;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.core.TouchZone;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.LowLevelApi;

/**
 * Page 8: Large Clip Launcher — Bitwig-oriented clip launcher.
 *
 * Layout (42×24 surface, origin bottom-left):
 *   y=21-23: Scene launch row (10 scenes across)
 *   y=3-20:  Clip grid (10 scenes × 6 tracks)
 *   y=0-2:   Transport row (play, stop, scroll, modifiers, record)
 *
 * Each column = one scene, each row = one track/instrument.
 * Touching a clip selects its track and creates a blank clip if empty.
 */
public class LargeClipLauncherPage extends EraePage
{
   private static final int NUM_SCENES = 10;
   private static final int NUM_TRACKS = 6;
   private static final int COL_STRIDE = 4;  // 3px button + 1px gap
   private static final int CELL_W = 3;
   private static final int CELL_H = 3;
   private static final int DEFAULT_CLIP_LENGTH = 16; // beats (4 bars in 4/4)
   private static final int X_OFFSET = 1; // center 40px grid in 42px surface

   private final TouchZone[] sceneZones = new TouchZone[NUM_SCENES];
   private final TouchZone[][] clipZones = new TouchZone[NUM_SCENES][NUM_TRACKS];
   private TouchZone playZone;
   private TouchZone stopZone;
   private boolean deleteModifierHeld = false;
   private boolean recordModifierHeld = false;
   private boolean copyModifierHeld = false;
   private boolean pasteModifierHeld = false;
   private ClipLauncherSlot copiedSlot = null;
   private ClipLauncherSlot selectedEmptySlot = null;
   private TouchZone selectedEmptyZone = null;
   private int selectedEmptyScene = -1;

   public LargeClipLauncherPage(final ControllerHost host,
                                final LowLevelApi api,
                                final BitwigModel model)
   {
      super(PageId.LARGE_CLIP_LAUNCHER, host, api, model);
   }

   @Override
   public void setupBindings()
   {
      setupSceneLaunch();
      setupClipGrid();
      setupTransport();
   }

   private void setupSceneLaunch()
   {
      final SceneBank sceneBank = model.getSceneBank();

      for (int s = 0; s < NUM_SCENES; s++)
      {
         final int x = X_OFFSET + s * COL_STRIDE;
         final TouchZone zone = new TouchZone("Scene_" + s, x, 21, CELL_W, CELL_H);
         zone.setColor(60, 60, 60);

         final Scene scene = sceneBank.getScene(s);
         final int sceneIdx = s;

         zone.onPress(e -> scene.launch());

         scene.exists().addValueObserver(exists ->
         {
            if (!isActive) return;
            zone.setColor(exists ? 80 : 20, exists ? 80 : 20, exists ? 80 : 20);
            redrawZone(zone);
         });

         sceneZones[s] = zone;
         zones.add(zone);
      }
   }

   private void setupClipGrid()
   {
      final TrackBank trackBank = model.getTrackBank();

      for (int t = 0; t < NUM_TRACKS; t++)
      {
         final Track track = trackBank.getItemAt(t);
         final ClipLauncherSlotBank clipSlots = track.clipLauncherSlotBank();

         for (int s = 0; s < NUM_SCENES; s++)
         {
            final int x = X_OFFSET + s * COL_STRIDE;
            final int y = 18 - t * CELL_H;
            final TouchZone zone = new TouchZone(
               "Clip_s" + s + "_t" + t, x, y, CELL_W, CELL_H);
            zone.setColor(5, 5, 5);

            final ClipLauncherSlot slot = clipSlots.getItemAt(s);
            final Track trackRef = track;

            final int sceneIdx = s;
            zone.onPress(e ->
            {
               // Select this track in the mixer/arranger
               trackRef.selectInMixer();

               if (deleteModifierHeld)
               {
                  clearEmptySelection();
                  if (slot.hasContent().get())
                  {
                     slot.deleteObject();
                  }
               }
               else if (copyModifierHeld)
               {
                  clearEmptySelection();
                  copiedSlot = slot;
               }
               else if (pasteModifierHeld)
               {
                  clearEmptySelection();
                  if (copiedSlot != null)
                  {
                     slot.copyFrom(copiedSlot);
                  }
               }
               else if (recordModifierHeld)
               {
                  clearEmptySelection();
                  trackRef.arm().set(true);
                  // Launch scene to stop other scenes, then launch slot to record
                  model.getSceneBank().getScene(sceneIdx).launch();
                  slot.launch();
               }
               else if (!slot.hasContent().get())
               {
                  if (selectedEmptySlot == slot)
                  {
                     // Second click — arm track, launch scene + slot to record
                     trackRef.arm().set(true);
                     model.getSceneBank().getScene(selectedEmptyScene).launch();
                     slot.launch();
                     clearEmptySelection();
                  }
                  else
                  {
                     // First click — clear previous, select this one
                     clearEmptySelection();
                     selectedEmptySlot = slot;
                     selectedEmptyZone = zone;
                     selectedEmptyScene = sceneIdx;
                     slot.select();
                     zone.setOutlined(true);
                     redrawZone(zone);
                  }
               }
               else
               {
                  clearEmptySelection();
                  slot.launch();
               }
            });

            // Observe all relevant clip/track states
            slot.hasContent().addValueObserver(v -> updateClipColor(zone, slot, trackRef));
            slot.isPlaying().addValueObserver(v -> updateClipColor(zone, slot, trackRef));
            slot.isRecording().addValueObserver(v -> updateClipColor(zone, slot, trackRef));
            slot.isPlaybackQueued().addValueObserver(v -> updateClipColor(zone, slot, trackRef));
            slot.isRecordingQueued().addValueObserver(v -> updateClipColor(zone, slot, trackRef));
            slot.isStopQueued().addValueObserver(v -> updateClipColor(zone, slot, trackRef));
            slot.color().addValueObserver((r, g, b) -> updateClipColor(zone, slot, trackRef));
            trackRef.color().addValueObserver((r, g, b) -> updateClipColor(zone, slot, trackRef));
            trackRef.isStopped().addValueObserver(v -> updateClipColor(zone, slot, trackRef));

            clipZones[s][t] = zone;
            zones.add(zone);
         }
      }
   }

   private void setupTransport()
   {
      final Transport transport = model.getTransport();
      final TrackBank trackBank = model.getTrackBank();
      final SceneBank sceneBank = model.getSceneBank();

      // Play
      playZone = new TouchZone("Play", X_OFFSET, 0, CELL_W, CELL_H);
      playZone.setColor(0, 50, 0);
      playZone.onPress(e -> transport.play());
      zones.add(playZone);

      // Stop
      stopZone = new TouchZone("Stop", X_OFFSET + COL_STRIDE, 0, CELL_W, CELL_H);
      stopZone.setColor(80, 0, 0);
      stopZone.onPress(e -> transport.stop());
      zones.add(stopZone);

      // Scroll scenes left
      final TouchZone scLeft = new TouchZone("ScLeft", X_OFFSET + 2 * COL_STRIDE, 0, CELL_W, CELL_H);
      scLeft.setColor(30, 30, 50);
      scLeft.onPress(e -> sceneBank.scrollBackwards());
      zones.add(scLeft);

      // Scroll scenes right
      final TouchZone scRight = new TouchZone("ScRight", X_OFFSET + 3 * COL_STRIDE, 0, CELL_W, CELL_H);
      scRight.setColor(30, 30, 50);
      scRight.onPress(e -> sceneBank.scrollForwards());
      zones.add(scRight);

      // Scroll tracks up
      final TouchZone trUp = new TouchZone("TrUp", X_OFFSET + 4 * COL_STRIDE, 0, CELL_W, CELL_H);
      trUp.setColor(50, 30, 30);
      trUp.onPress(e -> trackBank.scrollBackwards());
      zones.add(trUp);

      // Scroll tracks down
      final TouchZone trDown = new TouchZone("TrDown", X_OFFSET + 5 * COL_STRIDE, 0, CELL_W, CELL_H);
      trDown.setColor(50, 30, 30);
      trDown.onPress(e -> trackBank.scrollForwards());
      zones.add(trDown);

      // Copy modifier (hold + tap clip to copy)
      final TouchZone copyZone = new TouchZone("Copy", X_OFFSET + 6 * COL_STRIDE, 0, CELL_W, CELL_H);
      copyZone.setColor(0, 40, 60);
      copyZone.onPress(e -> copyModifierHeld = true);
      copyZone.onRelease(e -> copyModifierHeld = false);
      zones.add(copyZone);

      // Paste modifier (hold + tap clip(s) to paste)
      final TouchZone pasteZone = new TouchZone("Paste", X_OFFSET + 7 * COL_STRIDE, 0, CELL_W, CELL_H);
      pasteZone.setColor(0, 60, 40);
      pasteZone.onPress(e -> pasteModifierHeld = true);
      pasteZone.onRelease(e -> pasteModifierHeld = false);
      zones.add(pasteZone);

      // Record modifier (hold + tap clip to record into it)
      final TouchZone recZone = new TouchZone("Record", X_OFFSET + 9 * COL_STRIDE, 0, CELL_W, CELL_H);
      recZone.setColor(60, 0, 0);
      recZone.onPress(e -> recordModifierHeld = true);
      recZone.onRelease(e -> recordModifierHeld = false);
      zones.add(recZone);

      // Delete modifier (hold + tap clip to delete)
      final TouchZone deleteZone = new TouchZone("Delete", X_OFFSET + 8 * COL_STRIDE, 0, CELL_W, CELL_H);
      deleteZone.setColor(60, 0, 80);
      deleteZone.onPress(e -> deleteModifierHeld = true);
      deleteZone.onRelease(e -> deleteModifierHeld = false);
      zones.add(deleteZone);

      // Transport LED feedback + refresh clip colors on play state change
      transport.isPlaying().addValueObserver(playing ->
      {
         if (!isActive) return;
         playZone.setColor(0, playing ? 127 : 50, 0);
         redrawZone(playZone);
         stopZone.setColor(playing ? 40 : 100, 0, 0);
         redrawZone(stopZone);
         refreshAllColors();
      });

      // Scroll indicator feedback
      sceneBank.canScrollBackwards().addValueObserver(can ->
      {
         if (!isActive) return;
         scLeft.setColor(can ? 50 : 15, can ? 50 : 15, can ? 80 : 25);
         redrawZone(scLeft);
      });
      sceneBank.canScrollForwards().addValueObserver(can ->
      {
         if (!isActive) return;
         scRight.setColor(can ? 50 : 15, can ? 50 : 15, can ? 80 : 25);
         redrawZone(scRight);
      });
      trackBank.canScrollBackwards().addValueObserver(can ->
      {
         if (!isActive) return;
         trUp.setColor(can ? 80 : 25, can ? 50 : 15, can ? 50 : 15);
         redrawZone(trUp);
      });
      trackBank.canScrollForwards().addValueObserver(can ->
      {
         if (!isActive) return;
         trDown.setColor(can ? 80 : 25, can ? 50 : 15, can ? 50 : 15);
         redrawZone(trDown);
      });
   }

   @Override
   public void onActivate()
   {
      super.onActivate();
      refreshAllColors();
   }

   private void refreshAllColors()
   {
      final TrackBank trackBank = model.getTrackBank();
      for (int t = 0; t < NUM_TRACKS; t++)
      {
         final Track track = trackBank.getItemAt(t);
         final ClipLauncherSlotBank clipSlots = track.clipLauncherSlotBank();
         for (int s = 0; s < NUM_SCENES; s++)
         {
            updateClipColor(clipZones[s][t], clipSlots.getItemAt(s), track);
         }
      }
   }

   private void clearEmptySelection()
   {
      if (selectedEmptyZone != null)
      {
         selectedEmptyZone.setOutlined(false);
         redrawZone(selectedEmptyZone);
      }
      selectedEmptySlot = null;
      selectedEmptyZone = null;
      selectedEmptyScene = -1;
   }

   private void updateClipColor(final TouchZone zone, final ClipLauncherSlot slot,
                                final Track track)
   {
      if (!isActive) return;

      final boolean transportPlaying = model.getTransport().isPlaying().get();
      final boolean actuallyPlaying = slot.isPlaying().get() && transportPlaying;

      // Clear outline if slot now has content
      if (slot.hasContent().get() && zone.isOutlined())
      {
         zone.setOutlined(false);
      }

      if (slot.isRecordingQueued().get())
      {
         zone.setColor(127, 40, 0);
      }
      else if (slot.isRecording().get())
      {
         zone.setColor(127, 0, 0);
      }
      else if (slot.isPlaybackQueued().get() && transportPlaying)
      {
         zone.setColor(80, 127, 0);
      }
      else if (slot.isStopQueued().get())
      {
         zone.setColor(127, 80, 0);
      }
      else if (actuallyPlaying)
      {
         // Bright version of clip color
         final Color color = slot.color().get();
         zone.setColor(
            Math.min(127, (int) (color.getRed() * 127) + 40),
            Math.min(127, (int) (color.getGreen() * 127) + 40),
            Math.min(127, (int) (color.getBlue() * 127) + 40));
      }
      else if (slot.hasContent().get())
      {
         // Normal clip color
         final Color color = slot.color().get();
         zone.setColor(
            (int) (color.getRed() * 100),
            (int) (color.getGreen() * 100),
            (int) (color.getBlue() * 100));
      }
      else
      {
         // Empty slot: dim track color as background tint
         final Color tc = track.color().get();
         if (zone.isOutlined())
         {
            // Selected empty slot — brighter outline
            zone.setColor(
               Math.max(10, (int) (tc.getRed() * 60)),
               Math.max(10, (int) (tc.getGreen() * 60)),
               Math.max(10, (int) (tc.getBlue() * 60)));
         }
         else
         {
            zone.setColor(
               Math.max(3, (int) (tc.getRed() * 20)),
               Math.max(3, (int) (tc.getGreen() * 20)),
               Math.max(3, (int) (tc.getBlue() * 20)));
         }
      }
      redrawZone(zone);
   }
}
