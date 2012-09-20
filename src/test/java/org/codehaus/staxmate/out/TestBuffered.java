package org.codehaus.staxmate.out;

import java.io.*;

import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.codehaus.staxmate.SMOutputFactory;

/**
 * Basic set of simple unit tests that check that the buffered output
 * mode works as expected.
 */
public class TestBuffered
    extends BaseWriterTest
{
    /**
     * Unit test for verifying that buffered output works ok
     */
    public void testBuffered()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        final String NS_URI1 = "http://foo";
        final String NS_PREFIX2 = "myns";
        final String NS_URI2 = "urn://hihhei";

        SMNamespace ns1 = doc.getNamespace(NS_URI1);
        SMNamespace ns2 = doc.getNamespace(NS_URI2, NS_PREFIX2);

        final String COMMENT_CONTENT = "Comment!";
        doc.addComment(COMMENT_CONTENT);

        SMOutputElement elem = doc.addElement(ns1, "root");
        final String TEXT_CONTENT1 = "Rock & Roll";
        elem.addCharacters(TEXT_CONTENT1);
        SMBufferedFragment frag = elem.createBufferedFragment();
        elem.addBuffered(frag);
        final String TEXT_CONTENT2 = "[FRAG";
        frag.addCharacters(TEXT_CONTENT2);
        final String COMMENT_CONTENT2 = "!!!";
        frag.addComment(COMMENT_CONTENT2);
        frag.addElement(ns1, "tag");
        SMOutputElement elem2 = elem.addElement("branch");
        elem2.addElement(ns2, "leaf");
        final String TEXT_CONTENT3 = "ment!]";
        frag.addCharacters(TEXT_CONTENT3);
        frag.release();
        elem.addElement(ns2, "leaf2");
        doc.closeRoot();

        // Uncomment for debugging:
        //System.out.println("Result:");
        //System.out.println(sw.toString());
 
        // Ok let's verify, then:
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        // but just using plain old Stax...

        assertTokenType(COMMENT, sr.next());
        assertEquals(COMMENT_CONTENT, sr.getText());

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI1, "root");

        assertTokenType(CHARACTERS, sr.next());
        assertEquals(TEXT_CONTENT1 + TEXT_CONTENT2, sr.getText());

        assertTokenType(COMMENT, sr.next());
        assertEquals(COMMENT_CONTENT2, sr.getText());

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI1, "tag");
        assertTokenType(END_ELEMENT, sr.next());
        assertElem(sr, NS_URI1, "tag");
        assertTokenType(CHARACTERS, sr.next());
        assertEquals(TEXT_CONTENT3, sr.getText());

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "branch");
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI2, "leaf");
        // ideally will also use the prefix passed?
        assertEquals(NS_PREFIX2, sr.getPrefix());
        assertTokenType(END_ELEMENT, sr.next());
        assertElem(sr, NS_URI2, "leaf");
        assertTokenType(END_ELEMENT, sr.next());
        assertElem(sr, null, "branch");

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, NS_URI2, "leaf2");
        assertTokenType(END_ELEMENT, sr.next());
        assertElem(sr, NS_URI2, "leaf2");

        assertTokenType(END_ELEMENT, sr.next());
        assertElem(sr, NS_URI1, "root");

        sr.close();
    }

    /**
     * Even simpler unit test that verifies that it is ok to pass
     * null namespaces
     */
    public void testBufferedNoNs()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        SMOutputElement elem = doc.addElement("root");
        SMBufferedElement leafFrag = elem.createBufferedElement(null, "leaf");
        elem.addAndReleaseBuffered(leafFrag);
        doc.closeRoot();

        // Ok let's verify, then:
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        // but just using plain old Stax...
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        assertEquals(0, sr.getAttributeCount());
        assertEquals(0, sr.getNamespaceCount());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(0, sr.getAttributeCount());
        assertEquals(0, sr.getNamespaceCount());
        assertElem(sr, null, "leaf");
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /**
     * Another simple unit test that exercises some alternative text
     * output methods
     */
    public void testBufferedWithText() throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);
        final String TEXT1 = "A&B";
        final String TEXT2 = "something else";

        SMBufferedElement broot = doc.createBufferedElement(null, "x");
        broot.addCData(TEXT1);
        broot.addCData(TEXT2.toCharArray(), 0, TEXT2.length());
        broot.addCharacters(TEXT2);
        broot.addCharacters(TEXT1.toCharArray(), 0, TEXT1.length());

        doc.addAndReleaseBuffered(broot);
        doc.closeRootAndWriter();

        // Ok let's verify, then:
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        // but just using plain old Stax...
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "x");
        assertTokenType(CHARACTERS, sr.next());
        /* Just in case the underlying parser does not implement
         * coalescing (... an early version of Aalto, for example),
         * let's collect all text.
         */
        assertEquals(TEXT1+TEXT2+TEXT2+TEXT1, collectAllText(sr));
        // note: above moves cursor...
        assertTokenType(END_ELEMENT, sr.getEventType());

        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    public void testBufferedWithAttr()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        SMOutputElement elem = doc.addElement("root");
        SMBufferedElement leafFrag = elem.createBufferedElement(null, "leaf");
        leafFrag.addAttribute("attr", "value");
        leafFrag.addComment("comment");
        leafFrag.addProcessingInstruction("target", "data");
        elem.addAndReleaseBuffered(leafFrag);
        doc.closeRoot();

        // Ok let's verify, then:
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        // but just using plain old Stax...
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(1, sr.getAttributeCount());
        assertEquals(0, sr.getNamespaceCount());
        assertElem(sr, null, "leaf");
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("value", sr.getAttributeValue(0));

        assertTokenType(COMMENT, sr.next());
        assertEquals("comment", sr.getText());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("target", sr.getPITarget());
        assertEquals("data", sr.getPIData());

        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }
}
