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
package rice.visualization.server;

import rice.visualization.data.*;
import rice.pastry.*;
import rice.p2p.glacier.v2.*;
import rice.p2p.multiring.RingId;
import rice.persistence.*;
import rice.Continuation.*;

import java.util.*;

public class GlacierPanelCreator implements PanelCreator, GlacierStatisticsListener {

  private static final int HISTORY = 300;
  private final long startup;
  
  GlacierImpl glacier;
  GlacierStatistics history[] = new GlacierStatistics[HISTORY];
  int msgTotal[] = new int[20];
  int msgSent = 0;
  int historyPtr = 0;
  
  public GlacierPanelCreator(GlacierImpl glacier) {
    this.glacier = glacier;
    startup = glacier.getEnvironment().getTimeSource().currentTimeMillis();
    glacier.addStatisticsListener(this);
    Arrays.fill(msgTotal, 0);
    for (int i=0; i<HISTORY; i++)
      history[i] = null;
  }
  
  public DataPanel createPanel(Object[] objects) {

    DataPanel glacierPanel = new DataPanel("Traffic");

    Constraints glacierCons = new Constraints();
    glacierCons.gridx = 0;
    glacierCons.gridy = 0;
    glacierCons.fill = Constraints.HORIZONTAL;
    
    KeyValueListView glacierView = new KeyValueListView("Glacier Overview", 380, 200, glacierCons);
    GlacierStatistics current = history[(historyPtr + HISTORY - 1) % HISTORY];
    if (current != null) {
      rice.p2p.commonapi.IdRange responsibleRange = current.responsibleRange;
      rice.p2p.commonapi.Id ccwBoundary = responsibleRange.getCCWId();
      rice.p2p.commonapi.Id cwBoundary = responsibleRange.getCWId();
      
      if (ccwBoundary instanceof RingId)
        ccwBoundary = ((RingId)ccwBoundary).getId();
      if (cwBoundary instanceof RingId)
        cwBoundary = ((RingId)cwBoundary).getId();
        
      long uptime = glacier.getEnvironment().getTimeSource().currentTimeMillis() - startup;
      long upMin = uptime / (60*1000);
      long upHours = upMin / 60;
      long upDays = upHours / 24;
        
      glacierView.add("Pending req", "" + current.pendingRequests);
      glacierView.add("Neighbors", "" + current.numNeighbors);
      glacierView.add("Fragments", "" + current.numFragments);
      glacierView.add("Continuations", "" + current.numContinuations);
      glacierView.add("Objs in trash", "" + current.numObjectsInTrash);
      glacierView.add("Msg sent", "" + msgSent);
      glacierView.add("Range", ccwBoundary + " - " + cwBoundary);
      glacierView.add("Local time", (new Date()).toString());
      glacierView.add("Uptime", upDays+"d "+(upHours%24)+"h "+(upMin%60)+"m");
      glacierView.add("Fragm. total", "" + current.fragmentStorageSize + " bytes");
      glacierView.add("Trash total", "" + current.trashStorageSize + " bytes");
      glacierView.add("Bandw. limit", "" + current.bucketTokensPerSecond + " / " + current.bucketMaxBurstSize);
    } else {
      glacierView.add("Starting up...", "");
    }

    Constraints countCons = new Constraints();
    countCons.gridx = 1;
    countCons.gridy = 0;
    countCons.fill = Constraints.HORIZONTAL;
    KeyValueListView countView = new KeyValueListView("Message count", 380, 200, countCons);

    Constraints graphCons = new Constraints();
    graphCons.gridx = 2;
    graphCons.gridy = 0;
    graphCons.fill = Constraints.HORIZONTAL;
    LineGraphView graphView = new LineGraphView("Messages", 380, 200, graphCons, "Minutes", "Messages", false, false);

    String tagToString[] = new String[] { "--", "Neighbor", "Sync", "SyncManifests", "SyncFetch", "Handoff", "Debug", "Refresh", "Insert", "LookupHandles", "Lookup", "Fetch", "LocalScan" };
    Color tagToColor[] = new Color[] { Color.white, Color.green, Color.blue, Color.black, Color.cyan, Color.darkGray, Color.lightGray, Color.magenta, Color.orange, Color.pink, Color.red, Color.yellow, Color.gray };
    double[] timeSeries = new double[HISTORY];
    for (int i=1; i<=12; i++) {
      countView.add(tagToString[i], (current == null) ? "?" : ""+current.messagesSentByTag[i]+" ("+msgTotal[i]+" total)");
      double[] countSeries = new double[HISTORY];
      for (int j=0; j<HISTORY; j++) {
        GlacierStatistics statJ = history[(historyPtr+j) % HISTORY];
        timeSeries[j] = j;
        countSeries[j] = (statJ == null) ? 0 : statJ.messagesSentByTag[i];
      }
      
      graphView.addSeries("Tag "+i, timeSeries, countSeries, tagToColor[i]);
    }
      
    glacierPanel.addDataView(glacierView);
    glacierPanel.addDataView(countView);
    glacierPanel.addDataView(graphView);

    /* SECOND PANEL ======================================================= */

    DataPanel glacierPanel2 = new DataPanel("Queue");

    Constraints graph2Cons = new Constraints();
    graph2Cons.gridx = 0;
    graph2Cons.gridy = 0;
    graph2Cons.fill = Constraints.HORIZONTAL;

    LineGraphView fetchView = new LineGraphView("Pending fetches", 380, 200, graph2Cons, "Minutes", "Queue entries", false, false);
    timeSeries = new double[HISTORY];
    double[] activeSeries = new double[HISTORY];
    double[] pendingSeries = new double[HISTORY];
    for (int j=0; j<HISTORY; j++) {
      GlacierStatistics statJ = history[(historyPtr+j) % HISTORY];
      timeSeries[j] = j;
      activeSeries[j] = (statJ == null) ? 0 : statJ.activeFetches;
      pendingSeries[j] = (statJ == null) ? 0 : statJ.pendingRequests;
    }
    fetchView.addSeries("F", timeSeries, activeSeries, Color.green);
    fetchView.addSeries("F", timeSeries, pendingSeries, Color.blue);
    glacierPanel2.addDataView(fetchView);

    Constraints bucketCons = new Constraints();
    bucketCons.gridx = 1;
    bucketCons.gridy = 0;
    bucketCons.fill = Constraints.HORIZONTAL;

    LineGraphView bucketView = new LineGraphView("Token bucket", 380, 200, bucketCons, "Minutes", "Tokens", false, false);
    timeSeries = new double[HISTORY];
    double[] minSeries = new double[HISTORY];
    double[] maxSeries = new double[HISTORY];
    for (int j=0; j<HISTORY; j++) {
      GlacierStatistics statJ = history[(historyPtr+j) % HISTORY];
      timeSeries[j] = j;
      minSeries[j] = (statJ == null) ? 0 : statJ.bucketMin;
      maxSeries[j] = (statJ == null) ? 0 : statJ.bucketMax;
    }
    bucketView.addSeries("F", timeSeries, minSeries, Color.green);
    bucketView.addSeries("F", timeSeries, maxSeries, Color.red);
    glacierPanel2.addDataView(bucketView);

    Constraints consumedCons = new Constraints();
    consumedCons.gridx = 2;
    consumedCons.gridy = 0;
    consumedCons.fill = Constraints.HORIZONTAL;

    LineGraphView consumedView = new LineGraphView("Bandwidth", 380, 200, consumedCons, "Minutes", "Bytes/sec", false, false);
    timeSeries = new double[HISTORY];
    double[] consumedSeries = new double[HISTORY];
    for (int j=0; j<HISTORY; j++) {
      GlacierStatistics statJ = history[(historyPtr+j) % HISTORY];
      timeSeries[j] = j;
      consumedSeries[j] = (statJ == null) ? 0 : (statJ.bucketConsumed / 60);
    }
    consumedView.addSeries("F", timeSeries, consumedSeries, Color.orange);
    glacierPanel2.addDataView(consumedView);

    MultiDataPanel thePanel = new MultiDataPanel("Glacier");
    thePanel.addDataPanel(glacierPanel);
    thePanel.addDataPanel(glacierPanel2);

    return thePanel;
  }
  
  public void receiveStatistics(GlacierStatistics stat) {
    history[historyPtr] = stat;
    historyPtr = (historyPtr+1) % HISTORY;
    for (int i=0; i<stat.messagesSentByTag.length; i++) {
      msgTotal[i] += stat.messagesSentByTag[i];
      msgSent += stat.messagesSentByTag[i];
    }
  }
}
