package org.codehaus.staxmate.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.*;

import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.codehaus.staxmate.SMOutputFactory;

/**
 * Unit tests for entity-reference output. The buffered path exercises
 * {@link SMOEntityRef} (a deferred {@link SMOutputtable} node), whereas the
 * direct path writes straight through the stream writer.
 */
public class TestEntityRef
    extends BaseWriterTest
{
    /**
     * Adding an entity reference to a buffered (not-yet-released) fragment
     * forces creation of an {@link SMOEntityRef} node, whose output is
     * deferred until the fragment is released.
     */
    @Test
    public void testBufferedEntityRef()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        SMOutputElement root = doc.addElement("root");
        SMBufferedFragment frag = root.createBufferedFragment();
        root.addBuffered(frag);
        // Use the predefined "amp" entity so it round-trips without a DTD
        frag.addCharacters("a");
        frag.addEntityRef("amp");
        frag.addCharacters("b");
        frag.release();
        doc.closeRoot();

        // "&amp;" parses back to a single '&', coalesced with surrounding text
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("a&b", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    /**
     * Adding an entity reference directly to a writable element writes it
     * straight through (the non-buffered path).
     */
    @Test
    public void testDirectEntityRef()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        SMOutputElement root = doc.addElement("root");
        root.addCharacters("x");
        root.addEntityRef("amp");
        root.addCharacters("y");
        doc.closeRoot();

        XMLStreamReader sr = getCoalescingReader(sw.toString());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("x&y", sr.getText());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }
}
