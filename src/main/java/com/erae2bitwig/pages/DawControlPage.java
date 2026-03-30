package com.erae2bitwig.pages;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;

import com.erae2bitwig.core.BitwigModel;
import com.erae2bitwig.hardware.EraeButton;
import com.erae2bitwig.hardware.EraeSlider;
import com.erae2bitwig.hardware.HardwareElements;
import com.erae2bitwig.layer.EraePage;
import com.erae2bitwig.layer.PageId;
import com.erae2bitwig.sysex.ColorPalette;
import com.erae2bitwig.sysex.LedState;
import com.erae2bitwig.sysex.ScriptProtocol;
import com.erae2bitwig.sysex.SysExConstants;

/**
 * Page 2: DAW Control
 * Provides transport, clip launcher grid with scrolling, scene launch,
 * track mixer (mute/solo/arm), and volume/send sliders.
 */
public class DawControlPage extends EraePage
{
   public DawControlPage(final ControllerHost host,
                         final ScriptProtocol protocol,
                         final BitwigModel model,
                         final HardwareElements hardware)
   {
      super(PageId.DAW_CONTROL, host, protocol, model, hardware);
   }

   @Override
   public void setupBindings()
   {
      setupTransport();
      setupNavigation();
      setupClipGrid();
      setupSceneLaunch();
      setupMixer();
      setupVolumeSliders();
      setupSendSliders();
   }

   private void setupTransport()
   {
      final Transport transport = model.getTransport();

      // Play button
      hardware.getPlayButton().getButton().pressedAction().addBinding(
         transport.playAction());

      // Stop button
      hardware.getStopButton().getButton().pressedAction().addBinding(
         transport.stopAction());

      // Stop All Clips
      hardware.getStopAllButton().getButton().pressedAction().addBinding(
         host.createAction(() ->
         {
            final TrackBank tb = model.getTrackBank();
            for (int i = 0; i < model.getSessionWidth(); i++)
            {
               tb.getItemAt(i).stop();
            }
         }, () -> "Stop All Clips"));

      // LED feedback for transport
      transport.isPlaying().addValueObserver(playing ->
      {
         if (!isActive) return;
         hardware.getPlayButton().setLedState(
            playing ? LedState.pulse(ColorPalette.GREEN) : LedState.solid(ColorPalette.GREEN));
         hardware.getStopButton().setLedState(
            playing ? LedState.solid(ColorPalette.DARK_RED) : LedState.solid(ColorPalette.RED));
      });
   }

