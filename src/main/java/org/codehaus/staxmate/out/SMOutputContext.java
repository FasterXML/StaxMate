package org.codehaus.staxmate.out;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Class that encapsulates details about context in which StaxMate output
 * is done. The most important of the details is the stream writer to use
 * (since that is eventually invoked to do the real output), and its
 * properties.
 *<p>
 * Usually the process of outputting XML content with StaxMate starts by
 * instantiating an {@link SMOutputContext}. It can then be used to create
 * output fragments; all of which bind to that context. Context is thus
 * what "connects" various fragments when they are buffered (when there
 * may or may not be child/parent relationships yet defined).
 *<p>
 * Context is also used (in addition to storing output relevant settings and
 * acting as a fragment factory)
 * as the owner of various other objects, most notable namespaces. All
 * local namespaces are owned by one and only one context.
 */
public final class SMOutputContext
{
    /*
    //////////////////////////////////////////////////////
    // Constants
    //////////////////////////////////////////////////////
    */

    /* Any documents really use more than 16 explicit namespaces?
     * Probably not; those that do can expand the stack as needed
     */
    final static int DEF_NS_STACK_SIZE = 16;

    protected final static SMNamespace NS_EMPTY =
        new SMGlobalNamespace("", XMLConstants.DEFAULT_NS_PREFIX);
    protected final static SMNamespace NS_XML =
        new SMGlobalNamespace(XMLConstants.XML_NS_PREFIX,
                              XMLConstants.XML_NS_URI);
    protected final static SMNamespace NS_XMLNS =
        new SMGlobalNamespace(XMLConstants.XMLNS_ATTRIBUTE,
                              XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
    
    final static HashMap<String,SMNamespace> sGlobalNsMap = new HashMap<String, SMNamespace>();
    static {
        sGlobalNsMap.put(NS_EMPTY.getURI(), NS_EMPTY);
        sGlobalNsMap.put(NS_XML.getURI(), NS_XML);
        sGlobalNsMap.put(NS_XMLNS.getURI(), NS_XMLNS);
    }

    // // // We can use canonical values for some types...

    final static SMOTypedValue FALSE_VALUE = SMOTypedValue.create(false);
    final static SMOTypedValue TRUE_VALUE = SMOTypedValue.create(true);

    /*
    //////////////////////////////////////////////////////
    // Configuration settings
    //////////////////////////////////////////////////////
    */

    final XMLStreamWriter2 _streamWriter;
    final NamespaceContext _rootNsContext;
    final boolean _cfgRepairing;

    /**
     * Prefix to use for creating automatic namespace prefixes. For example,
     * setting this to "ns" would result in automatic prefixes of form
     * "ns1", "ns2" and so on.
     */
    String _nsPrefixPrefix = "ns";

    int _nsPrefixSeqNr = 1;

    /**
     * Configuration flag that specifies whether by default namespaces
     * should bind as the default namespaces for elements or not. If true,
     * all unbound namespaces are always bound as the default namespace,
     * when elements are output: if false, more complicated logics is used
     * (which considers preferred prefixes, past bindings etc).
     */
    boolean _cfgPreferDefaultNs = false;

    /*
    //////////////////////////////////////////////////////
    // State
    //////////////////////////////////////////////////////
    */

    /**
     * Map that contains all local namespaces, that is, namespaces
     * that have been created for use with documents output using
     * this context.
     */
    HashMap<String, SMNamespace> _localNsMap = null;
    
    /**
     * Currently active default namespace; one that is in effect within
     * current scope (inside currently open element, if any; if none,
     * within root level).
     */
    SMNamespace _defaultNS = NS_EMPTY;

    /**
     * Stack of bound non-default namespaces.
     */
    SMNamespace[] _nsStack = null;

    /**
     * Number of bound namespaces in {@link _nsStack}
     */
    int _boundNsCount = 0;
    
    /**
     * Namespace of the last START_ELEMENT output.
     */
    SMNamespace _currElemNS;

    /*
    //////////////////////////////////////////////////////
    // Indentation settings, state
    //////////////////////////////////////////////////////
    */

    /**
     * This String is null when not doing (heuristic) indentation. Otherwise
     * it defines the longest possible indentation String to use; subset
     * by the offset indexes as necessary.
     */
    String _indentString = null;

    /**
     * Current offset within indentation String, if indenting. Basically
     * offset of the first character after end of indentation String.
     */
    int _indentOffset = 0;

    /**
     * Number of characters to add to <code>_indentOffset</code> when
     * adding a new indentation level (and conversely, subtract when
     * closing such level).
     */
    int _indentStep = 0;

    /**
     * Counter used to suppress indentation, for levels where text
     * has been output (indicating either pure-text or mixed content).
     * Set to -1 when indentation is disabled.
     * This remains 0 when no explicit text output has been done,
     * and is set to 1 from such a state. After becoming non-zero,
     * it will be incremented by one for each new level (start
     * element output), and subtracted by one for close elements.
     *<p>
     * Since this needs to be 0 for any indentation to be output,
     * it is also used as a 'flag' to see if indentation is enabled.
     */
    int _indentSuppress = -1;

    /**
     * This flag is used to prevent indentation from being added
     * for empty leaf elements, which should either be output
     * as empty elements, or start/end tag pair, with no intervening
     * spaces.
     */
    boolean _indentLevelEmpty = true;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle; construction, configuration
    //////////////////////////////////////////////////////
    */

    protected SMOutputContext(XMLStreamWriter2 sw, NamespaceContext rootNsCtxt)
    {
        _streamWriter = sw;
        _rootNsContext = rootNsCtxt;
        Object o = sw.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
        _cfgRepairing = (o instanceof Boolean) && ((Boolean) o).booleanValue();
    }

    /**
     * This method can be called to enable or disable heuristic indentation
     * for the output done using this output context.
     *<p>
     * Here are some example calls:
     *<blockquote>
     * context.setIndentation("\n        ", 1, 2); // indent by lf and 2 spaces per level
     * context.setIndentation(null, 0, 0); // disable indentation
     * context.setIndentation("\r\n\t\t\t\t\t\t\t\t", 2, 1); // indent by windows lf and 1 tab per level
     *</blockquote>
     *
     * @param indentStr String to use for indentation; if non-null, will
     *   enable indentation, if null, will disable it. Used in conjunction
     *   with the other arguments
     * @param startOffset Initial character offset for the first level of
     *   indentation (current context; usually root context): basically,
     *   number of leading characters from <code>indentStr</code> to
     *   output.
     * @param step Number of characters to add from the indentation
     *   String for each new level (and to subtract when closing levels).
     */
    public void setIndentation(String indentStr, int startOffset, int step)
    {
        _indentString = indentStr;
        _indentOffset = startOffset;
        _indentStep = step;

        // Important: need to set counter to 0, starts with -1
        _indentSuppress = 0;
    }
    
    /*
    //////////////////////////////////////////////////////
    // Factory methods, context creation
    //////////////////////////////////////////////////////
    */

    public static SMOutputContext createInstance(XMLStreamWriter2 sw, NamespaceContext rootNsCtxt)
        throws XMLStreamException
    {
        return new SMOutputContext(sw, rootNsCtxt);
    }

    public static SMOutputContext createInstance(XMLStreamWriter2 sw)
        throws XMLStreamException
    {
        return createInstance(sw, sw.getNamespaceContext());
    }

    /*
    //////////////////////////////////////////////////////
    // Factory methods, full document writer creation
    //
    // These methods are used when creating full stand-alone
    // documents using StaxMate
    //////////////////////////////////////////////////////
    */

    /**
     * Method used to create a StaxMate output fragment that corresponds
     * to a single well-formed XML document. Assumption, then, is that
     * the underlying stream writer has only been created, but no writes
     * have yet been done.
     *<p>
     * This version of the method calls the matching no-arguments method
     * in the stream writer.
     */
    public SMOutputDocument createDocument()
        throws XMLStreamException
    {
        return new SMOutputDocument(this);
    }

    /**
     * Method used to create a StaxMate output fragment that corresponds
     * to a single well-formed XML document. Assumption, then, is that
     * the underlying stream writer has only been created, but no writes
     * have yet been done.
     *<p>
     * This version of the method calls the matching stream writer method
     * which takes full xml declaration information.
     */
    public SMOutputDocument createDocument(String version, String encoding)
        throws XMLStreamException
    {
        return new SMOutputDocument(this, version, encoding);
    }

    public SMOutputDocument createDocument(String version, String encoding,
                                           boolean standalone)
        throws XMLStreamException
    {
        return new SMOutputDocument(this, version, encoding, standalone);
    }
    
    /*
    //////////////////////////////////////////////////////
    // Factory methods, fragment creation
    //
    // These methods are used when only sub-trees are created
    // using StaxMate (although they can also be used to 
    // create buffered fragments; but usually that is simpler
    // to do via fragment object's factory methods)
    //////////////////////////////////////////////////////
    */

    /**
     * Method to use when outputting an XML sub-tree, in which case the
     * underlying stream writer may be (or has been) used for outputting
     * XML content in addition to content that is output using StaxMate.
     * Resulting fragment is not buffered, and will thus be fully
     * streamed (except for buffering caused by adding buffered children)
     */
    public SMRootFragment createRootFragment()
        throws XMLStreamException
    {
        return new SMRootFragment(this);
    }

    public SMBufferedFragment createBufferedFragment()
        throws XMLStreamException
    {
        return new SMBufferedFragment(this);
    }

    /*
    //////////////////////////////////////////////////////
    // Factory methods, simple node creation
    //////////////////////////////////////////////////////
    */

    public SMOutputtable createAttribute(SMNamespace ns, String localName, String value) {
        return new SMOAttribute(ns, localName, value);
    }

    /**
     * Method called by {@link SMOutputElement} to add buffered namespace
     * pre-declaration.
     */
    public SMOutputtable createNamespace(SMNamespace ns, SMNamespace parentDefaultNS, int parentNsCount) {
        return new SMONamespace(ns, parentDefaultNS, parentNsCount);
    }

    public SMOutputtable createCharacters(String text) {
        return SMOCharacters.create(text);
    }

    public SMOutputtable createCharacters(char[] buf, int offset, int len) {
        return SMOCharacters.createShared(buf, offset, len);
    }

    /**
     * Specialized alternative to {link #createCharacters(char[],int,int)}
     * that can count on the passed char array NOT being shared. This means
     * that no intermediate copy needs to be done -- instance can just use
     * the passed in reference knowing it will not be messed by other threads.
     */
    public SMOutputtable createNonSharedCharacters(char[] buf, int offset, int len) {
        return SMOCharacters.createNonShared(buf, offset, len);
    }

    public SMOutputtable createCData(String text) {
        return SMOCData.create(text);
    }

    public SMOutputtable createCData(char[] buf, int offset, int len) {
        return SMOCData.createShared(buf, offset, len);
    }

    /**
     * Specialized alternative to {link #createCData(char[],int,int)}
     * that can count on the passed char array NOT being shared. This means
     * that no intermediate copy needs to be done -- instance can just use
     * the passed in reference knowing it will not be messed by other threads.
     */
    public SMOutputtable createNonSharedCData(char[] buf, int offset, int len) {
        return SMOCData.createNonShared(buf, offset, len);
    }

    public SMOutputtable createComment(String text) {
        return new SMOComment(text);
    }

    public SMOutputtable createEntityRef(String name) {
        return new SMOEntityRef(name);
    }

    public SMOutputtable createProcessingInstruction(String target, String data) {
        return new SMOProcInstr(target, data);
    }

    // // // Typed value nodes

    public SMOutputtable createValue(boolean value) {
        // only 2 canonical immutable values...
        return value ? TRUE_VALUE : FALSE_VALUE;
    }

    public SMOutputtable createValue(int value) {
        return SMOTypedValue.create(value);
    }

    public SMOutputtable createValue(long value) {
        return SMOTypedValue.create(value);
    }

    public SMOutputtable createValue(double value) {
        return SMOTypedValue.create(value);
    }

    /*
    //////////////////////////////////////////////////////
    // Namespace handling
    //////////////////////////////////////////////////////
    */

    public final SMNamespace getNamespace(String uri)
    {
        if (uri == null || uri.length() == 0) {
            return NS_EMPTY;
        }
        if (_localNsMap != null) {
            SMNamespace ns = (SMNamespace) _localNsMap.get(uri);
            if (ns != null) {
                return ns;
            }
        }
        SMNamespace ns = (SMNamespace) sGlobalNsMap.get(uri);
        if (ns == null) {
            ns = new SMLocalNamespace(this, uri, _cfgPreferDefaultNs, null);
            if (_localNsMap == null) {
                _localNsMap = new HashMap<String,SMNamespace>();
            }
            _localNsMap.put(uri, ns);
        }
        return ns;
    }

    public final SMNamespace getNamespace(String uri, String prefPrefix)
    {
        if (uri == null || uri.length() == 0) {
            return NS_EMPTY;
        }
        if (_localNsMap != null) {
            SMNamespace ns = _localNsMap.get(uri);
            if (ns != null) {
                return ns;
            }
        }
        SMNamespace ns = sGlobalNsMap.get(uri);
        if (ns == null) {
            ns = new SMLocalNamespace(this, uri, _cfgPreferDefaultNs, prefPrefix);
            if (_localNsMap == null) {
                _localNsMap = new HashMap<String,SMNamespace>();
            }
            _localNsMap.put(uri, ns);
        }
        return ns;
    }

    public final static SMNamespace getEmptyNamespace()
    {
        return NS_EMPTY;
    }

    /*
    //////////////////////////////////////////////////////
    // Accessors
    //////////////////////////////////////////////////////
    */

    public final XMLStreamWriter2 getWriter() {
        return _streamWriter;
    }
    
    public final boolean isWriterRepairing() {
        return _cfgRepairing;
    }

    /*
    //////////////////////////////////////////////////////
    // Outputting of the actual content; done via context
    // so that overriding is possible
    //////////////////////////////////////////////////////
    */

    public void writeCharacters(String text)
        throws XMLStreamException
    {
        if (_indentSuppress == 0) {
            _indentSuppress = 1;
        }
        _streamWriter.writeCharacters(text);
    }
    
    public void writeCharacters(char[] buf, int offset, int len)
        throws XMLStreamException
    {
        if (_indentSuppress == 0) {
            _indentSuppress = 1;
        }
        _streamWriter.writeCharacters(buf, offset, len);
    }
    
    public void writeCData(String text)
        throws XMLStreamException
    {
        if (_indentSuppress == 0) {
            _indentSuppress = 1;
        }
        _streamWriter.writeCData(text);
    }
    
    public void writeCData(char[] buf, int offset, int len)
        throws XMLStreamException
    {
        if (_indentSuppress == 0) {
            _indentSuppress = 1;
        }
        _streamWriter.writeCData(buf, offset, len);
    }
    
    public void writeComment(String text)
        throws XMLStreamException
    {
        if (_indentSuppress == 0) {
            outputIndentation();
            _indentLevelEmpty = false;
        }
        _streamWriter.writeComment(text);
    }
    
    public void writeEntityRef(String name)
        throws XMLStreamException
    {
        // Entity references are like text output, so:
        if (_indentSuppress == 0) {
            _indentSuppress = 1;
        }
        _streamWriter.writeEntityRef(name);
    }
    
    public void writeProcessingInstruction(String target, String data)
        throws XMLStreamException
    {
        if (_indentSuppress == 0) {
            outputIndentation();
            _indentLevelEmpty = false;
        }
        if (data == null) {
            _streamWriter.writeProcessingInstruction(target);
        } else {
            _streamWriter.writeProcessingInstruction(target, data);
        }
    }

    public void writeAttribute(SMNamespace ns, String localName, String value)
        throws XMLStreamException
    {
        /* First things first: in repairing mode this is specifically
         * easy...
         */
        if (_cfgRepairing) {
            // If no prefix preference, let's not pass one:
            String prefix = ns.getPreferredPrefix();
            if (prefix == null) {
                _streamWriter.writeAttribute(ns.getURI(), localName, value);
            } else {
                _streamWriter.writeAttribute(prefix,
                                             ns.getURI(), localName, value);
            }
            return;
        }

        // If not repairing, we need to handle bindings:

        /* No/empty namespace is simple for attributes, though; the
         * default namespace is never used...
         */
        if (ns == NS_EMPTY) {
            _streamWriter.writeAttribute(localName, value);
            return;
        }

        String prefix = ns.getBoundPrefix();
        if (prefix == null || prefix.length() == 0) {
            // First check: maybe it is still bound in the root context?
            prefix = findRootPrefix(ns);
            if (prefix != null) {
                // Yup. Need to mark it as permanently bound, then
                ns._bindPermanentlyAs(prefix);
            } else {
                // Ok. So which prefix should we bind (can't use def ns)?
                prefix = ns.getLastBoundPrefix();
                if (prefix == null) {
                    prefix = ns.getPreferredPrefix();
                }
                if (prefix == null || isPrefixBound(prefix)) {
                    prefix = generateUnboundPrefix();
                }
                // Ok, can bind now...
                bindAndWriteNs(ns, prefix);
            }
        }

        _streamWriter.writeAttribute(prefix, ns.getURI(), localName, value);
    }

    /**
     * Method called to try to pre-declare given namespace
     */
    public void predeclareNamespace(SMNamespace ns, SMNamespace parentDefaultNS,
                                    int parentNsCount)
        throws XMLStreamException
    {
        String prefix = ns.getPreferredPrefix();

        // very easy in repairing mode...
        if (_cfgRepairing) {
            // If no prefix preference, let's not pass one:
            if (prefix == null) {
                _streamWriter.writeDefaultNamespace(ns.getURI());
            } else {
                _streamWriter.writeNamespace(prefix, ns.getURI());
            }
            return;
        }

        // If not repairing, we need to handle bindings.

        /* Other than that, just need to avoid re-declaring
         * default namespace or explicit prefix
         */
        if (prefix == null) {
            /* Default namespace is tricky; can only pre-declare if
             * (a) hasn't been declared yet and
             * (b) element itself has explicit prefix
             */
            if (_defaultNS == parentDefaultNS
                && _currElemNS != null
                && _currElemNS.isBoundToPrefix()) {
                _defaultNS = ns;
                _streamWriter.writeDefaultNamespace(ns.getURI());
            }
        } else { // explicit prefix...
            if (!isPrefixBoundLocally(prefix, parentNsCount)) {
                bindAndWriteNs(ns, prefix);
            }
        }
    }

    /**
     * Method called by the element object when it is about to get written
     * out. In this case, element will keep track of part of namespace
     * context information for this context object (to save allocation
     * of separate namespace context object).
     *
     * @return Namespace that was the active namespace in parent scope
     *   of this element. Will be different from the default namespace
     *   if a new default namespace was declared to be used by this
     *   element.
     */
    public SMNamespace writeStartElement(SMNamespace ns, String localName)
        throws XMLStreamException
    {
        // Indentation?
        if (_indentSuppress >= 0) {
            if (_indentSuppress == 0) {
                outputIndentation();
                _indentOffset += _indentStep;
            } else {
                ++_indentSuppress;
            }
            _indentLevelEmpty = true;
        }
        _currElemNS = ns;

        /* In repairing mode we won't do binding,
         * nor keep track of them
         */
        if (_cfgRepairing) {
            String prefix = ns.getPreferredPrefix();
            // If no prefix preference, let's not pass one:
            if (prefix == null) {
                _streamWriter.writeStartElement(ns.getURI(), localName);
            } else {
                _streamWriter.writeStartElement(prefix, localName, ns.getURI());
            }
            return _defaultNS;
        }

        SMNamespace oldDefaultNs = _defaultNS;
        String prefix;
        boolean needToBind = false;

        // Namespace we need is either already the default namespace?
        if (ns == oldDefaultNs) { // ok, simple; already the default NS:
            prefix = "";
        } else {
            // Perhaps it's already bound to a specific prefix though?
            prefix = ns.getBoundPrefix();
            if (prefix != null) { // yes, should be ok then
                /* ... except for one possible caveat: the "empty" namespace
                 * may have been masked (StaxMate never masks any explicitly
                 * bound namespace declarations)
                 */
                if (ns == NS_EMPTY) {
                    /* Only ends up here if the default ns is not the empty
                     * one any more... If so, need to re-bind it.
                     */
                    needToBind = true;
                }
            } else { // no such luck... need to bind
                /* Ok, how about the root namespace context? We may have
                 * "inherited" bindings; if so, they are accessible via
                 * namespace context.
                 */
                prefix = findRootPrefix(ns);
                if (prefix != null) {
                    // Yup. Need to mark it as permanently bound, then
                    ns._bindPermanentlyAs(prefix);
                } else {
                    needToBind = true; // yes, need to bind it
                    // Bind as the default namespace?
                    if (ns.prefersDefaultNs()) { // yes, please
                        prefix = "";
                    } else { // well, let's see if we have used a prefix earlier
                        prefix = ns.getLastBoundPrefix();
                        if (prefix != null && !isPrefixBound(prefix)) {
                            ; // can and should use last bound one, if possible
                        } else { // nope... but perhaps we have a preference?
                            prefix = ns.getPreferredPrefix();
                            if (prefix != null && !isPrefixBound(prefix)) {
                                // Ok, cool let's just bind it then:
                            } else {
                                // Nah, let's just bind as the default, then
                                prefix = "";
                            }
                        }
                    }
                }
            }
        }
        
        _streamWriter.writeStartElement(prefix, localName, ns.getURI());
        if (needToBind) {
            if (prefix.length() == 0) {
                _defaultNS = ns;
                _streamWriter.writeDefaultNamespace(ns.getURI());
            } else {
                bindAndWriteNs(ns, prefix);
            }
        }
        return oldDefaultNs;
    }
    
    public void writeEndElement(int parentNsCount, SMNamespace parentDefNs)
        throws XMLStreamException
    {
        // Indentation?
        if (_indentSuppress >= 0) {
	    _indentOffset -= _indentStep;
            if (_indentSuppress == 0) {
                if (!_indentLevelEmpty) {
                    outputIndentation();
                }
            } else {
                --_indentSuppress;
            }
            _indentLevelEmpty = false;
        }

        _streamWriter.writeEndElement();

        /* Ok, if we are not in repairing mode, may need to unbind namespace
         * bindings for namespaces bound with matching start element
         */
        if (!_cfgRepairing) {
            if (_boundNsCount > parentNsCount) {
                int i = _boundNsCount;
                _boundNsCount = parentNsCount;
                while (i-- > parentNsCount) {
                    SMNamespace ns = _nsStack[i];
                    _nsStack[i] = null;
                    ns._unbind();
                }
            }
        }

        _defaultNS = parentDefNs;
    }

    public void writeStartDocument()
        throws XMLStreamException
    {
        _streamWriter.writeStartDocument();
    }

    public void writeStartDocument(String version, String encoding)
        throws XMLStreamException
    {
        // note: Stax 1.0 has weird ordering for the args...
        _streamWriter.writeStartDocument(encoding, version);
    }

    public void writeStartDocument(String version, String encoding,
                                   boolean standalone)
        throws XMLStreamException
    {
        _streamWriter.writeStartDocument(version, encoding, standalone);
    }

    public void writeEndDocument()
        throws XMLStreamException
    {
        _streamWriter.writeEndDocument();
        // And finally, let's indicate stream writer about closure too...
        _streamWriter.close();
    }

    public void writeDoctypeDecl(String rootName,
                                 String systemId, String publicId,
                                 String intSubset)
        throws XMLStreamException
    {
        if (_indentSuppress == 0) {
            outputIndentation();
        }
        _streamWriter.writeDTD(rootName, systemId, publicId, intSubset);
    }

    /*
    //////////////////////////////////////////////////////
    // Typed Access API (Stax2 v3+) output methods
    //////////////////////////////////////////////////////
    */

    public void writeValue(boolean v) throws XMLStreamException {
        _streamWriter.writeBoolean(v);
    }

    public void writeValue(int v) throws XMLStreamException {
        _streamWriter.writeInt(v);
    }

    public void writeValue(long v) throws XMLStreamException {
        _streamWriter.writeLong(v);
    }

    public void writeValue(double d) throws XMLStreamException {
        _streamWriter.writeDouble(d);
    }

    /*
    //////////////////////////////////////////////////////
    // Methods for dealing with buffering and stream state
    //////////////////////////////////////////////////////
    */

    public void flushWriter() throws XMLStreamException
    {
        _streamWriter.flush();
    }

    /**
     * Method that can be called to force full closing of the
     * underlying stream writer as well as output target it
     * uses (usually a {@link java.io.OutputStream} or
     * {@link java.io.Writer}). Latter is done by calling
     * {@link org.codehaus.stax2.XMLStreamWriter2#closeCompletely}
     * on stream writer.
     */
    public void closeWriterCompletely() throws XMLStreamException
    {
        _streamWriter.closeCompletely();
    }

    /*
    //////////////////////////////////////////////////////
    // Other public utility methods
    //////////////////////////////////////////////////////
    */

    public String generateUnboundPrefix() {
        while (true) {
            String prefix = _nsPrefixPrefix + (_nsPrefixSeqNr++);
            if (!isPrefixBound(prefix)) {
                return prefix;
            }
        }
    }

    public boolean isPrefixBound(String prefix)
    {
        for (int i = _boundNsCount; --i >= 0; ) {
            SMNamespace ns = _nsStack[i];
            if (prefix.equals(ns.getBoundPrefix())) {
                /* Note: StaxMate never creates masking bindings, so we
                 * know it's still active
                 */
                return true;
            }
        }
        /* So far so good. But perhaps it's bound in the root NamespaceContext?
         */
        if (_rootNsContext != null) {
            String uri = _rootNsContext.getNamespaceURI(prefix);
            if (uri != null && uri.length() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Similar to {@link #isPrefixBound}, but only considers bindings
     * added by the current start element.
     */
    public boolean isPrefixBoundLocally(String prefix, int parentNsCount)
    {
        for (int i = parentNsCount; i < _boundNsCount; ++i) {
            SMNamespace ns = _nsStack[i];
            if (prefix.equals(ns.getBoundPrefix())) {
                return true;
            }
        }
        return false;
    }

    public String findRootPrefix(SMNamespace ns)
    {
        if (_rootNsContext != null) {
            String uri = ns.getURI();
            String prefix = _rootNsContext.getPrefix(uri);
            /* Should seldom if ever get a match for the default NS; but
             * if we do, let's not take it.
             */
            if (prefix != null && prefix.length() > 0) {
                return prefix;
            }
        }
        return null;
    }

    /*
    //////////////////////////////////////////////////////
    // Package methods
    //////////////////////////////////////////////////////
    */

    /**
     * @return Number of bound non-default namespaces (ones with explicit
     *   prefix) currently
     */
    int getNamespaceCount() {
        return _boundNsCount;
    }

    boolean isDefaultNs(SMNamespace ns) {
        return (_defaultNS == ns);
    }

    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
    */

    /**
     * Method for establishing binding between given namespace and
     * a non-empty prefix, as well as writing resulting namespace
     * declaration out.
     */
    private void bindAndWriteNs(SMNamespace ns, String prefix)
        throws XMLStreamException
    {
        // First, mark locally the fact that it's now bound
        SMNamespace[] stack = _nsStack;
        if (stack == null) {
            _nsStack = stack = new SMNamespace[DEF_NS_STACK_SIZE];
        } else if (_boundNsCount >= stack.length) {
            _nsStack = new SMNamespace[stack.length * 2];
            System.arraycopy(stack, 0, _nsStack, 0, stack.length);
            stack = _nsStack;
        }
        stack[_boundNsCount++] = ns;

        // And then write it out
        ns._bindAs(prefix);
        _streamWriter.writeNamespace(prefix, ns.getURI());
    }

    private void outputIndentation()
        throws XMLStreamException
    {
        int offset = _indentOffset;
        if (offset > 0) {
            int len = _indentString.length();
            if (offset > len) {
                offset = len;
            }
            // !!! TBI: Should have String-with-indexes method too in XMLStreamWriter2
            String ind = _indentString.substring(0, offset);
            _streamWriter.writeRaw(ind);
        }
    }
}
