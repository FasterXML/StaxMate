package org.codehaus.staxmate.out;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.*;

abstract class BaseWriterTest
    extends org.codehaus.staxmate.StaxMateTestBase
{
    protected SMOutputDocument createSimpleDoc(Writer w)
        throws XMLStreamException
    {
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        SMOutputFactory smo = new SMOutputFactory(f);
        return smo.createOutputDocument(w);
    }
}
