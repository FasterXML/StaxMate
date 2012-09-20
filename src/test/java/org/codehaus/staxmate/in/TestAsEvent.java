package org.codehaus.staxmate.in;

import java.io.*;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Basic tests to verify that {@link SMInputCursor#asEvent} works as
 * expected.
 */
public class TestAsEvent
    extends ReaderTestBase
{
    final static String SIMPLE_XML =
        "<root>fobar<?proc instr?><!--comment-stuff-->"
        +"<leaf attr='3' /></root>";

    public void testSimpleUsingHierarchic()
        throws XMLStreamException
    {
        SMInputCursor rootC = getInputFactory().rootElementCursor(new StringReader(SIMPLE_XML));
        rootC.getNext();
        XMLEvent evt = rootC.asEvent();
        assertNotNull(evt);
        assertTrue(evt.isStartElement());
        StartElement ee = evt.asStartElement();
        assertNotNull(ee);
        // should have no attributes
        Iterator<?> it = ee.getAttributes();
        assertFalse(it.hasNext());
        // hierarchic cursors do not deliver END_ELEMENTs...
        assertNull(rootC.getNext());
        assertNull(rootC.asEvent());
    }

    public void testSimpleUsingFlattening()
        throws XMLStreamException
    {
        SMInputCursor crsr = getInputFactory().flatteningCursor(new StringReader(SIMPLE_XML), null);

        assertNotNull(crsr.getNext());
        XMLEvent evt = crsr.asEvent();
        assertNotNull(evt);
        assertTrue(evt.isStartElement());

        /* theoretically text could be split, but let's assume
         * it's short enough segment not to do it...
         */
        assertEquals(crsr.getNext(), SMEvent.TEXT);
        evt = crsr.asEvent();
        assertTrue(evt.isCharacters());
        assertEquals("fobar", evt.asCharacters().getData());

        assertEquals(crsr.getNext(), SMEvent.PROCESSING_INSTR);
        evt = crsr.asEvent();
        assertFalse(evt.isStartElement());
        assertFalse(evt.isCharacters());

        assertEquals(crsr.getNext(), SMEvent.COMMENT);
        evt = crsr.asEvent();
        assertFalse(evt.isStartElement());
        assertFalse(evt.isCharacters());

        assertEquals(crsr.getNext(), SMEvent.START_ELEMENT);
        evt = crsr.asEvent();
        assertTrue(evt.isStartElement());

        // And exactly one attribute...
        Iterator<?> it = evt.asStartElement().getAttributes();
        assertTrue(it.hasNext());
        Attribute attr = (Attribute)it.next();
        assertTrue(attr.isAttribute());
        assertEquals("3", attr.getValue());
        assertEquals(new QName("attr"), attr.getName());
        assertFalse(it.hasNext());
        
        assertEquals(crsr.getNext(), SMEvent.END_ELEMENT);
        evt = crsr.asEvent();
        assertTrue(evt.isEndElement());

        assertEquals(crsr.getNext(), SMEvent.END_ELEMENT);
        evt = crsr.asEvent();
        assertTrue(evt.isEndElement());

        // None of cursors returns END_DOCUMENT
        assertNull(crsr.getNext());
        assertNull(crsr.asEvent());
    }
}
