package org.codehaus.staxmate.in;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import javax.xml.namespace.QName;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Tests targeting the name/namespace, attribute-metadata, current-event and
 * user-data accessors of {@link SMInputCursor} that are not exercised by the
 * other input-side tests. (Typed value access lives in {@code TestTyped};
 * node/element counts in {@code TestLocation}.)
 */
public class TestCursorAccessors
    extends ReaderTestBase
{
    /**
     * Verifies element name accessors (local name, prefix, ns-uri, qname,
     * prefixed name) and the various {@code hasName}/{@code hasLocalName}
     * checks, for both namespaced and non-namespaced elements.
     */
    @Test
    public void testElementNameAccessors()
        throws Exception
    {
        final String NS = "http://ns";
        String XML = "<root xmlns:p='"+NS+"'><p:leaf /></root>";
        SMInputFactory sf = getInputFactory();
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();

        // root has no namespace
        assertEquals("root", rootc.getLocalName());
        assertEmpty(rootc.getPrefix());
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
    @Test
    public void testAttributeMetadata()
        throws Exception
    {
        final String NS = "http://attr";
        String XML = "<root xmlns:a='"+NS+"' id='7' a:type='x' plain='p' />";
        SMInputFactory sf = getInputFactory();
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();

        assertEquals(3, rootc.getAttrCount());

        int idIdx = rootc.findAttrIndex(null, "id");
        assertTrue(idIdx >= 0, "'id' attribute should be found");
        assertEquals("id", rootc.getAttrLocalName(idIdx));
        assertEquals("7", rootc.getAttrValue(idIdx));
        assertEquals(new QName("id"), rootc.getAttrName(idIdx));
        // no prefix / namespace for plain attribute
        assertEmpty(rootc.getAttrPrefix(idIdx));
        assertEmpty(rootc.getAttrNsUri(idIdx));

        int typeIdx = rootc.findAttrIndex(NS, "type");
        assertTrue(typeIdx >= 0, "namespaced attribute should be found");
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
     * Verifies current-event accessors and the arbitrary user-data slot
     * ({@code getData}/{@code setData}).
     */
    @Test
    public void testEventAndDataAccessors()
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
    }
}
