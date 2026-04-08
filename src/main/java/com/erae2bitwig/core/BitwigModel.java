package com.erae2bitwig.core;

import com.bitwig.extension.controller.api.*;

/**
 * Centralized access to all Bitwig API objects.
 * Creates and configures TrackBank, Transport, SceneBank, CursorTrack, etc.
 */
public class BitwigModel
{
   private final ControllerHost host;
   private final Transport transport;
   private final Application application;
   private final TrackBank trackBank;
   private final TrackBank mixerTrackBank;
   private final TrackBank effectTrackBank;
   private final TrackBank instrumentNavBank;
   private final DrumPadBank cursorDrumPadBank;
   private final SceneBank sceneBank;
   private final CursorTrack cursorTrack;
   private final PinnableCursorDevice cursorDevice;
   private final CursorRemoteControlsPage remoteControls;
   private final MasterTrack masterTrack;

   private int sessionWidth = 12;
   private int sessionHeight = 10;

   private static final int MAX_TRACKS = 12;
   private static final int MAX_SCENES = 10;
   private static final int NUM_SENDS = 2;
   private static final int NUM_REMOTE_PARAMS = 8;
   private static final int MIXER_BANK_SIZE = 8;
   private static final int MIXER_FX_COUNT = 1;
   private static final int INSTRUMENT_NAV_BANK_SIZE = 64;
   private static final int DRUM_PAD_BANK_SIZE = 16;

