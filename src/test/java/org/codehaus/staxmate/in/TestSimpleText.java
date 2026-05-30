package org.codehaus.staxmate.in;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import javax.xml.stream.*;

/**
 *
 * @author Michel Goldstein
 * @author Tatu Saloranta
 */
public class TestSimpleText
    extends ReaderTestBase
{
    @Test
    public void testSimpleRead()
        throws XMLStreamException
    {
        final String text = "1";
        String XML = "<lvl1><lvl2>" + text + "</lvl2></lvl1>";
        SMInputCursor rootc = rootElementCursor(XML);
        rootc.getNext();
        String elemName = rootc.getLocalName();
        assertEquals("lvl1",elemName);
        SMInputCursor mainC = rootc.childElementCursor();
        while(mainC.getNext() != null) {
            SMInputCursor child = mainC.childCursor();
            child.getNext();
            String valText = child.getText();
            assertEquals(text,valText);
        }
    }

    @Test
    public void testCollectText()
        throws XMLStreamException
    {
        String XML = "<root>Some<?proc instr?> <foo>text</foo> to <![CDATA[collect]]>.</root>";
        SMInputCursor rootc = rootElementCursor(XML).advance();
        assertEquals("Some text to collect.", rootc.collectDescendantText(true));
    }
}
