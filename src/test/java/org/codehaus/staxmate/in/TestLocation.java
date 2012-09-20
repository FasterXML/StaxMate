package org.codehaus.staxmate.in;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Unit test for verifying that location-related information is properly
 * kept track of, and accessible.
 */
public class TestLocation
    extends ReaderTestBase
{
    /**
     * Unit test that will verify that "node count" is kept track of
     * when using hierarchic (nested) cursors.
     */
    public void testNodeCountNested()
        throws XMLStreamException
    {
        SMInputFactory sf = new SMInputFactory(XMLInputFactory.newInstance());
        String XML = "<root><!--comment--><a>text</a><?pi?><leaf /></root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        // let's traverse using element cursor, to skip comment
        SMInputCursor crsr = rootc.childElementCursor();
        assertEquals(0, crsr.getNodeCount());
        // should skip over comment, bump into element
        assertToken(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals(2, crsr.getNodeCount());
        // and then over children, its contents and PI
        assertToken(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals(4, crsr.getNodeCount());
        assertNull(crsr.getNext());
    }

    /**
     * Unit test that will verify that "element count" is kept track of
     * when using hierarchic (nested) cursors.
     */
    public void testElementCountNested()
        throws XMLStreamException
    {
        SMInputFactory sf = new SMInputFactory(XMLInputFactory.newInstance());
        String XML = "<root><!--comment--><a>text</a><?pi?><leaf /></root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        // let's traverse using element cursor, to skip comment
        SMInputCursor crsr = rootc.childElementCursor();
        assertEquals(0, crsr.getElementCount());
        // should skip over comment, bump into element
        assertToken(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals(1, crsr.getElementCount());
        // and then over children, its contents and PI
        assertToken(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals(2, crsr.getElementCount());
        assertNull(crsr.getNext());
    }
}