   public BitwigModel(final ControllerHost host)
   {
      this.host = host;

      application = host.createApplication();
      transport = host.createTransport();
      transport.isPlaying().markInterested();
      transport.isArrangerRecordEnabled().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();

      masterTrack = host.createMasterTrack(NUM_SENDS);

      // Cursor track for device/parameter following
      cursorTrack = host.createCursorTrack("erae2-cursor", "Erae Touch 2", NUM_SENDS, MAX_SCENES, true);
      cursorTrack.exists().markInterested();
      cursorTrack.position().markInterested();
      cursorTrack.playingNotes().markInterested();

      // Device cursor following track selection
      cursorDevice = cursorTrack.createCursorDevice("erae2-device", "Device Control", 0,
         CursorDeviceFollowMode.FOLLOW_SELECTION);
      cursorDevice.exists().markInterested();
      cursorDevice.isEnabled().markInterested();
      cursorDevice.name().markInterested();

      // Remote controls for instrument page (8 knobs)
      remoteControls = cursorDevice.createCursorRemoteControlsPage(NUM_REMOTE_PARAMS);
      for (int i = 0; i < NUM_REMOTE_PARAMS; i++)
      {
         remoteControls.getParameter(i).markInterested();
         remoteControls.getParameter(i).exists().markInterested();
      }

      // Track bank for session view
      trackBank = host.createTrackBank(MAX_TRACKS, NUM_SENDS, MAX_SCENES, false);
      trackBank.setShouldShowClipLauncherFeedback(true);

      sceneBank = trackBank.sceneBank();
      sceneBank.setIndication(true);

      // Mark all track values as interested
      for (int i = 0; i < MAX_TRACKS; i++)
      {
         final Track track = trackBank.getItemAt(i);
         track.exists().markInterested();
         track.mute().markInterested();
         track.solo().markInterested();
         track.arm().markInterested();
         track.volume().markInterested();
         track.pan().markInterested();
         track.color().markInterested();
         track.isStopped().markInterested();

         final ClipLauncherSlotBank clipSlots = track.clipLauncherSlotBank();
         for (int j = 0; j < MAX_SCENES; j++)
         {
            final ClipLauncherSlot slot = clipSlots.getItemAt(j);
            slot.hasContent().markInterested();
            slot.isPlaying().markInterested();
            slot.isRecording().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.isRecordingQueued().markInterested();
            slot.isStopQueued().markInterested();
            slot.color().markInterested();
         }

         final SendBank sendBank = track.sendBank();
         for (int s = 0; s < NUM_SENDS; s++)
         {
            sendBank.getItemAt(s).markInterested();
            sendBank.getItemAt(s).exists().markInterested();
         }
      }

      // Scene bank
      for (int i = 0; i < MAX_SCENES; i++)
      {
         sceneBank.getScene(i).exists().markInterested();
      }

      // Scroll position tracking
      trackBank.canScrollForwards().markInterested();
      trackBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();
      sceneBank.canScrollBackwards().markInterested();

      // Dedicated mixer track bank — 8 strips, no sends/scenes (no clip launcher feedback needed),
      // excludes effect tracks so the user gets only their normal tracks scrollable.
      mixerTrackBank = host.createTrackBank(MIXER_BANK_SIZE, 0, 0, false);
      mixerTrackBank.canScrollForwards().markInterested();
      mixerTrackBank.canScrollBackwards().markInterested();
      for (int i = 0; i < MIXER_BANK_SIZE; i++)
      {
         final Track t = mixerTrackBank.getItemAt(i);
         t.exists().markInterested();
         t.color().markInterested();
         t.volume().markInterested();
         t.volume().value().markInterested();
         t.mute().markInterested();
         t.solo().markInterested();
         t.arm().markInterested();
      }

      // Effect track bank — fixed-size, gives us FX1 (and any future FX strips).
      effectTrackBank = host.createEffectTrackBank(MIXER_FX_COUNT, 0);
      for (int i = 0; i < MIXER_FX_COUNT; i++)
      {
         final Track t = effectTrackBank.getItemAt(i);
         t.exists().markInterested();
         t.color().markInterested();
         t.volume().markInterested();
         t.volume().value().markInterested();
         t.mute().markInterested();
         t.solo().markInterested();
      }

      // Master track value subscriptions for the mixer
      masterTrack.color().markInterested();
      masterTrack.volume().markInterested();
      masterTrack.volume().value().markInterested();
      masterTrack.mute().markInterested();

      // Dedicated bank for navigating to next/previous instrument tracks.
      // Flat list so group children are visible. Track types are observed
      // by InstrumentPage to filter for "Instrument" / "Hybrid" only.
      instrumentNavBank = host.createMainTrackBank(INSTRUMENT_NAV_BANK_SIZE, 0, 0);
      for (int i = 0; i < INSTRUMENT_NAV_BANK_SIZE; i++)
      {
         final Track t = instrumentNavBank.getItemAt(i);
         t.exists().markInterested();
         t.trackType().markInterested();
         t.position().markInterested();
      }

      // Drum pad bank that follows the cursor device. Used by DrumPadPage to
      // render a 4×4 grid of drum slots and to read their colors / mute / solo.
      cursorDrumPadBank = cursorDevice.createDrumPadBank(DRUM_PAD_BANK_SIZE);
      cursorDrumPadBank.scrollPosition().markInterested();
      for (int i = 0; i < DRUM_PAD_BANK_SIZE; i++)
      {
         final DrumPad pad = cursorDrumPadBank.getItemAt(i);
         pad.exists().markInterested();
         pad.color().markInterested();
         pad.mute().markInterested();
         pad.solo().markInterested();
         pad.name().markInterested();
      }
   }

   public void setSessionSize(final int width, final int height)
   {
      this.sessionWidth = Math.min(width, MAX_TRACKS);
      this.sessionHeight = Math.min(height, MAX_SCENES);
   }

   // --- Accessors ---

   public ControllerHost getHost() { return host; }
   public Transport getTransport() { return transport; }
   public Application getApplication() { return application; }
   public TrackBank getTrackBank() { return trackBank; }
   public TrackBank getMixerTrackBank() { return mixerTrackBank; }
   public TrackBank getEffectTrackBank() { return effectTrackBank; }
   public TrackBank getInstrumentNavBank() { return instrumentNavBank; }
   public int getInstrumentNavBankSize() { return INSTRUMENT_NAV_BANK_SIZE; }
   public DrumPadBank getCursorDrumPadBank() { return cursorDrumPadBank; }
   public int getDrumPadBankSize() { return DRUM_PAD_BANK_SIZE; }
   public SceneBank getSceneBank() { return sceneBank; }
   public CursorTrack getCursorTrack() { return cursorTrack; }
   public PinnableCursorDevice getCursorDevice() { return cursorDevice; }
   public CursorRemoteControlsPage getRemoteControls() { return remoteControls; }
   public MasterTrack getMasterTrack() { return masterTrack; }

   public int getSessionWidth() { return sessionWidth; }
   public int getSessionHeight() { return sessionHeight; }
}
