package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Interface that denotes output objects (fragments, elements) that explicitly
 * start their life-cycle as buffered (other objects can be implicitly buffered
 * due to explict ones as parents or previous siblings).
 */
public interface SMBufferable
{
    /**
     * Method called to signal that the node need not be buffered any more
     * (if not required to do so by parent/children restrictions)
     */
    public void release()
        throws XMLStreamException;

    /**
     * @return True if this object is still buffered; false if not
     */
    public boolean isBuffered();

    /**
     * Method called by a container when bufferable item is linked as its
     * child. It should not only add parent linkage, but also do any
     * output necessary, if this item is not buffered or blocked.
     *
     * @param parent Container to attach bufferable instance under
     * @param blocked If true, parent output is blocked (and as the result
     *   so is bufferable's); if false, parent is (and will remain)
     *   unblocked.
     */
    public void linkParent(SMOutputContainer parent, boolean blocked)
        throws XMLStreamException;
}
