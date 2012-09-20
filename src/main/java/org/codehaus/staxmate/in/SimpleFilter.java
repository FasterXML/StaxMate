package org.codehaus.staxmate.in;

import javax.xml.stream.XMLStreamException;

/**
 * Simple bit-set based filter that can be configured by specifying allowed
 * event types. Such filters are immutable and can be easily shared, but
 * can only be used if checks are strictly based on only event type.
 *
 * @author Tatu Saloranta
 */
public class SimpleFilter
    extends SMFilter
{
    final int mAllowedTypes;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle
    /////////////////////////////////////////////////////
     */

    public SimpleFilter(int typeFlags)
    {
        mAllowedTypes = typeFlags;
    }

    public SimpleFilter(SMEvent eventToMatch)
    {
        this(1 << eventToMatch.getEventCode());
    }

    public SimpleFilter extend(int additionalFlags, int removeFlags)
    {
        int newTypes = mAllowedTypes | additionalFlags & ~removeFlags;
        return new SimpleFilter(newTypes);
    }

    /*
    /////////////////////////////////////////////////////
    // SMFilter implementation
    /////////////////////////////////////////////////////
     */

    public boolean accept(SMEvent evt, SMInputCursor caller)
        throws XMLStreamException
    {
       return (mAllowedTypes & (1 << evt.getEventCode())) != 0;
    }
}

