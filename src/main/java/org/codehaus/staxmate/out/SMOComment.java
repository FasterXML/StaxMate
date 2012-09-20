package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Simple container class for storing definition of a buffered
 * comment node.
 */
public class SMOComment
    extends SMSimpleOutput
{
    final String mText;

    public SMOComment(String text) {
        super();
        mText = text;
    }

    protected boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException
    {
        ctxt.writeComment(mText);
        return true;
    }
}
