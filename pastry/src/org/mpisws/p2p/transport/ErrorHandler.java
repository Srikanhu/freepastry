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
package org.mpisws.p2p.transport;

import java.io.IOException;
import java.util.Map;

/**
 * Notified when there is a problem not related to an outgoing message/socket,
 * or when a callback isn't provided for a message.
 *  
 * @author Jeff Hoye
 *
 * @param <Identifier> the type of identifier at this layer
 * @param <E> the types of exceptions to expect
 */
public interface ErrorHandler<Identifier> {
  /**
   * @param i the sender of the message (as can best be determined)
   * @param bytes the entire message/socket header
   * @param location the location in the bytes that is unexpected
   */
  public void receivedUnexpectedData(Identifier i, byte[] bytes, int location, Map<String, Integer> options);

  /**
   * We got an exception.
   * 
   * @param i the identifier responsible (if any)
   * @param error the exception
   */
  public void receivedException(Identifier i, Throwable error);
}
