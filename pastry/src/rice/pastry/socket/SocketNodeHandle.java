/**
 * "FreePastry" Peer-to-Peer Application Development Substrate Copyright 2002,
 * Rice University. All rights reserved. Redistribution and use in source and
 * binary forms, with or without modification, are permitted provided that the
 * following conditions are met: - Redistributions of source code must retain
 * the above copyright notice, this list of conditions and the following
 * disclaimer. - Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. -
 * Neither the name of Rice University (RICE) nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission. This software is provided by RICE and the
 * contributors on an "as is" basis, without any representations or warranties
 * of any kind, express or implied including, but not limited to,
 * representations or warranties of non-infringement, merchantability or fitness
 * for a particular purpose. In no event shall RICE or contributors be liable
 * for any direct, indirect, incidental, special, exemplary, or consequential
 * damages (including, but not limited to, procurement of substitute goods or
 * services; loss of use, data, or profits; or business interruption) however
 * caused and on any theory of liability, whether in contract, strict liability,
 * or tort (including negligence or otherwise) arising in any way out of the use
 * of this software, even if advised of the possibility of such damage.
 */

package rice.pastry.socket;

import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Random;

import rice.pastry.Log;
import rice.pastry.NodeId;
import rice.pastry.dist.DistNodeHandle;
import rice.pastry.messaging.Message;
import rice.pastry.messaging.MessageReceiver;
import rice.pastry.socket.exception.TooManyMessagesException;

/**
 * Represents a remote node for a "real" IP network.
 * 
 * Read the package specification for implementation details.
 * 
 * A feature that SocketNodeHandle has that other DistNodeHandles may not, is the 
 * ability to introspect into the congestion level.  (@see #getNumberMessagesAllowed())
 *
 * @version $Id$
 * @author Alan Mislove, Jeff Hoye
 */
public class SocketNodeHandle extends DistNodeHandle {

  /**
   * The default distance, which is used before a ping
   */
  public static int DEFAULT_PROXIMITY = Integer.MAX_VALUE;
  
  public static String REASON_TOO_LARGE = "message size was too large";

  public static String REASON_CONNECTION_FAULTY = "connection was faulty";

  public static String REASON_TOO_MANY_MESSAGES_QUEUED = "too many messages enqueued";

  /**
   * Constructor
   *
   * @param nodeId This node handle's node Id.
   * @param address the IP address of this node.
   */
  public SocketNodeHandle(InetSocketAddress address, NodeId nodeId) {
    super(nodeId, address);
  }

  /**
   * Returns the last known status information about the Pastry node
   * associated with this handle. Invoking this method does not cause network
   * activity.
   *
   * @return true if the node is alive, false otherwise.
   */
  public int getLiveness() {
    SocketPastryNode spn = (SocketPastryNode) getLocalNode();

    if (spn == null) {
      //System.out.println("SNH.getLiveness(): spn == null");
      return LIVENESS_ALIVE;
    } else {
      return spn.getSocketCollectionManager().getLiveness(this);
    } 
  }
  
  /**
   * Returns the number of messages that can be queued before an exception is thrown.
   * A typical way to call this would be:
   * <code>
   * SocketNodeHandle snh;
   * Message m;
   * ...
   * if (snh.getNumberMessagesAllowedToSend(snh.getMessageType(m) > 10) {
   *   snh.send(m);
   * } else {
   *  // send it later
   * }
   * </code>
   * @param type The type of message.
   * @return the number of messages of this type that 
   * can be sent before an exception is thrown.
   */
  public int getNumberMessagesAllowedToSend(int type) {
    SocketPastryNode spn = (SocketPastryNode) getLocalNode();
    if (spn == null) {
      //System.out.println("SNH.getLiveness(): spn == null");
      return -1;
    } else {
      ConnectionManager cm = spn.getSocketCollectionManager().getConnectionManager(this);
      return cm.getNumberMessagesAllowedToSend(type);
    }     
  }

  /**
   * Returns the type of message.
   * @param m The message you want to determine the type of.
   * @return The message type.  ConnectionManager.TYPE_CONTROL, TYPE_DATA.
   */
  public int getMessageType(Message m) {
    SocketPastryNode spn = (SocketPastryNode) getLocalNode();
    if (spn == null) {
      //System.out.println("SNH.getLiveness(): spn == null");
      return -1;
    } else {
      ConnectionManager cm = spn.getSocketCollectionManager().getConnectionManager(this);
      if (cm != null) {
        return cm.getMessageType(m);
      } 
      return -1;
    }     
  }

  /**
   * Called to send a message to the node corresponding to this handle.
   *
   * @param msg Message to be delivered, may or may not be routeMessage.
   */
  public void receiveMessage(Message msg) {
    assertLocalNode();
    
    SocketPastryNode spn = (SocketPastryNode) getLocalNode();

    if (spn.getNodeId().equals(nodeId)) {
      //debug("Sending message " + msg + " locally");
        spn.receiveMessage(msg);
    } else {
      debug("Passing message " + msg + " to the socket controller for writing");
      try {        
        spn.getSocketCollectionManager().send(this, msg);
      } catch (TooManyMessagesException tmme) {
        spn.sManager.messageNotSent(msg, REASON_TOO_MANY_MESSAGES_QUEUED);
//        throw tmme;
      }
    }
  }

