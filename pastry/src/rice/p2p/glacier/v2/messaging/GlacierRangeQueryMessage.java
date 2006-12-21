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
import rice.p2p.glacier.*;

public class GlacierRangeQueryMessage extends GlacierMessage {
  public static final short TYPE = 7;

  protected IdRange requestedRange;

  public GlacierRangeQueryMessage(int uid, IdRange requestedRange, NodeHandle source, Id dest, char tag) {
    super(uid, source, dest, false, tag);

    this.requestedRange = requestedRange;
  }

  public IdRange getRequestedRange() {
    return requestedRange;
  }

  public String toString() {
    return "[GlacierRangeQuery #"+getUID()+" for " + requestedRange + "]";
  }
    
  /***************** Raw Serialization ***************************************/
  public short getType() {
    return TYPE; 
  }

  public void serialize(OutputBuffer buf) throws IOException {
    buf.writeByte((byte)0); // version    
    super.serialize(buf);
    requestedRange.serialize(buf);
  }
  
  public static GlacierRangeQueryMessage build(InputBuffer buf, Endpoint endpoint) throws IOException {
    byte version = buf.readByte();
    switch(version) {
      case 0:
        return new GlacierRangeQueryMessage(buf, endpoint);
      default:
        throw new IOException("Unknown Version: "+version);
    }
  }
    
  private GlacierRangeQueryMessage(InputBuffer buf, Endpoint endpoint) throws IOException {
    super(buf, endpoint);
    requestedRange = endpoint.readIdRange(buf);
  }
}

