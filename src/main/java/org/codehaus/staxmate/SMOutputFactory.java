package org.codehaus.staxmate;

import java.io.*;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.io.Stax2FileResult;
import org.codehaus.stax2.ri.Stax2WriterAdapter;

import org.codehaus.staxmate.out.SMOutputContext;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMRootFragment;

/**
 * Factory class used to create root-level outputter object (like
 * {@link SMOutputDocument} and {@link SMRootFragment}) instances,
 * which are roots (global for documents, or local for fragments)
 * of output content.
 *<p>
 * Note about life-cycle of root-level outputter objects: once
 * you are done with a root-level outputter, you <b>MUST</b>
 * call {@link SMRootFragment#closeRoot} method to ensure that
 * all content is flushed to the underlying stream writer
 *<p>
 * Factory also has convenience method(s) for accessing a shared
 * global instance of a default {@link XMLOutputFactory}.
 */
public final class SMOutputFactory
{
    /**
     * Xml output stream factory used for constructing stream readers.
     */
    final XMLOutputFactory _staxFactory;

    /**
     * If the configured stax factory implements Stax2 API,
     * will contain upcast factory instance, otherwise null.
     */
    final XMLOutputFactory2 _stax2Factory;

    public SMOutputFactory(XMLOutputFactory staxF)
    {
        _staxFactory = staxF;
        _stax2Factory = (staxF instanceof XMLOutputFactory2) ?
            (XMLOutputFactory2) staxF : null;
    }

    /*
    /////////////////////////////////////////////////
    // Access to underlying Stax factory
    /////////////////////////////////////////////////
     */

    /**
     * Method for accessing Stax output factory this StaxMate
     * output factory was constructed with. Factory can be configured
     * using normal property-based configuration methods.
     */
    public XMLOutputFactory getStaxFactory() { return _staxFactory; }

    /*
    ////////////////////////////////////////////////////
    // Document output construction
    //
    // note: no buffered alternatives -- they are easy
    // to create, just add a buffered fragment inside
    // the document fragment
    ////////////////////////////////////////////////////
     */

