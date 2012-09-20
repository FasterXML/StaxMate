package org.codehaus.staxmate.in;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;

// important: keep abstract so JUnit won't be confused
abstract class ReaderTestBase
    extends org.codehaus.staxmate.StaxMateTestBase
{
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
    ////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////
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
