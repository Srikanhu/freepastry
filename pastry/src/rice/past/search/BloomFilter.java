package rice.past.search;

import java.io.Serializable;

/**
 * TODO: Derek Ruths finish this comment
 * 
 * @author Derek Ruths
 */
public interface BloomFilter extends Serializable {

    /**
     * @return <code>true</code> when the object specified may be contained
     * in this filter.  Since this is an inexact hash, false positives are
     * possible.
     */
    public boolean contains(Object obj);
    
    /**
     * This method adds an object to this filter so that calls to {@link contains}
     * providing this object as the argument will return <code>true</code>.
     * 
     * @param obj is the object to add to this filter.
     */
    public void add(Object obj);
}
