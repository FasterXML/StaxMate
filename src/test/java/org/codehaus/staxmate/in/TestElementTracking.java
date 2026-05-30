package org.codehaus.staxmate.in;

import java.io.StringReader;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor.Tracking;

/**
 * Unit tests for the element-tracking feature, which exercises the
 * {@link SMElementInfo} abstract base class and its default implementation
 * {@link DefaultElementInfo}.
 */
public class TestElementTracking
    extends ReaderTestBase
{
    /**
     * Verifies the info recorded for a tracked root element: it has no
     * parent and no previous sibling, and exposes name/depth/index data.
     */
    public void testRootTracking()
        throws Exception
    {
        SMInputCursor rootc = _cursor("<root><a/></root>");
        rootc.setElementTracking(Tracking.PARENTS);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext());

        SMElementInfo info = rootc.getTrackedElement();
        assertNotNull(info);
        assertEquals("root", info.getLocalName());

        // Root: no parent, no previous sibling
        assertNull(info.getParent());
        assertTrue(info.isRoot());
        assertNull(info.getPreviousSibling());
        assertTrue(info.isFirstChild());

        assertEquals(0, info.getDepth());
        assertEquals(0, info.getElementIndex());
    }

    /**
     * With VISIBLE_SIBLINGS tracking on a child cursor, each tracked child
     * should link to its parent (the root's tracked element) and to its
     * previous sibling.
     */
    public void testChildTrackingWithSiblings()
        throws Exception
    {
        SMInputCursor rootc = _cursor("<root><a/><b/><c/></root>");
        rootc.setElementTracking(Tracking.PARENTS);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext());
        SMElementInfo rootInfo = rootc.getTrackedElement();

        SMInputCursor childc = rootc.childElementCursor();
        childc.setElementTracking(Tracking.VISIBLE_SIBLINGS);

        // First child: "a"
        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        SMElementInfo a = childc.getTrackedElement();
        assertNotNull(a);
        assertEquals("a", a.getLocalName());
        assertSame(rootInfo, a.getParent());
        assertFalse(a.isRoot());
        // first iterated child has no previous sibling
        assertNull(a.getPreviousSibling());
        assertTrue(a.isFirstChild());
        assertEquals(1, a.getDepth());

        // Second child: "b", previous sibling is "a"
        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        SMElementInfo b = childc.getTrackedElement();
        assertEquals("b", b.getLocalName());
        assertSame(rootInfo, b.getParent());
        assertSame(a, b.getPreviousSibling());
        assertFalse(b.isFirstChild());
        assertEquals(1, b.getDepth());

        // Third child: "c", previous sibling is "b"
        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        SMElementInfo c = childc.getTrackedElement();
        assertEquals("c", c.getLocalName());
        assertSame(b, c.getPreviousSibling());

        assertNull(childc.getNext());
    }

    /**
     * PARENTS tracking should retain the parent linkage but not previous
     * sibling information.
     */
    public void testParentsTrackingHasNoSiblings()
        throws Exception
    {
        SMInputCursor rootc = _cursor("<root><a/><b/></root>");
        rootc.setElementTracking(Tracking.PARENTS);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext());
        SMElementInfo rootInfo = rootc.getTrackedElement();

        SMInputCursor childc = rootc.childElementCursor();
        childc.setElementTracking(Tracking.PARENTS);

        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        SMElementInfo a = childc.getTrackedElement();
        assertSame(rootInfo, a.getParent());

        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        SMElementInfo b = childc.getTrackedElement();
        assertSame(rootInfo, b.getParent());
        // PARENTS tracking does not record previous siblings
        assertNull(b.getPreviousSibling());
        assertTrue(b.isFirstChild());
    }

    /**
     * Sanity-check that the directly-constructed DefaultElementInfo exposes
     * the values it was given (covers the trivial accessors and the
     * namespace/prefix getters not used by the no-namespace cases above).
     */
    public void testDefaultElementInfoAccessors()
        throws Exception
    {
        SMElementInfo parent = new DefaultElementInfo(null, null,
                null, "", "root", 0, 0, 0);
        SMElementInfo child = new DefaultElementInfo(parent, null,
                "p", "http://foo", "leaf", 1, 1, 1);

        assertSame(parent, child.getParent());
        assertNull(child.getPreviousSibling());
        assertEquals("p", child.getPrefix());
        assertEquals("http://foo", child.getNamespaceURI());
        assertEquals("leaf", child.getLocalName());
        assertEquals(1, child.getNodeIndex());
        assertEquals(1, child.getElementIndex());
        assertEquals(1, child.getDepth());
        assertFalse(child.isRoot());
        assertTrue(child.isFirstChild());
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private SMInputCursor _cursor(String xml)
        throws XMLStreamException
    {
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        return SMInputFactory.rootElementCursor(sr);
    }
}
