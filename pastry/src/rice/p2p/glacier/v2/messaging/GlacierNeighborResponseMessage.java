/*************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate 

Copyright 2002, Rice University. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither  the name  of Rice  University (RICE) nor  the names  of its
contributors may be  used to endorse or promote  products derived from
this software without specific prior written permission.

This software is provided by RICE and the contributors on an "as is"
basis, without any representations or warranties of any kind, express
or implied including, but not limited to, representations or
warranties of non-infringement, merchantability or fitness for a
particular purpose. In no event shall RICE or contributors be liable
for any direct, indirect, incidental, special, exemplary, or
consequential damages (including, but not limited to, procurement of
substitute goods or services; loss of use, data, or profits; or
business interruption) however caused and on any theory of liability,
whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even
if advised of the possibility of such damage.

********************************************************************************/
package rice.p2p.glacier.v2.messaging;

import java.io.IOException;

import rice.*;
import rice.p2p.commonapi.*;
import rice.p2p.commonapi.rawserialization.*;

public class GlacierNeighborResponseMessage extends GlacierMessage {
  public static final short TYPE = 4;

  protected Id[] neighbors;
  protected long[] lastSeen;

  public GlacierNeighborResponseMessage(int uid, Id[] neighbors, long[] lastSeen, NodeHandle source, Id dest, char tag) {
    super(uid, source, dest, true, tag);

    this.neighbors = neighbors;
    this.lastSeen = lastSeen;
  }

  public int numNeighbors() {
    if ((neighbors == null) || (lastSeen == null))
      return 0;
      
    if (lastSeen.length < neighbors.length)
      return lastSeen.length;
      
    return neighbors.length;
  }

  public Id getNeighbor(int index) {
    return neighbors[index];
  }
  
  public long getLastSeen(int index) {
    return lastSeen[index];
  }

  public String toString() {
    return "[GlacierNeighborResponse with "+numNeighbors()+" keys]";
  }
  
  /***************** Raw Serialization ***************************************/
  public short getType() {
    return TYPE; 
  }

  public void serialize(OutputBuffer buf) throws IOException {
    buf.writeByte((byte)0); // version    
    super.serialize(buf);
    buf.writeInt(lastSeen.length);
    for (int i = 0; i < lastSeen.length; i++) {
      buf.writeLong(lastSeen[i]); 
    }
    
    buf.writeInt(neighbors.length);
    for (int i = 0; i < neighbors.length; i++) {
      buf.writeShort(neighbors[i].getType());
      neighbors[i].serialize(buf); 
    }
  }
  
  
  public static GlacierNeighborResponseMessage build(InputBuffer buf, Endpoint endpoint) throws IOException {
    byte version = buf.readByte();
    switch(version) {
      case 0:
        return new GlacierNeighborResponseMessage(buf, endpoint);
      default:
        throw new IOException("Unknown Version: "+version);
    }
  }
    
  private GlacierNeighborResponseMessage(InputBuffer buf, Endpoint endpoint) throws IOException {
    super(buf, endpoint);
    lastSeen = new long[buf.readInt()];
    for (int i = 0; i < lastSeen.length; i++) {
      lastSeen[i] = buf.readLong(); 
    }
    neighbors = new Id[buf.readInt()];
    for (int i = 0; i < lastSeen.length; i++) {
      neighbors[i] = endpoint.readId(buf, buf.readShort()); 
    }
  }
}

