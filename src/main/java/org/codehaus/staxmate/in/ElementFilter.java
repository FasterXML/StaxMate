package org.codehaus.staxmate.in;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * This is a simple element-only filter, that accepts those element
 * nodes that match the given element name.
 *
 * @author Tatu Saloranta
 */
public class ElementFilter
    extends SimpleFilter
{
    final String mNsURI;

    final String mLocalName;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle
    /////////////////////////////////////////////////////
     */

    public ElementFilter(String nsURI, String localName)
    {
        /* Note: END_ELEMENT only matters with non-nested (flat)
         * cursors; for nested/hierarchic ones only START_ELEMENTs
         * are encountered.
         */
        super((1 << XMLStreamConstants.START_ELEMENT) |
              (1 << XMLStreamConstants.END_ELEMENT));
        if (localName == null) {
            throw new NullPointerException("localName can not be null");
        }
        /* Let's allow empty, though -- will result in filter that never
         * matches anything
         */
        mLocalName = localName;
        mNsURI = (nsURI == null || nsURI.length() == 0) ? null : nsURI;
    }

    public ElementFilter(String localName)
    {
        this(null, localName);
    }

    public ElementFilter(QName elemName)
    {
        this(elemName.getNamespaceURI(), elemName.getLocalPart());
    }

    /*
    /////////////////////////////////////////////////////
    // SMFilter implementation
    /////////////////////////////////////////////////////
     */

    public boolean accept(SMEvent evt, SMInputCursor caller)
        throws XMLStreamException
    {
        if (super.accept(evt, caller)) {
            if (caller.getLocalName().equals(mLocalName)) {
                String uri = caller.getNsUri();
                if (mNsURI == null) {
                    return (uri == null || uri.length() == 0);
                } else {
                    return mNsURI.equals(uri);
                }
            }
        }
        return false;
    }
}

