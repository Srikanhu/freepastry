//////////////////////////////////////////////////////////////////////////////
// Rice Open Source Pastry Implementation                  //               //
//                                                         //  R I C E      //
// Copyright (c)                                           //               //
// Romer Gil                   rgil@cs.rice.edu            //   UNIVERSITY  //
// Andrew Ladd                 aladd@cs.rice.edu           //               //
// Tsuen Wan Ngan              twngan@cs.rice.edu          ///////////////////
//                                                                          //
// This program is free software; you can redistribute it and/or            //
// modify it under the terms of the GNU General Public License              //
// as published by the Free Software Foundation; either version 2           //
// of the License, or (at your option) any later version.                   //
//                                                                          //
// This program is distributed in the hope that it will be useful,          //
// but WITHOUT ANY WARRANTY; without even the implied warranty of           //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            //
// GNU General Public License for more details.                             //
//                                                                          //
// You should have received a copy of the GNU General Public License        //
// along with this program; if not, write to the Free Software              //
// Foundation, Inc., 59 Temple Place - Suite 330,                           //
// Boston, MA  02111-1307, USA.                                             //
//                                                                          //
// This license has been added in concordance with the developer rights     //
// for non-commercial and research distribution granted by Rice University  //
// software and patent policy 333-99.  This notice may not be removed.      //
//////////////////////////////////////////////////////////////////////////////

package rice.pastry.routing;

import rice.pastry.*;
import rice.pastry.messaging.*;
import rice.pastry.security.*;

import java.io.*;

/**
 * A route message is a pastry message which has been wrapped to
 * be sent to another pastry node.
 *
 * @author Andrew Ladd
 */

public class RouteMessage extends Message implements Serializable {
    private NodeId target;
    private Message internalMsg;

    private transient SendOptions opts;
    private transient Address auxAddress;
    public transient NodeHandle nextHop;

    /**
     * Constructor.
     *
     * @param target this is id of the node the message will be routed to.
     * @param msg the wrapped message.
     * @param cred the credentials for the message.
     */

    public RouteMessage(NodeId target, Message msg, Credentials cred) 
    {
	super(new RouterAddress());
	this.target = target;
	internalMsg = msg;
	this.opts = new SendOptions();

	nextHop = null;
    }

    /**
     * Constructor.
     *
     * @param target this is id of the node the message will be routed to.
     * @param msg the wrapped message.
     * @param cred the credentials for the message.
     * @param opts the send options for the message.
     */

    public RouteMessage(NodeId target, Message msg, Credentials cred, SendOptions opts) 
    {
	super(new RouterAddress());
	this.target = target;
	internalMsg = msg;
	this.opts = opts;
	
	nextHop = null;
    }

    /**
     * Constructor.
     *
     * @param dest the node this message will be routed to
     * @param msg the wrapped message.
     * @param cred the credentials for the message.
     * @param opts the send options for the message.
     * @param aux an auxilary address which the message after each hop.
     */

    public RouteMessage(NodeHandle dest, Message msg, Credentials cred, SendOptions opts, Address aux) 
    {
	super(new RouterAddress());
	this.target = dest.getNodeId();
	internalMsg = msg;
	this.opts = opts;
	nextHop = dest;
	auxAddress = aux;
    }

    /**
     * Constructor.
     *
     * @param target this is id of the node the message will be routed to.
     * @param msg the wrapped message.
     * @param cred the credentials for the message.
     * @param aux an auxilary address which the message after each hop.
     */

    public RouteMessage(NodeId target, Message msg, Credentials cred, Address aux) 
    {
	super(new RouterAddress());
	this.target = target;
	internalMsg = msg;
	this.opts = new SendOptions();

	auxAddress = aux;
	
	nextHop = null;
    }

    /**
     * Constructor.
     *
     * @param target this is id of the node the message will be routed to.
     * @param msg the wrapped message.
     * @param cred the credentials for the message.
     * @param opts the send options for the message.
     * @param aux an auxilary address which the message after each hop.
     */

    public RouteMessage(NodeId target, Message msg, Credentials cred, SendOptions opts, Address aux) 
    {
	super(new RouterAddress());
	this.target = target;
	internalMsg = msg;
	this.opts = opts;

	auxAddress = aux;
	
	nextHop = null;
    }

    /**
     * Routes the messages if the next hop has been set up.
     *
     * @param localId the node id of the local node.
     *
     * @return true if the message got routed, false otherwise.
     */

    public boolean routeMessage(NodeId localId) {
	if (nextHop == null) return false;

	NodeHandle handle = nextHop;
	nextHop = null;

	if (localId.equals(handle.getNodeId())) handle.receiveMessage(internalMsg);      
	else handle.receiveMessage(this);

	return true;
    }

    /**
     * Gets the target node id of this message.
     *
     * @return the target node id.
     */
    
    public NodeId getTarget() { return target; }

    /**
     * Get receiver address.
     * 
     * @return the address.
     */

    public Address getDestination() {
	if (nextHop == null || auxAddress == null) return super.getDestination();
	
	return auxAddress;
    }
    
    /**
     * The wrapped message.
     *
     * @return the wrapped message.
     */

    public Message unwrap() { return internalMsg; }
    
    /**
     * Get transmission options.
     *
     * @return the options.
     */

    public SendOptions getOptions() { return opts; }

    /**
     * Set the next hop of a message. (Added by Sitaram Iyer). Used in the
     * RMI subsystem: when remote node is found dead, it nulls the nexthop
     * and bounces the RouteMessage back to its local node.
     *
     * @param nh new next hop
     */
    public void setNextHop(NodeHandle nh) { nextHop = nh; }

    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException 
    {
	target = (NodeId) in.readObject();
	internalMsg = (Message) in.readObject();
    }

    private void writeObject(ObjectOutputStream out)
	throws IOException, ClassNotFoundException 
    {
	out.writeObject(target);
	out.writeObject(internalMsg);
    }

    public String toString() {
	String str = "";

	str += "RouteMessage for target " + target;
	
	if (auxAddress != null) str += " with aux address " + auxAddress;

	//str += "\n";

	str += ", wraps ";
	str += internalMsg.toString();

	return str;
    }
}
