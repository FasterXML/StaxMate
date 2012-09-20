package org.codehaus.staxmate.in;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.typed.TypedXMLStreamException;

import org.codehaus.staxmate.SMInputFactory;

/**
 * Basic unit tests to verify that typed access methods work as expected.
 *
 * @author Tatu Saloranta
 */
public class TestTyped extends ReaderTestBase
{
    private enum DummyEnum {
        OK, FAIL, UNKNOWN;
    }

    // 18-Nov-2010, tatu: Uncomment to test with SJSXP directly
    /*
    @Override
    protected XMLInputFactory getStaxInputFactory() {
        try {
            return (XMLInputFactory) Class.forName("com.sun.xml.internal.stream.XMLInputFactoryImpl").newInstance();
        } catch (Exception e) {
            fail("Unexpected problem: "+e);
            return null;
        }
    }
    */
    
    /*
    /**********************************************************************
    /* Tests for typed attributes
    /**********************************************************************
     */

    public void testTypedBooleanAttr()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root attr='true' attr2='1' attr3='' />";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        assertTrue(rootc.getAttrBooleanValue(0));
        // as per XML Schema, '0' and '1' are valid too
        assertTrue(rootc.getAttrBooleanValue(1));
        // empty is not, but works with defaults:
        assertTrue(rootc.getAttrBooleanValue(2, true));
    }

    public void testTypedIntAttr()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root attr='-37' attr2='foobar' />";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        assertEquals(-37, rootc.getAttrIntValue(0));
        // and then default
        assertEquals(13, rootc.getAttrIntValue(1, 13));
    }

    public void testTypedLongAttr()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root attr='-37' attr2='' />";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        assertEquals(-37L, rootc.getAttrLongValue(0));
        // and then default
        assertEquals(13L, rootc.getAttrLongValue(1, 13L));
    }

    public void testTypedDoubleAttr()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root attr='-0.1' attr2='' />";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        assertEquals(-0.1, rootc.getAttrDoubleValue(0));
        // and then default
        assertEquals(0.25, rootc.getAttrDoubleValue(1, 0.25));
    }

    public void testValidTypedEnumAttr()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root attr='' attr2='FAIL' />";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        assertNull(rootc.getAttrEnumValue(0, DummyEnum.class));
        assertEquals(DummyEnum.FAIL, rootc.getAttrEnumValue(1, DummyEnum.class));
    }

    public void testInvalidTypedEnumAttr()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root attr='Foobar' />";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        try {
            /*DummyEnum en =*/ rootc.getAttrEnumValue(0, DummyEnum.class);
        } catch (TypedXMLStreamException tex) {
            assertException(tex, "invalid enumeration value");
        }
    }

    /*
    /**********************************************************************
    /* Simple tests for typed elements
    /**********************************************************************
     */
    
    public void testTextElem()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root><a>xyz</a><b>abc</b></root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        SMInputCursor crsr = rootc.childElementCursor().advance();
        assertEquals("a", crsr.getLocalName());
        assertEquals("xyz", crsr.getElemStringValue());
        assertNotNull(crsr.getNext());
        assertEquals("b", crsr.getLocalName());
        assertEquals("abc", crsr.getElemStringValue());
        assertNull(crsr.getNext());
        assertNull(rootc.getNext());
    }

    public void testTypedBooleanElem()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root><a>true</a><b>   0 </b><c>...</c></root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        SMInputCursor crsr = rootc.childElementCursor().advance();
        assertEquals("a", crsr.getLocalName());
        assertTrue(crsr.getElemBooleanValue());
        assertNotNull(crsr.getNext());
        assertEquals("b", crsr.getLocalName());
        assertFalse(crsr.getElemBooleanValue());
        assertNotNull(crsr.getNext());
        assertEquals("c", crsr.getLocalName());
        // this would fail, if not for default:
        assertTrue(crsr.getElemBooleanValue(true));
        assertNull(crsr.getNext());
        assertNull(rootc.getNext());
    }

    public void testTypedIntElem()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root><a>  -1</a><b>  ?</b></root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        SMInputCursor crsr = rootc.childElementCursor().advance();
        assertEquals("a", crsr.getLocalName());
        assertEquals(-1, crsr.getElemIntValue());
        assertNotNull(crsr.getNext());
        assertEquals("b", crsr.getLocalName());
        // let's verify we get the failure as we should
        try {
            /*int v = */ crsr.getElemIntValue();
        } catch (TypedXMLStreamException tex) {
            assertException(tex, "not a valid lexical representation of int");
        }
        // nonetheless, cursors should be valid in this case
        assertNull(crsr.getNext());
        assertNull(rootc.getNext());
    }

    public void testValidTypedEnumElem()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root>    OK </root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        assertEquals(DummyEnum.OK, rootc.getElemEnumValue(DummyEnum.class));
        assertNull(rootc.getNext());
    }

    public void testInvalidTypedEnumElem()
        throws XMLStreamException
    {
        SMInputFactory sf = getInputFactory();
        String XML = "<root>  x </root>";
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        try {
            /*DummyEnum en =*/ rootc.getElemEnumValue(DummyEnum.class);
        } catch (TypedXMLStreamException tex) {
            assertException(tex, "invalid enumeration value");
        }
    }
}
