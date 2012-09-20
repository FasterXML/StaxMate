package org.codehaus.staxmate.in;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Basic unit tests for verifying that traversal using nested cursors
 * works as expected
 */
public class TestNested
    extends ReaderTestBase
{
    public void testSimpleTwoLevel()
        throws Exception
    {
        String XML = "<?xml version='1.0'?>"
            +"<root>\n"
            +"<leaf />"
            +"<leaf attr='xyz'>text</leaf>"
            +"</root>\n";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        assertEquals(0, rootc.getParentCount());
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext()); // should always have root
        assertElem(rootc, null, "root");
        assertEquals(0, rootc.getParentCount());
        SMInputCursor leafc = rootc.childElementCursor();

        assertEquals(1, leafc.getParentCount());
        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertElem(leafc, null, "leaf");
        assertEquals(1, leafc.getParentCount());

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals(1, leafc.getParentCount());
        assertElem(leafc, null, "leaf");
        assertEquals(1, leafc.getAttrCount());
        assertEquals("attr", leafc.getAttrLocalName(0));
        assertEquals("xyz", leafc.getAttrValue(0));

        assertEquals("text", leafc.collectDescendantText(true));

        assertNull(leafc.getNext());

        assertNull(rootc.getNext());
        
        sr.close();
    }

    public void testSimpleThreeLevel()
        throws XMLStreamException
    {
        String XML =
            /*
            "<root name='root'>"
            +"<branch name='br1'>"
            +"<leaf name='leaf1'>text</leaf>"
            +"<leaf name='leaf2'>text2</leaf>"
            +"</branch>"
            +"</root>"
            */
"<?xml version='1.0' encoding='UTF-8'?>\n"
+"<root name='123' xyx='abc' attr='!'>\n"
+"<pt name='...'><prop name='a'>Authority Non Buyable</prop><prop name='b'>something else</prop><prop name='c'>false</prop></pt>\n"
+"<pt name='pt2'><prop name='1'>Apparel</prop><prop name='2'>Apparel</prop></pt>\n"
+"</root>"
            ;

        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);

        assertEquals(SMEvent.START_ELEMENT, rootc.getNext()); // should always have root
        assertElem(rootc, null, "root");
        assertEquals(3, rootc.getAttrCount());
        assertEquals(0, rootc.getParentCount());

        SMInputCursor brc = rootc.childElementCursor();
        assertEquals(SMEvent.START_ELEMENT, brc.getNext());
        assertElem(brc, null, "pt");
        assertEquals(1, brc.getAttrCount());
        assertEquals(1, brc.getParentCount());
        assertEquals("...", brc.getAttrValue("name"));

        SMInputCursor leafc = brc.childElementCursor();
        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals(2, leafc.getParentCount());
        assertElem(leafc, null, "prop");
        assertEquals(1, leafc.getAttrCount());
        assertEquals("a", leafc.getAttrValue("name"));
        assertEquals("Authority Non Buyable", leafc.collectDescendantText(false));

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertElem(leafc, null, "prop");
        assertEquals(1, leafc.getAttrCount());
        assertEquals("b", leafc.getAttrValue("name"));

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertElem(leafc, null, "prop");

        assertNull(leafc.getNext());

        // Enough, let's move to the next at branch level:
        assertEquals(SMEvent.START_ELEMENT, brc.getNext());
        assertElem(brc, null, "pt");

        // And then check that root is done:

        assertNull(rootc.getNext());
        
        sr.close();
    }

    /**
     * This a complementary test, and checks, to verify against regression
     * in hierarchic cursor synchronization.
     */
    public void testThreeLevel2()
        throws XMLStreamException
    {
        String XML =
"<?xml version='1.0' encoding='UTF-8'?>\n"
+"<root name='123' xyx='abc' attr='!'>\n"
+"<pt name='...'><prop name='a'>Authority Non Buyable</prop><prop name='b'>Authority Non Buyable</prop><prop name='c'>false</prop></pt>\n"
+"<pt name='pt2'><prop name='1'>Apparel</prop><prop name='2'>Apparel</prop></pt>\n"
+"</root>"
            ;
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        rootc.getNext(); // should always have root

        assertElem(rootc, null, "root");
        assertEquals(0, rootc.getParentCount());
        SMInputCursor ptCursor = rootc.childElementCursor();
        while (ptCursor.getNext() != null) {
            assertEquals(1, ptCursor.getParentCount());
            assertElem(ptCursor, null, "pt");
            assertNotNull("Should be able to find attribute 'name' (current event: "
                          +ptCursor.getCurrEvent()+", location "+ptCursor.getCursorLocation()+")", ptCursor.getAttrValue("name"));
            SMInputCursor propCursor = ptCursor.childElementCursor();
            while (propCursor.getNext() != null) {
                assertEquals(2, propCursor.getParentCount());
                assertElem(propCursor, null, "prop");
                String propName = propCursor.getAttrValue("name");
                assertNotNull(propName);
                String value = propCursor.collectDescendantText(false);
                assertNotNull(value);
            }
        }
        sr.close();
    }

    public void testAdvance()
        throws Exception
    {
        String XML = "<?xml version='1.0'?><!-- xxx -->\n<root>...</root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor crsr = SMInputFactory.rootElementCursor(sr).advance();
        assertEquals(SMEvent.START_ELEMENT, crsr.getCurrEvent());
        assertEquals("root", crsr.getLocalName());
        assertNull(crsr.getNext());
        sr.close();
    }
}
