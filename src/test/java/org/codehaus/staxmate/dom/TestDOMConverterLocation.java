package org.codehaus.staxmate.dom;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.stream.*;
import static javax.xml.stream.XMLStreamConstants.*;

import org.w3c.dom.*;

import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


import org.codehaus.staxmate.StaxMateTestBase;

public class TestDOMConverterLocation
    extends StaxMateTestBase
{
    /**
     * Unit test that verifies that a proper DOM tree can be constructed
     * from Stax stream reader.
     */
    public void testattributes() throws java.io.IOException, XMLStreamException, XPathExpressionException, TransformerException, SAXException, ParserConfigurationException
    {
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        XMLInputFactory inputFactory=XMLInputFactory.newInstance();
        DOMConverter staxDomConverter = new DOMConverter();
        java.io.InputStream is = TestDOMConverterLocation.class.getClassLoader().getResource("org/codehaus/staxmate/dom/beantransform.xsl").openConnection().getInputStream();
        java.lang.String xmlAsString = readIntoString(is);
        is.close();
        org.w3c.dom.Document document = db.newDocument();
        StringReader sr = new StringReader(xmlAsString);
        StreamSource ss = new StreamSource(sr);
        XMLStreamReader stax = inputFactory.createXMLStreamReader(ss);
        staxDomConverter.buildDocument(stax, document);
        stax.close();
        sr.close();


        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(staxDomConverter.getSimpleNamespaceContext());
        org.w3c.dom.NodeList list = (NodeList) xpath.evaluate("//@name", document, XPathConstants.NODESET);
        org.junit.Assert.assertEquals(5, list.getLength());
        for (int i = 0; i < list.getLength(); ++i)
        {
            Tupel<Integer, Integer> tupel = staxDomConverter.getLocationForNode(xmlAsString, list.item(i));
            java.lang.String snip = xmlAsString.substring(tupel.getLefty(), tupel.getRighty());
            org.junit.Assert.assertEquals(list.item(i).toString(), snip);
        }
    }
    public void testcomment() throws java.io.IOException, XMLStreamException, XPathExpressionException, TransformerException, SAXException, ParserConfigurationException
    {
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        XMLInputFactory inputFactory=XMLInputFactory.newInstance();
        DOMConverter staxDomConverter = new DOMConverter();
        java.io.InputStream is = TestDOMConverterLocation.class.getClassLoader().getResource("org/codehaus/staxmate/dom/beantransform.xsl").openConnection().getInputStream();
        java.lang.String xmlAsString = readIntoString(is);
        is.close();
        org.w3c.dom.Document document = db.newDocument();
        StringReader sr = new StringReader(xmlAsString);
        StreamSource ss = new StreamSource(sr);
        XMLStreamReader stax = inputFactory.createXMLStreamReader(ss);
        staxDomConverter.buildDocument(stax, document);
        stax.close();
        sr.close();

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(staxDomConverter.getSimpleNamespaceContext());
        NodeList list = (NodeList) xpath.evaluate("//comment()", document, XPathConstants.NODESET);
        org.junit.Assert.assertEquals(7, list.getLength());
        for (int i = 0; i < list.getLength(); ++i)
        {
            Tupel<Integer, Integer> tupel = staxDomConverter.getLocationForNode(xmlAsString, list.item(i));
            java.lang.String snip = xmlAsString.substring(tupel.getLefty(), tupel.getRighty());
            org.junit.Assert.assertEquals("<!--" + list.item(i).getTextContent() + "-->", snip);
        }
    }

    @org.junit.Test
    public void testnoNameSpaceElement() throws java.io.IOException, XMLStreamException, XPathExpressionException, TransformerException, SAXException, ParserConfigurationException
    {
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        XMLInputFactory inputFactory=XMLInputFactory.newInstance();
        DOMConverter staxDomConverter = new DOMConverter();
        java.io.InputStream is = TestDOMConverterLocation.class.getClassLoader().getResource("org/codehaus/staxmate/dom/beantransform.xsl").openConnection().getInputStream();
        java.lang.String xmlAsString = readIntoString(is);
        is.close();
        org.w3c.dom.Document document = db.newDocument();
        StringReader sr = new StringReader(xmlAsString);
        StreamSource ss = new StreamSource(sr);
        XMLStreamReader stax = inputFactory.createXMLStreamReader(ss);
        staxDomConverter.buildDocument(stax, document);
        stax.close();
        sr.close();

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(staxDomConverter.getSimpleNamespaceContext());
        java.lang.String xPathExpression = "//object";
        NodeList list = (NodeList) xpath.evaluate(xPathExpression, document, XPathConstants.NODESET);
        java.lang.String nodeName = xPathExpression.substring(xPathExpression.lastIndexOf('/') + 1);
        org.junit.Assert.assertEquals(4,list.getLength());
        for (int i = 0; i < list.getLength(); ++i)
        {
            javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(list.item(i));
            java.io.StringWriter sw = new java.io.StringWriter();
            javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(sw);
            javax.xml.transform.TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            sw.flush();
            sw.close();
            java.lang.String serialized = sw.toString();
            Tupel<Integer, Integer> tupel = staxDomConverter.getLocationForNode(xmlAsString, list.item(i));
            java.lang.String snip = xmlAsString.substring(tupel.getLefty(), tupel.getRighty());
            java.lang.StringBuffer nsBuf = new java.lang.StringBuffer();
            NamespaceContext namespaceContext = staxDomConverter.getSimpleNamespaceContext();
            for (java.lang.String nameSpacePrefix : staxDomConverter.getNameSpacePrefixes())
            {
                if ((nameSpacePrefix != null) && (nameSpacePrefix.trim().length() > 0))
                {
                    if (nsBuf.length() > 0)
                        nsBuf.append(" ");
                    nsBuf.append("xmlns:");
                    nsBuf.append(nameSpacePrefix);
                    nsBuf.append("=\"");
                    nsBuf.append(namespaceContext.getNamespaceURI(nameSpacePrefix));
                    nsBuf.append("\"");
                }
            }
            snip = snip.replace("<" + nodeName, "<" + nodeName + " " + (nsBuf.toString()) + " ");
            org.w3c.dom.Document snipDom = db.parse(new java.io.ByteArrayInputStream(snip.getBytes()));
            org.w3c.dom.Document serializedDom = db.parse(new java.io.ByteArrayInputStream(serialized.getBytes()));
            Diff myDiff = DiffBuilder.compare(Input.fromDocument(snipDom)).withTest(Input.fromDocument(serializedDom))
                    .ignoreWhitespace()
                    .build();
            org.junit.Assert.assertFalse("XML similar " + myDiff.toString(), myDiff.hasDifferences());
        }
    }
    
    public void testnameSpaceElement() throws java.io.IOException, XMLStreamException, XPathExpressionException, TransformerException, SAXException, ParserConfigurationException
    {
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        XMLInputFactory inputFactory=XMLInputFactory.newInstance();
        DOMConverter staxDomConverter = new DOMConverter();
        java.io.InputStream is = TestDOMConverterLocation.class.getClassLoader().getResource("org/codehaus/staxmate/dom/beantransform.xsl").openConnection().getInputStream();
        java.lang.String xmlAsString = readIntoString(is);
        is.close();
        org.w3c.dom.Document document = db.newDocument();
        StringReader sr=new StringReader(xmlAsString);
        StreamSource ss=new StreamSource(sr);
        XMLStreamReader stax = inputFactory.createXMLStreamReader(ss);
        staxDomConverter.buildDocument(stax, document);
        stax.close();
        sr.close();

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(staxDomConverter.getSimpleNamespaceContext());
        java.lang.String xPathExpression="//xsl:attribute";
        NodeList list = (NodeList) xpath.evaluate(xPathExpression, document, XPathConstants.NODESET);
        java.lang.String nodeName=xPathExpression.substring(xPathExpression.lastIndexOf('/')+1);
        org.junit.Assert.assertEquals(5,list.getLength());
        for(int i=0;i<list.getLength();++i)
        {
            javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(list.item(i));
            java.io.StringWriter sw = new java.io.StringWriter();
            javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(sw);
            javax.xml.transform.TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            sw.flush();
            sw.close();
            java.lang.String serialized = sw.toString();
            Tupel<Integer, Integer> tupel = staxDomConverter.getLocationForNode(xmlAsString, list.item(i));
            java.lang.String snip = xmlAsString.substring(tupel.getLefty(), tupel.getRighty());
            java.lang.StringBuffer nsBuf=new java.lang.StringBuffer();
            NamespaceContext namespaceContext=staxDomConverter.getSimpleNamespaceContext();
            for(java.lang.String nameSpacePrefix:staxDomConverter.getNameSpacePrefixes())
            {
                if((nameSpacePrefix!=null)&&(nameSpacePrefix.trim().length()>0))
                {
                    if (nsBuf.length() > 0)
                        nsBuf.append(" ");
                    nsBuf.append("xmlns:");
                    nsBuf.append(nameSpacePrefix);
                    nsBuf.append("=\"");
                    nsBuf.append(namespaceContext.getNamespaceURI(nameSpacePrefix));
                    nsBuf.append("\"");
                }
            }
            snip=snip.replace("<"+nodeName,"<"+nodeName+" "+(nsBuf.toString())+" ");
            org.w3c.dom.Document snipDom = db.parse(new java.io.ByteArrayInputStream(snip.getBytes()));
            org.w3c.dom.Document serializedDom = db.parse(new java.io.ByteArrayInputStream(serialized.getBytes()));
            Diff myDiff = DiffBuilder.compare(Input.fromDocument(snipDom)).withTest(Input.fromDocument(serializedDom))
                    .ignoreWhitespace()
                    .build();
            org.junit.Assert.assertFalse("XML similar " + myDiff.toString(), myDiff.hasDifferences());

        }

    }
    

    private static String readIntoString(InputStream content) throws IOException
    {
        return readIntoString(content,java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String readIntoString(InputStream content, java.nio.charset.Charset charset) throws IOException
    {
        java.io.InputStreamReader isr=new java.io.InputStreamReader(content,charset);
        String rv=readIntoString(isr);
        content.close();
        return  rv;
    }

    private static String readIntoString(java.io.Reader isr) throws IOException
    {
        java.io.BufferedReader br=new java.io.BufferedReader(isr);
        StringBuffer assembler=new StringBuffer();
        String line=br.readLine();
        while(line!=null)
        {
            assembler.append(line);
            assembler.append("\n");
            line=br.readLine();
        }
        br.close();
        isr.close();
        return assembler.toString();
    }
}
