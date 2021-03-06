package rice.email.proxy;

import java.net.BindException;
import java.net.InetAddress;

import rice.email.*;
import rice.post.*;

/**
 * This class provides an IMAP interface into the POST pastry-based email system.  This will
 * allow IMAP-compliant email clients to interact with the POST library.  This proxy, with proper
 * configuration, can service multiple POST accounts (and therefore multiple, simultaneous IMAP
 * clients).
 * 
 * @author Derek Ruths
 */
public class IMAPProxy implements EmailServiceListener {
  
  // constructors
  /**
   * This constructor instantiates the proxy.  The address and port specified instruct the
   * IMAP service of the port and address it should bind to locally.
   * 
   * @param address is the local address to which the IMAP service will bind to.
   * @param port is the local port to which the IMAP service will bind to.
   * 
   * @throws BindException if the address and port specified are an illegal pair for this
   * service to bind to.
   */
  public IMAPProxy(InetAddress address, int port) throws BindException {
    return;    
  }
  
  // methods
  /**
   * This method attaches this IMAP proxy to a specific Post - which corresponds to
   * a specific user account.
   * 
   * @param post is the POST access point into the system.
   * @param imapUsername is the username that an IMAP-based email reader will use to
   * access this account through this proxy.
   * @param imapPassword is the password that an IMAP-based email reader will use in 
   * combination with the username to access this account through this proxy.
   */
  public void attach(Post post, String imapUsername, String imapPassword) {
    return;
  }
    
  /**
   * This method is called on this object when an email is received by an
   * EmailService object.
   * 
   * @param email is the email that was received.
   */
  public void messageReceived(Email email) {
  }
}
