package org.codehaus.staxmate.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.*;

import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.codehaus.staxmate.SMOutputFactory;

/**
 * Tests that exercise the various write paths of {@link SMOutputContext}
 * (comments, processing instructions, CDATA, entity references, mixed
 * content and root fragments) through the public output API.
 */
public class TestOutputContext
    extends BaseWriterTest
{
    /**
     * Writes a document mixing comments, processing instructions, CDATA and
     * plain text under a single element, then reads it back to verify each
     * node type round-trips.
     */
    @Test
    public void testMiscNodeTypes()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);

        SMOutputElement root = doc.addElement("root");
        root.addComment(" hello ");
        root.addProcessingInstruction("target", "data");
        SMOutputElement leaf = root.addElement("leaf");
        leaf.addCData("a < b");
        doc.closeRoot();

        // Note: do NOT coalesce, so CDATA stays a distinct event
        XMLStreamReader sr = getInputFactory().getStaxFactory()
            .createXMLStreamReader(new StringReader(sw.toString()));

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        assertTokenType(COMMENT, sr.next());
        assertEquals(" hello ", sr.getText());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("target", sr.getPITarget());
        assertEquals("data", sr.getPIData());

        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "leaf");
        assertTokenType(CDATA, sr.next());
        assertEquals("a < b", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());

        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /**
     * Verifies that character content added via the char[] overloads, and
     * an entity reference, are written through the context correctly.
     */
    @Test
    public void testCharArrayAndEntityRef()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);

        SMOutputElement root = doc.addElement("root");
        char[] buf = "Hello".toCharArray();
        root.addCharacters(buf, 0, buf.length);
        root.addEntityRef("amp");
        root.addCharacters("World");
        doc.closeRoot();

        // amp entity expands to '&' once coalesced
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        sr.next(); // advance onto the (coalesced) text node
        assertEquals("Hello&World", collectAllText(sr));
        sr.close();
    }

    /**
     * Verifies the root-fragment path (no XML declaration / document
     * wrapper) which goes through a different set of context methods than
     * the full-document path.
     */
    @Test
    public void testRootFragment()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMRootFragment frag = SMOutputFactory.createOutputFragment(xw);

        SMOutputElement root = frag.addElement("a");
        root.addCharacters("1");
        root.addElement("b").addCharacters("2");
        frag.closeRoot();

        String xml = sw.toString();
        // fragment must not contain an XML declaration
        assertFalse(xml.startsWith("<?xml"),
                "fragment should have no XML declaration: "+xml);

        XMLStreamReader sr = getCoalescingReader(xml);
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "a");
        assertEquals("1", collectAllText(advance(sr)));
        assertTokenType(START_ELEMENT, sr.getEventType());
        assertElem(sr, null, "b");
        assertEquals("2", sr.getElementText());
        sr.close();
    }

    private XMLStreamReader advance(XMLStreamReader sr)
        throws XMLStreamException
    {
        sr.next();
        return sr;
    }

    /**
     * Exercises indentation together with comments and processing
     * instructions, which hit the indentation-suppression branches in the
     * context for non-element nodes.
     */
    @Test
    public void testIndentedWithCommentsAndPI()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);
        doc.setIndentation("\n                    ", 1, 2);

        SMOutputElement root = doc.addElement("root");
        root.addComment("c");
        root.addProcessingInstruction("pi", "d");
        root.addElement("leaf");
        doc.closeRoot();

        String xml = sw.toString();
        // newlines and indentation should be present around the nodes
        assertTrue(xml.indexOf("\n  ") > 0,
                "expected indentation newlines, got: "+xml);
        // and it must still be well-formed / re-readable
        XMLStreamReader sr = getCoalescingReader(xml);
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        sr.close();
    }
}
