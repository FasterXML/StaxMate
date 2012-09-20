package org.codehaus.staxmate.in;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;

/**
 *
 * @author Michel Goldstein
 * @author Tatu Saloranta
 */
public class TestSimpleText
    extends ReaderTestBase
{
    public void testSimpleRead()
        throws XMLStreamException
    {
        final String text = "1";
        String XML = "<lvl1><lvl2>" + text + "</lvl2></lvl1>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        rootc.getNext();
        String elemName = rootc.getLocalName();
        assertEquals("lvl1",elemName);
        SMInputCursor mainC = rootc.childElementCursor();
        while(mainC.getNext() != null) {
            SMInputCursor child = mainC.childCursor();
            child.getNext();
            String valText = child.getText();
            assertEquals(text,valText);
        }
    }

    public void testCollectText()
        throws XMLStreamException
    {
        String XML = "<root>Some<?proc instr?> <foo>text</foo> to <![CDATA[collect]]>.</root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr).advance();
        assertEquals("Some text to collect.", rootc.collectDescendantText(true));
    }
}
