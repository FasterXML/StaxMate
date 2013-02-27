package org.codehaus.staxmate.out;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.StringWriter;

import javax.xml.stream.XMLStreamReader;

public class TestBinary extends BaseWriterTest
{
    final static byte[] STUFF = new byte[] { 1, 15, (byte) 0xFF, 3 };
    
    public void testSimpleAttributeWrite()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);
        doc.addElement("root").addAttribute("data", STUFF);
        doc.closeRoot();
        
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        // but just using plain old Stax...
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        String enc = sr.getAttributeValue(0);
        assertEquals("AQ//Aw==", enc);
        sr.close();
    }

    public void testSimpleElementWrite()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);
        doc.addElement("root").addValue(STUFF);
        doc.closeRoot();
        
        XMLStreamReader sr = getCoalescingReader(sw.toString());
        // but just using plain old Stax...
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        String str = sr.getElementText();
        assertEquals("AQ//Aw==", str);
        sr.close();
    }
}