  /**
   * Returns a String representation of this DistNodeHandle. This method is
   * designed to be called by clients using the node handle, and is provided in
   * order to ensure that the right node handle is being talked to.
   *
   * @return A String representation of the node handle.
   */
  public String toString() {
    if (getLocalNode() == null) {
      return "[SNH: " + nodeId + "@" + epoch+ "/" + address + "]";
    } else {
      return "[SNH: " + getLocalNode().getNodeId() + " -> " + nodeId + "@" + epoch + "/" + address + "]";
    }
  }

  /**
   * Equivalence relation for nodehandles. They are equal if and only if their
   * corresponding NodeIds are equal.
   *
   * @param obj the other nodehandle .
   * @return true if they are equal, false otherwise.
   */
  public boolean equals(Object obj) {
    SocketNodeHandle that = (SocketNodeHandle)obj;
    if (that == null) {
      return false;
    }
    //System.out.println(this+"equals("+that+")");  
    if (this.epoch != that.epoch) {
//      System.out.println(this+".equals("+that+"):"+false);
      return false;
    }
    boolean ret = that.getNodeId().equals(getNodeId());
//    System.out.println(this+".equals("+that+"):"+ret);
    return ret;
  }

  /**
   * Hash codes for node handles. It is the hashcode of their corresponding
   * NodeId's.
   *
   * @return a hash code.
   */
  public int hashCode() {
    return epoch*getNodeId().hashCode();
  }

  /**
   * Returns the last known proximity information about the Pastry node
   * associated with this handle. Invoking this method does not cause network
   * activity. Smaller values imply greater proximity. The exact nature and
   * interpretation of the proximity metric implementation-specific.
   *
   * @return the proximity metric value
   */
  public int proximity() {
    SocketPastryNode spn = (SocketPastryNode) getLocalNode();

    if (spn == null) {
      return 0;
    } else
      if (spn.getNodeId().equals(nodeId)) {
      return 0;
    } else {
      return spn.getSocketCollectionManager().proximity(this);
    }
  }

  /**
   * Ping the node. Refreshes the cached liveness status and proximity value of
   * the Pastry node associated with this. Invoking this method causes network
   * activity.
   *
   * @return true if node is currently alive.
   */
  public boolean ping() {
    SocketPastryNode spn = (SocketPastryNode) getLocalNode();

    if (spn != null) {
      spn.getPingManager().ping(this, null);
      //spn.getPingManager().ping(getAddress(),new TestResponseListener(getAddress(),getNodeId()));
    }

    return isAlive();
  }

  /**
   * DESCRIBE THE METHOD
   *
   * @param prl DESCRIBE THE PARAMETER
   * @return DESCRIBE THE RETURN VALUE
   */
  public boolean ping(PingResponseListener prl) {
    SocketPastryNode spn = (SocketPastryNode) getLocalNode();

    if (spn != null) {
      spn.getPingManager().ping(this, prl);
    }

    return isAlive();
  }

  /**
   * Method which registers this handle with the node that it is currently on.
   *
   */
  public void afterSetLocalNode() {
    ((SocketNodeHandlePool) ((SocketPastryNode) getLocalNode()).getNodeHandlePool()).record(this);
  }

  /**
   * DESCRIBE THE METHOD
   *
   * @param o DESCRIBE THE PARAMETER
   * @param obj DESCRIBE THE PARAMETER
   */
  public void update(Observable o, Object obj) {
  }

  /*
   *  static int lastResponseListener = 0;
   *  class TestResponseListener implements PingResponseListener {
   *  int myName;
   *  NodeId myId;
   *  public TestResponseListener(InetSocketAddress address, NodeId id) {
   *  myName = lastResponseListener++;
   *  myId = id;
   *  System.out.println(myName+".ping("+address+","+id+")");
   *  }
   *  public void pingResponse(
   *  InetSocketAddress address,
   *  long RTT,
   *  long timeHeardFrom) {
   *  System.out.println(myName+".pingResponse("+address+","+RTT+","+timeHeardFrom+")");
   *  }
   *  }
   */

  /**
   * Method which allows the observers of this socket node handle to be updated.
   * This method sets this object as changed, and then sends out the update.
   *
   * @param update The update
   */
  protected void update(Object update) {
    setChanged();
    notifyObservers(update);
  }

  /**
   * DESCRIBE THE METHOD
   *
   * @param s DESCRIBE THE PARAMETER
   */
  private void debug(String s) {
    if (Log.ifp(8)) {
      System.out.println(this + ": " + s);
    }
  }
}


