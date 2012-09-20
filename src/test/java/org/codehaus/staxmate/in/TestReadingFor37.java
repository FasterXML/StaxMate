package org.codehaus.staxmate.in;

import java.io.StringReader;

import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLStreamReader2;

public class TestReadingFor37 extends ReaderTestBase
{
    /*
    /**********************************************************************
    /* Actual tests
    /**********************************************************************
     */

    public void testStaxMate37aWithNative() throws Exception {
        _testStaxMate37a(false);
    }

    public void testStaxMate37aWithWrapper() throws Exception {
        _testStaxMate37a(true);
    }
    
    public void testStaxMate37bWithNative() throws Exception {
        _testStaxMate37b(false);
    }

    public void testStaxMate37bWithWrapper() throws Exception {
        _testStaxMate37b(true);
    }
    
    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _testStaxMate37a(boolean wrap) throws Exception
    {
        String XML = "<root>\n<a>xyz</a>\n</root>";
        SMInputCursor rootC = _rootCursor(wrap, XML);
        SMInputCursor c = rootC.childElementCursor().advance();
        assertEquals(SMEvent.START_ELEMENT, c.getCurrEvent());
        assertEquals("a", c.getLocalName());
        assertEquals("xyz", c.getElemStringValue());
        assertNull(c.getNext());
    }
    
    private void _testStaxMate37b(boolean wrap) throws Exception
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
