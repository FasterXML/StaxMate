package org.codehaus.staxmate.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.*;

import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.codehaus.staxmate.SMOutputFactory;

/**
 * Tests for {@link SMOutputElement} (and, through it, the shared
 * {@link SMOutputContainer} base): typed attribute overloads, namespaced
 * attributes/elements, {@code addElementWithCharacters} and the element
 * accessors.
 */
public class TestOutputElement
    extends BaseWriterTest
{
    /**
     * Exercises the typed {@code addAttribute} overloads (String, boolean,
     * int, long) and the no-namespace convenience overload.
     */
    @Test
    public void testTypedAttributes()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);

        SMOutputElement root = doc.addElement("root");
        SMNamespace noNs = null;
        root.addAttribute("str", "v");
        root.addAttribute(noNs, "flag", true);
        root.addAttribute(noNs, "count", 42);
        root.addAttribute(noNs, "big", 123456789012L);
        doc.closeRoot();

        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        assertEquals("v", sr.getAttributeValue(null, "str"));
        assertEquals("true", sr.getAttributeValue(null, "flag"));
        assertEquals("42", sr.getAttributeValue(null, "count"));
        assertEquals("123456789012", sr.getAttributeValue(null, "big"));
        sr.close();
    }

    /**
     * Attribute bound to a namespace should be written with a generated
     * prefix and correct namespace URI.
     */
    @Test
    public void testNamespacedAttribute()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);

        final String NS = "http://attr";
        SMOutputElement root = doc.addElement("root");
        SMNamespace ns = root.getNamespace(NS, "a");
        root.addAttribute(ns, "id", "7");
        doc.closeRoot();

        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        assertEquals("7", sr.getAttributeValue(NS, "id"));
        sr.close();
    }

    /**
     * {@code addElementWithCharacters} should create a child element holding
     * exactly the given text, with and without a namespace.
     */
    @Test
    public void testAddElementWithCharacters()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);

        final String NS = "http://e";
        SMOutputElement root = doc.addElement("root");
        SMNamespace ns = root.getNamespace(NS);
        root.addElementWithCharacters(null, "plain", "p");
        root.addElementWithCharacters(ns, "nsel", "n");
        doc.closeRoot();

        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "plain");
        assertEquals("p", sr.getElementText());

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS, "nsel");
        assertEquals("n", sr.getElementText());

        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /**
     * Verifies the element accessors: getLocalName / getNamespace, plus
     * getPath built across nested elements.
     */
    @Test
    public void testElementAccessorsAndPath()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);

        final String NS = "http://p";
        SMOutputElement root = doc.addElement("root");
        SMNamespace ns = root.getNamespace(NS);
        SMOutputElement child = root.addElement(ns, "child");

        assertEquals("root", root.getLocalName());
        assertEquals("child", child.getLocalName());
        // root has no namespace; child is bound to NS
        SMNamespace rootNs = root.getNamespace();
        assertTrue(rootNs == null || rootNs.getURI() == null || rootNs.getURI().length() == 0,
                "root should have no namespace, was: "+rootNs);
        assertEquals(NS, child.getNamespace().getURI());

        // getPath includes both ancestors
        StringBuilder sb = new StringBuilder();
        child.getPath(sb);
        String path = sb.toString();
        assertTrue(path.indexOf("root") >= 0, "path should include root: "+path);
        assertTrue(path.indexOf("child") >= 0, "path should include child: "+path);

        doc.closeRoot();
    }
}
