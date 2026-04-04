package com.erae2bitwig.sysex;

import com.erae2bitwig.core.TouchEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * Parses incoming FingerStream SysEx messages from the Erae Touch 2 API mode.
 *
 * Message format (after F0 stripped by Bitwig, hex string input):
 * The Erae sends API responses as SysEx. After the manufacturer/product header,
 * messages contain the receiver_prefix [01 02 03] followed by either:
 *   - 0x7F + reply type (zone boundary, API version)
 *   - action/finger byte + zone + finger ID + XYZ data (touch events)
 */
public class FingerStreamParser
{
   /** The receiver prefix we sent when enabling API mode. */
   private static final int[] RECEIVER_PREFIX = SysExConstants.API_RECEIVER_PREFIX;

   /** Non-finger message indicator. */
   private static final int NON_FINGER = 0x7F;

   /** Reply types for non-finger messages. */
   private static final int ZONE_BOUNDARY_REPLY = 0x01;
   private static final int API_VERSION_REPLY = 0x02;

   private Consumer<TouchEvent> touchHandler;
   private ZoneBoundaryHandler zoneBoundaryHandler;
   private Consumer<String> logger;

   public interface ZoneBoundaryHandler
   {
      void onZoneBoundary(int zoneId, int width, int height);
   }

   public void setTouchHandler(final Consumer<TouchEvent> handler)
   {
      this.touchHandler = handler;
   }

   public void setZoneBoundaryHandler(final ZoneBoundaryHandler handler)
   {
      this.zoneBoundaryHandler = handler;
   }

   public void setLogger(final Consumer<String> logger)
   {
      this.logger = logger;
   }

   /**
    * Parse an incoming SysEx message (as hex string from Bitwig callback).
    * Returns true if this was a recognized API message.
    */
   public boolean parse(final String hexData)
   {
      final int[] bytes = hexStringToBytes(hexData);
      if (bytes.length < 4) return false;

      // Find the receiver prefix in the message.
      // Bitwig provides full SysEx including F0 at start and F7 at end.
      // The API response has the manufacturer header then our receiver prefix.
      final int prefixStart = findReceiverPrefix(bytes);
      if (prefixStart < 0) return false;

      int offset = prefixStart + RECEIVER_PREFIX.length;
      if (offset >= bytes.length - 1) return false;

      if (bytes[offset] == NON_FINGER)
      {
         // Non-finger message (zone boundary, API version)
         offset++;
         if (offset >= bytes.length - 1) return false;

         if (bytes[offset] == ZONE_BOUNDARY_REPLY)
         {
            offset++;
            if (offset + 2 < bytes.length)
            {
               final int zoneId = bytes[offset];
               final int width = bytes[offset + 1];
               final int height = bytes[offset + 2];
               log("Zone boundary: zone=" + zoneId + " w=" + width + " h=" + height);
               if (zoneBoundaryHandler != null)
               {
                  zoneBoundaryHandler.onZoneBoundary(zoneId, width, height);
               }
            }
            return true;
         }
         else if (bytes[offset] == API_VERSION_REPLY)
         {
            offset++;
            if (offset < bytes.length - 1)
            {
               log("API version: " + bytes[offset]);
            }
            return true;
         }
         return true;
      }
      else
      {
         // FingerStream touch event
         return parseFingerStream(bytes, offset);
      }
   }

   private boolean parseFingerStream(final int[] bytes, int offset)
   {
      if (offset + 2 >= bytes.length) return false;

      // Byte 0: action/finger byte (action in low 3 bits)
      final int actionFingerByte = bytes[offset++];
      final int action = actionFingerByte & 0x07;

      // Byte 1: zone ID
      final int zoneId = bytes[offset++];

      // Finger ID: 8 bytes raw, 7-bit encoded
      final int fingerIdEncodedLen = SysExEncoder.bitized7Size(8);
      if (offset + fingerIdEncodedLen >= bytes.length) return false;

      final int[] fingerIdEncoded = new int[fingerIdEncodedLen];
      System.arraycopy(bytes, offset, fingerIdEncoded, 0, fingerIdEncodedLen);
      offset += fingerIdEncodedLen;

      final byte[] fingerIdBytes = SysExEncoder.decode7bit(fingerIdEncoded, fingerIdEncodedLen);
      // Finger ID is a 64-bit value, we just use the first byte as index
      final int fingerIndex = fingerIdBytes[0] & 0xFF;

      // XYZ: 3 x 4-byte floats = 12 bytes raw, 7-bit encoded
      final int xyzEncodedLen = SysExEncoder.bitized7Size(12);
      if (offset + xyzEncodedLen >= bytes.length) return false;

      final int[] xyzEncoded = new int[xyzEncodedLen];
      System.arraycopy(bytes, offset, xyzEncoded, 0, xyzEncodedLen);
      offset += xyzEncodedLen;

      // Checksum byte follows
      // (we skip validation for now)

      final byte[] xyzBytes = SysExEncoder.decode7bit(xyzEncoded, xyzEncodedLen);
      if (xyzBytes.length < 12) return false;

      // Decode 3 little-endian floats
      final ByteBuffer buf = ByteBuffer.wrap(xyzBytes).order(ByteOrder.LITTLE_ENDIAN);
      final float x = buf.getFloat(0);
      final float y = buf.getFloat(4);
      final float z = buf.getFloat(8);

      final TouchEvent event = new TouchEvent(fingerIndex, zoneId, action, x, y, z);

      if (touchHandler != null)
      {
         touchHandler.accept(event);
      }

      return true;
   }

   /**
    * Find the receiver prefix [01 02 03] in the SysEx byte array.
    * Returns the index where the prefix starts, or -1 if not found.
    */
   private int findReceiverPrefix(final int[] bytes)
   {
      for (int i = 0; i <= bytes.length - RECEIVER_PREFIX.length; i++)
      {
         boolean match = true;
         for (int j = 0; j < RECEIVER_PREFIX.length; j++)
         {
            if (bytes[i + j] != RECEIVER_PREFIX[j])
            {
               match = false;
               break;
            }
         }
         if (match) return i;
      }
      return -1;
   }

   private int[] hexStringToBytes(final String hex)
   {
      final String clean = hex.replace(" ", "");
      final int len = clean.length() / 2;
      final int[] result = new int[len];
      for (int i = 0; i < len; i++)
      {
         result[i] = Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
      }
      return result;
   }

   private void log(final String msg)
   {
      if (logger != null) logger.accept("Erae Touch 2: " + msg);
   }
}
