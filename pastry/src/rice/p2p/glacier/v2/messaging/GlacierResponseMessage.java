package rice.p2p.glacier.v2.messaging;

import rice.*;
import rice.p2p.commonapi.*;
import rice.p2p.glacier.*;

public class GlacierResponseMessage extends GlacierMessage {
  protected FragmentKey[] keys;
  protected long[] lifetimes;
  protected boolean[] haveIt;
  protected boolean[] authoritative;

  public GlacierResponseMessage(int uid, FragmentKey key, boolean haveIt, long lifetime, boolean authoritative, NodeHandle source, Id dest, boolean isResponse, char tag) {
    this(uid, new FragmentKey[] { key }, new boolean[] { haveIt }, new long[] { lifetime }, new boolean[] { authoritative }, source, dest, isResponse, tag);
  }

  public GlacierResponseMessage(int uid, FragmentKey[] keys, boolean[] haveIt, long[] lifetimes, boolean[] authoritative, NodeHandle source, Id dest, boolean isResponse, char tag) {
    super(uid, source, dest, isResponse, tag);

    this.keys = keys;
    this.haveIt = haveIt;
    this.authoritative = authoritative;
    this.lifetimes = lifetimes;
  }

  public int numKeys() {
    return keys.length;
  }

  public FragmentKey getKey(int index) {
    return keys[index];
  }

  public boolean getHaveIt(int index) {
    return haveIt[index];
  }

  public boolean getAuthoritative(int index) {
    return authoritative[index];
  }

  public long getExpiration(int index) {
    return lifetimes[index];
  }

  public String toString() {
    return "[GlacierResponse for " + keys[0] + " ("+(numKeys()-1)+" more keys)]";
  }
}

