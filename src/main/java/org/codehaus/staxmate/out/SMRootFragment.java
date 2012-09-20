package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Concrete non-buffered fragment (== container) class used as the root
 * level output container. Root-level does not necessarily have to mean
 * XML root level; it may also be a child context of a stream writer
 * in which StaxMate is only used to output specific sub-trees.
 * This class is also used as the base for the outputter that models
 * a complete document.
 */
public class SMRootFragment
    extends SMOutputContainer
{
    /**
     * Simple state flag; children can only be added when root container
     * is still active.
     */
    protected boolean _active = true;

    public SMRootFragment(SMOutputContext ctxt)
    {
        super(ctxt);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////////////////////////////
     */

    protected boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException
    {
        // Should never get called if not active...
        if (!_active) {
            throwIfClosed();
        }
        if (canClose) {
            return _closeAndOutputChildren();
        }
        return _closeAllButLastChild();
    }

    protected void _forceOutput(SMOutputContext ctxt)
        throws XMLStreamException
    {
        // Should never get called if not active...
        if (!_active) {
            throwIfClosed();
        }
        _forceChildOutput();
    }
    
    protected void _childReleased(SMOutputtable child)
        throws XMLStreamException
    {
        // Should never get called if not active...
        if (!_active) {
            throwIfClosed();
        }

        /* The only child that can block output is the first one... 
         * If that was released, may be able to output more as well.
         * Note that since there's never parent (this is the root fragment),
         * there's no need to try to inform anyone else.
         */
        if (child == _firstChild) {
            _closeAllButLastChild();
        }

        // Either way, we are now done
    }

    public boolean _canOutputNewChild()
        throws XMLStreamException
    {
        // Should never get called if not active...
        if (!_active) {
            throwIfClosed();
        }
        return (_firstChild == null) || _closeAndOutputChildren();
    }

    public void getPath(StringBuilder sb)
    {
        if (_parent != null) {
            _parent.getPath(sb);
        }
        /* Although fragments are "invisible", let's add an indicator
         * of some sort, since this path is used for trouble-shooting
         */
        sb.append("/{fragment}");
    }

    /**
     * Method that HAS to be called when all additions have been done
     * via StaxMate API. Since it is possible that the underlying stream
     * writer may be buffering some parts, it needs to be informed of
     * the closure.
     *<p>
     * Note that the underlying stream is <b>NOT</b> closed as part of
     * this call, just this logical outputter object.
     * If you do want the underlying writer to be closed too, call
     * {@link #closeRootAndWriter()} instead.
     */
    public void closeRoot()
        throws XMLStreamException
    {
        // Hmmh. Should we complain about duplicate closes?
        if (!_active) {
            return;
        }
        // Let's first try to close them nicely:
        if (!_output(_context, true)) {
            // but if that doesn't work, should just unbuffer all children...
            _forceOutput(_context);
        }
        // Either way, we are now closed:
        _active = false;
        // And this may also be a good idea:
        getContext().flushWriter();
    }

    public void closeRootAndWriter()
        throws XMLStreamException
    {
        closeRoot();
        getContext().closeWriterCompletely();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */

    private final void throwIfClosed() {
        throw new IllegalStateException("Can not modify root-level container once it has been closed");
    }
}
