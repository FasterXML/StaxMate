package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Output class that models an outputtable XML element.
 */
public class SMOutputElement
    extends SMOutputContainer
{
    // No output done, due to blocking (element or ancestor buffered and not-yet-released)
    protected final static int OUTPUT_NONE = 0;
    // Element name and prefix output, possibly some attributes
    protected final static int OUTPUT_ATTRS = 1;
    // Start element completely output:
    protected final static int OUTPUT_CHILDREN = 2;
    // End element output, ie. fully closed
    protected final static int OUTPUT_CLOSED = 3;

    /*
    /////////////////////////////////////////////
    // Element properties
    /////////////////////////////////////////////
    */

    /**
     * Local name of the element, name without preceding prefix or colon
     * (in namespace mode). In non-namespace mode fully-qualified name.
     */
    protected final String _localName;

    /**
     * Namespace of this element.
     *<p>
     * Note: can never be null -- event the default (empty) namespace
     * is presented by a global shared namespace instance.
     */
    protected final SMNamespace _namespace;

    /*
    /////////////////////////////////////////////
    // Output state information
    /////////////////////////////////////////////
    */

    protected int _outputState = OUTPUT_NONE;

    /**
     * Namespace that was bound as the default namespace in the context
     * where this element gets output. This is generally just stored here
     * to be able to write the end element matching start element,
     * since it's {@link SMOutputContext} that handles actual namespace
     * binding for output.
     * This is either the default declared
     * namespace of an ancestor element, or if none exists, the default
     * namespace of the root (either the empty namespace, or one found
     * via {@link javax.xml.namespace.NamespaceContext}.
     *<p>
     * Note: can never be null -- event the default (empty) namespace
     * is presented by a global shared namespace instance.
     */
    protected SMNamespace _parentDefaultNs;

    /**
     * Number of explicitly bound namespaces parent element has (or
     * for root elements 0). Stored for {@link SMOutputContext} during
     * time element is open; needed for closing namespace scopes
     * appropriately.
     */
    protected int _parentNsCount;

    /*
    ///////////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////////
     */

    protected SMOutputElement(SMOutputContext ctxt,
                              String localName, SMNamespace ns)
    {
        super(ctxt);
        _parent = null;
        _localName = localName;
        _namespace = ns;
    }

    public void linkParent(SMOutputContainer parent, boolean blocked)
        throws XMLStreamException
    {
        if (_parent != null) {
            _throwRelinking();
        }
        _parent = parent;
        if (!blocked) { // can output start element right away?
            doWriteStartElement();
        }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Simple public accessors
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to get the local name of this element
     */
    public String getLocalName() {
        return _localName;
    }
    
    /**
     * Method that can be used to get the namespace of the element.
     * Note that the returned value is never null; if no namespace
     * was passed during construction,
     * "no namespace" ({@link SMOutputContext#getEmptyNamespace()})
     * is used instead.
     *
     * @return Namespace of this element.
     */
    public SMNamespace getNamespace() {
        return _namespace;
    }

    /*
    ///////////////////////////////////////////////////////////
    // Additional (wrt SMOutputContainer) output methods
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method for adding an attribute to this element. For regular (non-buffered)
     * output elements, attribute is written right away; for buffered variants
     * output will be postponed until buffered element is completely released
     * (including its ancestors if they are unreleased buffered elements)
     */
    public void addAttribute(SMNamespace ns, String localName, String value)
        throws XMLStreamException
    {
        ns = _verifyNamespaceArg(ns);
        
        // Ok, what can we do, then?
        switch (_outputState) {
        case OUTPUT_NONE: // blocked
            _linkNewChild(_context.createAttribute(ns, localName, value));
            break;
        case OUTPUT_ATTRS: // perfect
            _context.writeAttribute(ns, localName, value);
            break;
        default:
            _throwClosedForAttrs();
        } 
    }

    /**
     * Convenience method for attributes that do not belong to a
     * namespace (no prefix)
     */
    public final void addAttribute(String localName, String value)
        throws XMLStreamException
    {
        addAttribute(null, localName, value);
    }

    /**
     * Typed Access write method to use for adding attribute with
     * boolean value.
     */
    public void addAttribute(SMNamespace ns, String localName, boolean value)
        throws XMLStreamException
    {
        addAttribute(ns, localName, value ? "true" : "false");
    }

    /**
     * Typed Access write method to use for adding attribute with
     * integer value.
     */
    public void addAttribute(SMNamespace ns, String localName, int value)
        throws XMLStreamException
    {
        addAttribute(ns, localName, String.valueOf(value));
    }

    /**
     * Typed Access write method to use for adding attribute with
     * long value.
     */
    public void addAttribute(SMNamespace ns, String localName, long value)
        throws XMLStreamException
    {
        addAttribute(ns, localName, String.valueOf(value));
    }

    /**
     * Method that can be (but never has to) called to force declaration
     * of given namespace for this element, if that is possible (i.e.
     * no binding has been added for the preferred prefix of given
     * namespace).
     * This is usually done as a minor optimization or
     * cosmetic improvement so that child elements need not declare
     * the namespace. Since namespace declarations are otherwise output
     * automatically when and as needed, this method never has to be called
     * (from correctness standpoint), but it may produce more aestethically
     * pleasing and compact output when properly used.
     *<p>
     * Default namespace can often not be pre-declared using this method,
     * because that would change namespace of the element itself if it
     * has no prefix, so method is most often called for namespaces with
     * explicit prefix.
     *<p>
     * Note: in cases where the given namespace can not be bound to preferred
     * URI, exact behavior is undefined: two possible outcomes are that no
     * namespace declaration is added, or that one with different prefix
     * (but given namespace URI) is added. No error is reported at any rate.
     *
     * @since 2.0.0
     */
    public void predeclareNamespace(SMNamespace ns)
        throws XMLStreamException
    {
        /* Let's validate argument; means that null is accepted as
         * "default namespace", actually
         */
        ns = _verifyNamespaceArg(ns);
        
        // Ok, what can we do, then?
        switch (_outputState) {
        case OUTPUT_NONE: // blocked
            _linkNewChild(_context.createNamespace(ns, _parentDefaultNs, _parentNsCount));

            break;
        case OUTPUT_ATTRS: // perfect
            _context.predeclareNamespace(ns, _parentDefaultNs, _parentNsCount);
            break;
        default:
            _throwClosedForNsDecls();
        } 
    }

    /*
    ///////////////////////////////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////////////////////////////
     */

    @Override
    protected void _childReleased(SMOutputtable child)
        throws XMLStreamException
    {
        // Ok; first of all, only first child matters:
        if (child == _firstChild) {
            switch (_outputState) {
            case OUTPUT_NONE:
                /* output blocked by parent (or lack of parent), can't output,
                 * nothing for parent to do either
                 */
                return;
            case OUTPUT_CLOSED: // error
                _throwClosed();
            case OUTPUT_ATTRS: // should never happen!
                throw new IllegalStateException("Internal error: illegal state (OUTPUT_ATTRS) on receiving 'childReleased' notification");
            }

            /* Ok, parent should know how to deal with it. In state
             * OUTPUT_START we will always have the parent defined.
             */
            /* It may seem wasteful to throw this all the way up the chain,
             * but it is necessary to do since children are not to handle
             * how preceding buffered siblings should be dealt with.
             */
            _parent._childReleased(this);
        }
    }
    
    @Override
    protected boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException
    {
        switch (_outputState) {
        case OUTPUT_NONE: // was blocked, need to output element
            doWriteStartElement();
            break;
        case OUTPUT_CLOSED:
            // If we are closed, let's report a problem
            _throwClosed();
        case OUTPUT_ATTRS: // can just "close" attribute writing scope
            _outputState = OUTPUT_CHILDREN;
        }

        // Any children? Need to try to close them too
        if (_firstChild != null) {
            if (canClose) {
                _closeAndOutputChildren();
            } else {
                _closeAllButLastChild();
            }
        }

        // Can we fully close this element?
        if (!canClose || _firstChild != null) {
            return false;
        }

        // Ok, can and should close for good:
        doWriteEndElement();
        return true;
    }
    
    @Override
    protected void _forceOutput(SMOutputContext ctxt)
        throws XMLStreamException
    {
        // Let's first ask nicely:
        if (_output(_context, true)) {
            ; // all done (including outputting end element)
        } else {
            // ... but if that doesn't work, let's negotiate bit more:
            _forceChildOutput();
            doWriteEndElement();
        }
    }
    
    @Override
    public boolean _canOutputNewChild()
        throws XMLStreamException
    {
        /* This is fairly simple; if we are blocked, can not output it right
         * away. Otherwise, if we have no children, can always output a new
         * one; if more than one, can't (first one is blocking, or
         * parent is blocking); if just one, need to try to close it first.
         */
        switch (_outputState) {
        case OUTPUT_NONE: // output blocked, no go:
            return false;
        case OUTPUT_CLOSED: // error
            _throwClosed();
        case OUTPUT_ATTRS: // can just "close" attribute writing scope
            _outputState = OUTPUT_CHILDREN;
            break;
        }

        if (_firstChild == null) { // no children -> ok
            return true;
        }
        return _closeAndOutputChildren();
    }

    @Override
    public void getPath(StringBuilder sb)
    {
        if (_parent != null) {
            _parent.getPath(sb);
        }
        sb.append('/');
        /* Figuring out namespace prefix is bit trickier, since it may
         * or may not have been bound yet. But we do know that the empty
         * Namespace can only bind to empty prefix; so we can only have
         * a prefix for non-empty namespace URI (but that doesn't yet
         * guarantee a prefix)
         */
        String uri = _namespace.getURI();
        if (uri != null && uri.length() > 0) {
            // Default ns?
            if (!_context.isDefaultNs(_namespace)) { // not the current one, no
                String prefix = _namespace.getBoundPrefix();
                if (prefix == null) { // not yet bound? (or masked default ns?)
                    prefix = "{unknown-prefix}";
                } else if (prefix.length() == 0) { // def. NS, no prefix
                    prefix = null;
                }
                if (prefix != null) {
                    sb.append(prefix);
                    sb.append(':');
                }
            }
        }
        sb.append(_localName);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */

    protected void doWriteStartElement()
        throws XMLStreamException
    {
        _outputState = OUTPUT_ATTRS;
        SMOutputContext ctxt = _context;
        _parentNsCount = ctxt.getNamespaceCount();
        _parentDefaultNs = ctxt.writeStartElement(_namespace, _localName);
    }

    protected void doWriteEndElement()
        throws XMLStreamException
    {
        _outputState = OUTPUT_CLOSED;
        _context.writeEndElement(_parentNsCount, _parentDefaultNs);
    }

    /**
     * Method for indicating illegal call to add attributes, when
     * the underlying stream state prevents addition.
     */
    protected void _throwClosedForAttrs()
    {
        String desc = (_outputState == OUTPUT_CLOSED) ?
            "ELEMENT-CLOSED" : "CHILDREN-ADDED";
        throw new IllegalStateException
            ("Can't add attributes for an element (path = '"
             +getPath()+"'), element state '"+desc+"'");
    }

    protected void _throwClosedForNsDecls()
    {
        String desc = (_outputState == OUTPUT_CLOSED) ?
            "ELEMENT-CLOSED" : "CHILDREN-ADDED";
        throw new IllegalStateException
            ("Can't add namespace declaration for an element (path = '"
             +getPath()+"'), element state '"+desc+"'");
    }
}
