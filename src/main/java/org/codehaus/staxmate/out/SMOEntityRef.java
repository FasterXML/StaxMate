package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

public class SMOEntityRef
    extends SMSimpleOutput
{
    final String mName;

    public SMOEntityRef(String name) {
        super();
        mName = name;
    }

    protected boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException
    {
        ctxt.writeEntityRef(mName);
        return true;
    }
}
