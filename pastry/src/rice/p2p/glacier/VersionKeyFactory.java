package rice.p2p.glacier;
import java.util.StringTokenizer;

import rice.p2p.commonapi.*;
import rice.p2p.multiring.MultiringIdFactory;
import rice.p2p.multiring.RingId;
import rice.pastry.Id;
import java.util.Random;

/**
 * DESCRIBE THE CLASS
 *
 * @version $Id$
 * @author ahae
 */
public class VersionKeyFactory implements IdFactory {

  private MultiringIdFactory FACTORY;

  /**
   * Constructor for VersionKeyFactory.
   *
   * @param factory DESCRIBE THE PARAMETER
   */
  public VersionKeyFactory(MultiringIdFactory factory) {
    FACTORY = factory;
  }

  /**
   * DESCRIBE THE METHOD
   *
   * @param material DESCRIBE THE PARAMETER
   * @return DESCRIBE THE RETURN VALUE
   */
  public rice.p2p.commonapi.Id buildId(byte[] material) {
    System.err.println("VersionKeyFactory.buildId(byte[]) called");
    System.exit(1);
    return null;
  }

  /**
   * Builds a protocol-specific Id given the source data.
   *
   * @param material The material to use
   * @return The built Id.
   */
  public rice.p2p.commonapi.Id buildId(int[] material) {
    System.err.println("VersionKeyFactory.buildId(int[]) called");
    System.exit(1);
    return null;
  }

  /**
   * Builds a protocol-specific Id by using the hash of the given string as
   * source data.
   *
   * @param string The string to use as source data
   * @return The built Id.
   */
  public rice.p2p.commonapi.Id buildId(String string) {
    System.err.println("VersionKeyFactory.buildId(String) called");
    System.exit(1);
    return null;
  }

  /**
   * Builds a random protocol-specific Id.
   *
   * @param rng A random number generator
   * @return The built Id.
   */
  public rice.p2p.commonapi.Id buildRandomId(Random rng) {
    return new VersionKey(FACTORY.buildRandomId(rng), rng.nextLong());
  }

  /**
   * DESCRIBE THE METHOD
   *
   * @param string DESCRIBE THE PARAMETER
   * @return DESCRIBE THE RETURN VALUE
   */
  public rice.p2p.commonapi.Id buildIdFromToString(String string) {
    StringTokenizer stok = new StringTokenizer(string, "(,)- :v");
    if (stok.countTokens() < 3) {
      return null;
    }

    String keyRingS = stok.nextToken();
    String keyNodeS = stok.nextToken();
    String versionS = stok.nextToken();
    RingId key = FACTORY.buildRingId(rice.pastry.Id.build(keyRingS), rice.pastry.Id.build(keyNodeS));

    return new VersionKey(key, Long.valueOf(versionS).longValue());
  }

  public rice.p2p.commonapi.Id buildIdFromToString(char[] chars, int offset, int length) {
    System.err.println("VersionKeyFactory.buildIdFromToString(char[], int, int) called");
    System.exit(1);
    return null;
  }

  /**
   * Builds a protocol-specific Id.Distance given the source data.
   *
   * @param material The material to use
   * @return The built Id.Distance.
   */
  public rice.p2p.commonapi.Id.Distance buildIdDistance(byte[] material) {
    System.err.println("VersionKeyFactory.buildIdDistance() called");
    System.exit(1);
    return null;
  }

  /**
   * Creates an IdRange given the CW and CCW ids.
   *
   * @param cw The clockwise Id
   * @param ccw The counterclockwise Id
   * @return An IdRange with the appropriate delimiters.
   */
  public IdRange buildIdRange(rice.p2p.commonapi.Id cw, rice.p2p.commonapi.Id ccw) {
    System.err.println("VersionKeyFactory.buildIdRange() called");
    System.exit(1);
    return null;
  }

  /**
   * Creates an empty IdSet.
   *
   * @return an empty IdSet
   */
  public IdSet buildIdSet() {
    return new VersionKeySet();
  }

  /**
   * Creates an empty NodeHandleSet.
   *
   * @return an empty NodeHandleSet
   */
  public NodeHandleSet buildNodeHandleSet() {
    System.err.println("VersionKeyFactory.buildNodeHandleSet() called");
    System.exit(1);
    return null;
  }
  
  public int getIdToStringLength() {
    return FACTORY.getIdToStringLength() + 2;
  }
}