/*
 * Created on Feb 15, 2005
 */
package rice.tutorial.lesson3;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;

/**
 * An example message.
 * 
 * @author Jeff Hoye
 */
public class MyMsg implements Message {
  /**
   * Where the Message came from.
   */
  Id from;
  /**
   * Where the Message is going.
   */
  Id to;
  
  /**
   * Constructor.
   */
  public MyMsg(Id from, Id to) {
    this.from = from;
    this.to = to;
  }
  
  public String toString() {
    return "TestMsg from "+from+" to "+to;
  }

  /**
   * Use low priority to prevent interference with overlay maintenance traffic.
   */
  public int getPriority() {
    return Message.LOW_PRIORITY;
  }
}