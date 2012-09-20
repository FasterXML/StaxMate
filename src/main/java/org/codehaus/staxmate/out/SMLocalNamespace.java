package org.codehaus.staxmate.out;

/**
 * Namespace that is local to a specific output context
 * ({@link SMOutputContext}), think of it as the document or sub-tree
 * StaxMate will output using a stream writer).
 */
public final class SMLocalNamespace
    extends SMNamespace
{
    /**
     * Output context in which this namespace is to be used (scope of
     * which it is bound)
     */
    protected final SMOutputContext _context;

    /**
     * Preferred (or suggested) prefix for the namespace;
     * StaxMate will try to use this prefix if possible when binding
     * namespaces and also passes it to the underlying stream writer.
     *<p>
     * This value will be automatically set when namespace is created,
     * and there is also a way to explicitly set it. Finally, it will
     * also be set if a dynamic prefix is created for the namespace
     */
    protected String _preferredPrefix;

    /**
     * Prefix this namespace is currently bound to, if any.
     */
    protected String _currPrefix = null;

    /**
     * Last prefix this name was bound to, if any.
     */
    protected String _prevPrefix = null;

    /**
     * Flag that indicates whether this namespaces prefers to be bound
     * as the default namespace (for elements), or not. Output context
     * will use this preference in some situations to determine how to
     * best bind this namespace to a prefix or as the default namespace.
     */
    protected boolean _preferDefaultNs;

    /**
     * Flag that indicates whether this binding (with current prefix) is
     * permanent or not; that is, whether prefix associated with the
     * namespace URI can still change or not.
     */
    protected boolean _isPermanent;

    /**
     * @param ctxt Output context that "owns" this namespace (within which
     *    namespace will be bound when output)
     * @param uri URI that defines identity of the namespace
     * @param prefPrefix Prefererred (or suggested) prefix for the namespace;
     *   StaxMate will try to use this prefix if possible when binding
     *   namespaces and also passes it to the underlying stream writer.
     * @param preferDefaultNs Whether this namespaces prefers to be bound
     *   as the default namespace when used for elements.
     */
    protected SMLocalNamespace(SMOutputContext ctxt,
                               String uri, boolean preferDefaultNs,
                               String prefPrefix)
    {
        super(uri);
        _context = ctxt;
        _preferredPrefix = prefPrefix;
        _preferDefaultNs = preferDefaultNs;
    }

    /*
    ///////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////
     */

    public String getPreferredPrefix() {
        return _preferredPrefix;
    }
    
    public String getBoundPrefix() {
        return _currPrefix;
    }

    public String getLastBoundPrefix() {
        return _prevPrefix;
    }

    public boolean prefersDefaultNs() {
        return _preferDefaultNs;
    }

    public void prefersDefaultNs(boolean state) {
        _preferDefaultNs = state;
    }

    public void setPreferredPrefix(String prefPrefix) {
        _preferredPrefix = prefPrefix;
    }

    protected boolean isValidIn(SMOutputContext ctxt) {
        return ctxt == _context;
    }

    /**
     * The only trick with regard to binding/unbinding of local namespaces
     * is that "re-binding" is not allowed (by StaxMate design; XML would
     * allow it). So let's allow transitions to and from null, but not
     * between two non-empty prefixes.
     */
    protected void _bindAs(String prefix)
    {
        if (_currPrefix != null) {
            /* Let's not bother checking for equality -- any calls to re-bind
             * are errors in implementation, and are never called by the
             * end application
             */
            throw new IllegalStateException("Trying to re-bind URI '"+_uri
                                            +"', from prefix '"+_currPrefix
                                            +"' to prefix '"+prefix+"'");
        }
        _currPrefix = _prevPrefix = prefix;
    }

    protected void _bindPermanentlyAs(String prefix)
    {
        // First, let's do the binding
        _bindAs(prefix);
        // and then let's mark it as a permanent one...
        if (_isPermanent) {
            throw new IllegalStateException("Trying to call permanentlyBindAs() twice (for URI '"+_uri+"', prefix '"+prefix+"')");
        }
        _isPermanent = true;
    }

    protected void _unbind()
    {
        // Sanity check:
        if (_currPrefix == null) {
            throw new IllegalStateException("Trying to unbind an unbound namespace (URI '"+_uri+"')");
        }
        if (!_isPermanent) { // permanent ones just won't unbind... 
            _currPrefix = null;
        }
    }
}
