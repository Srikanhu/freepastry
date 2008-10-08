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
package org.mpisws.p2p.transport.peerreview;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.mpisws.p2p.transport.ErrorHandler;
import org.mpisws.p2p.transport.MessageCallback;
import org.mpisws.p2p.transport.MessageRequestHandle;
import org.mpisws.p2p.transport.P2PSocket;
import org.mpisws.p2p.transport.SocketCallback;
import org.mpisws.p2p.transport.SocketRequestHandle;
import org.mpisws.p2p.transport.TransportLayer;
import org.mpisws.p2p.transport.TransportLayerCallback;
import org.mpisws.p2p.transport.peerreview.audit.AuditProtocol;
import org.mpisws.p2p.transport.peerreview.audit.AuditProtocolImpl;
import org.mpisws.p2p.transport.peerreview.audit.EvidenceTool;
import org.mpisws.p2p.transport.peerreview.challenge.ChallengeResponseProtocol;
import org.mpisws.p2p.transport.peerreview.challenge.ChallengeResponseProtocolImpl;
import org.mpisws.p2p.transport.peerreview.commitment.Authenticator;
import org.mpisws.p2p.transport.peerreview.commitment.AuthenticatorSerializer;
import org.mpisws.p2p.transport.peerreview.commitment.AuthenticatorStore;
import org.mpisws.p2p.transport.peerreview.commitment.AuthenticatorStoreImpl;
import org.mpisws.p2p.transport.peerreview.commitment.CommitmentProtocol;
import org.mpisws.p2p.transport.peerreview.commitment.CommitmentProtocolImpl;
import org.mpisws.p2p.transport.peerreview.evidence.EvidenceTransferProtocol;
import org.mpisws.p2p.transport.peerreview.evidence.EvidenceTransferProtocolImpl;
import org.mpisws.p2p.transport.peerreview.evidence.ProofInconsistent;
import org.mpisws.p2p.transport.peerreview.history.HashProvider;
import org.mpisws.p2p.transport.peerreview.history.SecureHistory;
import org.mpisws.p2p.transport.peerreview.history.SecureHistoryFactory;
import org.mpisws.p2p.transport.peerreview.history.SecureHistoryFactoryImpl;
import org.mpisws.p2p.transport.peerreview.identity.IdentityTransport;
import org.mpisws.p2p.transport.peerreview.identity.IdentityTransportCallback;
import org.mpisws.p2p.transport.peerreview.identity.UnknownCertificateException;
import org.mpisws.p2p.transport.peerreview.infostore.Evidence;
import org.mpisws.p2p.transport.peerreview.infostore.EvidenceSerializer;
import org.mpisws.p2p.transport.peerreview.infostore.IdStrTranslator;
import org.mpisws.p2p.transport.peerreview.infostore.PeerInfoStore;
import org.mpisws.p2p.transport.peerreview.infostore.PeerInfoStoreImpl;
import org.mpisws.p2p.transport.peerreview.infostore.StatusChangeListener;
import org.mpisws.p2p.transport.peerreview.message.AccusationMessage;
import org.mpisws.p2p.transport.peerreview.message.AckMessage;
import org.mpisws.p2p.transport.peerreview.message.ChallengeMessage;
import org.mpisws.p2p.transport.peerreview.message.PeerReviewMessage;
import org.mpisws.p2p.transport.peerreview.message.ResponseMessage;
import org.mpisws.p2p.transport.peerreview.message.UserDataMessage;
import org.mpisws.p2p.transport.peerreview.statement.Statement;
import org.mpisws.p2p.transport.peerreview.statement.StatementProtocolImpl;
import org.mpisws.p2p.transport.util.MessageRequestHandleImpl;
import org.mpisws.p2p.transport.util.Serializer;

import rice.Continuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Cancellable;
import rice.p2p.commonapi.rawserialization.RawSerializable;
import rice.p2p.util.MathUtils;
import rice.p2p.util.rawserialization.SimpleInputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.selector.TimerTask;

/**
 * 
 * @author Jeff Hoye
 *
 * @param <Handle> (Usually a NodeHandle)
 * @param <Identifier> (Permanent Identifier), can get an Identifier from a Handle
 */
