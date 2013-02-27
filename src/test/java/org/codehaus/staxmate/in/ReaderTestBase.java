package org.codehaus.staxmate.in;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;

abstract class ReaderTestBase
    extends org.codehaus.staxmate.StaxMateTestBase
{
    final static String BASE64_ENCODED = "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
            +"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
            +"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
            +"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
            +"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=";

    final static String BASE64_DECODED_STRING = "Man is distinguished, not only by his reason, but by this singular passion"
            +" from other animals, which is a lust of the mind, that by a perseverance of delight"
            +" in the continued and indefatigable generation of knowledge, exceeds the short vehemence"
            +" of any carnal pleasure.";
    final static byte[] BASE64_DECODED_BYTES;
    static {
        byte[] data = null;
        
        try {
            data = BASE64_DECODED_STRING.getBytes("UTF-8");
        } catch (Exception e) { }
        BASE64_DECODED_BYTES = data;
    }    

    protected void assertElem(SMInputCursor crsr, String expURI, String expLN)
        throws XMLStreamException
    {
        assertEquals(expLN, crsr.getLocalName());
        assertTrue(crsr.hasLocalName(expLN));
        assertTrue(crsr.hasName(expURI, expLN));

        String uri = crsr.getNsUri();
        if (expURI == null) {
            if (uri != null && uri.length() > 0) {
                fail("Expected element to have no namespace, got '"+uri+"'");
            }
        } else {
            if (!expURI.equals(uri)) {
                fail("Expected element to have non-empty namespace '"+expURI+"', got '"+uri+"'");
            }
        }
    }

    protected XMLStreamReader2 forceWrapping(XMLStreamReader sr)
    {
        return new ForcedWrapper(sr);
    }

    /*
    /**********************************************************
    /( Helper classes
    /**********************************************************
     */

    /**
     * Helper class needed to be able to wrap any stream reader, not
     * just ones that do not implement XMLStreamReader2
     */
    final static class ForcedWrapper extends Stax2ReaderAdapter
    {
        public ForcedWrapper(XMLStreamReader sr) {
            super(sr);
        }
    }
}
