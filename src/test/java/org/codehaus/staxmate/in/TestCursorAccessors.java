package org.codehaus.staxmate.in;

import java.io.StringReader;

import javax.xml.namespace.QName;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Tests targeting the various name, namespace, attribute-metadata, count
 * and user-data accessors of {@link SMInputCursor} that are not exercised
 * by the other input-side tests.
 */
public class TestCursorAccessors
    extends ReaderTestBase
{
    /**
     * Verifies element name accessors (local name, prefix, ns-uri, qname,
     * prefixed name) and the various {@code hasName}/{@code hasLocalName}
     * checks, for both namespaced and non-namespaced elements.
     */
    public void testElementNameAccessors()
        throws Exception
    {
        final String NS = "http://ns";
        String XML = "<root xmlns:p='"+NS+"'><p:leaf /></root>";
        SMInputFactory sf = getInputFactory();
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();

        // root has no namespace
        assertEquals("root", rootc.getLocalName());
        String rootPrefix = rootc.getPrefix();
        assertTrue("root prefix should be null or empty, was '"+rootPrefix+"'",
                rootPrefix == null || rootPrefix.length() == 0);
        assertEquals("", rootc.getNsUri());
        assertEquals("root", rootc.getPrefixedName());
        assertEquals(new QName("root"), rootc.getQName());
        assertTrue(rootc.hasLocalName("root"));
        assertFalse(rootc.hasLocalName("nope"));
        assertTrue(rootc.hasName(null, "root"));
        assertFalse(rootc.hasName(NS, "root"));
        assertTrue(rootc.hasName(new QName("root")));

        // child is prefixed/namespaced
        SMInputCursor leafc = rootc.childElementCursor().advance();
        assertEquals("leaf", leafc.getLocalName());
        assertEquals("p", leafc.getPrefix());
        assertEquals(NS, leafc.getNsUri());
        assertEquals("p:leaf", leafc.getPrefixedName());
        assertEquals(new QName(NS, "leaf", "p"), leafc.getQName());
        assertTrue(leafc.hasLocalName("leaf"));
        assertTrue(leafc.hasName(NS, "leaf"));
        assertFalse(leafc.hasName(null, "leaf"));
        assertTrue(leafc.hasName(new QName(NS, "leaf")));
        assertFalse(leafc.hasName(new QName(NS, "other")));
    }

    /**
     * Verifies attribute-metadata accessors: count, index lookup, and
     * per-index name/prefix/ns-uri/value, plus the by-name value lookups.
     */
    public void testAttributeMetadata()
        throws Exception
    {
        final String NS = "http://attr";
        String XML = "<root xmlns:a='"+NS+"' id='7' a:type='x' plain='p' />";
        SMInputFactory sf = getInputFactory();
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();

        assertEquals(3, rootc.getAttrCount());

        int idIdx = rootc.findAttrIndex(null, "id");
        assertTrue("'id' attribute should be found", idIdx >= 0);
        assertEquals("id", rootc.getAttrLocalName(idIdx));
        assertEquals("7", rootc.getAttrValue(idIdx));
        assertEquals(new QName("id"), rootc.getAttrName(idIdx));
        // no prefix / namespace for plain attribute
        String idPrefix = rootc.getAttrPrefix(idIdx);
        assertTrue(idPrefix == null || idPrefix.length() == 0);
        String idNs = rootc.getAttrNsUri(idIdx);
        assertTrue(idNs == null || idNs.length() == 0);

        int typeIdx = rootc.findAttrIndex(NS, "type");
        assertTrue("namespaced attribute should be found", typeIdx >= 0);
        assertEquals("a", rootc.getAttrPrefix(typeIdx));
        assertEquals(NS, rootc.getAttrNsUri(typeIdx));
        assertEquals(new QName(NS, "type", "a"), rootc.getAttrName(typeIdx));

        // lookup of non-existent attribute
        assertEquals(-1, rootc.findAttrIndex(null, "missing"));

        // by-name value lookups
        assertEquals("7", rootc.getAttrValue("id"));
        assertEquals("p", rootc.getAttrValue("plain"));
        assertNull(rootc.getAttrValue("missing"));
        assertEquals("x", rootc.getAttrValue(NS, "type"));
        assertNull(rootc.getAttrValue(NS, "missing"));
    }

    /**
     * Verifies typed element value accessors not covered elsewhere
     * (long and double, with and without default values).
     */
    public void testTypedLongAndDoubleElem()
        throws Exception
    {
        // one typed accessor per element (each consumes the element content):
        String XML = "<root><a>  -123456789012  </a><b>2.5</b><c/><d/></root>";
        SMInputFactory sf = getInputFactory();
        SMInputCursor crsr = sf.rootElementCursor(new StringReader(XML))
            .advance().childElementCursor().advance();

        assertEquals("a", crsr.getLocalName());
        assertEquals(-123456789012L, crsr.getElemLongValue());

        assertEquals(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals("b", crsr.getLocalName());
        assertEquals(2.5, crsr.getElemDoubleValue());

        // empty elements fall back to the supplied default
        assertEquals(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals("c", crsr.getLocalName());
        assertEquals(99L, crsr.getElemLongValue(99L));

        assertEquals(SMEvent.START_ELEMENT, crsr.getNext());
        assertEquals("d", crsr.getLocalName());
        assertEquals(1.5, crsr.getElemDoubleValue(1.5));
    }

    /**
     * Verifies node/element counters, current-event accessors, and the
     * arbitrary user-data slot ({@code getData}/{@code setData}).
     */
    public void testCountsEventsAndData()
        throws Exception
    {
        String XML = "<root><a/>text<b/></root>";
        SMInputFactory sf = getInputFactory();
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();

        assertEquals(SMEvent.START_ELEMENT, rootc.getCurrEvent());
        assertEquals(SMEvent.START_ELEMENT.getEventCode(), rootc.getCurrEventCode());
        assertTrue(rootc.readerAccessible());
        assertTrue(rootc.isRootCursor());

        // user-data slot starts empty, round-trips an arbitrary object
        assertNull(rootc.getData());
        Object marker = new Object();
        rootc.setData(marker);
        assertSame(marker, rootc.getData());

        // iterate all descendants, counting nodes vs elements
        SMInputCursor desc = rootc.descendantMixedCursor();
        int events = 0;
        while (desc.getNext() != null) {
            ++events;
        }
        assertTrue("should have iterated several descendant nodes", events > 0);
        // two elements (a, b) seen by the child cursor's parent
        assertTrue(rootc.getNodeCount() >= rootc.getElementCount());
        assertTrue(rootc.getElementCount() >= 1);
    }
}
