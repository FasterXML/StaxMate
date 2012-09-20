package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Intermediate base class used for simple (non-container) output nodes;
 * comments, processing instructions, text, CDATA and entities.
 * Instances of such nodes are only created when output is blocked, and
 * they can not be output in fully streaming manner.
 */
abstract class SMSimpleOutput
    extends SMOutputtable
{
    protected SMSimpleOutput() {
        super();
    }

    /*
    /////////////////////////////////////////////////////
    // Output handling
    /////////////////////////////////////////////////////
     */

    /*
     * While there is some common behaviour that all instances share
     * (specifically, that the output never fails), 
     * output differs, and there isn't much point in factoring out
     * 'return true;' part... so let's leave this abstract
     */
    protected abstract boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException;

    protected void _forceOutput(SMOutputContext ctxt)
        throws XMLStreamException
    {
        /* For simple output nodes this is simple; can just call
         * normal output methods as these are never buffered
         */
        _output(ctxt, true);
    }
}
