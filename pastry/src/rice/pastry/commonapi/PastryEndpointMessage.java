
package rice.pastry.commonapi;

import rice.p2p.commonapi.Message;
import rice.pastry.NodeHandle;
import rice.pastry.messaging.Address;

/**
 * This class is an internal message to the commonapi gluecode.
 *
 * @version $Id$
 *
 * @author Alan Mislove
 * @author Peter Druschel
 */
public class PastryEndpointMessage extends rice.pastry.messaging.Message {

  private static final long serialVersionUID = 4499456388556140871L;
  
  protected Message message;
  
  /**
    * Constructor.
   *
   * @param pn the pastry node that the application attaches to.
   */
  public PastryEndpointMessage(Address address, Message message, NodeHandle sender) {
    super(address);
    setSender(sender);
    this.message = message;
    setPriority(message.getPriority());
  }

  /**
   * Returns the internal message
   *
   * @return the credentials.
   */
  public Message getMessage() {
    return message;
  }


  /**
    * Returns the internal message
   *
   * @return the credentials.
   */
  public void setMessage(Message message) {
    this.message = message;
  }

  /**
   * Returns the String representation of this message
   *
   * @return The string
   */
  public String toString() {
    return "[PEM " + message + "]";
  }
}