public class PeerReviewImpl<Handle extends RawSerializable, Identifier extends RawSerializable> implements 
    TransportLayer<Handle, ByteBuffer>,
    TransportLayerCallback<Handle, ByteBuffer>,
    PeerReview<Handle, Identifier>, StatusChangeListener<Identifier> {

  // above/below layers
  protected PeerReviewCallback<Handle, Identifier> callback;
  protected IdentityTransport<Handle, Identifier> transport;

  // strategies for management of Generics
  protected Serializer<Identifier> idSerializer;
  protected Serializer<Handle> handleSerializer;
  protected IdentifierExtractor<Handle, Identifier> identifierExtractor;
  protected IdStrTranslator<Identifier> stringTranslator;
  protected EvidenceSerializer evidenceSerializer;
  protected AuthenticatorSerializer authenticatorSerialilzer;
  protected EvidenceTool<Handle, Identifier> evidenceTool;
  
  // compatibility with rice environment
  protected Environment env;
  protected Logger logger;

  // storage
  protected AuthenticatorStore<Identifier> authInStore;
  protected AuthenticatorStore<Identifier> authOutStore;
  protected AuthenticatorStore<Identifier> authCacheStore;
  protected AuthenticatorStore<Identifier> authPendingStore;
  protected PeerInfoStore<Handle, Identifier> infoStore;
  protected SecureHistoryFactory historyFactory;
  protected SecureHistory history;

  // protocols
  protected CommitmentProtocol<Handle, Identifier> commitmentProtocol;
  protected EvidenceTransferProtocol<Handle, Identifier> evidenceTransferProtocol;
  protected AuditProtocol<Identifier> auditProtocol;
  protected ChallengeResponseProtocol<Handle, Identifier> challengeProtocol;
  protected StatementProtocolImpl<Handle, Identifier> statementProtocol;


  
  long lastLogEntry = -1;
  boolean initialized = false;
  protected long timeToleranceMillis = DEFAULT_TIME_TOLERANCE_MILLIS;
  
  public PeerReviewImpl(IdentityTransport<Handle, Identifier> transport,
      Environment env, Serializer<Handle> handleSerializer,
      Serializer<Identifier> idSerializer,      
      IdentifierExtractor<Handle, Identifier> identifierExtractor,
      IdStrTranslator<Identifier> stringTranslator,
      AuthenticatorSerializer authenticatorSerialilzer,
      EvidenceSerializer evidenceSerializer) {
    super();
    this.transport = transport;
    this.transport.setCallback(this);
    this.stringTranslator = stringTranslator;
    this.env = env;
    this.logger = env.getLogManager().getLogger(PeerReviewImpl.class, null);
    this.idSerializer = idSerializer;
    this.handleSerializer = handleSerializer;
    this.identifierExtractor = identifierExtractor;
    this.evidenceSerializer = evidenceSerializer;

    this.authenticatorSerialilzer = authenticatorSerialilzer; 

    this.historyFactory = new SecureHistoryFactoryImpl(transport, env);
  }
  
  /**
   * PeerReview checks the timestamps on messages against the local clock, and
   * ignores them if the timestamp is too far out of sync. The definition of
   * 'too far' can be controlled with this method.
   */
  public void setTimeToleranceMillis(long timeToleranceMicros) {
   this.timeToleranceMillis = timeToleranceMicros;
   if (commitmentProtocol != null)
     commitmentProtocol.setTimeToleranceMillis(timeToleranceMicros);
  }


  public static String getStatusString(int status) {
    switch(status) {
    case STATUS_EXPOSED:
      return "exposed";
    case STATUS_TRUSTED:
      return "trusted";
    case STATUS_SUSPECTED:
      return "suspected";
    }
    return "unknown status:"+status;
  }
  
  public void notifyStatusChange(final Identifier id, final int newStatus) {
//    char buf1[256];
    if (logger.level <= Logger.INFO) logger.log("Status change: <"+id+"> becomes "+getStatusString(newStatus));
    challengeProtocol.notifyStatusChange(id, newStatus);
    commitmentProtocol.notifyStatusChange(id, newStatus);
    
    // let pr finish first
    env.getSelectorManager().schedule(new TimerTask() {    
      public void run() {
        callback.notifyStatusChange(id, newStatus);
      }    
    }, 3);
    
  }
  
  public void init(String dirname) throws IOException {    
    File dir = new File(dirname);
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        throw new IllegalStateException("Cannot open PeerReview directory: "+dir.getAbsolutePath());
      }
    }
    if (!dir.isDirectory()) throw new IllegalStateException("Cannot open PeerReview directory: "+dir.getAbsolutePath());
    
    File namebuf = new File(dir,"peers");
    
    infoStore = new PeerInfoStoreImpl<Handle, Identifier>(transport, stringTranslator, authenticatorSerialilzer, evidenceSerializer, env);
    infoStore.setStatusChangeListener(this);

    /* Open history */

    String historyName = dirname+"/local";
    try {
      this.history = historyFactory.open(historyName, "w");
    } catch (IOException ioe) {
      this.history = historyFactory.create(historyName, 0, transport.getEmptyHash());      
    }
    
    updateLogTime();
    
    if (!infoStore.setStorageDirectory(namebuf)) {
      throw new IllegalStateException("Cannot open info storage directory '"+namebuf+"'");
    }
    
    /* Initialize authenticator store */
    
    authInStore = new AuthenticatorStoreImpl<Identifier>(this);
    authInStore.setFilename(new File(dir,"authenticators.in"));

    authOutStore = new AuthenticatorStoreImpl<Identifier>(this);
    authOutStore.setFilename(new File(dir,"authenticators.out"));

    authPendingStore = new AuthenticatorStoreImpl<Identifier>(this, true);
    authPendingStore.setFilename(new File(dir,"authenticators.pending"));

    authCacheStore = new AuthenticatorStoreImpl<Identifier>(this, true);
    authCacheStore.setFilename(new File(dir,"authenticators.cache"));

    /* Remaining protocols */
    this.evidenceTransferProtocol = new EvidenceTransferProtocolImpl<Handle, Identifier>(this,transport,infoStore);
    this.commitmentProtocol = new CommitmentProtocolImpl<Handle, Identifier>(this,transport,infoStore,authOutStore,history, timeToleranceMillis);    
    this.auditProtocol = new AuditProtocolImpl<Identifier>(this, history, infoStore, authInStore, transport, authOutStore, evidenceTransferProtocol, authCacheStore);
    this.challengeProtocol = new ChallengeResponseProtocolImpl<Handle, Identifier>(this, transport, infoStore, history, authOutStore, auditProtocol, commitmentProtocol);
    this.statementProtocol = new StatementProtocolImpl<Handle, Identifier>(this, challengeProtocol, infoStore, transport);
    
    this.evidenceTool = null; // TODO: implement
    initialized = true;
  }
    
  public PeerReviewCallback<Handle, Identifier> getApp() {
    return callback;
  }
  
  public SocketRequestHandle<Handle> openSocket(Handle i, SocketCallback<Handle> deliverSocketToMe, Map<String, Object> options) {
    return transport.openSocket(i, deliverSocketToMe, options);
  }

  public void incomingSocket(P2PSocket<Handle> s) throws IOException {
    callback.incomingSocket(s);
  }

  public MessageRequestHandle<Handle, ByteBuffer> sendMessage(Handle target,
      ByteBuffer message,
      final MessageCallback<Handle, ByteBuffer> deliverAckToMe,
      Map<String, Object> options) {
    
    /*
     * If the 'datagram' flag is set, the message is passed through to the
     * transport layer. This is used e.g. for liveness/proximity pings in
     * Pastry.
     */
    if (options != null && options.containsKey(DONT_COMMIT)) {
      final MessageRequestHandleImpl<Handle, ByteBuffer> ret = new MessageRequestHandleImpl<Handle, ByteBuffer>(
          target, message, options);
      ByteBuffer msg = ByteBuffer.allocate(message.remaining() + 1);
      msg.put(PEER_REVIEW_PASSTHROUGH);
      msg.put(message);
      msg.flip();
      ret.setSubCancellable(transport.sendMessage(target, msg,
          new MessageCallback<Handle, ByteBuffer>() {

            public void ack(MessageRequestHandle<Handle, ByteBuffer> msg) {
              if (deliverAckToMe != null)
                deliverAckToMe.ack(ret);
            }

            public void sendFailed(
                MessageRequestHandle<Handle, ByteBuffer> msg, Exception reason) {
              if (deliverAckToMe != null)
                deliverAckToMe.sendFailed(ret, reason);
            }
          }, options));
      return ret;
    }

    assert(initialized);

    /* Maybe do some mischief for testing? */

    // if (misbehavior)
    // misbehavior.maybeTamperWithData((unsigned char*)message, msglen);
    updateLogTime();

    /* Pass the message to the Commitment protocol */
    return commitmentProtocol.handleOutgoingMessage(target, message,
        deliverAckToMe, options);
  }

  /* PeerReview only updates its internal clock when it returns to the main loop, but not
  in between (e.g. while it is handling messages). When the clock needs to be
  updated, this function is called. */

  private void updateLogTime() {
    long now = env.getTimeSource().currentTimeMillis();
  
    if (now > lastLogEntry) {
      if (!history.setNextSeq(now * 1000000))
        panic("PeerReview: Cannot roll back history sequence number from "+history.getLastSeq()+" to "+now*1000000+"; did you change the local time?");
        
      lastLogEntry = now;
    }
  }

  public void messageReceived(Handle handle, ByteBuffer message, Map<String, Object> options) throws IOException {
//    char buf1[256];
    assert(initialized);
    
    /* Maybe do some mischief for testing */
    
//    if (misbehavior.dropIncomingMessage(handle, datagram, message, msglen))
//      return;

//    plog(1, "Received %s from %s (%d bytes)", datagram ? "DATAGRAM" : "MESSAGE", handle->render(buf1), msglen);
//    dump(2, message, msglen);
    
    /* Deliver datagrams */
    byte passthrough = message.get();
    switch(passthrough) {
    case PEER_REVIEW_PASSTHROUGH:
//      switch (message.get()) {
//      case MSG_AUTHPUSH :
//        authPushProtocol.handleIncomingAuthenticators(handle, message, msglen);
//        break;
//      case MSG_AUTHREQ :
//      case MSG_AUTHRESP :
//        auditProtocol.handleIncomingDatagram(handle, message, msglen);
//        break;
//      case MSG_USERDGRAM :
//        app.receive(handle, true, &message[1], msglen-1);
//        break;
//      default:
//        panic("Unknown datagram type in PeerReview: #%d", message[0]);
//        break;
//      }
      callback.messageReceived(handle, message, options);
      break;
      
    case PEER_REVIEW_COMMIT:
      Statement<Identifier> m = null;
      updateLogTime();
      byte type = message.get();      
      SimpleInputBuffer sib = new SimpleInputBuffer(message);
      switch (type) {      
      case MSG_ACK:        
        commitmentProtocol.handleIncomingAck(handle, AckMessage.build(sib,idSerializer,transport.getHashSizeBytes(),transport.getSignatureSizeBytes()), options);
        break;
      case MSG_CHALLENGE:
        ChallengeMessage<Identifier> challenge = new ChallengeMessage<Identifier>(sib,idSerializer,evidenceSerializer);
        challengeProtocol.handleChallenge(handle, challenge, options);
        break;
      case MSG_ACCUSATION:        
        m = new AccusationMessage<Identifier>(sib, idSerializer, evidenceSerializer);
      case MSG_RESPONSE:
        if (m == null) m = new ResponseMessage<Identifier>(sib, idSerializer, evidenceSerializer);
//        challengeProtocol.handleStatement(handle, m, options);
        statementProtocol.handleIncomingStatement(handle, m, options);
        break;
      case MSG_USERDATA:
        UserDataMessage<Handle> udm = UserDataMessage.build(sib, handleSerializer, transport.getHashSizeBytes(), transport.getSignatureSizeBytes());
        challengeProtocol.handleIncomingMessage(handle, udm, options);
//        commitmentProtocol.handleIncomingMessage(handle, udm, options);
        break;
      default:
        panic("Unknown message type in PeerReview: #"+ type);
        break;
      }
    }    
  }
  
  public void panic(String s) {
    if (logger.level <= Logger.SEVERE) logger.log("panic:"+s);
    env.destroy();
  }
  
  public void acceptMessages(boolean b) {
    transport.acceptMessages(b);
  }

  public void acceptSockets(boolean b) {
    transport.acceptSockets(b);
  }

  public Identifier getLocalId() {
    return identifierExtractor.extractIdentifier(transport.getLocalIdentifier());
  }
  
  public Handle getLocalHandle() {
    return transport.getLocalIdentifier();
  }

  public Handle getLocalIdentifier() {
    return transport.getLocalIdentifier();
  }

  public void setApp(PeerReviewCallback<Handle, Identifier> callback) {
    logger.log("setApp("+callback+")");
    this.callback = callback;
  }
  
  /**
   * 
   */
  public void setCallback(TransportLayerCallback<Handle, ByteBuffer> callback) {
    setApp((PeerReviewCallback<Handle, Identifier>)callback);
  }

  public void setErrorHandler(ErrorHandler<Handle> handler) {
    // TODO Auto-generated method stub
    
  }

  public void destroy() {
    transport.destroy();
  }

  public AuthenticatorSerializer getAuthenticatorSerializer() {
    return authenticatorSerialilzer;
  }

  public Environment getEnvironment() {
    return env;
  }

  public Serializer<Identifier> getIdSerializer() {
    return idSerializer;
  }

  public long getTime() {
    return env.getTimeSource().currentTimeMillis();
  }
  
  /** 
   * A helper function that extracts an authenticator from an incoming message and adds it to our local store. 
   */
  public Authenticator extractAuthenticator(Identifier id, long seq, short entryType, byte[] entryHash, byte[] hTopMinusOne, byte[] signature) {
//    *(long long*)&authenticator[0] = seq;
    
    byte[] hash = transport.hash(seq,entryType,hTopMinusOne, entryHash);
    Authenticator ret = new Authenticator(seq,hash,signature);
    if (addAuthenticatorIfValid(authOutStore, id, ret)) {
      return ret;
    }
    return null;
  }

  /**
   * Helper function called internally from the library. It takes a (potentially
   * new) authenticator and adds it to our local store if (a) it hasn't been
   * recorded before, and (b) its signature is valid.
   */
  public boolean addAuthenticatorIfValid(AuthenticatorStore<Identifier> store, Identifier subject, Authenticator auth) {
    // see if we can exit early
    Authenticator existingAuth = store.statAuthenticator(subject, auth.getSeq());
    if (existingAuth != null) {       
      /* If yes, then it should be bit-wise identical to the new one */
    
      if (auth.equals(existingAuth)) {
        return true;
      }
    }
   
     /* maybe the new authenticator is a forgery? Let's check the signature! 
        If the signature doesn't check out, then we simply discard the 'authenticator' and
        move on. */
     assert(transport.hasCertificate(subject));

     try {
//       System.out.println("Verifying "+auth.getSeq()+" "+MathUtils.toBase64(auth.getHash()));
       SimpleOutputBuffer sob = new SimpleOutputBuffer();
       sob.writeLong(auth.getSeq());
       sob.write(auth.getHash());
       byte[] signedHash = transport.hash(sob.getByteBuffer());
       transport.verify(subject, ByteBuffer.wrap(signedHash), ByteBuffer.wrap(auth.getSignature()));
       if (!verify(subject,auth)) {
         return false; 
       }
//    char buf1[1000];
       
       /* Do we already have an authenticator with the same sequence number and from the same node? */
       if (existingAuth != null) {
         /* The signature checks out, so the node must have signed two different authenticators
         with the same sequence number! This is a proof of misbehavior, and we must
         notify the witness set! */
             if (logger.level < Logger.WARNING) logger.log("Authenticator conflict for "+subject+" seq #"+auth.getSeq());
             if (logger.level < Logger.FINE) logger.log("Existing: ["+existingAuth+"]");
             if (logger.level < Logger.FINE) logger.log("New:      ["+auth+"]");
             
             /**
              * PROOF_INCONSISTENT
              * byte type = PROOF_INCONSISTENT
              * authenticator auth1
              * char whichInconsistency   // 0=another auth, 1=a log snippet
              * -----------------------
              * authenticator auth2       // if whichInconsistency==0
              * -----------------------
              * long long firstSeq        // if whichInconsistency==1
              * hash baseHash
              * [entries]
              */
             ProofInconsistent proof = new ProofInconsistent(auth,existingAuth);
        long evidenceSeq = getEvidenceSeq();
        infoStore.addEvidence(identifierExtractor.extractIdentifier(transport.getLocalIdentifier()), subject, evidenceSeq, proof, null);
        sendEvidenceToWitnesses(subject, evidenceSeq, proof);
         return false;
       }
       
     
       /* We haven't seen this authenticator... Signature is ok, so we keep the new authenticator in our store. */  
       store.addAuthenticator(subject, auth);
       return true;       
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
   
  /* Gets a fresh, unique sequence number for evidence */
  long nextEvidenceSeq = 0L;
  public long getEvidenceSeq() {
    if (nextEvidenceSeq < getTime()) {
      nextEvidenceSeq = getTime();
    }
    return nextEvidenceSeq++;
  }

  public Serializer<Handle> getHandleSerializer() {
    return handleSerializer;
  }

  public int getHashSizeInBytes() {
    return transport.getHashSizeBytes();
  }

  public int getSignatureSizeInBytes() {
    return transport.getSignatureSizeBytes();
  }

  public IdentifierExtractor<Handle, Identifier> getIdentifierExtractor() {
    return identifierExtractor;
  }

  public void challengeSuspectedNode(Handle handle) {
    challengeProtocol.challengeSuspectedNode(handle);
  }

  /**
   * Called internally by other classes if they have found evidence against one of our peers.
   * We ask the EvidenceTransferProtocol to send it to the corresponding witness set. 
   */
  public void sendEvidenceToWitnesses(Identifier subject, long evidenceSeq,
      Evidence evidence) {
    AccusationMessage<Identifier> accusation = new AccusationMessage<Identifier>(getLocalId(),subject,evidenceSeq,evidence);
   
    if (logger.level <= Logger.FINE) logger.log("Relaying evidence to <"+subject+">'s witnesses");
    evidenceTransferProtocol.sendMessageToWitnesses(subject, accusation, null, null);  
  }

  /**
   * Note, must include PEER_REVIEW_COMMIT and the type
   * 
   * @param dest
   * @param message
   * @param deliverAckToMe
   * @param options
   * @return
   */
  public void transmit(Handle dest, 
      PeerReviewMessage message,
      MessageCallback<Handle, ByteBuffer> deliverAckToMe, 
      Map<String, Object> options) {
    try {
      
      SimpleOutputBuffer sob = new SimpleOutputBuffer();
      sob.writeByte(PeerReview.PEER_REVIEW_COMMIT);
      sob.writeByte((byte)message.getType());
      message.serialize(sob);
      
      ByteBuffer buf = sob.getByteBuffer();
      
      transport.sendMessage(dest, buf, deliverAckToMe, options);
    } catch (IOException ioe) {
      throw new RuntimeException("Error serializing:"+message,ioe);
    }
  }

  public void notifyCertificateAvailable(Identifier id) {
    commitmentProtocol.notifyCertificateAvailable(id); 
//    authPushProtocol.notifyCertificateAvailable(id);
//    statementProtocol.notifyCertificateAvailable(id);
  }

  public void statusChange(Identifier id, int newStatus) {
    throw new RuntimeException("implement");
  }

  public boolean hasCertificate(Identifier id) {
    return transport.hasCertificate(id);
  }

  public Cancellable requestCertificate(Handle source, Identifier certHolder,
      Continuation<X509Certificate, Exception> c, Map<String, Object> options) {
    return transport.requestCertificate(source, certHolder, c, options);
  }

  public byte[] sign(byte[] bytes) {
    return transport.sign(bytes);
  }

  public short getSignatureSizeBytes() {
    return transport.getSignatureSizeBytes();
  }

  public boolean verify(Identifier id, Authenticator auth) {
    try {
      SimpleOutputBuffer sob = new SimpleOutputBuffer();
      sob.writeLong(auth.getSeq());
      sob.write(auth.getHash());
      byte[] signedHash = transport.hash(sob.getByteBuffer());
      transport.verify(id, ByteBuffer.wrap(signedHash), ByteBuffer.wrap(auth.getSignature()));
      return true;
    } catch (UnknownCertificateException uce) {
      throw new RuntimeException(uce);
    } catch (SignatureException se) {
      return false;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
 
  }
  
  public void verify(Identifier id, ByteBuffer msg, ByteBuffer signature) throws SignatureException,
      UnknownCertificateException {
    transport.verify(id, msg, signature);
  }

  public byte[] getEmptyHash() {
    return transport.getEmptyHash();
  }

  public short getHashSizeBytes() {
    return transport.getHashSizeBytes();
  }

  public byte[] hash(long seq, short type, byte[] nodeHash, byte[] contentHash) {
    return transport.hash(seq, type, nodeHash, contentHash);
  }

  public byte[] hash(ByteBuffer... hashMe) {
    return transport.hash(hashMe);
  }

  public EvidenceSerializer getEvidenceSerializer() {
    return evidenceSerializer;
  }

  public EvidenceTool<Handle, Identifier> getEvidenceTool() {
    return evidenceTool;
  }
}
