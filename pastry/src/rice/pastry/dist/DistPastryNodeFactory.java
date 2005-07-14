
package rice.pastry.dist;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.Random;

import rice.Continuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.CancellableTask;
import rice.pastry.*;
import rice.pastry.socket.SocketPastryNodeFactory;

/**
 * An abstraction of the nodeId factory for distributed nodes. In order to
 * obtain a nodeId factory, a client should use the getFactory method, passing
 * in either PROTOCOL_RMI or PROTOCOL_WIRE as the protocol, and the port number
 * the factory should use. In the wire protocol, the port number is the starting
 * port number that the nodes are constructed on, and in the rmi protocol, the
 * port number is the location of the local RMI registry.
 *
 * @version $Id: DistPastryNodeFactory.java,v 1.8 2003/12/22 03:24:46 amislove
 *      Exp $
 * @author Alan Mislove
 */
public abstract class DistPastryNodeFactory extends PastryNodeFactory {

  // choices of protocols
  /**
   * DESCRIBE THE FIELD
   */
  public static int PROTOCOL_SOCKET = 2;

  public static int PROTOCOL_DEFAULT = PROTOCOL_SOCKET;
  
  /**
   * Constructor. Protected - one should use the getFactory method.
   */
  protected DistPastryNodeFactory(Environment env) {
    super(env);
  }

  /**
   * Method which a client should use in order to get a bootstrap node from the
   * factory. In the wire protocol, this method will generate a node handle
   * corresponding to the pastry node at location address. In the rmi protocol,
   * this method will generate a node handle for the pastry node bound to
   * address.
   *
   * @param address The address of the remote node.
   * @return The NodeHandle value
   */
  public final NodeHandle getNodeHandle(InetSocketAddress address) {
    return generateNodeHandle(address);
  }
  
  public final CancellableTask getNodeHandle(InetSocketAddress address, Continuation c) {
    return generateNodeHandle(address, c);
  }
  
  /**
   * Method which a client should use in order to get a bootstrap node from the
   * factory. In the wire protocol, this method will generate a node handle
   * corresponding to the pastry node at location address. In the rmi protocol,
   * this method will generate a node handle for the pastry node bound to
   * address.
   *
   * @param address The address of the remote node.
   * @return The NodeHandle value
   */
  public final NodeHandle getNodeHandle(InetSocketAddress[] addresses) {
    // first, randomize the addresses
    Random r = new Random();
    for (int i=0; i<addresses.length; i++) {
      int j = r.nextInt(addresses.length);
      InetSocketAddress tmp = addresses[j];
      addresses[j] = addresses[i];
      addresses[i] = tmp;
    } 
    
    // then boot
    for (int i=0; i<addresses.length; i++) {
      NodeHandle result = getNodeHandle(addresses[i]);
      if (result != null)
        return result;
    }
    
    return null;
  }

  /**
   * Implements non-blocking multiple address selection, in parallel.  
   *  
   * @author Jeff Hoye
   */
  class GNHContinuation implements Continuation, CancellableTask {
    /**
     * Points to next address to try
     */
    int index;
    int numInParallel;
    int outstandingRequests;
    InetSocketAddress[] addresses;
    Continuation subContinuation;
    
    /**
     * Stored list of sub CancellableTasks to make the whole parallel process cancellable.
     */
    ArrayList outstandingTasks = new ArrayList();
    /**
     * Set to true when complete.
     */
    boolean done = false;
    
    public GNHContinuation(InetSocketAddress[] addresses, Continuation subContinuation, int numInParallel) {
      this.addresses = addresses;
      this.subContinuation = subContinuation;
      // must request at least one in parallel (this is serial)
      if (numInParallel < 1) numInParallel = 1;
      this.numInParallel = numInParallel;      
      index = 0;
    }
    
    
    public synchronized void receiveResult(Object result) {
      if (done) return;
      outstandingRequests--;
      if (result != null) {
        cancel();
        subContinuation.receiveResult(result);
        return;
      } else {
        tryNext();
      }
    }

    public synchronized void receiveException(Exception result) {
      if (done) return;
      environment.getLogManager().getLogger(DistPastryNodeFactory.class, null).logException(Logger.WARNING, 
          "Received exception while booting, trying next bootstap address",result);
      outstandingRequests--;
      tryNext();      
    }
    
