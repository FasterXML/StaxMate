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

import java.util.regex.Pattern;
import javax.xml.namespace.NamespaceContext;

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

	private SimpleNamespaceContext simpleNamespaceContext;
	private java.util.Map<Node,Node> attributeElementMap;
	private java.util.Map<Node, Tupel<java.lang.Integer,java.lang.Integer> > nodeLocationMap;

	/**
	 * Map for holding thw mapping between URIs and prefixes 
	 * for resolving namespaces
	 */
	private java.util.Map<java.lang.String, java.lang.String> nsmap;

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
            db = dbf.newDocumentBuilder();
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

		nsmap=new java.util.HashMap();

        XMLStreamReader2 sr = Stax2ReaderAdapter.wrapIfNecessary(r);
        QNameRecycler recycler = new QNameRecycler();
        boolean nsAware = _isNamespaceAware(sr);
        Node current = doc; // At top level
		int position=0;
		nodeLocationMap=new java.util.HashMap();
		attributeElementMap=new java.util.HashMap();

		Node justClosed=null;
    main_loop:
        for (int evtType = sr.getEventType(); true; evtType = sr.next()) {
            Node child;

            switch (evtType) {
            case XMLStreamConstants.CDATA:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
                child = doc.createCDATASection(sr.getText());
				nodeLocationMap.put(child,new Tupel(sr.getLocation().getCharacterOffset(),sr.getLocation().getCharacterOffset()));
				position=sr.getLocation().getCharacterOffset();
				justClosed=child;
                break;

            case XMLStreamConstants.SPACE:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
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
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
                child = doc.createTextNode(sr.getText());
				nodeLocationMap.put(child,new Tupel(sr.getLocation().getCharacterOffset(),sr.getLocation().getCharacterOffset()));
				position=sr.getLocation().getCharacterOffset();
				justClosed=child;
                break;

            case XMLStreamConstants.COMMENT:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
                child = doc.createComment(sr.getText());
				nodeLocationMap.put(child,new Tupel(sr.getLocation().getCharacterOffset(),sr.getLocation().getCharacterOffset()));
				position=sr.getLocation().getCharacterOffset();
				justClosed=child;
                break;

            case XMLStreamConstants.END_DOCUMENT:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
                break main_loop;

            case XMLStreamConstants.END_ELEMENT:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
				justClosed=current;
				position=sr.getLocation().getCharacterOffset();
                current = current.getParentNode(); // lgtm [java/dereferenced-value-may-be-null]
                if (current == null || current == doc) {
                    // 19-Nov-2010, tatu: If the root element closed, we now need
                    //    to bail out UNLESS we are building "whole document"
                    //    (in which case still need to get possible PIs, comments)
                    if (!wholeDoc) {
                        break main_loop;
                    }
                }
                continue main_loop;

            case XMLStreamConstants.ENTITY_DECLARATION:
            case XMLStreamConstants.NOTATION_DECLARATION:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
                // Shouldn't really get these, but maybe some stream readers
                // do provide the info. If so, better ignore it -- DTD event
                // should have most/all we need.
                continue main_loop;

            case XMLStreamConstants.ENTITY_REFERENCE:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
                child = doc.createEntityReference(sr.getLocalName());
				nodeLocationMap.put(child,new Tupel(position,sr.getLocation().getCharacterOffset()));
				position=sr.getLocation().getCharacterOffset();
                break;

            case XMLStreamConstants.PROCESSING_INSTRUCTION:
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
                child = doc.createProcessingInstruction(sr.getPITarget(), sr.getPIData());
				nodeLocationMap.put(child,new Tupel(sr.getLocation().getCharacterOffset(),sr.getLocation().getCharacterOffset()));
				position=sr.getLocation().getCharacterOffset();
				justClosed=child;
                break;

            case XMLStreamConstants.START_ELEMENT:
                // Ok, need to add a new element...
                {
					if(justClosed!=null)
					{
						nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
						justClosed=null;
					}
                    String ln = sr.getLocalName();
                    Element newElem;

                    if (nsAware) {
                        String qname = sr.getPrefixedName();
                        newElem = doc.createElementNS(sr.getNamespaceURI(), qname);
						if((sr.getNamespaceURI()!=null)&&(nsmap.containsKey(sr.getPrefix())==false))
							nsmap.put(sr.getPrefix(),sr.getNamespaceURI());
                    } else { // if non-ns-aware, things are simpler:
                        newElem = doc.createElement(ln);
                    }

                    // Silly old DOM: must mix in namespace declarations in there...
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
 							Attr attr=doc.createAttributeNS(sr.getAttributeNamespace(i), ln);
							attr.setValue(sr.getAttributeValue(i));
							newElem.setAttributeNode(attr);
							attributeElementMap.put(attr,newElem);
                        } else {
							Attr attr=doc.createAttribute(ln);
							attr.setValue(sr.getAttributeValue(i));
							newElem.setAttributeNode(attr);
							attributeElementMap.put(attr,newElem);
                        }
                    }
                    // And then 'push' new element...
                    current.appendChild(newElem); // lgtm [java/dereferenced-value-may-be-null]
                    current = newElem;
					nodeLocationMap.put(newElem,new Tupel(sr.getLocation().getCharacterOffset(),sr.getLocation().getCharacterOffset()));
					position=sr.getLocation().getCharacterOffset()+1;
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
				if(justClosed!=null)
				{
					nodeLocationMap.put(justClosed,new Tupel(nodeLocationMap.get(justClosed).getLefty(),sr.getLocation().getCharacterOffset()));
					justClosed=null;
				}
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
                current.appendChild(child); // lgtm [java/dereferenced-value-may-be-null]
            }
        }
   		simpleNamespaceContext=new SimpleNamespaceContext(nsmap);
    }

    /*
    /**********************************************************************
    /* Public API, output (DOM written using stax stream writer)
    /**********************************************************************
     */

    /**
     * Method for getting all namespace prefixes in document
     */
	public java.util.Set<java.lang.String> getNameSpacePrefixes()
	{
		return nsmap.keySet();
	}
	public Tupel<Integer, Integer> getLocationForNode(java.lang.String xmlAsString,org.w3c.dom.Node node)
	{
		Tupel<Integer, Integer> rv=null;
		Tupel<Integer, Integer> tupel = nodeLocationMap.containsKey(node)?nodeLocationMap.get(node):nodeLocationMap.get(attributeElementMap.get(node));
		if(tupel!=null)
		{
			if(Attr.class.isAssignableFrom(node.getClass()))
			{
				String element=xmlAsString.substring(tupel.getLefty() - 1,tupel.getRighty());
				//https://blog.stevenlevithan.com/archives/match-quoted-string
				java.lang.String patStrng="((.*?)"+node.getNodeName()+"=([\"']).*?\\3).*";
				//https://stackoverflow.com/questions/22793989/regex-matching-quoted-string-but-ignoring-escaped-quotation-mark
				//						java.lang.String patStrng="((.*?)"+node.getNodeName()+"=\"(?:[^\\\\\"]+|\\\\.|\\\\\\\\)*\").*";
				java.util.regex.Pattern pat=java.util.regex.Pattern.compile(patStrng, Pattern.DOTALL|Pattern.MULTILINE);
				java.util.regex.Matcher matcher=pat.matcher(element);
				if(matcher.matches())
				{
					int left=tupel.getLefty() +matcher.group(2).length()-1;
					int right=tupel.getLefty() +matcher.group(1).length()-1;
					rv=new Tupel<Integer, Integer>(left,right);
				}
			}
			else
			{
				rv=new Tupel<Integer, Integer>(tupel.getLefty() ,tupel.getRighty());
			}
		}
		return rv;
	}

	public NamespaceContext getSimpleNamespaceContext()
	{
		return simpleNamespaceContext;
	}

	private String getPrefixedName(XMLStreamReader sr)
	{
		switch (sr.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
			case XMLStreamConstants.END_ELEMENT:
			{
				String prefix = sr.getPrefix();
				String ln = sr.getLocalName();

				if (prefix == null || prefix.length() == 0) {
					return ln;
				}
				StringBuffer sb = new StringBuffer(ln.length() + 1 + prefix.length());
				sb.append(prefix);
				sb.append(':');
				sb.append(ln);
				return sb.toString();
			}
			case XMLStreamConstants.ENTITY_REFERENCE:
				return sr.getLocalName();
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				return sr.getPITarget();
			case XMLStreamConstants.DTD:
				return getDTDRootName();

		}
		throw new IllegalStateException("Current state not START_ELEMENT, END_ELEMENT, ENTITY_REFERENCE, PROCESSING_INSTRUCTION or DTD");
	}

	public String getDTDRootName() {
		return null;
	}

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

