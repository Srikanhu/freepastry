package rice.p2p.util;

import java.io.*;
import java.math.*;
import java.util.*;

/**
 * This class contains a large number of static methods for performing
 * math operations.
 *
 * @version $Id$
 * @author amislove
 */
public class MathUtils {
  
  /**
  * The array used for conversion to hexidecimal
   */
  public final static char[] HEX_ARRAY = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
  
  /**
   * The random number generate for generating random bytes
   */
  private static Random random = new Random();

  /**
   * Make the constructor private so no MathUtils is ever created.
   */
  private MathUtils() {
  }
  
  /**
   * Utility which does *proper* modding, where the result is guaranteed to be
   * positive.
   *
   * @param a The modee
   * @param b The value to mod by
   * @return The result
   */
  public static int mod(int a, int b) {
    return ((a % b) + b) % b;
  }
  
  /**
   * Utility method which xors two given byte arrays, of equal length, and returns the results
   *
   * @param a The first array
   * @param b The second array
   * @return A new byte array, xored
   */
  public static byte[] xor(byte[] a, byte[] b) {
    byte[] result = new byte[a.length];
    
    for (int i=0; i<result.length; i++) 
      result[i] = (byte) (a[i] ^ b[i]);
    
    return result;
  }

  /**
   * Method which returns a specified number of random bytes
   *
   * @param len The number of random bytes to generate
   */
  public static byte[] randomBytes(int len) {
    byte[] result = new byte[len];
    random.nextBytes(result);
    
    return result;
  }
  
  /**
   * Method which returns a random int
   *
   * @param len The number of random bytes to generate
   */
  public static int randomInt() {
    return random.nextInt();
  }
  
  /**
   * Utility method which converts a byte[] to a hexidecimal string of characters, in lower case
   *
   * @param text The array to convert
   * @return A string representation
   */
  public static String toHex(byte[] text) {
    StringBuffer buffer = new StringBuffer();
    
    for (int i=0; i<text.length; i++) {
      buffer.append(HEX_ARRAY[0x0f & (text[i] >> 4)]);
      buffer.append(HEX_ARRAY[0x0f & text[i]]);
    }
    
    return buffer.toString(); 
  }
  
  /**
   * Utility method which converts a hex string to a byte[]
   *
   * @param text The text to convert
   * @return The bytes
   */
  public static byte[] fromHex(String text) {
    byte[] result = new byte[text.length() / 2];
    
    for (int i=0; i<result.length; i++) 
      result[i] = (byte) ((byte) ((byte) 0xf0 & ((getByte(text.charAt(2*i))) << 4)) | getByte(text.charAt(2*i + 1)));
    
    return result;
  }

  /**  
   * Utility method for converting a char to a byte
   *
   * @param c The char
   * @return The byte
   */  
  protected static byte getByte(char c) {
    if ((c >= '0') && (c <= '9'))
      return (byte) (c - '0');
    else if ((c >= 'A') && (c <= 'F'))
      return (byte) (10 + (byte) (c - 'A'));
    else if ((c >= 'a') && (c <= 'f'))
      return (byte) (10 + (byte) (c - 'a'));
    else
      throw new RuntimeException("Could not decode hex character '" + c + "'");
  }
  
  /**
   * Utility method for converting a int into a byte[]
   *
   * @param input The log to convert
   * @return a byte[] representation
   */
  public static byte[] intToByteArray(int input) {
    byte[] output = new byte[4];
    intToByteArray(input, output, 0);
    return output;
  }
  
  /**
   * Utility method for converting a int into a byte[]
   *
   * @param input The log to convert
   * @return a byte[] representation
   */
  public static void intToByteArray(int input, byte[] output, int offset) {
    output[offset + 0] = (byte) (0xFF & (input >> 24));
    output[offset + 1] = (byte) (0xFF & (input >> 16));
    output[offset + 2] = (byte) (0xFF & (input >> 8));
    output[offset + 3] = (byte) (0xFF & input);
  }
  
  /**
   * Utility method for converting a byte[] into a int
   *
   * @param input The byte[] to convert
   * @return a int representation
   */
  public static int byteArrayToInt(byte[] input) {
    input = correctLength(input, 4);

    int result;
    result  = (input[0] & 0xFF) << 24;
    result |= (input[1] & 0xFF) << 16;
    result |= (input[2] & 0xFF) << 8;
    result |= (input[3] & 0xFF);

    return result;
  }

  /**
   * Utility method for converting a long into a byte[]
   *
   * @param input The log to convert
   * @return a byte[] representation
   */
  public static byte[] longToByteArray(long input) {
    byte[] output = new byte[8];
    longToByteArray(input, output, 0);
    return output;
  }
  
  /**
   * Utility method for converting a long into a byte[]
   *
   * @param input The log to convert
   * @return a byte[] representation
   */
  public static void longToByteArray(long input, byte[] output, int offset) {    
    output[offset + 0] = (byte) (0xFF & (input >> 56));
    output[offset + 1] = (byte) (0xFF & (input >> 48));
    output[offset + 2] = (byte) (0xFF & (input >> 40));
    output[offset + 3] = (byte) (0xFF & (input >> 32));
    output[offset + 4] = (byte) (0xFF & (input >> 24));
    output[offset + 5] = (byte) (0xFF & (input >> 16));
    output[offset + 6] = (byte) (0xFF & (input >> 8));
    output[offset + 7] = (byte) (0xFF & input);
  }

  /**
   * Utility method for converting a byte[] into a long
   *
   * @param input The byte[] to convert
   * @return a long representation
   */
  public static long byteArrayToLong(byte[] input) {
    input = correctLength(input, 8);
 
    long result;
    result  = ((long)(input[0] & 0xFF)) << 56;
    result |= ((long)(input[1] & 0xFF)) << 48;
    result |= ((long)(input[2] & 0xFF)) << 40;
    result |= ((long)(input[3] & 0xFF)) << 32;
    result |= ((long)(input[4] & 0xFF)) << 24;
    result |= ((long)(input[5] & 0xFF)) << 16;
    result |= ((long)(input[6] & 0xFF)) << 8;
    result |= ((long)(input[7] & 0xFF));

    return result;
  }

  /**
   * Utility method for ensuring the array is of the proper length.  THis
   * method enforces the length by appending 0's or returning a subset of
   * the input array.
   *
   * @param data The input array
   * @param length The length the array should be
   * @return A correct-length array
   */
  private static byte[] correctLength(byte[] data, int length) {
    if (data.length >= length)
      return data;
    
    byte[] result = new byte[length];

    for (int i=0; (i<data.length) && (i<result.length); i++) 
      result[i] = data[i];

    return result;
  }
}
