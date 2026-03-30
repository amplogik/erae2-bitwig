package com.erae2bitwig.sysex;

import com.erae2bitwig.core.Erae2MidiPorts;

/**
 * Protocol B: Low-Level API for Erae Touch 2 drawing and FingerStream.
 * Communicates via the Lab MIDI port.
 * Currently a stub - will be fully implemented for pages that need
 * custom visual feedback (MPE surface, XY controller, etc.)
 */
public class LowLevelApi
{
   private final Erae2MidiPorts ports;

   public LowLevelApi(final Erae2MidiPorts ports)
   {
      this.ports = ports;
   }

   /** Enable API mode on the Erae. Must be called before draw commands. */
   public void enableApiMode()
   {
      final int[] msg = new int[SysExConstants.API_HEADER.length +
         1 + SysExConstants.API_RECEIVER_PREFIX.length + 1];
      System.arraycopy(SysExConstants.API_HEADER, 0, msg, 0, SysExConstants.API_HEADER.length);
      int idx = SysExConstants.API_HEADER.length;
      msg[idx++] = SysExConstants.API_MODE_ENABLE;
      for (final int b : SysExConstants.API_RECEIVER_PREFIX)
      {
         msg[idx++] = b;
      }
      msg[idx] = SysExConstants.SYSEX_END;
      ports.sendLabSysEx(SysExConstants.toHexString(msg));
   }

   /** Disable API mode. */
   public void disableApiMode()
   {
      final int[] msg = buildApiCommand(SysExConstants.API_MODE_DISABLE);
      ports.sendLabSysEx(SysExConstants.toHexString(msg));
   }

   /** Clear all pixels in a zone. */
   public void clearZone(final int zoneId)
   {
      final int[] msg = buildApiCommand(SysExConstants.CLEAR_ZONE, zoneId);
      ports.sendLabSysEx(SysExConstants.toHexString(msg));
   }

   /** Draw a single pixel. Colors are 0-127 (7-bit). */
   public void drawPixel(final int zoneId, final int x, final int y,
                         final int r, final int g, final int b)
   {
      final int[] msg = buildApiCommand(SysExConstants.DRAW_PIXEL,
         zoneId, x, y, r, g, b);
      ports.sendLabSysEx(SysExConstants.toHexString(msg));
   }

   /** Draw a filled rectangle. Colors are 0-127 (7-bit). */
   public void drawRectangle(final int zoneId, final int x, final int y,
                             final int width, final int height,
                             final int r, final int g, final int b)
   {
      final int[] msg = buildApiCommand(SysExConstants.DRAW_RECTANGLE,
         zoneId, x, y, width, height, r, g, b);
      ports.sendLabSysEx(SysExConstants.toHexString(msg));
   }

   /** Request zone boundary dimensions. */
   public void requestZoneBoundary(final int zoneId)
   {
      final int[] msg = buildApiCommand(SysExConstants.ZONE_BOUNDARY_REQUEST, zoneId);
      ports.sendLabSysEx(SysExConstants.toHexString(msg));
   }

   private int[] buildApiCommand(final int command, final int... params)
   {
      final int[] header = SysExConstants.API_HEADER;
      final int[] msg = new int[header.length + 1 + params.length + 1];
      System.arraycopy(header, 0, msg, 0, header.length);
      msg[header.length] = command;
      System.arraycopy(params, 0, msg, header.length + 1, params.length);
      msg[msg.length - 1] = SysExConstants.SYSEX_END;
      return msg;
   }
}
