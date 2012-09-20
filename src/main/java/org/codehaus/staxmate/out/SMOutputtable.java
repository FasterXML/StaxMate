package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Basic building block for all outputtable content within StaxMate.
 * Used as the base for both "active" nodes (elements, fragments; both
 * buffered and unbuffered variations, entities that are created for
 * output scoping, ie. as output containers)
 * and those "passive" nodes that are blocked (ones for which instances
 * are only created when they can not be output right away).
 * It will not be passed for content that can be directly output
 * without buffering (non-blocked text, CDATA, PIs, entity references
 * and so on).
 *<p>
 * Note that parent linkage is not included at this level since it is
 * really only needed for active nodes (output containers; all of them
 * since a non-bufferable container may still contain buffered containers).
 */
public abstract class SMOutputtable
{
    protected SMOutputtable _next = null;

    protected SMOutputtable() { }

    /*
    /////////////////////////////////////////////////////
    // Link handling
    /////////////////////////////////////////////////////
     */

    protected SMOutputtable getNext() {
        return _next;
    }

    protected void _linkNext(SMOutputtable next) {
        if (_next != null) {
            throw new IllegalStateException("Can not re-set next once it has been set once");
        }
        _next = next;
    }

    /*
    /////////////////////////////////////////////////////
    // Output handling
    /////////////////////////////////////////////////////
     */

    /**
     * Method called to request that the entity output itself; either
     * as much as it can without closing, or as much as it can if it is to
     * get closed. In both cases output can fail or be only a partial one:
     * buffered nodes will not be output at all, and nodes with buffered
     * children can only be partially output.
     *
     * @param ctxt Output context to use for outputting this node (and
     *   its contents)
     * @param canClose If true, indicates that the node can (and should)
     *   be fully closed if possible. This (passing true) is usually done
     *    when a new sibling
     *   is added after a node (element/fragment); if so, current one
     *   should be recursively closed. If false, should only try to output
     *   as much as can be done without forcing closures.
     *
     * @return True if the whole node could be output, ie. neither it nor
     *   its children are buffered.
     */
    protected abstract boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException;

    /**
     * Method similar to {@link #_output}, except that this method will
     * always succeed in doing the output. Specifically, it will force all
     * buffered nodes to be unbuffered, and then output.
     */
    protected abstract void _forceOutput(SMOutputContext ctxt)
        throws XMLStreamException;
}
