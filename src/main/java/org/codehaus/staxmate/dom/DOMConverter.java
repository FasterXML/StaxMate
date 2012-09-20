package org.codehaus.staxmate.dom;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.Stax2ReaderAdapter;
import org.codehaus.stax2.ri.Stax2WriterAdapter;

/**
 * Class that can build DOM trees and fragments using
 * Stax stream readers, and write them out using
 * Stax stream writers.
 */
public class DOMConverter
{
    /*
    /**********************************************************************
    /* Input configuration
    /**********************************************************************
     */

    protected final DocumentBuilder _docBuilder;

    /**
     * Whether ignorable white space should be ignored, ie not added
     * in the resulting JDOM tree. If true, it will be ignored; if false,
     * it will be added in the tree. Default value if false.
     */
    protected boolean _inputCfgIgnoreWs = false;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public DOMConverter()
    {
        this(_constructBuilder());
    }

    public DOMConverter(DocumentBuilder b)
    {
        _docBuilder = b;
    }

    private final static DocumentBuilder _constructBuilder()
    {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException pe) {
            // should seldom (~= never) occur, so:
            throw new IllegalStateException(pe);
        }
    }


    /*
    /**********************************************************************
    /* Public API, configuration
    /**********************************************************************
     */

    /**
     * Method used to change whether the build methods will add ignorable
     * (element) white space in the DOM tree or not.
     *<p>
     * Whether all-whitespace text segment is ignorable white space or
     * not is based on DTD read in, as per XML specifications (white space
     * is only significant in mixed content or pure text elements).
     */
    public void setIgnoreWhitespace(boolean state) {
        _inputCfgIgnoreWs = state;
    }

    /*
    /**********************************************************************
    /* Public API, input (DOM from stax stream reader)
    /**********************************************************************
     */

    /**
     * This method will create a {@link org.w3c.dom.Document} instance using
     * the default JAXP DOM document construction mechanism and
     * populated using the given StAX stream reader.
     * Namespace-awareness will be enabled for the
     * {@link DocumentBuilderFactory} constructed; if this is not wanted,
     * caller should construct DocumentBuilder separately.
     *<p>
     * Note: underlying stream reader will not be closed by calling this
     * method.
     *
     * @param r Stream reader from which input is read.
     * @return <code>Document</code> - DOM document object.
     * @throws XMLStreamException If the reader threw such exception (to
     *   indicate a parsing or I/O problem)
     */
    public Document buildDocument(XMLStreamReader r)
        throws XMLStreamException
    {
        // Let's enable namespace awareness by default
        DocumentBuilder db;
        try{
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new XMLStreamException(pce);
        }
        return buildDocument(r, db);
    }

    /**
     * This method will create a {@link org.w3c.dom.Document} instance using
     * given DocumentBuilder and
     * populated using the given StAX stream reader.
     *<p>
     * Note: underlying stream reader will not be closed by calling this
     * method.
     *
     * @param r Stream reader from which input is read.
     * @return <code>Document</code> - DOM document object.
     * @throws XMLStreamException If the reader threw such exception (to
     *   indicate a parsing or I/O problem)
     */
    public Document buildDocument(XMLStreamReader r, DocumentBuilder docbuilder)
        throws XMLStreamException
    {
        Document doc = docbuilder.newDocument();
        buildDocument(r, doc);
        return doc;
    }

    /**
     * This method will populate given {@link org.w3c.dom.Document} using
     * the given StAX stream reader instance.
     *<p>
     * This method takes a <code>XMLStreamReader</code> and builds up
     * a DOM tree under given document object.
     *<p>
     * Implementation note: recursion has been eliminated by using nodes'
     * parent/child relationship; this improves performance somewhat
     * (classic recursion-by-iteration-and-explicit stack transformation)
     *<p>
     * Note: underlying stream reader will not be closed by calling this
     * method.
     *
     * @param r Stream reader from which input is read.
     * @param doc <code>Document</code> being built.
     * @throws XMLStreamException If the reader threw such exception (to
     *   indicate a parsing or I/O problem)
     */
    public void buildDocument(XMLStreamReader r, Document doc)
        throws XMLStreamException
    {
        /* One important distinction; to build "whole" document (including
         * PIs and comments outside of root node), we must start with
         * START_DOCUMENT event.
         */
        boolean wholeDoc = (r.getEventType() == XMLStreamConstants.START_DOCUMENT);

        XMLStreamReader2 sr = Stax2ReaderAdapter.wrapIfNecessary(r);
        QNameRecycler recycler = new QNameRecycler();
        boolean nsAware = _isNamespaceAware(sr);
        Node current = doc; // At top level

    main_loop:
        for (int evtType = sr.getEventType(); true; evtType = sr.next()) {
            Node child;

            switch (evtType) {
            case XMLStreamConstants.CDATA:
                child = doc.createCDATASection(sr.getText());
                break;

            case XMLStreamConstants.SPACE:
                if (_inputCfgIgnoreWs) {
                    continue main_loop;
                }
                /* Oh great. DOM is brain-dead in that ignorable white space
                 * can not be added, even though it is legal, and often
                 * reported by StAX/SAX impls...
                 */
                if (current == doc) { // better just ignore, thus...
                    continue;
                }
                // fall through

            case XMLStreamConstants.CHARACTERS:
                child = doc.createTextNode(sr.getText());
                break;

            case XMLStreamConstants.COMMENT:
                child = doc.createComment(sr.getText());
                break;

            case XMLStreamConstants.END_DOCUMENT:
                break main_loop;

            case XMLStreamConstants.END_ELEMENT:
                current = current.getParentNode();
                if (current == null || current == doc) {
                    /* 19-Nov-2010, tatu: If the root element closed, we now need
                     *    to bail out UNLESS we are building "whole document"
                     *    (in which case still need to get possible PIs, comments)
                     */
                    if (!wholeDoc) {
                        break main_loop;
                    }
                }
                continue main_loop;

            case XMLStreamConstants.ENTITY_DECLARATION:
            case XMLStreamConstants.NOTATION_DECLARATION:
                /* Shouldn't really get these, but maybe some stream readers
                 * do provide the info. If so, better ignore it -- DTD event
                 * should have most/all we need.
                 */
                continue main_loop;

            case XMLStreamConstants.ENTITY_REFERENCE:
                child = doc.createEntityReference(sr.getLocalName());
                break;

            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                child = doc.createProcessingInstruction(sr.getPITarget(), sr.getPIData());
                break;

            case XMLStreamConstants.START_ELEMENT:
                // Ok, need to add a new element...
                {
                    String ln = sr.getLocalName();
                    Element newElem;

                    if (nsAware) {
                        String qname = sr.getPrefixedName();
                        newElem = doc.createElementNS(sr.getNamespaceURI(), qname);
                    } else { // if non-ns-aware, things are simpler:
                        newElem = doc.createElement(ln);
                    }

                    /* Silly old DOM: must mix in namespace declarations
                     * in there...
                     */
                    for (int i = 0, len = sr.getNamespaceCount(); i < len; ++i) {
                        String prefix = sr.getNamespacePrefix(i);
                        String qname;
                        if (prefix == null || prefix.length() == 0) {
                            qname = "xmlns";
                        } else {
                            qname = recycler.getQualified("xmlns", prefix);
                        }
                        newElem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, qname,  sr.getNamespaceURI(i));
                    }

                    // And then the attributes:
                    for (int i = 0, len = sr.getAttributeCount(); i < len; ++i) {
                        ln = sr.getAttributeLocalName(i);
                        if (nsAware) {
                            String prefix = sr.getAttributePrefix(i);
                            if (prefix != null && prefix.length() > 0) {
                                ln = recycler.getQualified(prefix, ln);
                            }
                            newElem.setAttributeNS(sr.getAttributeNamespace(i), ln, sr.getAttributeValue(i));
                        } else {
                            newElem.setAttribute(ln, sr.getAttributeValue(i));
                        }
                    }
                    // And then 'push' new element...
                    current.appendChild(newElem);
                    current = newElem;
                    continue main_loop;
                }

            case XMLStreamConstants.START_DOCUMENT:
                /* This should only be received at the beginning of document...
                 * so, should we indicate the problem or not?
                 */
                /* For now, let it pass: maybe some (broken) readers pass
                 * that info as first event in beginning of doc?
                 */
                continue main_loop;

            case XMLStreamConstants.DTD:
                /* !!! Note: StAX does not expose enough information about
                 *  doctype declaration (specifically, public and system id!);
                 *  (altough StAX2 would...)
                 *
                 * Worse, DOM1/2 do not specify a way to create the DocType
                 * node, even if StAX provided it. This is pretty silly,
                 * all in all.
                 */
                continue main_loop;

            // Should never get these, from a stream reader:
                
            /* (commented out entries are just FYI; default catches
             * them all)
             */

            //case XMLStreamConstants.ATTRIBUTE:
            //case XMLStreamConstants.NAMESPACE:
            default:
                throw new XMLStreamException("Unrecognized iterator event type: "+sr.getEventType()+"; should not receive such types (broken stream reader?)");
            }

            if (child != null) {
                current.appendChild(child);
            }
        }
    }

    /*
    /**********************************************************************
    /* Public API, output (DOM written using stax stream writer)
    /**********************************************************************
     */

    /**
     * Method for writing out given DOM document using specified
     * stream writer.
     *<p>
     * Note: only regular <code>XMLStreamWriter.close()</code> is
     * called on the stream writer. This usually means that the underlying
     * stream is not closed (as per Stax 1.0 specification).
     */
    public void writeDocument(Document doc, XMLStreamWriter sw0)
        throws XMLStreamException
    {
        XMLStreamWriter2 sw = Stax2WriterAdapter.wrapIfNecessary(sw0);

        sw.writeStartDocument();
        for (Node child = doc.getFirstChild(); child != null; child = child.getNextSibling()) {
            _writeNode(sw, child);
        }
        sw.writeEndDocument();
        sw.close();
    }

    public void writeFragment(NodeList nodes, XMLStreamWriter sw0)
        throws XMLStreamException
    {
        XMLStreamWriter2 sw = Stax2WriterAdapter.wrapIfNecessary(sw0);
        for (int i = 0, len = nodes.getLength(); i < len; ++i) {
            _writeNode(sw, (Node) nodes.item(i));
        }
    }

    public void writeFragment(Node node, XMLStreamWriter sw0)
        throws XMLStreamException
    {
        XMLStreamWriter2 sw = Stax2WriterAdapter.wrapIfNecessary(sw0);
        _writeNode(sw, node);
    }

    /*
    /**********************************************************************
    /* Helper methods, property detection
    /**********************************************************************
     */

    protected static boolean _isNamespaceAware(XMLStreamReader r)
        throws XMLStreamException
    {
        Object o = r.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE);
        /* StAX defaults to namespace aware, so let's use similar
         * logics (although all compliant implementations really should
         * return a valid value)
         */
        if ((o instanceof Boolean) && !((Boolean) o).booleanValue()) {
            return false;
        }
        return true;
    }

    /* Not used, 06-Mar-2009, tatu
    protected static boolean _isRepairing(XMLStreamWriter sw)
        throws XMLStreamException
    {
        Object o = sw.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
        return (o instanceof Boolean) && ((Boolean) o).booleanValue();
    }
    */

    /*
    /**********************************************************************
    /* Helper methods, output
    /**********************************************************************
     */

    protected void _writeNode(XMLStreamWriter2 sw, Node node)
        throws XMLStreamException
    {
        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            _writeElement(sw, (Element) node);
            break;
        case Node.TEXT_NODE:
            // Do we care about whether it's actually CDATA?
            sw.writeCharacters(node.getNodeValue());
            break;
        case Node.CDATA_SECTION_NODE:
            sw.writeCData(node.getNodeValue());
            break;
        case Node.COMMENT_NODE:
            sw.writeComment(node.getNodeValue());
            break;
        case Node.ENTITY_REFERENCE_NODE:
            sw.writeEntityRef(node.getNodeName());
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            String target = node.getNodeName();
            String data = node.getNodeValue();
            if (data == null || data.length() == 0) {
                sw.writeProcessingInstruction(target);
            } else {
                sw.writeProcessingInstruction(target, data);
            }
            break;
        case Node.DOCUMENT_TYPE_NODE:
            sw.writeDTD(_buildDTD((DocumentType) node));
            break;
        default:
            throw new XMLStreamException("Unrecognized or unexpected node class: "+node.getClass().getName());
        }
    }

    protected String _buildDTD(DocumentType doctype)
    {
        /* For StAX 1.0, need to construct it: for StAX2 we could
         * pass these as they are...
         */
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE ");
        // root elem should never be null
        sb.append(doctype.getName());
        String pubId = doctype.getPublicId();
        String sysId = doctype.getSystemId();
        if (pubId == null || pubId.length() == 0) { // no public id?
            if (sysId != null && sysId.length() > 0) { // but have sys id
                sb.append("SYSTEM \"");
                sb.append(sysId);
                sb.append('"');
            }
        } else {
            sb.append("PUBLIC \"");
            sb.append(pubId);
            sb.append("\" \"");
            // System id can not be null, if so
            sb.append(sysId);
            sb.append('"');
        }
        String intSubset = doctype.getInternalSubset();
        if (intSubset != null && intSubset.length() > 0) {
            sb.append(" [");
            sb.append(intSubset);
            sb.append(']');
        }
        sb.append('>');
        return sb.toString();
    }


    /**
     * Method called to output an element node and all of its children
     * (recursively).
     *
     * @param elem Element to output
     */
    protected void _writeElement(XMLStreamWriter2 sw, Element elem)
        throws XMLStreamException
    {
        String elemPrefix = elem.getPrefix();
        if (elemPrefix == null) {
            elemPrefix = "";
        }
        String elemUri = elem.getNamespaceURI();
        if (elemUri == null) {
            elemUri = "";
        }
        String ln = elem.getLocalName();
        // as per [STAXMATE-41], localName not always available...
        if (ln == null) {
            ln = elem.getNodeName();
        }

        sw.writeStartElement(elemPrefix, ln, elemUri);

        /* And in any case, may have attributes; list also contains
         * namespace declarations (stupid DOM)
         */
        NamedNodeMap attrs = elem.getAttributes();
        for (int i = 0, len = attrs.getLength(); i < len; ++i) {
            Attr attr = (Attr) attrs.item(i);
            String aPrefix = attr.getPrefix();
            ln = attr.getLocalName();
            // as per [STAXMATE-41], localName not always available...
            if (ln == null) {
                ln = attr.getName();
            }
            
            String value = attr.getValue();

            /* With attributes things are bit simpler: they will never use
             * the default namespace, so if prefix is empty, they will bound
             * to the empty namespace.
             */
            if (aPrefix == null || aPrefix.length() == 0) { // no NS
                if ("xmlns".equals(ln)) {
                    sw.writeDefaultNamespace(value);
                } else {
                    sw.writeAttribute(ln, value);
                }
            } else {
                // Ok: is it a namespace declaration?
                if ("xmlns".equals(aPrefix)) {
                    sw.writeNamespace(ln, value);
                } else {
                    sw.writeAttribute(aPrefix, attr.getNamespaceURI(), ln, value);
                }
            }
        }

        // And then children, recursively:
        for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
            _writeNode(sw, child);
        }

        sw.writeEndElement();
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * We can do simple reuse of commonly seen names
     */
    final static class QNameRecycler
    {
        String _lastPrefix = null;
        String _lastLocalName = null;
        String _lastQName = null;

        public QNameRecycler() { }

        public String getQualified(String prefix, String localName)
        {
            /* This mostly/only helps with empty/text-only elements...
             * might make sense to do 'real' caching...
             */
            if (localName == _lastLocalName && prefix == _lastPrefix) {
                return _lastQName;
            }
            _lastLocalName = localName;
            _lastPrefix = prefix;
            StringBuilder sb = new StringBuilder(1 + prefix.length() + localName.length());
            sb.append(prefix).append(':').append(localName);
            _lastQName = sb.toString();
            return _lastQName;
        }
    }
}

