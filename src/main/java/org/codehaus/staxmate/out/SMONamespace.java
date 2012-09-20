package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Simple container class for storing a namespace pre-declaration
 */
public class SMONamespace
    extends SMSimpleOutput
{
    final SMNamespace _namespace;

    /**
     * Default namespace in effect for the parent element of the element
     * on which this namespace (pre)declaration applies.
     */
    final SMNamespace _parentDefaultNS;

    /**
     * Number of namespace declarations parent element of the element
     * on which this namespace (pre)declaration applies.
     */
    final int _parentNsCount;

    public SMONamespace(SMNamespace ns, SMNamespace parentDefaultNS, int parentNsCount)
    {
        _namespace = ns;
        _parentDefaultNS = parentDefaultNS;
        _parentNsCount = parentNsCount;
    }

    protected boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException
    {
        ctxt.predeclareNamespace(_namespace, _parentDefaultNS, _parentNsCount);
        return true;
    }
}