    private synchronized void tryNext() {
      if (done) return;
      while(outstandingRequests < numInParallel &&
          index < addresses.length) {
        outstandingRequests++; 
        index++; // do this in case call comes back on same thread, in same stack
        outstandingTasks.add(getNodeHandle(addresses[index-1], this));
      }
      if (outstandingRequests == 0) {
        subContinuation.receiveResult(null);
        // cant be any tasks to cancel at this point
        done = true;
      }
    }


    public void run() {    }


    public synchronized boolean cancel() {
      if (done) return false;
      Iterator i = outstandingTasks.iterator();
      while(i.hasNext()) {
        CancellableTask ct = (CancellableTask)i.next(); 
        ct.cancel();
      }
      done = true;
      return true;
    }


    public long scheduledExecutionTime() {
      return 0;
    }    
  }
  
  public final CancellableTask getNodeHandle(InetSocketAddress[] addresses, Continuation c) {
    // first, randomize the addresses
    Random r = new Random();
    for (int i=0; i<addresses.length; i++) {
      int j = r.nextInt(addresses.length);
      InetSocketAddress tmp = addresses[j];
      addresses[j] = addresses[i];
      addresses[i] = tmp;
    }     

    GNHContinuation gnh = new GNHContinuation(addresses,c,environment.getParameters().getInt("pastry_factory_bootsInParallel"));
    gnh.tryNext();
    return gnh;
  }

  /**
   * Method which all subclasses should implement allowing the client to
   * generate a node handle given the address of a node. This is designed to
   * allow the client to get their hands on a bootstrap node during the
   * initialization phase of the client application.
   *
   * @param address DESCRIBE THE PARAMETER
   * @return DESCRIBE THE RETURN VALUE
   */
  public abstract NodeHandle generateNodeHandle(InetSocketAddress address);
  public abstract CancellableTask generateNodeHandle(InetSocketAddress address, Continuation c);
  
  /**
   * Generates a new pastry node with a random NodeId using the bootstrap
   * bootstrap.
   *
   * @param bootstrap Node handle to bootstrap from.
   * @return DESCRIBE THE RETURN VALUE
   */
  public abstract PastryNode newNode(NodeHandle bootstrap);

  /**
   * Generates a new pastry node with the specified NodeId using the bootstrap
   * bootstrap.
   *
   * @param bootstrap Node handle to bootstrap from.
   * @param nodeId DESCRIBE THE PARAMETER
   * @return DESCRIBE THE RETURN VALUE
   */
  public abstract PastryNode newNode(NodeHandle bootstrap, NodeId nodeId);
  
  /**
   * Generates a new pastry node with the specified NodeId using the bootstrap
   * bootstrap.
   *
   * @param bootstrap Node handle to bootstrap from.
   * @param nodeId DESCRIBE THE PARAMETER
   * @return DESCRIBE THE RETURN VALUE
   */
  public abstract PastryNode newNode(NodeHandle bootstrap, NodeId nodeId, InetSocketAddress proxy);
  
  /** 
   * Generates a new pastry node with the specified NodeId using the bootstrap
   * bootstrap.
   *
   * @param bootstrap Node handle to bootstrap from.
   * @param nodeId DESCRIBE THE PARAMETER
   * @return DESCRIBE THE RETURN VALUE
   */
  public abstract PastryNode newNode(NodeHandle bootstrap, InetSocketAddress proxy);

  /**
   * Static method which is designed to be used by clients needing a distrubuted
   * pastry node factory. The protocol should be one of PROTOCOL_RMI or
   * PROTOCOL_WIRE. The port is protocol-dependent, and is the port number of
   * the RMI registry if using RMI, or is the starting port number the nodes
   * should be created on if using wire.
   *
   * @param protocol The protocol to use (PROTOCOL_RMI or PROTOCOL_WIRE)
   * @param port The RMI registry port if RMI, or the starting port if wire.
   * @param nf DESCRIBE THE PARAMETER
   * @return A DistPastryNodeFactory using the given protocol and port.
   * @throws IllegalArgumentException If protocol is an unsupported port.
   */
  public static DistPastryNodeFactory getFactory(NodeIdFactory nf, int protocol, int port, Environment env) {
    if (protocol == PROTOCOL_SOCKET) {
      return new SocketPastryNodeFactory(nf, port, env);
    }

    throw new IllegalArgumentException("Unsupported Protocol " + protocol);
  }
}

