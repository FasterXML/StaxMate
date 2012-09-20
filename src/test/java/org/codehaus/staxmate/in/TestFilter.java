package org.codehaus.staxmate.in;

import java.io.StringReader;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Unit tests for verifying that bundled SMFilter implementations
 * work as expected
 */
public class TestFilter
    extends ReaderTestBase
{
    public void testChildElementFilter()
        throws Exception
    {
        String XML = "<!-- foo --><root>text<branch /><!-- comment -->  </root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext());
        assertEquals("root", rootc.getLocalName());
        SMInputCursor leafc = rootc.childElementCursor();
        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals("branch", leafc.getLocalName());
        assertNull(leafc.getNext());

        assertNull(rootc.getNext());
    }

    public void testDescendantElementFilter()
        throws Exception
    {
        String XML = "<?pi data?><root>"
            +"text<branch>2</branch><!-- comment -->123</root>";
        XMLInputFactory inf = XMLInputFactory.newInstance();
        inf.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader sr = inf.createXMLStreamReader(new StringReader(XML));
        SMInputCursor crsr = SMInputFactory.flatteningCursor(sr, SMFilterFactory.getElementOnlyFilter());

        assertEquals(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals("root", crsr.getLocalName());
        assertEquals(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals("branch", crsr.getLocalName());
        assertEquals(SMEvent.END_ELEMENT, crsr.getNext());
        assertEquals("branch", crsr.getLocalName());
        assertEquals(SMEvent.END_ELEMENT, crsr.getNext());
        assertEquals("root", crsr.getLocalName());
        assertNull(crsr.getNext());
    }

    public void testChildTextFilter()
        throws Exception
    {
        String XML = "<!-- foo --><root>text<branch /><!-- comment --> x </root>";
        XMLInputFactory inf = XMLInputFactory.newInstance();
        inf.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader sr = inf.createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        rootc.getNext();
        SMInputCursor leafc = rootc.childCursor(SMFilterFactory.getTextOnlyFilter());
        assertEquals(SMEvent.TEXT, leafc.getNext());
        assertEquals("text", leafc.getText());
        /* Can't coalesce over underlying other types: but will skip
         * child elem, and comment
         */
        assertEquals(SMEvent.TEXT, leafc.getNext());
        assertEquals(" x ", leafc.getText());
        assertNull(leafc.getNext());

        assertNull(rootc.getNext());
    }

    public void testDescendantTextFilter()
        throws Exception
    {
        String XML = "<?pi?><root>"
            +"ab<branch>c</branch>de<!-- comment -->f<foo />gh</root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor crsr = SMInputFactory.flatteningCursor(sr, SMFilterFactory.getTextOnlyFilter());
        StringBuilder sb = new StringBuilder();
        while (crsr.getNext() != null) {
            sb.append(crsr.getText());
        }
        assertEquals("abcdefgh", sb.toString());
    }

    public void testChildMixedFilter()
        throws Exception
    {
        String XML = "  <?pi data?><root>"
            +"text<branch>2</branch><!-- comment -->123</root>";
        XMLInputFactory inf = XMLInputFactory.newInstance();
        inf.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader sr = inf.createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        rootc.getNext();
        SMInputCursor branchc = rootc.childMixedCursor();
        assertEquals(SMEvent.TEXT, branchc.getNext());
        assertEquals("text", branchc.getText());
        assertEquals(SMEvent.START_ELEMENT, branchc.getNext());
        assertEquals("branch", branchc.getLocalName());
        SMInputCursor leafc = branchc.childMixedCursor();
        assertEquals(SMEvent.TEXT, leafc.getNext());
        assertEquals("2", leafc.getText());
        assertNull(leafc.getNext());

        assertEquals(SMEvent.TEXT, branchc.getNext());
        assertEquals("123", branchc.getText());
        assertNull(branchc.getNext());

        assertNull(rootc.getNext());
    }

    /**
     * Simple test for a flattening mixed (text, start/end element)
     * cursor
     */
    public void testDescendantMixedFilter()
        throws Exception
    {
        String XML = "<?pi data?><root>"
            +"text<branch>2</branch><!-- comment -->123</root>";
        XMLInputFactory inf = XMLInputFactory.newInstance();
        inf.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader sr = inf.createXMLStreamReader(new StringReader(XML));
        SMInputCursor crsr = SMInputFactory.flatteningCursor(sr, SMFilterFactory.getMixedFilter());

        assertEquals(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals("root", crsr.getLocalName());
        assertEquals(SMEvent.TEXT, crsr.getNext());
        assertEquals("text", crsr.getText());
        assertEquals(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals("branch", crsr.getLocalName());
        assertEquals(SMEvent.TEXT, crsr.getNext());
        assertEquals("2", crsr.getText());
        assertEquals(SMEvent.END_ELEMENT, crsr.getNext());
        assertEquals("branch", crsr.getLocalName());
        assertEquals(SMEvent.TEXT, crsr.getNext());
        assertEquals("123", crsr.getText());
        assertEquals(SMEvent.END_ELEMENT, crsr.getNext());
        assertEquals("root", crsr.getLocalName());
        assertNull(crsr.getNext());
    }

    public void testFlatteningFilterWithAdvance()
        throws Exception
    {
        String XML = "<?pi data?><root />";
        XMLStreamReader sr = getCoalescingReader(XML);

        SMInputCursor crsr = SMInputFactory.flatteningCursor(sr, SMFilterFactory.getElementOnlyFilter()).advance();

        assertEquals(SMEvent.START_ELEMENT, crsr.getCurrEvent());
        assertEquals("root", crsr.getLocalName());
        // with flattening cursor, will get end-element too:
        assertEquals(SMEvent.END_ELEMENT, crsr.getNext());

        sr.close();
    }
}

