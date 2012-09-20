package org.codehaus.staxmate.in;

/**
 * This is the abstract base class that defines standard set of element
 * information stored when element tracking is enabled for a
 * {@link SMInputCursor}. Note that all information is actually optional
 * for implementations; some implementations may want to minimize
 * memory usage and store only some of properties that are accessible
 * via base API. The reason for including "full" set of properties is
 * convenience; there is no need to upcast as the base class has
 * necesary accessors.
 *
 * @author Tatu Saloranta
 */
public abstract class SMElementInfo
{
    /*
    /////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////
     */

    /**
     * Optional operation that will return information about the parent
     * element of this element, if one exists; null if not (case for the
     * root element).
     * If not implemented, needs to return null
     */
    public abstract SMElementInfo getParent();

    /**
     * Optional operation that will return information about the previous
     * sibling in the sibling element chain. Depending on iterator, this
     * may contain only those elements iterator returned (that filter
     * accepted), or all elements iterator traversed over.
     * If not implemented, needs to return null
     */
    public abstract SMElementInfo getPreviousSibling();

    public boolean isRoot() { return getParent() == null; }

    public boolean isFirstChild() { return getPreviousSibling() == null; }

    /**
     * Optional operation that will return the zero-based index of the element
     * node amongst all nodes iterated over by the iterator that created
     * this element info object. If not implemented, needs to return -1.
     */
    public abstract int getNodeIndex();

    /**
     * Optional operation that will return the zero-based index of the element
     * amongst all (start) elements iterated over by the iterator that created
     * this element info object. If not implemented, needs to return -1.
     *<p>
     * Note that the element indices for consequtive elements stored may
     * not be consequtive, if the iterator filtered out some elements.
     */
    public abstract int getElementIndex();

    /**
     * Optional operation that will return number of parent start elements
     * this element has, if any.
     * If not implemented, needs to return -1.
     */
    public abstract int getDepth();

    /**
     * Optional operation that will return the URI of the namespace
     * of the element; this will be empty String ("") for the default
     * namespace (if none declared), or if namespace support was not
     * enabled for the parser.
     * If operation is not implemented, needs to return null.
     */
    public abstract String getNamespaceURI();

    /**
     * Optional operation that will return the local name
     * of the element, or, if namespace support was not enabled for
     * the parser, full name including possible namespace prefix.
     * If operation is not implemented, needs to return null.
     */
    public abstract String getLocalName();

    /**
     * Optional operation that will return the prefix
     * of the element, if it had one and namespace support was enabled
     * for the parser. Otherwise will return null.
     * If operation is not implemented, needs to return null.
     *
     * @return Namespace prefix of the element; or null if either name
     *   space support was not enabled, or if the element had no namespace
     *   prefix (ie. uses the default namespace)
     */
    public abstract String getPrefix();
}
 
