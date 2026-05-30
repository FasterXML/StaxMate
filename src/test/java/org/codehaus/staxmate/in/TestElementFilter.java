package org.codehaus.staxmate.in;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Unit tests specifically for {@link ElementFilter}, verifying that it
 * accepts only element nodes matching the configured (namespace +) local
 * name.
 */
public class TestElementFilter
    extends ReaderTestBase
{
    /**
     * Filter constructed with just a local name should match elements
     * with that local name and no namespace, and skip everything else.
     */
    @Test
    public void testLocalNameOnly()
        throws Exception
    {
        String XML = "<root><a>1</a><b/><a/><c/></root>";
        SMInputCursor childc = _rootChildCursor(XML, new ElementFilter("a"));

        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        assertEquals("a", childc.getLocalName());
        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        assertEquals("a", childc.getLocalName());
        // only the two no-namespace <a> elements should be visible
        assertNull(childc.getNext());
    }

    /**
     * A namespace-qualified filter should only match elements in that
     * namespace, ignoring same-local-name elements in no/other namespace.
     */
    @Test
    public void testNamespaceMatching()
        throws Exception
    {
        final String NS = "http://foo";
        String XML = "<root xmlns:p='" + NS + "'><a/><p:a/><a/></root>";

        // Filter for namespaced "a": only <p:a/> should match
        SMInputCursor nsc = _rootChildCursor(XML, new ElementFilter(NS, "a"));
        assertEquals(SMEvent.START_ELEMENT, nsc.getNext());
        assertEquals(NS, nsc.getNsUri());
        assertEquals("a", nsc.getLocalName());
        assertNull(nsc.getNext());

        // Filter for no-namespace "a": the two plain <a/> elements match
        SMInputCursor plainc = _rootChildCursor(XML, new ElementFilter("a"));
        assertEquals(SMEvent.START_ELEMENT, plainc.getNext());
        assertEquals("a", plainc.getLocalName());
        assertEquals(SMEvent.START_ELEMENT, plainc.getNext());
        assertEquals("a", plainc.getLocalName());
        assertNull(plainc.getNext());
    }

    /**
     * The QName constructor should behave identically to the explicit
     * (nsURI, localName) constructor.
     */
    @Test
    public void testQNameConstructor()
        throws Exception
    {
        final String NS = "http://foo";
        String XML = "<root xmlns:p='" + NS + "'><a/><p:a/></root>";
        SMInputCursor childc = _rootChildCursor(XML, new ElementFilter(new QName(NS, "a")));

        assertEquals(SMEvent.START_ELEMENT, childc.getNext());
        assertEquals(NS, childc.getNsUri());
        assertEquals("a", childc.getLocalName());
        assertNull(childc.getNext());
    }

    /**
     * An empty local name is explicitly allowed by the constructor but
     * should never match any element.
     */
    @Test
    public void testEmptyLocalNameMatchesNothing()
        throws Exception
    {
        String XML = "<root><a/><b/></root>";
        SMInputCursor childc = _rootChildCursor(XML, new ElementFilter(""));
        assertNull(childc.getNext());
    }

    @Test
    public void testNullLocalNameFails()
    {
        try {
            new ElementFilter((String) null);
            fail("Expected NullPointerException for null local name");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().indexOf("localName") >= 0);
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private SMInputCursor _rootChildCursor(String xml, SMFilter filter)
        throws XMLStreamException
    {
        XMLStreamReader sr = getInputFactory().createStax2Reader(new StringReader(xml));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext());
        return rootc.childCursor(filter);
    }
}
