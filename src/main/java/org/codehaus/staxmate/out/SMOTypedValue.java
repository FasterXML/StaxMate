package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Base class for buffered values
 */
public abstract class SMOTypedValue
    extends SMSimpleOutput
{
    protected SMOTypedValue() { }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    public static SMOTypedValue create(boolean value) { return new BooleanValue(value); }
    public static SMOTypedValue create(int value) { return new IntValue(value); }
    public static SMOTypedValue create(long value) { return new LongValue(value); }
    public static SMOTypedValue create(double value) { return new DoubleValue(value); }
    public static SMOTypedValue create(byte[] src, int offset, int length) {
        byte[] data = new byte[length];
        System.arraycopy(src, offset, data, 0, length);
        return new BinaryValue(data);
    }

    protected abstract boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException;

    /*
    /**********************************************************
    /* Sub-classes
    /**********************************************************
     */
    
    private final static class BooleanValue extends SMOTypedValue
    {
        final boolean _value;

        BooleanValue(boolean v) { _value = v; }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose) throws XMLStreamException
        {
            ctxt.writeValue(_value);
            return true;
        }
    }

    private final static class IntValue extends SMOTypedValue
    {
        final int _value;

        IntValue(int v) { _value = v; }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose) throws XMLStreamException
        {
            ctxt.writeValue(_value);
            return true;
        }
    }

    private final static class LongValue extends SMOTypedValue
    {
        final long _value;

        LongValue(long v) { _value = v; }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose) throws XMLStreamException
        {
            ctxt.writeValue(_value);
            return true;
        }
    }

    private final static class DoubleValue extends SMOTypedValue
    {
        final double _value;

        DoubleValue(double v) { _value = v; }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose) throws XMLStreamException
        {
            ctxt.writeValue(_value);
            return true;
        }
    }

    private final static class BinaryValue extends SMOTypedValue
    {
        final byte[] _value;

        BinaryValue(byte[] v) { _value = v; }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose) throws XMLStreamException {
            ctxt.writeValue(_value);
            return true;
        }
    }
}
