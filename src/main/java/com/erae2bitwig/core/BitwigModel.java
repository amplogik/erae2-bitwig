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

      // Device cursor following track selection
      cursorDevice = cursorTrack.createCursorDevice("erae2-device", "Device Control", 0,
         CursorDeviceFollowMode.FOLLOW_SELECTION);
      cursorDevice.exists().markInterested();

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
   public SceneBank getSceneBank() { return sceneBank; }
   public CursorTrack getCursorTrack() { return cursorTrack; }
   public PinnableCursorDevice getCursorDevice() { return cursorDevice; }
   public CursorRemoteControlsPage getRemoteControls() { return remoteControls; }
   public MasterTrack getMasterTrack() { return masterTrack; }

   public int getSessionWidth() { return sessionWidth; }
   public int getSessionHeight() { return sessionHeight; }
}
