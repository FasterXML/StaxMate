package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Simple container class for storing definition of a buffered
 * element attribute.
 */
public class SMOAttribute
    extends SMSimpleOutput
{
    protected final SMNamespace _namespace;
    protected final String _localName;
    protected final String _value;

    /**
     * @deprecated Since 2.2 Use factory methods instead of direct construction.
     */
    @Deprecated
    public SMOAttribute(SMNamespace namespace, String localName, String value)
    {
        _namespace = namespace;
        _localName = localName;
        _value = value;
    }

    public static SMSimpleOutput attribute(SMNamespace namespace, String localName, String value) {
        return new SMOAttribute(namespace, localName, value);
    }
    
    public static SMSimpleOutput attribute(SMNamespace namespace, String localName, byte[] value) {
        return new Binary(namespace, localName, value);
    }

    public static SMSimpleOutput attribute(SMNamespace namespace, String localName, int value) {
        return new IntAttribute(namespace, localName, value);
    }
    
    protected boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException
    {
        ctxt.writeAttribute(_namespace, _localName, _value);
        return true;
    }

    public static class IntAttribute extends SMSimpleOutput
    {
        protected final SMNamespace _namespace;
        protected final String _localName;
        protected final int _value;

        public IntAttribute(SMNamespace namespace, String localName, int value)
        {
            _namespace = namespace;
            _localName = localName;
            _value = value;
        }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose)
            throws XMLStreamException
        {
            ctxt.writeAttribute(_namespace, _localName, _value);
            return true;
        }
    }

    public static class Binary extends SMSimpleOutput
    {
        protected final SMNamespace _namespace;
        protected final String _localName;
        protected final byte[] _value;

        public Binary(SMNamespace namespace, String localName, byte[] value)
        {
            _namespace = namespace;
            _localName = localName;
            _value = value;
        }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose)
            throws XMLStreamException
        {
            ctxt.writeAttribute(_namespace, _localName, _value);
            return true;
        }
    }
}
