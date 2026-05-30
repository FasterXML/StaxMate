package org.codehaus.staxmate.in;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Tests for reading element text via {@link SMInputCursor#getElemStringValue}
 * through child element cursors. Exercises both the native Stax2 reader and a
 * forced Stax 1.0 wrapper, since the two go through different code paths.
 *<p>
 * Regression coverage for [STAXMATE-37]: {@code getElemStringValue} used to
 * cause an unexpected END_DOCUMENT with the default Java 6 XMLInputFactory.
 */
public class TestGetElemStringValue extends ReaderTestBase
{
    /*
    /**********************************************************************
    /* Actual tests
    /**********************************************************************
     */

    @Test
    public void testSingleChildNative() throws Exception {
        _testSingleChild(false);
    }

    @Test
    public void testSingleChildWrapped() throws Exception {
        _testSingleChild(true);
    }

    @Test
    public void testTwoChildrenNative() throws Exception {
        _testTwoChildren(false);
    }

    @Test
    public void testTwoChildrenWrapped() throws Exception {
        _testTwoChildren(true);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _testSingleChild(boolean wrap) throws Exception
    {
        String XML = "<root>\n<a>xyz</a>\n</root>";
        SMInputCursor rootC = _rootCursor(wrap, XML);
        SMInputCursor c = rootC.childElementCursor().advance();
        assertEquals(SMEvent.START_ELEMENT, c.getCurrEvent());
        assertEquals("a", c.getLocalName());
        assertEquals("xyz", c.getElemStringValue());
        assertNull(c.getNext());
    }

    private void _testTwoChildren(boolean wrap) throws Exception
    {
        String XML = "<root>\n<a>xyz</a>\n<b>abc</b>\n</root>";
        SMInputCursor rootC = _rootCursor(wrap, XML);
        SMInputCursor c = rootC.childElementCursor().advance();
        assertEquals(SMEvent.START_ELEMENT, c.getCurrEvent());
        assertEquals("a", c.getLocalName());
        assertEquals("xyz", c.getElemStringValue());
        assertEquals(SMEvent.START_ELEMENT, c.getNext());
        assertEquals("b", c.getLocalName());
        assertNull(c.getNext());
    }

    private SMInputCursor _rootCursor(boolean wrap, String XML) throws Exception
    {
        XMLStreamReader sr = getStaxInputFactory().createXMLStreamReader(new StringReader(XML));
        XMLStreamReader2 sr2;
        if (wrap || !(sr instanceof XMLStreamReader2)) {
            sr2 = forceWrapping(sr);
        } else {
            sr2 = (XMLStreamReader2) sr;
        }
        SMInputContext ctxt = new SMInputContext(sr2);
        return new SMHierarchicCursor(ctxt, null, SMFilterFactory.getElementOnlyFilter()).advance();
    }
}
