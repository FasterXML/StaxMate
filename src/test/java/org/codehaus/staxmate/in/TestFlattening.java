package org.codehaus.staxmate.in;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Basic unit tests for verifying that traversal using flattening
 * (non-nested) cursors works as expected
 */
public class TestFlattening
    extends ReaderTestBase
{
    public void testTwoLevelMixed()
        throws Exception
    {
        String XML = "<?xml version='1.0'?>"
            +"<root>\n"
            +"<leaf />"
            +"<leaf attr='xyz'>R&amp;b</leaf>"
            +"</root>";
        XMLStreamReader sr = getCoalescingReader(XML);
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext());
        assertEquals(0, rootc.getParentCount());
        assertElem(rootc, null, "root");
        SMInputCursor leafc = rootc.descendantCursor();
        assertEquals(1, leafc.getParentCount());

        assertEquals(SMEvent.TEXT, leafc.getNext());
        assertEquals("\n", leafc.getText());
        assertEquals(1, leafc.getParentCount());

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertElem(leafc, null, "leaf");
        assertEquals(1, leafc.getParentCount());
        assertEquals(SMEvent.END_ELEMENT, leafc.getNext());
        assertElem(leafc, null, "leaf");
        assertEquals(1, leafc.getParentCount());

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertElem(leafc, null, "leaf");
        assertEquals(1, leafc.getParentCount());
        assertEquals(SMEvent.TEXT, leafc.getNext());
        assertEquals("R&b", leafc.getText());
        assertEquals(2, leafc.getParentCount());
        assertEquals(SMEvent.END_ELEMENT, leafc.getNext());
        assertElem(leafc, null, "leaf");
        assertEquals(1, leafc.getParentCount());

        assertNull(leafc.getNext());

        assertEquals(0, rootc.getParentCount());
        assertNull(rootc.getNext());
        
        sr.close();
    }
}

