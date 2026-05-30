package org.codehaus.staxmate.in;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

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
    @Test
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
    @Test
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

    /**
     * A child/descendant cursor keeps its own counts; iterating it must not
     * bump the parent cursor's node/element counts.
     */
    @Test
    public void testCountsNotPropagatedToParent()
        throws XMLStreamException
    {
        SMInputFactory sf = new SMInputFactory(XMLInputFactory.newInstance());
        String XML = "<root><a/>text<b/></root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();

        // mixed descendant cursor exposes a-start, a-end, text, b-start, b-end
        SMInputCursor desc = rootc.descendantMixedCursor();
        int events = 0;
        while (desc.getNext() != null) {
            ++events;
        }
        assertEquals(5, events);
        // getNodeCount is 6: it also counts the final advance over the
        // outermost END_ELEMENT (</root>) that terminates iteration.
        assertEquals(6, desc.getNodeCount());
        assertEquals(2, desc.getElementCount());
        // parent only ever advanced over the single root start element:
        assertEquals(1, rootc.getNodeCount());
        assertEquals(1, rootc.getElementCount());
    }
}
