/*************************************************************************

"Free Pastry" Peer-to-Peer Application Development Substrate 

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

package rice.pastry;

import java.util.*;

/**
 * Represents a contiguous range of Pastry ids.
 * *
 * @version $Id$
 *
 * @author Peter Druschel
 */

public class IdRange {
    private boolean empty;
    private Id ccw;
    private Id cw;

    /**
     * Constructor.
     *
     * @param ccw the id at the counterclockwise edge of the range (inclusive)
     * @param cw the id at the clockwise edge of the range (exclusive)
     */
    public IdRange(Id ccw, Id cw) {
	empty = false;
	this.ccw = ccw;
	this.cw = cw;
    }

    /**
     * Constructor, constructs an empty IdRange
     *
     */
    public IdRange() {
	empty = true;
	this.ccw = new Id();
	this.cw = new Id();
    }

    /**
     * Copy constructor.
     */
    public IdRange(IdRange o) {
	this.empty = o.empty;
	this.ccw  = o.ccw;
	this.cw = o.cw;
    }


    /**
     * equality operator
     *
     * @param obj the other IdRange
     * @return true if the IdRanges are equal
     */
    public boolean equals(Object obj) {
	IdRange o = (IdRange) obj;

	if (empty == o.empty && ccw.equals(o.ccw) && cw.equals(o.cw)) return true;
	else return false;
    }
        
    /**
     * return the size of the range
     * @return the numerical distance of the range
     */ 
    public Id.Distance size() {
	if (ccw.clockwise(cw))
	    return ccw.distance(cw);
	else
	    return ccw.longDistance(cw);
    }

    /**
     * test if the range is empty
     * @return true if the range is empty
     */ 
    public boolean isEmpty() {
	return empty;
    }

    /**
     * test if a given key lies within this range
     *
     * @param key the key
     * @return true if the key lies within this range, false otherwise
     */
    public boolean contains(Id key) {
	if (ccw.equals(cw) && !empty) return true;
	else return key.isBetween(ccw, cw);
    }

    /**
     * get counterclockwise edge of range
     * @return the id at the counterclockwise edge of the range (inclusive)
     */ 
    public Id getCCW() {
	return ccw;
    }

    /**
     * get clockwise edge of range
     * @return the id at the clockwise edge of the range (exclusive)
     */ 
    public Id getCW() {
	return cw;
    }

    /**
     * set counterclockwise edge of range
     * @param ccw the new id at the counterclockwise edge of the range (inclusive)
     */ 
    private void setCCW(Id ccw) {
	this.ccw = ccw;
	empty = false;
    }

    /**
     * set clockwise edge of range
     * @param cw the new id at the clockwise edge of the range (exclusive)
     */ 
    private void setCW(Id cw) {
	this.cw = cw;
	empty = false;
    }

    /**
     * merge two ranges
     * if this and other don't overlap and are not adjacent, the result is this
     * 
     * @param o the other range
     * @return the resulting range
     */
    public IdRange merge(IdRange o) {

	if (o.empty || (ccw.equals(cw) && !empty) ) return this;
	if (empty || (o.ccw.equals(o.cw) && !o.empty) ) return o;

	boolean ccwIn = ccw.isBetween(o.ccw, o.cw) || ccw.equals(o.cw);
	boolean cwIn = cw.isBetween(o.ccw, o.cw);
	boolean occwIn = o.ccw.isBetween(ccw, cw) || o.ccw.equals(cw);
	boolean ocwIn = o.cw.isBetween(ccw, cw);

	if (ccwIn && cwIn && occwIn && ocwIn) {
	    // ranges cover entire ring
	    return new IdRange(ccw, ccw);
	}

	if (ccwIn) {
	    if (cwIn) return o;
	    else return new IdRange(o.ccw, cw);
	} 

	if (cwIn) {
	    return new IdRange(ccw, o.cw);
	}

	if (occwIn) {
	    return this;
	}

	// no intersection
	return this;

    }

    /**
     * get the complement of this range on the ring
     *
     * @return the complement range
     */
    public IdRange complement() {
	if (ccw.equals(cw) && !empty) 
	    return new IdRange();
	else 
	    return new IdRange(cw, ccw);
    }

