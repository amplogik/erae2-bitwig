package com.erae2bitwig.core;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;

import com.bitwig.extension.callback.ShortMidiDataReceivedCallback;

import java.util.function.Consumer;

/**
 * Manages the two MIDI port pairs for Erae Touch 2.
 * Port 0 (Main): Script protocol - buttons, sliders, LEDs, page-switch CC
 * Port 1 (Lab): Low-level API - drawing, FingerStream
 */
public class Erae2MidiPorts
{
   private final MidiIn mainIn;
   private final MidiOut mainOut;
   private final MidiIn labIn;
   private final MidiOut labOut;
   private final ControllerHost host;

   private Consumer<String> scriptSysExHandler;

   public Erae2MidiPorts(final ControllerHost host)
   {
      this.host = host;

      mainIn = host.getMidiInPort(0);
      mainOut = host.getMidiOutPort(0);
      labIn = host.getMidiInPort(1);
      labOut = host.getMidiOutPort(1);

      // Register SysEx callback on Main port for Script Protocol
      mainIn.setSysexCallback(this::onMainSysEx);

      // Register SysEx callback on Lab port for Low-Level API (future use)
      labIn.setSysexCallback(this::onLabSysEx);
   }

   /** Set handler for Script Protocol SysEx messages (inbound on Main port). */
   public void setScriptSysExHandler(final Consumer<String> handler)
   {
      this.scriptSysExHandler = handler;
   }

   /** Set MIDI callback on Main port for CC messages (page switching). */
   public void setMainMidiCallback(final ShortMidiDataReceivedCallback callback)
   {
      mainIn.setMidiCallback(callback);
   }

   /** Send raw SysEx hex string on Main port. */
   public void sendMainSysEx(final String hex)
   {
      mainOut.sendSysex(hex);
   }

   /** Send raw SysEx hex string on Lab port. */
   public void sendLabSysEx(final String hex)
   {
      labOut.sendSysex(hex);
   }

   /** Send MIDI message on Main port. */
   public void sendMainMidi(final int status, final int data1, final int data2)
   {
      mainOut.sendMidi(status, data1, data2);
   }

   public MidiIn getMainIn()
   {
      return mainIn;
   }

   public MidiOut getMainOut()
   {
      return mainOut;
   }

   public MidiIn getLabIn()
   {
      return labIn;
   }

   public MidiOut getLabOut()
   {
      return labOut;
   }

   public ControllerHost getHost()
   {
      return host;
   }

   private void onMainSysEx(final String data)
   {
      if (scriptSysExHandler != null)
      {
         scriptSysExHandler.accept(data);
      }
   }

   private void onLabSysEx(final String data)
   {
      // Future: handle FingerStream and zone boundary replies
      host.println("Erae Touch 2: Lab SysEx received: " + data);
   }
}
