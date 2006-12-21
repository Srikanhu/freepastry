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
package rice.visualization;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.security.*;


import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.pastry.dist.DistNodeHandle;
import rice.visualization.client.UpdateJarResponse;
import rice.visualization.client.VisualizationClient;
import rice.visualization.data.Data;
import rice.visualization.data.DataProvider;

public class Visualization implements DataProvider {
  
  public static int PORT_OFFSET = 3847;
  
  public static int STATE_ALIVE = VisualizationClient.STATE_ALIVE;
  public static int STATE_DEAD = VisualizationClient.STATE_DEAD;
  public static int STATE_UNKNOWN = VisualizationClient.STATE_UNKNOWN;
  public static int STATE_FAULT = VisualizationClient.STATE_FAULT;
  
  public static int REFRESH_TIME = 1000;
    
  /**
   * String name to Ring
   */
  protected Hashtable rings;
  
  /**
   * Parallel data structure to provide order
   */
  protected Ring[] ringArray;
  
  protected VisualizationFrame frame;
  
  protected Node selectedNode = null;
  protected Ring selectedRing;
  
  protected Node highlightedNode = null;
  protected Ring highlightedRing = null;
  
  protected Data data;
    
  protected Environment environment;  
  protected Logger logger;
  
  public Visualization(Ring[] bootstrapNodes, Environment env) {
    this.environment = env;
    logger = environment.getLogManager().getLogger(Visualization.class,null);
    if (logger.level <= Logger.INFO) {
      for (int i = 0; i < bootstrapNodes.length; i++) {
        logger.log(bootstrapNodes[i].toString());
      }        
    }
    ringArray = bootstrapNodes;

    this.rings = new Hashtable();
    for (int i = 0; i < bootstrapNodes.length; i++) {
      rings.put(bootstrapNodes[i].name,bootstrapNodes[i]);
      bootstrapNodes[i].setVisualization(this);
    }
    selectedRing = bootstrapNodes[0];
    bootstrapNodes[0].select();
    
    this.frame = new VisualizationFrame(this);
    
    //selectedRing.touchAllNodes();
    
    Thread t = new Thread() {
      public void run() {
        try {
          while (true) {
            Thread.sleep(REFRESH_TIME);
            refreshData();
          }
        } catch (Exception e) {
          if (logger.level <= Logger.SEVERE) logger.logException(
              "",e);
        }
      }
    };
    
    t.start(); 
    
    //addNode(handle);
  }
  
  public Node getSelectedNode() {
    return selectedNode;
  }
  
  public Ring getSelectedRing() {
    return selectedRing;
  }

  /**
   * @return The number of rings.
   */  
  public int getNumRings() {
    return rings.size();
  }
  
  /**
   * This is kind of a silly way to lookup rings, but hey, this is graphics programming.
   * @param index
   * @return the index'th ring.
   */
  public Ring getRingByIndex(int index) {
    return ringArray[index];
  }
  
  public Node getHighlighted() {
    return highlightedNode;
  }
  
  protected void refreshData() {
    Node handle = getSelectedNode();
    if (handle != null) {
      getData(handle);
      frame.repaint();
    }
  }
  
  public Node[] getNodes() {
    return getNodes(selectedRing);
  }
  
  public Node[] getNodes(Ring r) {
    return r.getNodes();
  }
  
  public Data getData() {
    return data;
  }
  
  public void setHighlighted(Node node, Ring ring) {
    if ((highlightedNode != node) || (highlightedRing != ring)) {
      highlightedNode = node;
      highlightedRing = ring;      
      try {
        frame.nodeHighlighted(node);
      } catch (NullPointerException npe) {
        if (logger.level <= Logger.SEVERE) logger.logException(
            "ERROR: Visualization.setHighlighted() frame == null!!!", npe);
      }
    }
  }
  
//  public void setSelected(InetSocketAddress addr) {
//    setSelected(addr,selectedRing);
//  }
  
  public void setSelected(InetSocketAddress addr, Ring r) {
    Node[] handles = r.getNodes();
    
    for (int i=0; i<handles.length; i++) {
      if (handles[i].handle.getAddress().equals(addr)) {
        setSelected(handles[i]);
        return;
      }
    }
  }

  public Ring getRoot() {
    return getRingByIndex(0);
  }

  class MyTimerTask extends TimerTask {
		public void run() {
      curStep++;
      getRoot().rootCenterIsStale = true;
      
      if (curStep >= NUM_STEPS) {
        cancel();		
        myTask = null;	
      }
      frame.pastryRingPanel.repaint();
		}    
  }

