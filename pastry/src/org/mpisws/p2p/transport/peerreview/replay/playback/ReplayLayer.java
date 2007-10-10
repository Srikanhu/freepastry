package org.mpisws.p2p.transport.peerreview.replay.playback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.mpisws.p2p.transport.ErrorHandler;
import org.mpisws.p2p.transport.MessageCallback;
import org.mpisws.p2p.transport.MessageRequestHandle;
import org.mpisws.p2p.transport.SocketCallback;
import org.mpisws.p2p.transport.SocketRequestHandle;
import org.mpisws.p2p.transport.TransportLayer;
import org.mpisws.p2p.transport.TransportLayerCallback;
import org.mpisws.p2p.transport.peerreview.Verifier;
import org.mpisws.p2p.transport.peerreview.history.HashProvider;
import org.mpisws.p2p.transport.peerreview.history.IndexEntry;
import org.mpisws.p2p.transport.peerreview.history.SecureHistory;
import org.mpisws.p2p.transport.peerreview.replay.IdentifierSerializer;
import org.mpisws.p2p.transport.util.MessageRequestHandleImpl;

import rice.environment.Environment;
import rice.environment.logging.LogManager;
import rice.environment.logging.Logger;
import rice.environment.params.Parameters;
import rice.environment.params.simple.SimpleParameters;
import rice.environment.processing.Processor;
import rice.environment.processing.sim.SimProcessor;
import rice.environment.random.RandomSource;
import rice.environment.random.simple.SimpleRandomSource;
import rice.environment.time.simulated.DirectTimeSource;
import rice.p2p.util.MathUtils;
import rice.selector.SelectorManager;
import rice.selector.TimerTask;

public class ReplayLayer<Identifier> extends Verifier<Identifier> implements 
  TransportLayer<Identifier, ByteBuffer> {

  TransportLayerCallback<Identifier, ByteBuffer> callback;
  Map<Integer, ReplaySocket<Identifier>> sockets = new HashMap<Integer, ReplaySocket<Identifier>>();

  public ReplayLayer(IdentifierSerializer<Identifier> serializer, HashProvider hashProv, SecureHistory history, Identifier localHandle, Environment environment) throws IOException {
    super(serializer, hashProv, history, localHandle, (short)0, (short)0, 0, environment.getLogManager().getLogger(ReplayLayer.class, localHandle.toString()));
    this.environment = environment;
  }
  
  public SocketRequestHandle<Identifier> openSocket(final Identifier i, SocketCallback<Identifier> deliverSocketToMe, final Map<String, Integer> options) {
    try {
      int socketId = openSocket(i);
//      logger.log("openSocket("+i+"):"+socketId);
      ReplaySocket<Identifier> socket = new ReplaySocket<Identifier>(i,socketId,this,options);
      socket.setDeliverSocketToMe(deliverSocketToMe);
      sockets.put(socketId, socket);
      return socket;
    } catch (IOException ioe) {      
      SocketRequestHandle<Identifier> ret = new SocketRequestHandle<Identifier>(){

        public Identifier getIdentifier() {
          return i;
        }

        public Map<String, Integer> getOptions() {
          return options;
        }

        public boolean cancel() {
          return true;
        }      
      };
      
      deliverSocketToMe.receiveException(ret, ioe);
      return ret;
    }
  }
  
  public MessageRequestHandle<Identifier, ByteBuffer> sendMessage(Identifier i, ByteBuffer m, MessageCallback<Identifier, ByteBuffer> deliverAckToMe, Map<String, Integer> options) {
    if (logger.level <= Logger.FINEST) {
      logger.logException("sendMessage("+i+","+m+"):"+MathUtils.toHex(m.array()), new Exception("Stack Trace"));      
    } else if (logger.level <= Logger.FINER) {
      logger.log("sendMessage("+i+","+m+"):"+MathUtils.toHex(m.array()));
    } else if (logger.level <= Logger.FINE) {
      logger.log("sendMessage("+i+","+m+")");      
    }
    MessageRequestHandleImpl<Identifier, ByteBuffer> ret = new MessageRequestHandleImpl<Identifier, ByteBuffer>(i, m, options);
    try {
      send(i, m, -1);
      if (deliverAckToMe != null) deliverAckToMe.ack(ret);
    } catch (IOException ioe) {
      if (logger.level <= Logger.WARNING) logger.logException("", ioe);
      throw new RuntimeException(ioe);
    }
    return ret;
  }

  public Identifier getLocalIdentifier() {
    return localHandle;
  }

  public void setCallback(TransportLayerCallback<Identifier, ByteBuffer> callback) {
    this.callback = callback;
  }

  public void setErrorHandler(ErrorHandler<Identifier> handler) {
    // TODO Auto-generated method stub    
  }

  public void destroy() {
  }

  public void acceptMessages(boolean b) {
  }

  public void acceptSockets(boolean b) {    
  }

  Environment environment;

  @Override
  protected void receive(final Identifier from, final ByteBuffer msg) throws IOException {
    if (logger.level <= Logger.FINER) logger.log("receive("+from+","+msg+")");
        
//          if (logger.level <= Logger.FINE) logger.log("receive("+from+","+msg+","+timeToDeliver+")");
    callback.messageReceived(from, msg, null);
  }

  @Override
  protected void socketIO(int socketId, boolean canRead, boolean canWrite) throws IOException {
    sockets.get(socketId).notifyIO(canRead, canWrite);
  }

  @Override
  protected void incomingSocket(Identifier from, int socketId) throws IOException {
    ReplaySocket<Identifier> socket = new ReplaySocket<Identifier>(from, socketId, this, null);
    sockets.put(socketId, socket);
    callback.incomingSocket(socket);
  }
  
  public static Environment generateEnvironment(String name, long startTime, long randSeed) {
    Parameters params = new SimpleParameters(Environment.defaultParamFileArray,null);
    DirectTimeSource dts = new DirectTimeSource(startTime);
    LogManager lm = Environment.generateDefaultLogManager(dts,params);
    RandomSource rs = new SimpleRandomSource(randSeed, lm);
    dts.setLogManager(lm);
    SelectorManager selector = new ReplaySM("Replay "+name, dts, lm);
    dts.setSelectorManager(selector);
    Processor proc = new SimProcessor(selector);
    Environment env = new Environment(selector,proc,rs,dts,lm,
        params, Environment.generateDefaultExceptionStrategy(lm));
    return env;
  }

  @Override
  protected void socketOpened(int socketId) throws IOException {
//    logger.log("socketOpened("+socketId+")");
    sockets.get(socketId).socketOpened();
  }  
}
