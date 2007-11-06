/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice  University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/ 
package rice.pastry.transport;

import java.util.Map;

import org.mpisws.p2p.transport.MessageRequestHandle;
import org.mpisws.p2p.transport.commonapi.TransportLayerNodeHandle;
import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;

import rice.p2p.commonapi.rawserialization.RawMessage;
import rice.pastry.NodeHandle;
import rice.pastry.messaging.Message;

public class PMessageReceiptImpl implements PMessageReceipt {
  MessageRequestHandle<NodeHandle, RawMessage> internal;
  Message message;
  
  public PMessageReceiptImpl(Message msg) {
    this.message = msg;
  }

  public NodeHandle getIdentifier() {
    if (internal == null) return null;
    return (NodeHandle)internal.getIdentifier();
  }

  public Message getMessage() {
    return message;
  }

  public Map<String, Object> getOptions() {
    return internal.getOptions();
  }

  public boolean cancel() {
    if (internal != null) return internal.cancel();
    return false;
  }

  public void setInternal(MessageRequestHandle<NodeHandle, RawMessage> name) {
    this.internal = name;
  }
  
  public String toString() {
    return "PMsgRecptI{"+message+","+(internal==null?null:getIdentifier())+"}";
  }
}
