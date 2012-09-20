package org.codehaus.staxmate.out;

import java.io.*;

import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.codehaus.staxmate.SMOutputFactory;

public class TestSimple
    extends BaseWriterTest
{
    /**
     * Simple test to verify namespace bindings.
     */
    public void testSimpleNS()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        final String NS_URI = "http://foo";
        SMNamespace ns = doc.getNamespace(NS_URI);

        SMOutputElement elem = doc.addElement("root"); // no NS
        elem.addElement(ns, "leaf1");
        elem.addElement(ns, "leaf2");
        doc.closeRoot();
 
        // Ok let's verify, then using just plain old Stax
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI, "leaf1");
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI, "leaf2");
        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(END_ELEMENT, sr.next());

        sr.close();
    }

    /**
     * Another namespace binding test; this time checking
     * whether use of optional prefix argument might confuse writer.
     */
    public void testPrefixedNS()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);

        final String NS_URI1 = "http://foo";
        SMNamespace ns1 = doc.getNamespace(NS_URI1);
        final String NS_URI2 = "http://bar";
        final String NS_PREFIX2 = "pr";
        SMNamespace ns2 = doc.getNamespace(NS_URI2, NS_PREFIX2);

        SMOutputElement elem = doc.addElement("root"); // no NS
        elem.addElement(ns1, "leaf1");
        elem.addElement(ns2, "leaf2");
        doc.closeRoot();
 
        // Ok let's verify using just plain old Stax
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI1, "leaf1");
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI2, "leaf2");
        assertEquals(NS_PREFIX2, sr.getPrefix());
        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(END_ELEMENT, sr.next());

        sr.close();
    }

    /**
     * Unit test for checking fix for [STAXMATE-20], incorrect
     * scoping for non-default namespaces.
     */
    public void testPrefixedNS2()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        final String NS_PREFIX = "pr";
        final String NS_URI = "http://foo";
        SMNamespace ns = doc.getNamespace(NS_URI, NS_PREFIX);

        SMOutputElement elem = doc.addElement("root"); // no NS
        elem.addElement(ns, "leaf1");
        elem.addElement(ns, "leaf2");
        doc.closeRoot();
 
        // Ok let's verify using just plain old Stax
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI, "leaf1");
        assertEquals(NS_PREFIX, sr.getPrefix());
        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI, "leaf2");
        assertEquals(NS_PREFIX, sr.getPrefix());
        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(END_ELEMENT, sr.next());

        sr.close();
    }

    /**
     * Unit test for verifying that attribute-namespace binding
     * works correctly, distinct from handling of element namespaces.
     */
    public void testAttrNS2()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        final String NS_URI = "http://foo";
        // this should create default namespace
        SMNamespace ns = doc.getNamespace(NS_URI);

        SMOutputElement elem = doc.addElement(ns, "root");
        /* Note: attributes can NOT use default namespace, must generate
         * additional binding.
         */
        elem.addAttribute(ns, "attr", "value");
        doc.closeRoot();
 
        // Ok let's verify using just plain old Stax
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI, "root");

        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("value", sr.getAttributeValue(0));
        assertEquals(NS_URI, sr.getAttributeNamespace(0));

        assertTokenType(END_ELEMENT, sr.next());

        sr.close();
    }
}
