package org.codehaus.staxmate.in;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;

abstract class ReaderTestBase
    extends org.codehaus.staxmate.StaxMateTestBase
{
    /**
     * Convenience helper: builds a hierarchic (nested) root cursor over the
     * given XML using the shared input factory. Caller advances as needed.
     */
    protected SMInputCursor rootElementCursor(String xml)
        throws XMLStreamException
    {
        return getInputFactory().rootElementCursor(new StringReader(xml));
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
