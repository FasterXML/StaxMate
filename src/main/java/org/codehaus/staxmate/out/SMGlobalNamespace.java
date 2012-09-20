package org.codehaus.staxmate.out;

/**
 * Namespace that is global and shared for all {@link SMOutputContext})s
 * (~= XML documents or sub-trees). This includes the
 * pre-defined namespaces (ones with "xml" and "xmlns" prefixes as well
 * as the default "empty"/missing namespace, one bound to "" if no explicit
 * declaration is made).
 */
public final class SMGlobalNamespace
    extends SMNamespace
{
    /**
     * Prefix this namespace is (permanently) bound to.
     */
    protected final String _prefix;

    protected SMGlobalNamespace(String uri, String prefix)
    {
        super(uri);
        _prefix = prefix;
    }
    
    /*
    ///////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////
    */
    
    public String getPreferredPrefix() {
        return _prefix;
    }

    public String getBoundPrefix() {
        return _prefix;
    }

    public String getLastBoundPrefix() {
        return _prefix;
    }

    public boolean prefersDefaultNs() {
        return false; // doesn't matter
    }

    public void prefersDefaultNs(boolean state) {
        ; // NOP
    }

    public void setPreferredPrefix(String prefPrefix) {
        ; // NOP
    }

    protected boolean isValidIn(SMOutputContext ctxt) {
        // global namespaces are always valid for all contexts
        return true;
    }
    
    /**
     * Global namespaces should never be bound/unbound, so if this
     * gets called, an exception will be thrown (but note that this
     * being an 'internal' method, this is more like an assertion).
     */
    protected void _bindAs(String prefix) {
        throw new IllegalArgumentException("Global namespace (prefix '"
                                           +_prefix+"') can not be bound to a different prefix");
    }

    protected void _bindPermanentlyAs(String prefix) {
        _bindAs(prefix); // to throw the error
    }

    protected void _unbind() {
        throw new IllegalArgumentException("Global namespace (prefix '"
                                           +_prefix+"') can not be unbound");
    }
}