  int NUM_STEPS = 30;
  int curStep = NUM_STEPS;
  MyTimerTask myTask = null;
  Timer timer = new Timer();
  private void startAnimation() {
    curStep = 0;
    if (myTask != null) {
      myTask.cancel();
      myTask = null;
    }
    myTask = new MyTimerTask();
    timer.schedule(myTask,30,30);
  }

  public void selectRing(Ring r) {
    selectedRing = r;
    r.select();
    startAnimation();
    
    boolean repaint = false;
    if (selectedNode == null)
      repaint = true;
    setSelected((Node)null);
    if (repaint) {
      refreshData();
      frame.nodeSelected(selectedNode, data);
    }
  }  

//  public void setSelected(NodeId id) {
//    setSelected(id,selectedRing);
//  }  

  public void setSelected(rice.pastry.Id id, Ring r) {
    Node[] handles = r.getNodes();
      
    for (int i=0; i<handles.length; i++) {
      if (handles[i].handle.getNodeId().equals(id)) {
        setSelected(handles[i]);
        return;
      }
    }
  }
  
  public Node getNode(int x, int y) {
    // try the selected ring
    return selectedRing.getNode(x,y);
 /*   Ring root = getRingByIndex(0);
    // try the main ring, which will recursively try all rings
    if ((n == null) && (selectedRing != root)) { 
      n = root.getNode(x,y);
    }
    return n; */
  }
  
  public Ring getRing(int x, int y) {
    // try the selected ring
    Ring root = getRingByIndex(0);
    // try the main ring, which will recursively try all rings
    Ring sel = root.getRing(x,y);
    return sel;
  }
  
  public void setSelected(Node node) {
    //Thread.dumpStack();
    if ((selectedNode == null) || (! selectedNode.equals(node))) {
      selectedNode = node;      
      frame.nodeSelected(node, data);
    }
  }
  
  public int getState(Node node) {
    if (node.ring.clients.get(node.handle.getNodeId()) != null)
      return ((VisualizationClient) node.ring.clients.get(node.handle.getNodeId())).getState();
    else 
      return STATE_UNKNOWN;
  }
  
  public Node[] getNeighbors(Node handle) {
    if (handle.neighbors.size() == 0)
      return new Node[0];
    
    return (Node[]) handle.neighbors.toArray(new Node[0]);
  }
  
  public synchronized UpdateJarResponse updateJar(File[] files, String executionString) {
    if (selectedNode == null) {
      throw new RuntimeException("No Node Selected");
    }
    VisualizationClient client = (VisualizationClient) selectedRing.clients.get(selectedNode.handle.getNodeId());
    return client.updateJar(files,executionString);    
  }
  
  public void openDebugConsole() {
    if (selectedNode == null) {
      throw new RuntimeException("No Node Selected");
    }
    VisualizationClient client = (VisualizationClient) selectedRing.clients.get(selectedNode.handle.getNodeId());
    DebugCommandFrame consoleFrame = new DebugCommandFrame(client);
    consoleFrame.pack();
  }

//  protected Data getData(DistNodeHandle handle) {
//    return getData(handle,selectedRing);
//  }

  protected Data getData(Node handle) {
    return getData(handle, false); 
  }
  protected Data getData(Node handle, boolean leafsetOnly) {
    Ring r = handle.ring;
      VisualizationClient client = (VisualizationClient) r.clients.get(handle.handle.getNodeId());
      
      if (client == null) {
        InetSocketAddress address = new InetSocketAddress(handle.handle.getAddress().getAddress(), handle.handle.getAddress().getPort() + PORT_OFFSET);
        client = new VisualizationClient(r.getKeyPair().getPrivate(), address, environment);
        r.clients.put(handle.handle.getId(), client);
        client.connect();
      }
      
      DistNodeHandle[] handles = client.getHandles();
      
      if (handles == null) {
        handle.neighbors = new Vector(); // clear the neighbors
        return new Data();
      } else {
        handle.neighbors = new Vector(); // clear the neighbors
//        r.neighbors.put(handle.handle.getId(), handles);
      
        for (int i=0; i<handles.length; i++) 
          handle.neighbors.add(r.addNode(handles[i]));
        if (leafsetOnly) return null;
        data = client.getData();
      
        return data;
      }
  }
  
  public Environment getEnvironment() {
    return environment;
  }
}
