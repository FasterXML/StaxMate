package org.codehaus.staxmate.out;

import javax.xml.stream.XMLStreamException;

/**
 * Output class that models a full XML document, with xml declaration.
 */
public class SMOutputDocument
    extends SMRootFragment
{
    protected SMOutputDocument(SMOutputContext ctxt)
        throws XMLStreamException
    {
        super(ctxt);
        ctxt.writeStartDocument();
    }
    
    protected SMOutputDocument(SMOutputContext ctxt,
                               String version, String encoding)
        throws XMLStreamException
    {
        super(ctxt);
        ctxt.writeStartDocument(version, encoding);
    }

    protected SMOutputDocument(SMOutputContext ctxt,
                               String version, String encoding, boolean standalone)
        throws XMLStreamException
    {
        super(ctxt);
        ctxt.writeStartDocument(version, encoding, standalone);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Overridden output methods
    ///////////////////////////////////////////////////////////
    */

    /*
    ///////////////////////////////////////////////////////////
    // Additional output methods
    //
    // note: no validation is done WRT ordering since underlying
    // stream writer is likely to catch them.
    ///////////////////////////////////////////////////////////
    */

    public void addDoctypeDecl(String rootName, String systemId, String publicId)
        throws XMLStreamException
    {
        addDoctypeDecl(rootName, systemId, publicId, null);
    }

    public void addDoctypeDecl(String rootName, String systemId, String publicId,
                               String intSubset)
        throws XMLStreamException
    {
        getContext().writeDoctypeDecl(rootName, systemId, publicId, intSubset);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////////////////////////////
    */

    /**
     * Method that HAS to be called when all additions have been done
     * via StaxMate API. Since it is possible that the underlying stream
     * writer may be buffering some parts, it needs to be informed of
     * the closure.
     */
    public void closeRoot()
        throws XMLStreamException
    {
        super.closeRoot();
        getContext().writeEndDocument();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
    */
}
