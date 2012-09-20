package org.codehaus.staxmate.dom;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.w3c.dom.*;

import org.codehaus.staxmate.StaxMateTestBase;

public class TestDOMConverter
    extends StaxMateTestBase
{
    /**
     * Unit test that verifies that a proper DOM tree can be constructed
     * from Stax stream reader.
     */
    public void testDOMReadFromStax()
        throws XMLStreamException
    {
        String XML =
            "<root>abc<?proc    instr?>"
            +"<leaf xmlns:a='http://foo' attr='3' a:b='c' /></root>"
            +"    <!--comment-stuff-->   "
            ;
        DOMConverter conv = new DOMConverter();
        XMLStreamReader sr = getCoalescingReader(XML);
        Document doc = conv.buildDocument(sr);
        assertNotNull(doc);
        Node root = doc.getFirstChild();
        assertNotNull(root);
        // should be <root> elem
        assertEquals(Node.ELEMENT_NODE, root.getNodeType());
        assertEquals("root", root.getNodeName());

        // First, let's check stuff beyond root
        Node n = root.getNextSibling();
        assertEquals(Node.COMMENT_NODE, n.getNodeType());
        assertEquals("comment-stuff", n.getNodeValue());

        // Then children of root

        n = root.getFirstChild();
        assertEquals(Node.TEXT_NODE, n.getNodeType());
        assertEquals("abc", n.getNodeValue());
        n = n.getNextSibling();
        assertEquals(Node.PROCESSING_INSTRUCTION_NODE, n.getNodeType());
        assertEquals("proc", n.getNodeName());
        assertEquals("instr", n.getNodeValue());
        n = n.getNextSibling();
        assertEquals(Node.ELEMENT_NODE, n.getNodeType());
        assertEquals("leaf", n.getNodeName());
        assertEmpty(n.getNamespaceURI());

        assertNull(n.getFirstChild());
        NamedNodeMap attrs = n.getAttributes();
        assertEquals(3, attrs.getLength());

        Attr a = (Attr) attrs.getNamedItem("attr");
        assertNotNull(a);
        assertEquals("attr", a.getName());
        assertEmpty(a.getNamespaceURI());
        assertEquals("3", a.getValue());

        a = (Attr) attrs.getNamedItem("xmlns:a");
        assertNotNull(a);
        assertEquals("xmlns:a", a.getName());
        assertEquals("http://foo", a.getValue());

        a = (Attr) attrs.getNamedItemNS("http://foo", "b");
        assertNotNull(a);
        assertEquals("a:b", a.getName());
        assertEquals("a", a.getPrefix());
        assertEquals("b", a.getLocalName());
        assertEquals("http://foo", a.getNamespaceURI());
        assertEquals("c", a.getValue());

        assertNull(n.getNextSibling());
    }

    public void testDOMWrittenUsingStax()
        throws Exception
    {
        String XML =
            "<root>abc<?proc instr?><!--comment-stuff-->"
            +"<leaf xmlns:a='http://foo' attr='3' a:b='c' /></root>";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputStream in = new ByteArrayInputStream(XML.getBytes("UTF-8"));
        Document doc = db.parse(in);

        StringWriter sw = new StringWriter(XML.length() + 16);
        DOMConverter conv = new DOMConverter();
        XMLStreamWriter strw = getSimpleWriter(sw);
        conv.writeDocument(doc, strw);

        String docStr = sw.toString();

        XMLStreamReader sr = getCoalescingReader(docStr);
        assertTokenType(START_ELEMENT, sr.next());

        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertTokenType(COMMENT, sr.next());

        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(END_DOCUMENT, sr.next());
    }

    public void testIssue39a() throws Exception
    {
        final String XML =
"<?xml version='1.0'?>\n"
+"<root><node att1='n1-att1' att2='n1-att2'>n1-text</node>\n"
+"<node att1='n2-att1' att2='n2-att2'>n2-text</node></root>"
        ;
        XMLStreamReader sr = getStaxInputFactory().createXMLStreamReader(new StringReader(XML));

        while (true) {
            if (sr.getEventType() == XMLStreamReader.START_ELEMENT) {
                Document doc = new DOMConverter().buildDocument(sr);
                assertNotNull(doc);
            }
            if (!sr.hasNext()) {
                break;
            }
            sr.next();
        }
        sr.close();
    }

    public void testIssue39b() throws Exception
    {
        final String XML =
"<root>\n"
+"        <children>\n"
+"                <child>\n"
+"                        <schild>a</schild>A\n"
+"                        <schild>b</schild>B\n"
+"                        <schild>c</schild>C\n"
+"                </child>D\n"
+"                <child>E\n"
+"                        <schild>d</schild>\n"
+"                        <schild>e</schild>\n"
+"                        <schild>f</schild>\n"
+"                </child>\n"
+"                <child>\n"
+"                        <schild>g</schild>\n"
+"                        <schild>h</schild>\n"
+"                        <schild>i</schild>\n"
+"                </child>\n"
+"        </children>\n"
+"       </root>"
            ;
        XMLStreamReader sr = getStaxInputFactory().createXMLStreamReader(new StringReader(XML));

        int count = 0;
        
        while (true) {
            if (sr.getEventType() == XMLStreamReader.START_ELEMENT) {
                if (sr.getLocalName().equals("child")) {                    
                    Document doc = new DOMConverter().buildDocument(sr);
                    assertNotNull(doc);
                    ++count;
                }
            }
            if (!sr.hasNext()) {
                break;
            }
            sr.next();
        }
        sr.close();
        assertEquals(3, count);
    }
    
    // Test for [STAXMATE-41]
    public void testReadWriteWithAttributes() throws Exception
    {
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(
                new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?><test>A test.</test>".getBytes("UTF-8")));
        Document doc = new DOMConverter().buildDocument(sr);
        sr.close();

        // Attempt to modify dom.Document
        doc.getDocumentElement().setAttribute("new", "test");

        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        StringWriter out = new StringWriter();
        XMLStreamWriter writerX = xof.createXMLStreamWriter(out);
        new DOMConverter().writeDocument(doc, writerX);
        out.close();
        String xml = out.toString();
        // let's convert all double-quotes to apostrophes, for comparison
        xml = xml.replaceAll("\"", "'");
        // should probably traverse, instead of direct comparison but...
        assertEquals("<?xml version='1.0' encoding='UTF-8'?><test new='test'>A test.</test>", xml);
    }

    // Test for [STAXMATE-41]
    public void testReadWriteElement() throws Exception
    {
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(
                new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?><test></test>".getBytes("UTF-8")));
        Document doc = new DOMConverter().buildDocument(sr);
        sr.close();

        // Attempt to modify dom.Document
        doc.getDocumentElement().appendChild(doc.createElement("child"));

        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        StringWriter out = new StringWriter();
        XMLStreamWriter writerX = xof.createXMLStreamWriter(out);
        new DOMConverter().writeDocument(doc, writerX);
        out.close();
        String xml = out.toString();
        // let's convert all double-quotes to apostrophes, for comparison
        xml = xml.replaceAll("\"", "'");
        // should probably traverse, instead of direct comparison but...
        assertEquals("<?xml version='1.0' encoding='UTF-8'?><test><child/></test>", xml);
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void assertEmpty(String str)
    {
        assertTrue((str == null) || str.length() == 0);
    }
}
