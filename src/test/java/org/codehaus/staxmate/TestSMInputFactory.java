package org.codehaus.staxmate;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;

import org.codehaus.staxmate.in.SMFilter;
import org.codehaus.staxmate.in.SMFilterFactory;
import org.codehaus.staxmate.in.SMFlatteningCursor;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;

/**
 * Tests for the various input-source entry points of {@link SMInputFactory}
 * (byte[], InputStream, Reader, File, URL overloads, the static cursor
 * constructors and the global factory accessors), most of which are not
 * exercised by the cursor-behavior tests.
 */
public class TestSMInputFactory
    extends StaxMateTestBase
{
    private final static String XML = "<root><a>1</a><b>2</b></root>";

    /*
    /**********************************************************************
    /* Static cursor constructors
    /**********************************************************************
     */

    @Test
    public void testStaticRootElementCursor() throws Exception
    {
        XMLStreamReader sr = _stax(XML);
        SMHierarchicCursor rootc = SMInputFactory.rootElementCursor(sr);
        assertEquals(1, _countStartElements(rootc)); // single root element
        sr.close();
    }

    @Test
    public void testStaticRootCursorIncludesProlog() throws Exception
    {
        // rootCursor (null filter) exposes comments/PIs too, unlike rootElementCursor
        String doc = "<?xml version='1.0'?><!--c--><root/>";
        SMHierarchicCursor rootc = SMInputFactory.rootCursor(_stax(doc));
        // first node should be the comment (not filtered out)
        assertTokenType(XMLStreamConstants.COMMENT, rootc.getNext().getEventCode());
        assertNotNull(rootc.getNext()); // root element
        assertNull(rootc.getNext());
    }

    @Test
    public void testStaticHierarchicAndFlatteningCursor() throws Exception
    {
        SMHierarchicCursor hc = SMInputFactory.hierarchicCursor(_stax(XML),
                SMFilterFactory.getElementOnlyFilter());
        assertNotNull(hc.getNext());
        assertEquals("root", hc.getLocalName());

        SMFlatteningCursor fc = SMInputFactory.flatteningCursor(_stax(XML),
                SMFilterFactory.getElementOnlyFilter());
        // flattening cursor descends into all elements: root, a, b
        assertEquals(3, _countStartElements(fc));
    }

    /*
    /**********************************************************************
    /* Instance entry points over different input sources
    /**********************************************************************
     */

    @Test
    public void testRootElementCursorFromByteArray() throws Exception
    {
        byte[] pad = ("xx" + XML).getBytes("UTF-8");
        SMInputFactory sf = getInputFactory();
        // offset 2 skips the "xx" padding, proving offset/len are honored
        SMInputCursor rootc = sf.rootElementCursor(pad, 2, pad.length - 2).advance();
        assertEquals("root", rootc.getLocalName());
    }

    @Test
    public void testRootElementCursorFromInputStream() throws Exception
    {
        SMInputFactory sf = getInputFactory();
        InputStream in = new ByteArrayInputStream(XML.getBytes("UTF-8"));
        SMInputCursor rootc = sf.rootElementCursor(in).advance();
        assertEquals("root", rootc.getLocalName());
        in.close();
    }

    @Test
    public void testRootElementCursorFromReader() throws Exception
    {
        SMInputFactory sf = getInputFactory();
        SMInputCursor rootc = sf.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
    }

    @Test
    public void testRootElementCursorFromFileAndURL() throws Exception
    {
        File f = _tempXmlFile();
        SMInputFactory sf = getInputFactory();

        SMInputCursor fromFile = sf.rootElementCursor(f).advance();
        assertEquals("root", fromFile.getLocalName());

        URL url = f.toURI().toURL();
        SMInputCursor fromUrl = sf.rootElementCursor(url).advance();
        assertEquals("root", fromUrl.getLocalName());
    }

    @Test
    public void testCreateStax2ReaderOverloads() throws Exception
    {
        SMInputFactory sf = getInputFactory();
        byte[] bytes = XML.getBytes("UTF-8");

        XMLStreamReader2 r1 = sf.createStax2Reader(bytes, 0, bytes.length);
        assertNotNull(r1);
        r1.close();

        XMLStreamReader2 r2 = sf.createStax2Reader(new ByteArrayInputStream(bytes));
        assertNotNull(r2);
        r2.close();

        XMLStreamReader2 r3 = sf.createStax2Reader(new StringReader(XML));
        assertNotNull(r3);
        r3.close();

        File f = _tempXmlFile();
        XMLStreamReader2 r4 = sf.createStax2Reader(f);
        assertNotNull(r4);
        r4.close();

        XMLStreamReader2 r5 = sf.createStax2Reader(f.toURI().toURL());
        assertNotNull(r5);
        r5.close();
    }

    @Test
    public void testFlatteningCursorFromStreamAndReader() throws Exception
    {
        SMInputFactory sf = getInputFactory();
        SMFilter f = SMFilterFactory.getElementOnlyFilter();

        SMFlatteningCursor c1 = sf.flatteningCursor(
                new ByteArrayInputStream(XML.getBytes("UTF-8")), f);
        assertEquals(3, _countStartElements(c1));

        SMFlatteningCursor c2 = sf.flatteningCursor(new StringReader(XML), f);
        assertEquals(3, _countStartElements(c2));

        File file = _tempXmlFile();
        SMFlatteningCursor c3 = sf.flatteningCursor(file, f);
        assertEquals(3, _countStartElements(c3));

        SMFlatteningCursor c4 = sf.flatteningCursor(file.toURI().toURL(), f);
        assertEquals(3, _countStartElements(c4));
    }

    /*
    /**********************************************************************
    /* Factory accessors
    /**********************************************************************
     */

    @Test
    public void testGetStaxFactory() throws Exception
    {
        SMInputFactory sf = getInputFactory();
        assertNotNull(sf.getStaxFactory());
    }

    @Test
    public void testGlobalFactories() throws Exception
    {
        assertNotNull(SMInputFactory.getGlobalXMLInputFactory());
        SMInputFactory global = SMInputFactory.getGlobalSMInputFactory();
        assertNotNull(global);
        // and it should be usable
        SMInputCursor rootc = global.rootElementCursor(new StringReader(XML)).advance();
        assertEquals("root", rootc.getLocalName());
    }

    /*
    /**********************************************************************
    /* Helpers
    /**********************************************************************
     */

    private int _countStartElements(SMInputCursor c) throws XMLStreamException
    {
        int n = 0;
        while (c.getNext() != null) {
            if (c.getCurrEventCode() == XMLStreamConstants.START_ELEMENT) {
                ++n;
            }
        }
        return n;
    }

    private XMLStreamReader _stax(String xml) throws XMLStreamException
    {
        return getStaxInputFactory().createXMLStreamReader(new StringReader(xml));
    }

    private File _tempXmlFile() throws IOException
    {
        File f = File.createTempFile("staxmate-test", ".xml");
        f.deleteOnExit();
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        w.write(XML);
        w.close();
        return f;
    }
}
