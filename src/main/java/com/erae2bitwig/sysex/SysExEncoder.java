package com.erae2bitwig.sysex;

/**
 * Utility for 7-bit MIDI SysEx encoding/decoding.
 * Used by the Low-Level API (Protocol B) for image data and FingerStream.
 */
public final class SysExEncoder
{
   private SysExEncoder() {}

   /**
    * Encode 8-bit data into 7-bit format with XOR checksum.
    * Each group of 7 bytes becomes 8 bytes:
    * [high-bits byte] [7 masked bytes]
    */
   public static int[] encode7bit(final byte[] data)
   {
      final int encodedSize = bitized7Size(data.length);
      final int[] result = new int[encodedSize + 1]; // +1 for checksum
      int outIdx = 0;

      for (int i = 0; i < data.length; i += 7)
      {
         int highBits = 0;
         final int chunkSize = Math.min(7, data.length - i);

         for (int j = 0; j < chunkSize; j++)
         {
            highBits |= ((data[i + j] & 0x80) >> (j + 1));
         }
         result[outIdx++] = highBits;

         for (int j = 0; j < chunkSize; j++)
         {
            result[outIdx++] = data[i + j] & 0x7F;
         }
      }

      // XOR checksum over all encoded bytes
      int checksum = 0;
      for (int k = 0; k < outIdx; k++)
      {
         checksum ^= result[k];
      }
      result[outIdx] = checksum;

      return result;
   }

   /**
    * Decode 7-bit encoded data back to 8-bit.
    */
   public static byte[] decode7bit(final int[] data, final int length)
   {
      final int decodedSize = unbitized7Size(length);
      final byte[] result = new byte[decodedSize];
      int outIdx = 0;

      for (int i = 0; i < length; i += 8)
      {
         final int highBits = data[i];
         final int chunkSize = Math.min(7, length - i - 1);

         for (int j = 0; j < chunkSize; j++)
         {
            result[outIdx + j] = (byte) (((highBits << (j + 1)) & 0x80) | data[i + j + 1]);
         }
         outIdx += 7;
      }

      return result;
   }

   /** Calculate encoded size (excluding checksum). */
   public static int bitized7Size(final int length)
   {
      return (length / 7) * 8 + ((length % 7 > 0) ? (1 + length % 7) : 0);
   }

   /** Calculate decoded size from encoded data length. */
   public static int unbitized7Size(final int encodedLength)
   {
      return (encodedLength / 8) * 7 + Math.max(0, (encodedLength % 8) - 1);
   }
}
