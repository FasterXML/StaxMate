package org.codehaus.staxmate.in;

import javax.xml.stream.XMLStreamException;

/**
 * Simple factory class that can be used to customize instances of
 * {@link SMElementInfo} that iterators construct and store when element
 * tracking is enabled.
 */
public interface ElementInfoFactory
{
    public SMElementInfo constructElementInfo(SMInputCursor it,
                                              SMElementInfo parent,
                                              SMElementInfo prevSibling)
        throws XMLStreamException;
}

