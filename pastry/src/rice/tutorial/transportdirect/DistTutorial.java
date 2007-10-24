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
package rice.tutorial.transportdirect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;

import org.mpisws.p2p.transport.TransportLayer;
import org.mpisws.p2p.transport.direct.DirectTransportLayer;
import org.mpisws.p2p.transport.proximity.ProximityProvider;

import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.direct.EuclideanNetwork;
import rice.pastry.direct.NetworkSimulator;
import rice.pastry.direct.NodeRecord;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.pastry.transport.NodeHandleAdapter;
import rice.pastry.transport.TLPastryNode;

/**
 * This tutorial shows how to setup a FreePastry node using the Socket Protocol.
 * 
 * @author Jeff Hoye
 */
public class DistTutorial {

  // this will keep track of our applications
  Vector apps = new Vector();
  
  /**
   * This constructor launches numNodes PastryNodes.  They will bootstrap 
   * to an existing ring if one exists at the specified location, otherwise
   * it will start a new ring.
   * 
   * @param bindport the local port to bind to 
   * @param bootaddress the IP:port of the node to boot from
   * @param numNodes the number of nodes to create in this JVM
   * @param env the environment for these nodes
   */
  public DistTutorial(int bindport, InetSocketAddress bootaddress, int numNodes, Environment env) throws Exception {
    // Construct the simulator
    final NetworkSimulator<InetSocketAddress, ByteBuffer> simulator = new EuclideanNetwork<InetSocketAddress, ByteBuffer>(env);
    simulator.setMaxSpeed(10.0f);
    
    // Generate the NodeIds Randomly
    NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
    
    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
    PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env) {
      
      /**
       * Swap in Direct instead of wire.
       */
      @Override
      protected TransportLayer<InetSocketAddress, ByteBuffer> getWireTransportLayer(final InetSocketAddress innermostAddress, TLPastryNode pn) throws IOException {
        DirectTransportLayer<InetSocketAddress, ByteBuffer> tl = new DirectTransportLayer<InetSocketAddress, ByteBuffer>(
            innermostAddress, simulator.getGenericSimulator(), simulator.getLivenessProvider(), pn.getEnvironment());


//        NodeHandleAdapter nha = new NodeHandleAdapter(tl,simulator.getLivenessProvider(),new ProximityProvider<InetSocketAddress>(){    
//          public int proximity(InetSocketAddress i) {
//            return (int)simulator.proximity(innermostAddress, i);
//          }
//        
//          List<ProximityListener<InetSocketAdd>> proxListeners = new ArrayList<ProximityListener<NodeHandle>>();
//          public void addProximityListener(ProximityListener<NodeHandle> name) {
//            proxListeners.add(name);
//          }
//
//          public boolean removeProximityListener(ProximityListener<NodeHandle> name) {
//            return proxListeners.remove(name);
//          }    
//        });
        
        simulator.registerNode(innermostAddress, tl, simulator.generateNodeRecord());

        return tl; //super.getWireTransportLayer(innermostAddress, pn);
      }      
    };

    
    simulator.start();
    // loop to construct the nodes/apps
    for (int curNode = 0; curNode < numNodes; curNode++) {
      // This will return null if we there is no node at that location
      NodeHandle bootHandle = ((SocketPastryNodeFactory)factory).getNodeHandle(bootaddress);
  
      // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
      PastryNode node = factory.newNode(bootHandle);
        
      // the node may require sending several messages to fully boot into the ring
      synchronized(node) {
        while(!node.isReady() && !node.joinFailed()) {
          // delay so we don't busy-wait
          node.wait(500);
          
          // abort if can't join
          if (node.joinFailed()) {
            throw new IOException("Could not join the FreePastry ring.  Reason:"+node.joinFailedReason()); 
          }
        }       
      }
      
      System.out.println("Finished creating new node "+node);
      
      // construct a new MyApp
      MyApp app = new MyApp(node);
      
      apps.add(app);
    }
      
    // wait 10 seconds
    env.getTimeSource().sleep(10000);

      
    // route 10 messages
    for (int i = 0; i < 10; i++) {
        
      // for each app
      Iterator appIterator = apps.iterator();
      while(appIterator.hasNext()) {
        MyApp app = (MyApp)appIterator.next();
        
        // pick a key at random
        Id randId = nidFactory.generateNodeId();
        
        // send to that key
        app.routeMyMsg(randId);
        
        // wait a bit
        env.getTimeSource().sleep(100);
      }
    }
    // wait 1 second
    env.getTimeSource().sleep(1000);
      
    // for each app
    Iterator appIterator = apps.iterator();
    while(appIterator.hasNext()) {
      MyApp app = (MyApp)appIterator.next();
      PastryNode node = (PastryNode)app.getNode();
      
      // send directly to my leafset
      LeafSet leafSet = node.getLeafSet();
      
      // this is a typical loop to cover your leafset.  Note that if the leafset
      // overlaps, then duplicate nodes will be sent to twice
      for (int i=-leafSet.ccwSize(); i<=leafSet.cwSize(); i++) {
        if (i != 0) { // don't send to self
          // select the item
          NodeHandle nh = leafSet.get(i);
          
          // send the message directly to the node
          app.routeMyMsgDirect(nh);   
          
          // wait a bit
          env.getTimeSource().sleep(100);
        }
      }
    }
  }

  /**
   * Usage: 
   * java [-cp FreePastry-<version>.jar] rice.tutorial.lesson4.DistTutorial localbindport bootIP bootPort numNodes
   * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001 10
   */
  public static void main(String[] args) throws Exception {
    // Loads pastry settings
    Environment env = Environment.directEnvironment();
    
    // disable the UPnP setting (in case you are testing this on a NATted LAN)
    env.getParameters().setString("nat_search_policy","never");
    
    try {
      // the port to use locally
      int bindport = Integer.parseInt(args[0]);
      
      // build the bootaddress from the command line args
      InetAddress bootaddr = InetAddress.getByName(args[1]);
      int bootport = Integer.parseInt(args[2]);
      InetSocketAddress bootaddress = new InetSocketAddress(bootaddr,bootport);
  
      // the number of nodes to use
      int numNodes = Integer.parseInt(args[3]);    
      
      // launch our node!
      DistTutorial dt = new DistTutorial(bindport, bootaddress, numNodes, env);
    } catch (Exception e) {
      // remind user how to use
      System.out.println("Usage:"); 
      System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.lesson4.DistTutorial localbindport bootIP bootPort numNodes");
      System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001 10");
      throw e; 
    }
  }
}
