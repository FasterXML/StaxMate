package org.codehaus.staxmate;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.*;

import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.codehaus.stax2.XMLStreamWriter2;

import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;
import org.codehaus.staxmate.out.SMRootFragment;

/**
 * Tests for the various output-target entry points of {@link SMOutputFactory}
 * (Writer, OutputStream and File overloads for both documents and fragments,
 * the declaration-controlling overload, the {@code createStax2Writer} methods
 * and the global factory accessors).
 */
public class TestSMOutputFactory
    extends StaxMateTestBase
{
    private SMOutputFactory _outputFactory()
    {
        return new SMOutputFactory(XMLOutputFactory.newInstance());
    }

    /*
    /**********************************************************************
    /* Document construction over different targets
    /**********************************************************************
     */

    @Test
    public void testCreateOutputDocumentFromWriter() throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = _outputFactory().createOutputDocument(sw);
        _writeSimple(doc);
        _assertSimpleRoundTrip(sw.toString());
    }

    @Test
    public void testCreateOutputDocumentFromOutputStream() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SMOutputDocument doc = _outputFactory().createOutputDocument(out);
        _writeSimple(doc);
        _assertSimpleRoundTrip(out.toString("UTF-8"));
    }

    @Test
    public void testCreateOutputDocumentFromFile() throws Exception
    {
        File f = File.createTempFile("staxmate-out", ".xml");
        f.deleteOnExit();
        SMOutputDocument doc = _outputFactory().createOutputDocument(f);
        _writeSimple(doc);
        _assertSimpleRoundTrip(_readFile(f));
    }

    @Test
    public void testCreateOutputDocumentWithDeclaration() throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = getSimpleWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw, "1.0", "UTF-8", true);
        _writeSimple(doc);

        String xml = sw.toString();
        assertTrue(xml.startsWith("<?xml"), "expected XML declaration, got: "+xml);
        assertTrue(xml.indexOf("version=") > 0, "expected version in declaration: "+xml);
        // content should still round-trip
        _assertSimpleRoundTrip(xml);
    }

    /*
    /**********************************************************************
    /* Fragment construction over different targets
    /**********************************************************************
     */

    @Test
    public void testCreateOutputFragmentFromWriter() throws Exception
    {
        StringWriter sw = new StringWriter();
        SMRootFragment frag = _outputFactory().createOutputFragment(sw);
        frag.addElement("root").addCharacters("x");
        frag.closeRoot();

        String xml = sw.toString();
        assertFalse(xml.startsWith("<?xml"), "fragment should have no declaration: "+xml);
        assertTrue(xml.indexOf("<root>x</root>") >= 0, "unexpected fragment: "+xml);
    }

    @Test
    public void testCreateOutputFragmentFromOutputStreamAndFile() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SMRootFragment frag = _outputFactory().createOutputFragment(out);
        frag.addElement("root").addCharacters("x");
        frag.closeRoot();
        assertTrue(out.toString("UTF-8").indexOf("<root>x</root>") >= 0);

        File f = File.createTempFile("staxmate-frag", ".xml");
        f.deleteOnExit();
        SMRootFragment frag2 = _outputFactory().createOutputFragment(f);
        frag2.addElement("root").addCharacters("y");
        frag2.closeRoot();
        assertTrue(_readFile(f).indexOf("<root>y</root>") >= 0);
    }

    /*
    /**********************************************************************
    /* Low-level stream-writer construction
    /**********************************************************************
     */

    @Test
    public void testCreateStax2WriterOverloads() throws Exception
    {
        SMOutputFactory sf = _outputFactory();

        StringWriter sw = new StringWriter();
        XMLStreamWriter2 w1 = sf.createStax2Writer(sw);
        assertNotNull(w1);
        _writeMinimalDoc(w1);
        assertTrue(sw.toString().indexOf("<a") >= 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter2 w2 = sf.createStax2Writer(out);
        assertNotNull(w2);
        _writeMinimalDoc(w2);
        assertTrue(out.toString("UTF-8").indexOf("<a") >= 0);

        File f = File.createTempFile("staxmate-w", ".xml");
        f.deleteOnExit();
        XMLStreamWriter2 w3 = sf.createStax2Writer(f);
        assertNotNull(w3);
        _writeMinimalDoc(w3);
        assertTrue(_readFile(f).indexOf("<a") >= 0);
    }

    private void _writeMinimalDoc(XMLStreamWriter2 w) throws XMLStreamException
    {
        w.writeStartDocument();
        w.writeEmptyElement("a");
        w.writeEndDocument();
        w.close();
    }

    /*
    /**********************************************************************
    /* Factory accessors
    /**********************************************************************
     */

    @Test
    public void testGetStaxFactory() throws Exception
    {
        assertNotNull(_outputFactory().getStaxFactory());
    }

    @Test
    public void testGlobalFactories() throws Exception
    {
        assertNotNull(SMOutputFactory.getGlobalXMLOutputFactory());
        SMOutputFactory global = SMOutputFactory.getGlobalSMOutputFactory();
        assertNotNull(global);
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = global.createOutputDocument(sw);
        _writeSimple(doc);
        _assertSimpleRoundTrip(sw.toString());
    }

    /*
    /**********************************************************************
    /* Helpers
    /**********************************************************************
     */

    private void _writeSimple(SMOutputDocument doc) throws XMLStreamException
    {
        SMOutputElement root = doc.addElement("root");
        root.addElement("leaf").addCharacters("text");
        doc.closeRoot();
    }

    private void _assertSimpleRoundTrip(String xml) throws XMLStreamException
    {
        XMLStreamReader sr = getCoalescingReader(xml);
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "root");
        assertTokenType(START_ELEMENT, sr.next());
        assertElem(sr, null, "leaf");
        assertEquals("text", sr.getElementText());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }

    private String _readFile(File f) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        InputStream in = new FileInputStream(f);
        byte[] buf = new byte[1024];
        int count;
        while ((count = in.read(buf)) > 0) {
            bytes.write(buf, 0, count);
        }
        in.close();
        return bytes.toString("UTF-8");
    }
}
