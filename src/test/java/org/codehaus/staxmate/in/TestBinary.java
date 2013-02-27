package org.codehaus.staxmate.in;

import java.io.StringReader;

public class TestBinary extends ReaderTestBase
{
    public void testSimpleAttribute() throws Exception
    {
        String XML = "<tag data='"+BASE64_ENCODED+"' />";
        SMInputCursor rootc = getInputFactory().rootElementCursor(new StringReader(XML));
        rootc.getNext();
        assertEquals("tag", rootc.getLocalName());
        byte[] data = rootc.getAttrBinaryValue(0);
        assertNotNull(data);
        assertEquals(BASE64_DECODED_STRING, new String(data, "UTF-8"));

        assertNull(rootc.getNext());
    }

    public void testSimpleElement() throws Exception
    {
        String XML = "<root><data>"+BASE64_ENCODED+"</data></root>";
        SMInputCursor rootc = getInputFactory().rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
        SMInputCursor kid = rootc.childElementCursor().advance();
        byte[] data = kid.getElemBinaryValue();
        assertNotNull(data);
        assertEquals(BASE64_DECODED_STRING, new String(data, "UTF-8"));

        assertNull(kid.getNext());
        assertNull(rootc.getNext());
    }
}