    /**
     * intersect two ranges
     * returns an empty range if the ranges don't intersect
     *
     * two ranges may intersect in two ranges on the circle; this method produces one such range of intersection if one exists
     * the other range of intersection can be computed by invoking o.intersect(this)
     *
     * @param o the other range
     * @return the result range
     */
    public IdRange intersect(IdRange o) {

	if (empty || o.empty) return new IdRange();
	if (ccw.equals(cw)) return o;
	if (o.ccw.equals(o.cw)) return this;

	boolean ccwIn = ccw.isBetween(o.ccw, o.cw);
	boolean cwIn = cw.isBetween(o.ccw, o.cw) && !cw.equals(o.ccw);
	boolean occwIn = o.ccw.isBetween(ccw, cw);
	boolean ocwIn = o.cw.isBetween(ccw, cw) && !o.cw.equals(ccw);

	if (ccwIn && cwIn && occwIn && ocwIn) {
	    // ranges intersect in two ranges, return ccw range
	    return new IdRange(ccw, o.cw);
	}

	if (ccwIn) {
	    if (cwIn) return this;
	    else return new IdRange(ccw, o.cw);
	} 

	if (cwIn) {
	    return new IdRange(o.ccw, cw);
	}

	if (occwIn) {
	    return o;
	}

	// no intersection
	return new IdRange();

    }

    /**
     * compute the difference between two ranges 
     * (exclusive or of keys in the two ranges)
     *
     * two ranges may differ in two ranges on the circle; this method produces one such range of difference if one exists
     * the other range of difference can be computed by invoking o.diff(this)
     *
     * @param o the other range
     * @return the result range
     */
     public IdRange diff(IdRange o) {
	 IdRange res = intersect(o.complement());
	 if (res.isEmpty()) res = o.intersect(complement());
	 return res;
     }

    /**
     * subtract the other range from this
     * computes the ranges of keys that are in this but not in o
     *
     * subtracting a range may produce two ranges on the circle; this method produces one such ranges under control
     * of the cwPart parameter
     *
     * @param o the other range
     * @param cwPart if true, returns the clockwise part of the range subtraction, else the counterclockwise part
     * @return the result range
     */
    public IdRange subtract(IdRange o, boolean cwPart) {
	if (!cwPart) return intersect(o.complement());
	else         return o.complement().intersect(this);
    }

    /**
     * compute the difference between two ranges 
     * (exclusive or of keys in the two ranges)
     *
     * @param o the other range
     * @param cwPart if true, returns the clockwise part of the range difference, else the counterclockwise part
     * @return the result range
     */
     public IdRange diff_old(IdRange o, boolean cwPart) {

	 if (equals(o)) return new IdRange();

	 if (ccw.equals(cw)) {
	     if (empty) return o;
	     else return o.complement();
	 }

	 if (o.ccw.equals(o.cw)) {
	     if (o.empty) return this;
	     else return complement();
	 }

	 if (!cwPart) {
	     if (o.ccw.equals(ccw))
		 return new IdRange();

	     if (ccw.isBetween(o.ccw, o.cw))
		 return new IdRange(o.ccw, ccw);

	     if (o.ccw.isBetween(ccw, cw))
		 return new IdRange(ccw, o.ccw);

	     return this;
	 }
	 else {

	     if (o.cw.equals(cw))
		 return new IdRange();

	     if (o.cw.isBetween(ccw, cw))
		 return new IdRange(o.cw, cw);

	     if (cw.isBetween(o.ccw, o.cw))
		 return new IdRange(cw, o.cw);

	     return o;
	 }
	
    }
    
    /**
     * subtract the other range from this
     * computes the ranges of keys that are in this but not in o
     *
     * returns - an empty range if the ranges are identical
     *         - if the subtraction results in a single range, then this method
     * returns an empty range when called with one boolean value of 
     * parameter 'cwPart' and returns the desired range when called with
     * the other boolean value of parameter 'cwPart'
     *
     * @param o the other range
     * @param cwPart if true, returns the clockwise part of the range subtraction, else the counterclockwise part
     * @return the result range
     */
    public IdRange subtract_old(IdRange o, boolean cwPart) {
	IdRange diffRange;

	diffRange = diff_old(o, cwPart);
	return intersect(diffRange);
    }

    /**
     * get counterclockwise half of the range
     * @return the range corresponding to the ccw half of this range
     */ 
    public IdRange ccwHalf() {
	Id newCW = ccw.add(size().shift(1,0,true));
	return new IdRange(ccw, newCW);
    }

    /**
     * get clockwise half of the range
     * @return the range corresponding to the cw half of this range
     */ 
    public IdRange cwHalf() {
	Id newCCW = ccw.add(size().shift(1,0,true));
	return new IdRange(newCCW, cw);
    }


    /**
     * Returns a string representation of the range.
     */

    public String toString() 
    {
	if (empty) return "IdRange: empty";
	else return "IdRange: from:" + ccw + " to:" + cw;
    }

}




