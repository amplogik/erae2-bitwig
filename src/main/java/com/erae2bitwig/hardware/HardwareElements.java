package com.erae2bitwig.hardware;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;

import com.erae2bitwig.core.Erae2MidiPorts;
import com.erae2bitwig.sysex.ScriptProtocol;
import com.erae2bitwig.sysex.SysExConstants;

/**
 * Factory that creates all hardware control elements for the Erae Touch 2.
 * Button IDs and slider IDs match the Ableton script protocol.
 */
public class HardwareElements
{
   private final HardwareSurface surface;

   // Navigation
   private final EraeButton upButton;
   private final EraeButton downButton;
   private final EraeButton leftButton;
   private final EraeButton rightButton;

   // Transport
   private final EraeButton playButton;
   private final EraeButton stopButton;
   private final EraeButton stopAllButton;

   // Session matrix (up to 12x10)
   private final EraeButton[][] matrixButtons;

   // Scene launch (up to 10)
   private final EraeButton[] sceneLaunchButtons;

   // Mixer: mute, solo, arm (up to 12 each)
   private final EraeButton[] muteButtons;
   private final EraeButton[] soloButtons;
   private final EraeButton[] armButtons;

   // Sliders: volume (0-11), send1 (12-23), send2 (24-35)
   private final EraeSlider[] volumeSliders;
   private final EraeSlider[] send1Sliders;
   private final EraeSlider[] send2Sliders;

   private static final int MAX_TRACKS = 12;
   private static final int MAX_SCENES = 10;

   public HardwareElements(final ControllerHost host,
                           final Erae2MidiPorts midiPorts,
                           final ScriptProtocol protocol)
   {
      surface = host.createHardwareSurface();
      surface.setPhysicalSize(300, 200);

      final MidiIn mainIn = midiPorts.getMainIn();

      // Navigation buttons
      upButton = createActionButton(host, mainIn, protocol, SysExConstants.BUTTON_UP, "Up");
      downButton = createActionButton(host, mainIn, protocol, SysExConstants.BUTTON_DOWN, "Down");
      leftButton = createActionButton(host, mainIn, protocol, SysExConstants.BUTTON_LEFT, "Left");
      rightButton = createActionButton(host, mainIn, protocol, SysExConstants.BUTTON_RIGHT, "Right");

      // Transport
      playButton = createActionButton(host, mainIn, protocol, SysExConstants.BUTTON_PLAY, "Play");
      stopButton = createActionButton(host, mainIn, protocol, SysExConstants.BUTTON_STOP, "Stop");
      stopAllButton = createActionButton(host, mainIn, protocol, SysExConstants.BUTTON_STOP_ALL, "StopAll");

      // Session matrix: 12 columns x 10 rows
      matrixButtons = new EraeButton[MAX_TRACKS][MAX_SCENES];
      for (int row = 0; row < MAX_SCENES; row++)
      {
         for (int col = 0; col < MAX_TRACKS; col++)
         {
            final int id = col + (row * SysExConstants.MATRIX_COLUMN_STRIDE);
            matrixButtons[col][row] = new EraeButton(host, surface, mainIn, protocol,
               SysExConstants.MATRIX_BUTTON_PREFIX, id,
               "Matrix_" + col + "_" + row);
         }
      }

      // Scene launch: IDs 0-9
      sceneLaunchButtons = new EraeButton[MAX_SCENES];
      for (int i = 0; i < MAX_SCENES; i++)
      {
         sceneLaunchButtons[i] = createActionButton(host, mainIn, protocol, i,
            "SceneLaunch_" + i);
      }

      // Mute: IDs 21-32
      muteButtons = new EraeButton[MAX_TRACKS];
      for (int i = 0; i < MAX_TRACKS; i++)
      {
         muteButtons[i] = createActionButton(host, mainIn, protocol,
            SysExConstants.MUTE_OFFSET + i, "Mute_" + i);
      }

      // Solo: IDs 9-20
      soloButtons = new EraeButton[MAX_TRACKS];
      for (int i = 0; i < MAX_TRACKS; i++)
      {
         soloButtons[i] = createActionButton(host, mainIn, protocol,
            SysExConstants.SOLO_OFFSET + i, "Solo_" + i);
      }

      // Arm: IDs 33-44
      armButtons = new EraeButton[MAX_TRACKS];
      for (int i = 0; i < MAX_TRACKS; i++)
      {
         armButtons[i] = createActionButton(host, mainIn, protocol,
            SysExConstants.ARM_OFFSET + i, "Arm_" + i);
      }

      // Volume sliders: IDs 0-11
      volumeSliders = new EraeSlider[MAX_TRACKS];
      for (int i = 0; i < MAX_TRACKS; i++)
      {
         volumeSliders[i] = new EraeSlider(host, surface, mainIn, protocol, i,
            "Volume_" + i);
      }

      // Send1 sliders: IDs 12-23
      send1Sliders = new EraeSlider[MAX_TRACKS];
      for (int i = 0; i < MAX_TRACKS; i++)
      {
         send1Sliders[i] = new EraeSlider(host, surface, mainIn, protocol, i + 12,
            "Send1_" + i);
      }

      // Send2 sliders: IDs 24-35
      send2Sliders = new EraeSlider[MAX_TRACKS];
      for (int i = 0; i < MAX_TRACKS; i++)
      {
         send2Sliders[i] = new EraeSlider(host, surface, mainIn, protocol, i + 24,
            "Send2_" + i);
      }
   }

   private EraeButton createActionButton(final ControllerHost host, final MidiIn midiIn,
                                         final ScriptProtocol protocol,
                                         final int buttonId, final String name)
   {
      return new EraeButton(host, surface, midiIn, protocol,
         SysExConstants.ACTION_BUTTON_PREFIX, buttonId, name);
   }

   /** Call from extension flush() to push pending hardware updates. */
   public void flush()
   {
      surface.updateHardware();
   }

   // --- Accessors ---

   public HardwareSurface getSurface() { return surface; }

   public EraeButton getUpButton() { return upButton; }
   public EraeButton getDownButton() { return downButton; }
   public EraeButton getLeftButton() { return leftButton; }
   public EraeButton getRightButton() { return rightButton; }

   public EraeButton getPlayButton() { return playButton; }
   public EraeButton getStopButton() { return stopButton; }
   public EraeButton getStopAllButton() { return stopAllButton; }

   public EraeButton getMatrixButton(final int col, final int row) { return matrixButtons[col][row]; }
   public EraeButton getSceneLaunchButton(final int index) { return sceneLaunchButtons[index]; }

   public EraeButton getMuteButton(final int track) { return muteButtons[track]; }
   public EraeButton getSoloButton(final int track) { return soloButtons[track]; }
   public EraeButton getArmButton(final int track) { return armButtons[track]; }

   public EraeSlider getVolumeSlider(final int track) { return volumeSliders[track]; }
   public EraeSlider getSend1Slider(final int track) { return send1Sliders[track]; }
   public EraeSlider getSend2Slider(final int track) { return send2Sliders[track]; }

   public int getMaxTracks() { return MAX_TRACKS; }
   public int getMaxScenes() { return MAX_SCENES; }
}