   private void setupNavigation()
   {
      final TrackBank trackBank = model.getTrackBank();
      final SceneBank sceneBank = model.getSceneBank();

      // Navigation buttons
      hardware.getUpButton().getButton().pressedAction().addBinding(
         sceneBank.scrollBackwardsAction());
      hardware.getDownButton().getButton().pressedAction().addBinding(
         sceneBank.scrollForwardsAction());
      hardware.getLeftButton().getButton().pressedAction().addBinding(
         trackBank.scrollBackwardsAction());
      hardware.getRightButton().getButton().pressedAction().addBinding(
         trackBank.scrollForwardsAction());

      // LED feedback: lit when can scroll, dim when at boundary
      trackBank.canScrollBackwards().addValueObserver(can ->
      {
         if (!isActive) return;
         hardware.getLeftButton().setLedState(
            can ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));
      });
      trackBank.canScrollForwards().addValueObserver(can ->
      {
         if (!isActive) return;
         hardware.getRightButton().setLedState(
            can ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));
      });
      sceneBank.canScrollBackwards().addValueObserver(can ->
      {
         if (!isActive) return;
         hardware.getUpButton().setLedState(
            can ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));
      });
      sceneBank.canScrollForwards().addValueObserver(can ->
      {
         if (!isActive) return;
         hardware.getDownButton().setLedState(
            can ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));
      });
   }

   private void setupClipGrid()
   {
      final TrackBank trackBank = model.getTrackBank();

      for (int col = 0; col < hardware.getMaxTracks(); col++)
      {
         final Track track = trackBank.getItemAt(col);
         final ClipLauncherSlotBank clipSlots = track.clipLauncherSlotBank();

         for (int row = 0; row < hardware.getMaxScenes(); row++)
         {
            final EraeButton matrixBtn = hardware.getMatrixButton(col, row);
            final ClipLauncherSlot slot = clipSlots.getItemAt(row);
            final int c = col;
            final int r = row;

            // Press launches the clip
            matrixBtn.getButton().pressedAction().addBinding(
               host.createAction(() -> slot.launch(), () -> "Launch Clip " + c + "," + r));

            // LED feedback based on clip state
            slot.hasContent().addValueObserver(has -> updateClipLed(c, r));
            slot.isPlaying().addValueObserver(playing -> updateClipLed(c, r));
            slot.isRecording().addValueObserver(recording -> updateClipLed(c, r));
            slot.isPlaybackQueued().addValueObserver(queued -> updateClipLed(c, r));
            slot.isRecordingQueued().addValueObserver(queued -> updateClipLed(c, r));
            slot.isStopQueued().addValueObserver(queued -> updateClipLed(c, r));
            slot.color().addValueObserver((red, green, blue) -> updateClipLed(c, r));
         }
      }
   }

   private void updateClipLed(final int col, final int row)
   {
      if (!isActive) return;
      if (col >= model.getSessionWidth() || row >= model.getSessionHeight()) return;

      final Track track = model.getTrackBank().getItemAt(col);
      final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(row);
      final EraeButton btn = hardware.getMatrixButton(col, row);

      final LedState state;
      if (slot.isRecordingQueued().get())
      {
         state = LedState.blink(ColorPalette.RED);
      }
      else if (slot.isRecording().get())
      {
         state = LedState.pulse(ColorPalette.RED);
      }
      else if (slot.isPlaybackQueued().get())
      {
         state = LedState.blink(ColorPalette.GREEN);
      }
      else if (slot.isStopQueued().get())
      {
         state = LedState.blink(ColorPalette.ORANGE);
      }
      else if (slot.isPlaying().get())
      {
         state = LedState.pulse(ColorPalette.GREEN);
      }
      else if (slot.hasContent().get())
      {
         final Color color = slot.color().get();
         final int colorIndex = ColorPalette.findNearestColor(
            color.getRed(), color.getGreen(), color.getBlue());
         state = LedState.solid(colorIndex);
      }
      else
      {
         state = LedState.OFF;
      }

      btn.setLedState(state);
   }

   private void setupSceneLaunch()
   {
      final SceneBank sceneBank = model.getSceneBank();

      for (int i = 0; i < hardware.getMaxScenes(); i++)
      {
         final Scene scene = sceneBank.getScene(i);
         final EraeButton sceneBtn = hardware.getSceneLaunchButton(i);
         final int idx = i;

         sceneBtn.getButton().pressedAction().addBinding(
            host.createAction(() -> scene.launch(), () -> "Launch Scene " + idx));

         // LED: white if exists, off if not
         scene.exists().addValueObserver(exists ->
         {
            if (!isActive) return;
            sceneBtn.setLedState(exists ? LedState.solid(ColorPalette.WHITE) : LedState.OFF);
         });
      }
   }

   private void setupMixer()
   {
      final TrackBank trackBank = model.getTrackBank();

      for (int i = 0; i < hardware.getMaxTracks(); i++)
      {
         final Track track = trackBank.getItemAt(i);
         final int idx = i;

         // Mute
         final EraeButton muteBtn = hardware.getMuteButton(i);
         muteBtn.getButton().pressedAction().addBinding(
            host.createAction(() -> track.mute().toggle(), () -> "Toggle Mute " + idx));
         track.mute().addValueObserver(muted ->
         {
            if (!isActive) return;
            muteBtn.setLedState(muted
               ? LedState.solid(ColorPalette.YELLOW)
               : LedState.solid(ColorPalette.YELLOW_HALF));
         });

         // Solo
         final EraeButton soloBtn = hardware.getSoloButton(i);
         soloBtn.getButton().pressedAction().addBinding(
            host.createAction(() -> track.solo().toggle(), () -> "Toggle Solo " + idx));
         track.solo().addValueObserver(soloed ->
         {
            if (!isActive) return;
            soloBtn.setLedState(soloed
               ? LedState.solid(ColorPalette.BLUE)
               : LedState.solid(ColorPalette.BLUE_HALF));
         });

         // Arm
         final EraeButton armBtn = hardware.getArmButton(i);
         armBtn.getButton().pressedAction().addBinding(
            host.createAction(() -> track.arm().toggle(), () -> "Toggle Arm " + idx));
         track.arm().addValueObserver(armed ->
         {
            if (!isActive) return;
            armBtn.setLedState(armed
               ? LedState.solid(ColorPalette.RED)
               : LedState.solid(ColorPalette.RED_HALF));
         });
      }
   }

   private void setupVolumeSliders()
   {
      final TrackBank trackBank = model.getTrackBank();

      for (int i = 0; i < hardware.getMaxTracks(); i++)
      {
         final Track track = trackBank.getItemAt(i);
         final EraeSlider slider = hardware.getVolumeSlider(i);
         final int idx = i;

         // Bind hardware slider to track volume
         slider.getSlider().addBinding(track.volume());

         // Outbound feedback: when volume changes in Bitwig, update device
         track.volume().value().addValueObserver(128, value ->
         {
            if (!isActive) return;
            slider.sendValue(value);
         });
      }
   }

   private void setupSendSliders()
   {
      final TrackBank trackBank = model.getTrackBank();

      for (int i = 0; i < hardware.getMaxTracks(); i++)
      {
         final Track track = trackBank.getItemAt(i);
         final SendBank sendBank = track.sendBank();

         // Send 1
         if (sendBank.getItemAt(0).exists().get())
         {
            final EraeSlider send1 = hardware.getSend1Slider(i);
            send1.getSlider().addBinding(sendBank.getItemAt(0));
            final int idx = i;
            sendBank.getItemAt(0).value().addValueObserver(128, value ->
            {
               if (!isActive) return;
               send1.sendValue(value);
            });
         }

         // Send 2
         if (sendBank.getItemAt(1).exists().get())
         {
            final EraeSlider send2 = hardware.getSend2Slider(i);
            send2.getSlider().addBinding(sendBank.getItemAt(1));
            final int idx = i;
            sendBank.getItemAt(1).value().addValueObserver(128, value ->
            {
               if (!isActive) return;
               send2.sendValue(value);
            });
         }
      }
   }

   @Override
   public void handleFaderValue(final int target, final int value)
   {
      final TrackBank trackBank = model.getTrackBank();

      if (target < 12)
      {
         // Volume
         if (target < model.getSessionWidth())
         {
            trackBank.getItemAt(target).volume().set(value, 128);
         }
      }
      else if (target < 24)
      {
         // Send 1
         final int trackIdx = target % 12;
         if (trackIdx < model.getSessionWidth())
         {
            trackBank.getItemAt(trackIdx).sendBank().getItemAt(0).set(value, 128);
         }
      }
      else if (target < 36)
      {
         // Send 2
         final int trackIdx = target % 12;
         if (trackIdx < model.getSessionWidth())
         {
            trackBank.getItemAt(trackIdx).sendBank().getItemAt(1).set(value, 128);
         }
      }
   }

   @Override
   public void onActivate()
   {
      super.onActivate();
      // Send handshake request to get window size and trigger full update
      protocol.sendRequestState();
   }

   @Override
   public void refresh()
   {
      if (!isActive) return;

      // Refresh all clip LEDs
      for (int col = 0; col < model.getSessionWidth(); col++)
      {
         for (int row = 0; row < model.getSessionHeight(); row++)
         {
            updateClipLed(col, row);
         }
      }

      // Refresh transport LEDs
      final boolean playing = model.getTransport().isPlaying().get();
      hardware.getPlayButton().setLedState(
         playing ? LedState.pulse(ColorPalette.GREEN) : LedState.solid(ColorPalette.GREEN));
      hardware.getStopButton().setLedState(
         playing ? LedState.solid(ColorPalette.DARK_RED) : LedState.solid(ColorPalette.RED));

      // Refresh navigation LEDs
      hardware.getLeftButton().setLedState(
         model.getTrackBank().canScrollBackwards().get()
            ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));
      hardware.getRightButton().setLedState(
         model.getTrackBank().canScrollForwards().get()
            ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));
      hardware.getUpButton().setLedState(
         model.getSceneBank().canScrollBackwards().get()
            ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));
      hardware.getDownButton().setLedState(
         model.getSceneBank().canScrollForwards().get()
            ? LedState.solid(ColorPalette.WHITE) : LedState.solid(ColorPalette.DARK_GREY));

      // Refresh mixer LEDs
      final TrackBank trackBank = model.getTrackBank();
      for (int i = 0; i < model.getSessionWidth(); i++)
      {
         final Track track = trackBank.getItemAt(i);
         hardware.getMuteButton(i).setLedState(
            track.mute().get()
               ? LedState.solid(ColorPalette.YELLOW)
               : LedState.solid(ColorPalette.YELLOW_HALF));
         hardware.getSoloButton(i).setLedState(
            track.solo().get()
               ? LedState.solid(ColorPalette.BLUE)
               : LedState.solid(ColorPalette.BLUE_HALF));
         hardware.getArmButton(i).setLedState(
            track.arm().get()
               ? LedState.solid(ColorPalette.RED)
               : LedState.solid(ColorPalette.RED_HALF));
      }

      // Refresh volume sliders
      for (int i = 0; i < model.getSessionWidth(); i++)
      {
         final int value = (int) (trackBank.getItemAt(i).volume().get() * 127);
         hardware.getVolumeSlider(i).sendValue(value);
      }
   }
}
