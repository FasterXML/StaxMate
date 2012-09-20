package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

public abstract class SMOCharacters
    extends SMSimpleOutput
{
    private SMOCharacters() { }

    /*
    ////////////////////////////////////////////////////////////////
    // Factory methods
    ////////////////////////////////////////////////////////////////
     */

    public static SMOutputtable create(String text) {
        return new StringBased(text);
    }
    
    public static SMOutputtable createShared(char[] buf, int offset, int len) {
    	if (len < 1) {
    		return create("");
    	}
    	char[] arr = new char[len];
    	System.arraycopy(buf, offset, arr, 0, len);
    	return new ArrayBased(arr);
    }
    
    public static SMOutputtable createNonShared(char[] buf, int offset, int len) {
        if (offset == 0 && len == buf.length) {
            return new ArrayBased(buf);
	}
        return new ArrayBased3(buf, offset, len);
    }
    
    protected abstract boolean _output(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException;
    
    /*
    ////////////////////////////////////////////////////////////////
    // Sub-classes
    ////////////////////////////////////////////////////////////////
    */
    
    private final static class StringBased
        extends SMOCharacters
    {
        final String mText;
        
        StringBased(String text) {
            mText = text;
        }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose)
            throws XMLStreamException
        {
            ctxt.writeCharacters(mText);
            return true;
        }
    }

    private final static class ArrayBased
        extends SMOCharacters
    {
        final char[] mBuf;
        
        ArrayBased(char[] buf) {
            super();
            mBuf = buf;
        }

        protected boolean _output(SMOutputContext ctxt, boolean canClose)
            throws XMLStreamException
        {
            ctxt.writeCharacters(mBuf, 0, mBuf.length);
            return true;
        }
    }

    private final static class ArrayBased3
        extends SMOCharacters
    {
        final char[] mBuf;
        final int mOffset, mLen;
        
        ArrayBased3(char[] buf, int offset, int len) {
            super();
            mBuf = buf;
            mOffset = offset;
            mLen = len;
        }
        
        protected boolean _output(SMOutputContext ctxt, boolean canClose)
            throws XMLStreamException
        {
            ctxt.writeCharacters(mBuf, mOffset, mLen);
            return true;
        }
    }
}
