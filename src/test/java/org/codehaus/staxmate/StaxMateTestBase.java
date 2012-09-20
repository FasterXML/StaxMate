package org.codehaus.staxmate;

import java.io.*;

import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.codehaus.staxmate.in.*;

public abstract class StaxMateTestBase
    extends junit.framework.TestCase
{
    protected SMInputFactory _inputFactory;

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    protected SMInputFactory getInputFactory()
    {
        if (_inputFactory == null) {
            _inputFactory = new SMInputFactory(getStaxInputFactory());
        }
        return _inputFactory;
    }

    protected XMLInputFactory getStaxInputFactory() {
        return XMLInputFactory.newInstance();
    }
    
    protected XMLStreamReader getCoalescingReader(String content)
        throws XMLStreamException
    {
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        return f.createXMLStreamReader(new StringReader(content));
    }

    protected XMLStreamWriter getSimpleWriter(Writer w)
        throws XMLStreamException
    {
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        return f.createXMLStreamWriter(w);
    }

    /*
    ////////////////////////////////////////////////////////
    // Assertion support
    ////////////////////////////////////////////////////////
     */

    protected void assertTokenType(int expType, int actType)
        throws XMLStreamException
    {
        assertEquals(expType, actType);
    }

    protected void assertTokenType(int expType, XMLStreamReader sr)
        throws XMLStreamException
    {
        assertTokenType(expType, sr.getEventType());
    }

    protected void assertTokenType(int expType, SMInputCursor crsr)
        throws XMLStreamException
    {
        assertTokenType(expType, crsr.getCurrEventCode());
    }

    protected void assertToken(SMEvent expEvent, SMEvent actEvent)
        throws XMLStreamException
    {
        assertTokenType(expEvent.getEventCode(), actEvent.getEventCode());
    }

    protected void assertElem(XMLStreamReader sr, String expURI, String expLN)
        throws XMLStreamException
    {
        assertEquals(expLN, sr.getLocalName());
        String actURI = sr.getNamespaceURI();
        if (expURI == null || expURI.length() == 0) {
            if (actURI != null && actURI.length() > 0) {
                fail("Expected no namespace, got URI '"+actURI+"'");
            }
        } else {
            assertEquals(expURI, sr.getNamespaceURI());
        }
    }

    protected void assertException(Throwable e, String match)
    {
        String msg = e.getMessage();
        String lmsg = msg.toLowerCase();
        String lmatch = match.toLowerCase();
        if (lmsg.indexOf(lmatch) < 0) {
            fail("Expected an exception with sub-string \""+match+"\": got one with message \""+msg+"\"");
        }
    }

    /*
    /**********************************************************************
    /* Other accessors
    /**********************************************************************
     */

    /**
     * Note: calling this method will move stream to the next
     * non-textual event.
     */
    protected String collectAllText(XMLStreamReader sr)
        throws XMLStreamException
    {
        StringBuilder sb = new StringBuilder(100);
        while (true) {
            int type = sr.getEventType();
            if (type == CHARACTERS || type == SPACE || type == CDATA) {
                sb.append(sr.getText());
                sr.next();
            } else {
                break;
            }
        }
        return sb.toString();
    }
}
