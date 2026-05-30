package org.codehaus.staxmate.in;

import javax.xml.stream.XMLStreamException;

/**
 * The default implementation of {@link SMElementInfo}; fully implements
 * all accessors by storing information necessary. 
 *
 * @author Tatu Saloranta
 */
public class DefaultElementInfo
    extends SMElementInfo
{
    final SMElementInfo mParentElem, mPrevSiblingElem;

    final String mNsURI;
    final String mLocalName;
    final String mPrefix;

    final int mNodeIndex, mElemIndex;
    final int mDepth;

    public DefaultElementInfo(SMElementInfo parent, SMElementInfo prevSibling,
                              String prefix, String nsURI, String localName,
                              int nodeIndex, int elemIndex, int depth)
        throws XMLStreamException
    {
        super();
        mParentElem = parent;
        mPrevSiblingElem = prevSibling;

        mPrefix = prefix;
        mNsURI = nsURI;
        mLocalName = localName;

        mNodeIndex = nodeIndex;
        mElemIndex = elemIndex;
        mDepth = depth;
    }

    /*
    /////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////
     */

    @Override
    public SMElementInfo getParent() { return mParentElem; }
    @Override
    public SMElementInfo getPreviousSibling() { return mPrevSiblingElem; }

    @Override
    public int getNodeIndex() { return mNodeIndex; }
    @Override
    public int getElementIndex() { return mElemIndex; }
    @Override
    public int getDepth() { return mDepth; }

    @Override
    public String getNamespaceURI() { return mNsURI; }
    @Override
    public String getLocalName() { return mLocalName; }
    @Override
    public String getPrefix() { return mPrefix; }
}
 