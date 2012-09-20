package org.codehaus.staxmate.in;

import javax.xml.stream.XMLStreamException;

/**
 * Simple class that defines for objects used to configure iterators so that
 * they will filter out "uninteresting" events.
 *<p>
 * Note: instances can do other things
 * too (keep track of things they iterate over) if they are stateful;
 * for example, collect text, or validate structure, although main purpose
 * is filtering.
 *
 * @author Tatu Saloranta
 */
public abstract class SMFilter
{
    /**
     * Methods iterators call to check whether specified event should
     * be return, or filtered out.
     *
     * @param evt Enumerated (type of the) event that would be passed/filtered
     * @param caller Iterator that is calling this filter. Note that at
     *   this point it is possible that not all state information
     *   of iterator have been updated; however, its stream reader
     *   should be accessible, as well as tracked element information
     *   PRIOR to current node (which may or may not be a start element)
     *
     * @return If true, event is to be returned; if false, it should be
     *    filtered out.
     */
    public abstract boolean accept(SMEvent evt, SMInputCursor caller)
        throws XMLStreamException;
}