    /**
     * Factory method for constructing output object that represents
     * a complete xml document including xml declaration and will
     * contain root element plus other optional elements (doctype
     * declaration, comment(s), PI(s)).
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct fragments,
     * for this purpose check out {@link #createOutputFragment}.
     */
    public static SMOutputDocument createOutputDocument(XMLStreamWriter sw)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(Stax2WriterAdapter.wrapIfNecessary(sw)).createDocument();
    }

    /**
     * Factory method for constructing output object that represents
     * a complete xml document including xml declaration and will
     * contain root element plus other optional elements (doctype
     * declaration, comment(s), PI(s)).
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct fragments,
     * for this purpose check out {@link #createOutputFragment}.
     */
    public SMOutputDocument createOutputDocument(File f)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(createStax2Writer(f)).createDocument();
    }

    /**
     * Factory method for constructing output object that represents
     * a complete xml document including xml declaration and will
     * contain root element plus other optional elements (doctype
     * declaration, comment(s), PI(s)).
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct fragments,
     * for this purpose check out {@link #createOutputFragment}.
     */
    public SMOutputDocument createOutputDocument(OutputStream out)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(createStax2Writer(out)).createDocument();
    }

    /**
     * Factory method for constructing output object that represents
     * a complete xml document including xml declaration and will
     * contain root element plus other optional elements (doctype
     * declaration, comment(s), PI(s)).
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct fragments,
     * for this purpose check out {@link #createOutputFragment}.
     */
    public SMOutputDocument createOutputDocument(Writer w)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(createStax2Writer(w)).createDocument();
    }

    /**
     * Factory method for constructing output object that represents
     * a complete xml document including xml declaration and will
     * contain root element plus other optional elements (doctype
     * declaration, comment(s), PI(s)).
     *<p>
     * Note: you can not use this method to construct fragments,
     * for this purpose check out {@link #createOutputFragment}.
     */
    public static SMOutputDocument createOutputDocument(XMLStreamWriter sw,
                                                        String version,
                                                        String encoding,
                                                        boolean standAlone)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(Stax2WriterAdapter.wrapIfNecessary(sw)).createDocument(version, encoding, standAlone);
    }

    /*
    ///////////////////////////////////////////////////////
    // Fragment output construction
    // 
    // May be useful when only sub-tree(s) of a document
    // is done using StaxMate
    ///////////////////////////////////////////////////////
     */

    /**
     * Factory method for constructing output object that represents
     * root-level of an xml fragment; container that can contain
     * non-element markup (comments, PIs), textual data and
     * zero or more elements.
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct actual documents,
     * for this purpose check out {@link #createOutputDocument}.
     */
    public static SMRootFragment createOutputFragment(XMLStreamWriter sw)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(Stax2WriterAdapter.wrapIfNecessary(sw)).createRootFragment();
    }

    /**
     * Factory method for constructing output object that represents
     * root-level of an xml fragment; container that can contain
     * non-element markup (comments, PIs), textual data and
     * zero or more elements.
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct actual documents,
     * for this purpose check out {@link #createOutputDocument}.
     *
     * @param resultFile file xml contents get written to
     */
    public SMRootFragment createOutputFragment(File resultFile)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(createStax2Writer(resultFile)).createRootFragment();
    }

    /**
     * Factory method for constructing output object that represents
     * root-level of an xml fragment; container that can contain
     * non-element markup (comments, PIs), textual data and
     * zero or more elements.
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct actual documents,
     * for this purpose check out {@link #createOutputDocument}.
     *
     * @param out Output stream through with xml contents get written
     */
    public SMRootFragment createOutputFragment(OutputStream out)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(createStax2Writer(out)).createRootFragment();
    }

    /**
     * Factory method for constructing output object that represents
     * root-level of an xml fragment; container that can contain
     * non-element markup (comments, PIs), textual data and
     * zero or more elements.
     *<p>
     * Note that after you have completed output using the
     * result object (and its children), you must call
     * {@link SMRootFragment#closeRoot} method to ensure that
     * all the content is properly output via underlying stream writer.
     *<p>
     * Note: you can not use this method to construct actual documents,
     * for this purpose check out {@link #createOutputDocument}.
     *
     * @param w Writer used for writing xml contents
     */
    public SMRootFragment createOutputFragment(Writer w)
        throws XMLStreamException
    {
        return SMOutputContext.createInstance(createStax2Writer(w)).createRootFragment();
    }

    /*
    /////////////////////////////////////////////////
    // Stream reader construction
    /////////////////////////////////////////////////
     */

    /**
     * Method for constructing Stax stream writer to write xml content
     * to specified file.
     * Underlying stream writer will be constructed using Stax factory
     * this StaxMate factory was constructed with.
     *<p>
     * Encoding used will be UTF-8.
     */
    public XMLStreamWriter2 createStax2Writer(File f)
        throws XMLStreamException
    {
        if (_stax2Factory != null) {
            /* have real stax2 factory; can create more optimal writer
             * (most importantly: one that automatically closes the writer)
             */
            Stax2FileResult res = new Stax2FileResult(f);
            return (XMLStreamWriter2) _stax2Factory.createXMLStreamWriter(res);
        }
        /* No, just stax1 factory. Could use StreamResult; but some
         * impls might not recognize it... immediate problem here:
         * auto-closing won't work.
         */
        try {
            FileOutputStream out = new FileOutputStream(f);
            return Stax2WriterAdapter.wrapIfNecessary(_staxFactory.createXMLStreamWriter(out));
        } catch (FileNotFoundException fex) {
            throw new XMLStreamException(fex);
        }
    }

    /**
     * Method for constructing Stax stream writer to write xml content
     * to specified output stream.
     * Underlying stream writer will be constructed using Stax factory
     * this StaxMate factory was constructed with.
     *<p>
     * Encoding used will be UTF-8.
     */
    public XMLStreamWriter2 createStax2Writer(OutputStream out)
        throws XMLStreamException
    {
        return Stax2WriterAdapter.wrapIfNecessary(_staxFactory.createXMLStreamWriter(out));
    }

    /**
     * Method for constructing Stax stream writer to write xml content
     * using specified Writer.
     * Underlying stream writer will be constructed using Stax factory
     * this StaxMate factory was constructed with.
     *<p>
     * Encoding used will be UTF-8.
     */
    public XMLStreamWriter2 createStax2Writer(Writer w)
        throws XMLStreamException
    {
        return Stax2WriterAdapter.wrapIfNecessary(_staxFactory.createXMLStreamWriter(w));
    }

    /*
    ///////////////////////////////////////////////////////
    // Convenience methods
    ///////////////////////////////////////////////////////
    */

    /**
     * Convenience method that will get a lazily constructed shared
     * {@link SMOutputFactory} instance. Instance is built using
     * similarly shared {@link XMLOutputFactory} instance (which
     * is accessed using {@link #getGlobalXMLOutputFactory}).
     * See notes on  {@link #getGlobalXMLOutputFactory} for limitations
     * on when (if ever) you should use this method.
     *<p>
     * Note that this single(ton) instance is global to the class loader
     * that loaded <code>SMOutputFactory</code> (and usually hence
     * global to a single JVM instance).
     *
     * @throws FactoryConfigurationError If there are problems with
     *   configuration of Stax output factory (most likely because
     *   there is no implementation available)
     */
    public static SMOutputFactory getGlobalSMOutputFactory()
        throws FactoryConfigurationError
    {
        return SMFactoryAccessor.getFactory();
    }

    /**
     * Convenience method that will get a lazily constructed shared
     * {@link XMLOutputFactory} instance. Note that this instance
     * should only be used iff:
     *<ul>
     * <li>Default settings (non-repairing) for the factory are acceptable
     *  </li>
     * <li>Settings of the factory are not modified: thread-safety
     *   of the factory instance is only guaranteed for factory methods,
     *   not for configuration change methods
     *  </li>
     * </ul>
     */
    public static XMLOutputFactory getGlobalXMLOutputFactory()
        throws XMLStreamException
    {
        try {
            return XmlFactoryAccessor.getFactory();
        } catch (FactoryConfigurationError err) {
            throw new XMLStreamException(err);
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////////////
    */

    /**
     * Separate helper class is used so that the shared factory instance
     * is only created if needed: this happens if the accessor class
     * needs to be instantiate, which in turn happens if the method
     * for accessing the global output factory is called.
     */
    private final static class XmlFactoryAccessor
    {
        final static XmlFactoryAccessor sInstance = new XmlFactoryAccessor();
        XMLOutputFactory mFactory = null;

        public static XMLOutputFactory getFactory()
            throws FactoryConfigurationError
        {
            return sInstance.get();
        }

        public synchronized XMLOutputFactory get()
            throws FactoryConfigurationError
        {
            if (mFactory == null) {
                mFactory = XMLOutputFactory.newInstance();
            }
            return mFactory;
        }
    }

    /**
     * Helper class used for implementing efficient lazy instantiation of
     * the global StaxMate output factory.
     */
    private final static class SMFactoryAccessor
    {
        final static SMFactoryAccessor sInstance = new SMFactoryAccessor();
        SMOutputFactory mFactory = null;

        public static SMOutputFactory getFactory()
            throws FactoryConfigurationError
        {
            return sInstance.get();
        }

        private synchronized SMOutputFactory get()
            throws FactoryConfigurationError
        {
            if (mFactory == null) {
                mFactory = new SMOutputFactory(XmlFactoryAccessor.getFactory());
            }
            return mFactory;
        }
    }
}
