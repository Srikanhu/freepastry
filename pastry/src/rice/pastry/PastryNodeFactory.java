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

package rice.pastry;

import rice.pastry.messaging.*;
import rice.pastry.security.*;
import rice.pastry.leafset.*;
import rice.pastry.routing.*;

import java.util.*;

/**
 * The interface to an object which can construct PastryNodes.
 *
 * @version $Id$
 *
 * @author Andrew Ladd
 * @author Alan Mislove
 * @author Merziyah Poonawala
 * @author Abhishek Ray
 */
public abstract class PastryNodeFactory {

  /**
   * Hashtable which keeps track of temporary ping values, which are
   * only used during the getNearest() method
   */
  private Hashtable pingCache = new Hashtable();

  /**
   * Call this to construct a new node of the type chosen by the factory.
   *
   * @param bootstrap The node handle to bootstrap off of
   */
  public abstract PastryNode newNode(NodeHandle bootstrap);

  /**
   * Call this to construct a new node of the type chosen by the factory, with
   * the given nodeId.
   *
   * @param bootstrap The node handle to bootstrap off of
   * @param nodeId The nodeId of the new node
   */
  public abstract PastryNode newNode(NodeHandle bootstrap, NodeId nodeId);

  /**
   * This method returns the remote leafset of the provided handle
   * to the caller, in a protocol-dependent fashion.  Note that this method
   * may block while sending the message across the wire.
   *
   * @param handle The node to connect to
   * @return The leafset of the remote node
   */
  public abstract LeafSet getLeafSet(NodeHandle handle);

  /**
   * This method returns the remote route row of the provided handle
   * to the caller, in a protocol-dependent fashion.  Note that this method
   * may block while sending the message across the wire.
   *
   * @param handle The node to connect to
   * @param row The row number to retrieve
   * @return The route row of the remote node
   */
  public abstract RouteSet[] getRouteRow(NodeHandle handle, int row);

  /**
   * This method determines and returns the proximity of the current local
   * node the provided NodeHandle.  This will need to be done in a protocol-
   * dependent fashion and may need to be done in a special way.
   *
   * @param handle The handle to determine the proximity of
   * @return The proximity of the provided handle
   */
  public abstract int getProximity(NodeHandle local, NodeHandle handle);

  /**
   * Method which checks to see if we have a cached value of the remote ping, and
   * if not, initiates a ping and then caches the value
   *
   * @param handle The handle to ping
   * @return The proximity of the handle
   */
  private int proximity(NodeHandle local, NodeHandle handle) {
    Hashtable localTable = (Hashtable) pingCache.get(local.getNodeId());
    
    if (localTable == null) {
      localTable = new Hashtable();
      pingCache.put(local.getNodeId(), localTable);
    }
    
    if (localTable.get(handle.getNodeId()) == null) {
      int value = getProximity(local, handle);
      localTable.put(handle.getNodeId(), new Integer(value));

      return value;
    } else {
      return ((Integer) localTable.get(handle.getNodeId())).intValue();
    }
  }
  
  /**
   * This method implements the algorithm in the Pastry locality paper
   * for finding a close node the the current node through iterative
   * leafset and route row requests.  The seed node provided is any
   * node in the network which is a member of the pastry ring.  This
   * algorithm is designed to work in a protocol-independent manner, using
   * the getResponse(Message) method provided by subclasses.
   *
   * @param seed Any member of the pastry ring
   * @return A node suitable to boot off of (which is close the this node)
   */
  public NodeHandle getNearest(NodeHandle local, NodeHandle seed) {
 //   System.out.println("GET NEAREST STARTED WITH " + local + " SEED " + seed);
    
    // if the seed is null, we can't do anything
    if (seed == null)
      return null;
    
    // seed is the bootstrap node that we use to enter the pastry ring
    NodeHandle currentClosest = seed;
    NodeHandle nearNode = seed;

 //   System.out.println("GETTING LEAFSET");
    
    // get closest node in leafset
    nearNode = closestToMe(local, nearNode, getLeafSet(nearNode));

 //   System.out.println("GOT LEAFSET");

    // get the number of rows in a routing table
    // -- Here, we're going to be a little inefficient now.  It doesn't
    // -- impact correctness, but we're going to walk up from the bottom
    // -- of the routing table, even through some of the rows are probably
    // -- unfilled.  We'll optimize this in a later iteration.
    int depth = (NodeId.nodeIdBitLength / RoutingTable.idBaseBitLength);
    int i = 0;

    // now, iteratively walk up the routing table, picking the closest node
    // each time for the next request
    while (i < depth) {
 //     System.out.println("GETTING ROW "  + i);
      nearNode = closestToMe(local, nearNode, getRouteRow(nearNode, i));
      i++;
    }

    // finally, recursively examine the top level routing row of the nodes
    // until no more progress can be made
    do {
 //     System.out.println("RUNNING LAST STEP - CLOSEST IS " + nearNode + " " + proximity(local, nearNode));
      currentClosest = nearNode;
      nearNode = closestToMe(local, nearNode, getRouteRow(nearNode, depth-1));
    } while (! currentClosest.equals(nearNode));
    
 //   System.out.println("DONE - RETURNING NODE " + nearNode + " FROM SEED " + seed);

    if (nearNode.getLocalNode() == null) {
      nearNode.setLocalNode(local.getLocalNode());
    }

    // return the resulting closest node
    return nearNode;
  }

  /**
   * This method returns the closest node to the current node out of
   * the union of the provided handle and the node handles in the
   * leafset
   *
   * @param handle The handle to include
   * @param leafSet The leafset to include
   * @return The closest node out of handle union leafset
   */
  private NodeHandle closestToMe(NodeHandle local, NodeHandle handle, LeafSet leafSet) {
    Vector handles = new Vector();

    for (int i = 1; i <= leafSet.cwSize() ; i++)
      handles.add(leafSet.get(i));

    for (int i = -leafSet.ccwSize(); i < 0; i++)
      handles.add(leafSet.get(i));

    return closestToMe(local, handle, (NodeHandle[]) handles.toArray(new NodeHandle[0]));
  }

  /**
   * This method returns the closest node to the current node out of
   * the union of the provided handle and the node handles in the
   * routeset
   *
   * @param handle The handle to include
   * @param routeSet The routeset to include
   * @return The closest node out of handle union routeset
   */
  private NodeHandle closestToMe(NodeHandle local, NodeHandle handle, RouteSet[] routeSets) {
    Vector handles = new Vector();

    for (int i=0 ; i<routeSets.length ; i++) {
      RouteSet set = routeSets[i];

      if (set != null) {
        for (int j=0; j<set.size(); j++)
          handles.add(set.get(j));
      }
    }

    return closestToMe(local, handle, (NodeHandle[]) handles.toArray(new NodeHandle[0]));
  }

  /**
   * This method returns the closest node to the current node out of
   * the union of the provided handle and the node handles in the
   * array
   *
   * @param handle The handle to include
   * @param handles The array to include
   * @return The closest node out of handle union array
   */
  private NodeHandle closestToMe(NodeHandle local, NodeHandle handle, NodeHandle[] handles) {
    NodeHandle closestNode = handle;

    // shortest distance found till now    
    int nearestdist = proximity(local, closestNode);  

    for (int i=0; i < handles.length; i++) {
      NodeHandle tempNode = handles[i];

      if ((proximity(local, tempNode) < nearestdist) && tempNode.isAlive()) {
        nearestdist = proximity(local, tempNode);
        closestNode = tempNode;
      }
    }
    
    return closestNode;
  }
}
