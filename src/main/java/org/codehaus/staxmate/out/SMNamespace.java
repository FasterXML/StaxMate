package org.codehaus.staxmate.out;

/**
 * Abstract base class for all namespace objects (local and global ones).
 */
public abstract class SMNamespace
{
    /**
     * URI of the actual namespace this class encapsulates
     */
    protected final String _uri;

    protected SMNamespace(String uri)
    {
        _uri = uri;
    }

    /*
    ///////////////////////////////////
    // Accessors
    ///////////////////////////////////
     */

    public final String getURI() {
        return _uri;
    }

    /**
     * @return Prefix that the caller application has at some point
     *  indicated to be a prefix it'd like to see; StaxMate may try to
     *  use it as the prefix to bind if there are no
     */
    public abstract String getPreferredPrefix();

    public abstract String getBoundPrefix();

    public abstract String getLastBoundPrefix();

    public abstract boolean prefersDefaultNs();

    public final boolean isBoundToPrefix() {
        String prefix = getBoundPrefix();
        return (prefix != null) && prefix.length() != 0;
    }

    /*
    ///////////////////////////////////
    // Mutators
    ///////////////////////////////////
     */

    public abstract void prefersDefaultNs(boolean state);

    /**
     * Method that can be called to associate a "preferred" prefix; prefix
     * that this namespace should be bound to instead of some automatically
     * generated one (unless that prefix is already bound to another URI).
     */
    public abstract void setPreferredPrefix(String prefPrefix);

    /*
    ///////////////////////////////////
    // Internal API
    ///////////////////////////////////
     */

    /**
     * Method used to verify that the namespace is actually valid within
     * the specified output context.
     */
    protected abstract boolean isValidIn(SMOutputContext ctxt);

    /**
     * Method called to indicate that the namespace is now bound to a
     * specific prefix within current output context. Note that this
     * will not be called when the namespace is defined as the default
     * namespace, but only when it is also bound to a prefix.
     */
    protected abstract void _bindAs(String prefix);

    /**
     * Method used to permanently bind this (local) namespace to a prefix.
     * Generally called if a new "global" binding is found at point where
     * a global instance can not be created. Calling this method will
     * essentially mark a local instace as behaving similar to a global
     * one.
     */
    protected abstract void _bindPermanentlyAs(String prefix);

    /**
     * Method called to indicate that the namespace is no longer bound
     * to its current prefix within the current output context.
     * Since default namespace declarations do not cause binding, this
     * method will not be called for default namespaces either.
     */
    protected abstract void _unbind();
}
