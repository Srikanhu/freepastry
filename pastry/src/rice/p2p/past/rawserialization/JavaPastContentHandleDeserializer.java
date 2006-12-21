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
/*
 * Created on Mar 23, 2006
 */
package rice.p2p.past.rawserialization;

import java.io.*;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.past.*;
import rice.p2p.util.rawserialization.JavaDeserializer;

public class JavaPastContentHandleDeserializer implements
    PastContentHandleDeserializer {

  public PastContentHandle deserializePastContentHandle(InputBuffer buf, Endpoint endpoint, short contentType) throws IOException {   
    switch(contentType) {
      case 0:
        byte[] array = new byte[buf.readInt()];
        buf.read(array);
        
        ObjectInputStream ois = new JavaDeserializer(new ByteArrayInputStream(array), endpoint);

        try {
          Object o = ois.readObject();
          PastContentHandle ret = (PastContentHandle)o;
          return ret;
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Unknown class type in message - cant deserialize.", e);
        }            
      case ContentHashPastContentHandle.TYPE:
        return new ContentHashPastContentHandle(buf, endpoint);
    }
    throw new IllegalArgumentException("contentType must be 0 was:"+contentType+" endpoint:"+endpoint); 
  }
}
