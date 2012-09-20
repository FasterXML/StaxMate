package org.codehaus.staxmate.in;

import java.io.IOException;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader; // for javadocs
import javax.xml.stream.events.XMLEvent; 

import org.codehaus.stax2.DTDInfo;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.typed.TypedXMLStreamException;

/**
 * Base class for reader-side cursors that form the main input side
 * abstraction offered by StaxMate. This class offers the main API
 * for applications to use: sub-classes generally just implement
 * abstract methods or override default behavior, but do not offer
 * new public methods. As a result, up-casts are usually not
 * necessary.
 *<p>
 * Implementation Note: as cursors are thin wrappers around
 * {@link XMLStreamReader2},
 * and since not all Stax implementations implement
 * {@link XMLStreamReader2}, some wrapping may be needed to expose
 * basic Stax 1.0 {@link XMLStreamReader}s as Stax2
 * {@link XMLStreamReader2} readers.
 * But without native support, not all stax2 features may be available
 * for such stream readers.
 * This should usuall not be a problem with cursor functionality,
 * as cursors will try to limit their usage to known working subset,
 * but care must be taken if application code wants to directly
 * access underlying stream reader.
 *
 * @author Tatu Saloranta
 */
public abstract class SMInputCursor
    extends CursorBase
{
    /*
    /**********************************************************************
    /* Constants, tracking
    /**********************************************************************
     */

    // // // Constants for element tracking:

    /**
     * Different tracking behaviors available for cursors.
     * Tracking is a feature that can be used to store
     * information about traversed sub-trees, to allow for a limited
     * access to information that is not limited to ancestor stack.
     * Using tracking will consume more memory, but generally less
     * than constructing a full in-memory tree object model (such
     * as DOM), since it the represenation is compact, read-only,
     * and only subset of a full tree. Size (and hence memory overhead)
     * of that sub-tree depends on tracking settings.
     */
    public enum Tracking
    {
        /**
         * Value that indicates that no element state information should
         * be tracked. This means that {@link #getTrackedElement} will always
         * return null for this element, as well as that if immediate child
         * cursors do have tracking enabled, element states it saves have
         * no parent element information available.
         */
        NONE,

        /**
         * Value that indicates that element basic state information should
         * be tracked, including linkage to the parent element (but only
         * if the parent cursor was tracking elements).
         * This means that {@link #getTrackedElement} will return non-null
         * values, as soon as this cursor has been advanced over its first
         * element node. However, element will return null from its
         * {@link SMElementInfo#getPreviousSibling} since sibling information
         * is not tracked.
         */
        PARENTS,

        /**
         * Value that indicates full element state information should
         * be tracked for all "visible" elements: visible meaning that element
         * node was accepted by the filter this cursor uses.
         * This means that {@link #getTrackedElement} will return non-null
         * values, as soon as this cursor has been advanced over its first
         * element node, and that element will return non-null from its
         * {@link SMElementInfo#getPreviousSibling} unless it's the first element
         * iterated by this cursor.
         */
        VISIBLE_SIBLINGS,

        /**
         * Value that indicates full element state information should
         * be tracked for ALL elements (including ones not visible to the
         * caller via {@link #getNext} method).
         * This means that {@link #getTrackedElement} will return non-null
         * values, as soon as this cursor has been advanced over its first
         * element node, and that element will return non-null from its
         * {@link SMElementInfo#getPreviousSibling} unless it's the first element
         * iterated by this cursor.
         */
        ALL_SIBLINGS
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Optional filter object that can be used to filter out events of
     * types caller is not interested in.
     */
    protected SMFilter mFilter = null;

    /**
     * Whether element information is to be tracked or not, and if it is,
     * how much of it will be stored. See {@link Tracking} for details.
     */
    protected Tracking mElemTracking = Tracking.NONE;

    /**
     * Optional factory instance that is used to create element info
     * objects if element tracking is enabled. If null, will use default
     * generation mechanism, implemented by SMInputCursor itself.
     *<p>
     * Note that by default, this factory will be passed down to child
     * and descendant cursors this cursor creates, so usually one
     * only needs to set the factory of the root cursor.
     */
    protected ElementInfoFactory mElemInfoFactory;

    /*
    /**********************************************************************
    /* Additional data
    /**********************************************************************
     */

    /**
     * Non-typesafe payload data that applications can use, to pass
     * an extra argument along with cursors. Not used by the framework
     * itself for anything.
     */
    protected Object mData;

    /*
    /**********************************************************************
    /* Life cycle, configuration
    /**********************************************************************
     */

    public SMInputCursor(SMInputContext ctxt, SMInputCursor parent, SMFilter filter)
    {
        super(ctxt, (parent == null) ? 0 : ctxt.getDepth());
        mFilter = filter;
        /* By default, we use parent cursor's element tracking setting;
         * or "no tracking" if we have no parent
         */
        if (parent == null) {
            mElemTracking = Tracking.NONE;
            _parentTrackedElement = null;
            mElemInfoFactory = null;
        } else {
            mElemTracking = parent.getElementTracking();
            _parentTrackedElement = parent.getTrackedElement();
            mElemInfoFactory = parent.getElementInfoFactory();
        }
    }

    /**
     * Method for setting filter used for selecting which events
     * are to be returned to the caller when {@link #getNext}
     * is called.
     */
    public final void setFilter(SMFilter f) {
        mFilter = f;
    }

    /**
     * Changes tracking mode of this cursor to the new specified
     * mode. Default mode for cursors is the one their parent uses;
     * {@link Tracking#NONE} for root cursors with no parent.
     *<p>
     * See also {@link #getPathDesc} for information on how
     * to display tracked path/element information.
     */
    public final void setElementTracking(Tracking tracking) {
        mElemTracking = tracking;
    }

    public final Tracking getElementTracking() {
        return mElemTracking;
    }

    /**
     * Set element info factory used for constructing
     * {@link SMElementInfo} instances during traversal for this
     * cursor, as well as all of its children.
     */
    public final void setElementInfoFactory(ElementInfoFactory f) {
        mElemInfoFactory = f;
    }

    public final ElementInfoFactory getElementInfoFactory() {
        return mElemInfoFactory;
    }

    /*
    /**********************************************************************
    /* Public API, accessing cursor state information
    /**********************************************************************
     */

    /**
     * Method to access number of nodes cursor has traversed
     * (including ones that were filtered out, if any).
     * Starts with 0, and is incremented each time
     * underlying stream reader's {@link XMLStreamReader#next} method
     * is called, but not counting child cursors' node counts.
     * Whether END_ELEMENTs (end tags) are included depends on type
     * of cursor: for nested (which do not return events for end tags)
     * they are not counted, but for flattened one (that do return)
     * they are counted as nodes.
     *
     * @return Number of nodes (events) cursor has traversed
     */
    public int getNodeCount() {
        return _nodeCount;
    }

    /**
     * Method to access number of start elements cursor has traversed
     * (including ones that were filtered out, if any).
     * Starts with 0, and is incremented each time
     * underlying stream reader's {@link XMLStreamReader#next} method
     * is called and has moved over a start element, but not counting
     * child cursors' element counts.
     *
     * @return Number of start elements cursor has traversed
     */
    public int getElementCount() {
        return _elemCount;
    }

    /**
     * Number of parent elements that the token/event cursor points to has,
     * if it points to one. If not, either most recent valid parent
     * count (if cursor is closed), or the depth that it will have
     * once is is advanced. One practical result is that a nested
     * cursor instance will always have a single constant value it
     * returns, whereas flattening cursors can return different
     * values during traversal. Another thing to notice that matching
     * START_ELEMENT and END_ELEMENT will always correspond to the
     * same parent count.
     *<p>
     * For example, here are expected return values
     * for an example XML document:
     *<pre>
     *  &lt;!-- Comment outside tree --> [0]
     *  &lt;root> [0]
     *    Text [1]
     *    &lt;branch> [1]
     *      Inner text [2]
     *      &lt;child /> [2]/[2]
     *    &lt;/branch> [1]
     *  &lt;/root> [0]
     *</pre>
     * Numbers in bracket are depths that would be returned when the
     * cursor points to the node.
     *<p>
     * Note: depths are different from what many other xml processing
     * APIs (such as Stax and XmlPull)return.
     *
     * @return Number of enclosing nesting levels, ie. number of parent
     *   start elements for the node that cursor currently points to (or,
     *   in case of initial state, that it will point to if scope has
     *   node(s)).
     */
    public abstract int getParentCount();

    /**
     * Returns the type of event this cursor either currently points to
     * (if in valid state), or pointed to (if ever iterated forward), or
     * null if just created.
     *
     * @return Type of event this cursor points to, if it currently points
     *   to one, or last one it pointed to otherwise (if ever pointed to
     *   a valid event), or null if neither.
     */
    public SMEvent getCurrEvent() {
        return _currEvent;
    }

    /**
     * Convenience method for accessing type of the current event
     * (as would be returned by {@link #getCurrEvent}) as
     * one of event types defined in {@link XMLStreamConstants}
     * (like {@link XMLStreamConstants#START_ELEMENT}).
     */
    public int getCurrEventCode() {
        return (_currEvent == null) ? 0 : _currEvent.getEventCode();
    }

    /**
     * Method for determining whether this cursor iterates over root level of
     *   the underlying stream reader
     *
     * @return True if this cursor iterates over root level of
     *   the underlying stream reader
     */
    public final boolean isRootCursor() {
        return (_baseDepth == 0);
    }

    /*
    /**********************************************************************
    /* Public API, accessing tracked elements
    /**********************************************************************
     */

    /**
     * @return Information about last "tracked" element; element we have
     *    last iterated over when tracking has been enabled.
     */
    public SMElementInfo getTrackedElement() {
        return _trackedElement;
    }

    /**
     * @return Information about the tracked element the parent cursor
     *    had, if parent cursor existed and was tracking element
     *    information.
     */
    public SMElementInfo getParentTrackedElement() {
        return _parentTrackedElement;
    }

    /*
    /**********************************************************************
    /* Public API, accessing current document state
    /**********************************************************************
     */

    /**
     * Method that can be used to check whether this cursor is
     * currently valid; that is, it is the cursor that points
     * to the event underlying stream is at. Only one cursor
     * at any given time is valid in this sense, although other
     * cursors may be made valid by advancing them (and by process
     * invalidating the cursor that was valid at that point).
     * It is also possible that none of cursors is valid at
     * some point: this is the case when formerly valid cursor
     * reached end of its content (END_ELEMENT).
     *
     * @return True if the cursor is currently valid; false if not
     */
    public final boolean readerAccessible() {
        return (_state == State.ACTIVE);
    }

    /**
     * Method that can be used to get direct access to the underlying
     * stream reader. Custom sub-classed versions (which can be constructed
     * by overriding this classes factory methods) can choose to block
     * such access, but the default implementation does allow access
     * to it.
     *<p>
     * Note that this method should not be needed (or extensively used)
     * for regular StaxMate usage, because direct access to the stream
     * may cause cursor's understanding of stream reader state to be
     * incompatible with its actual state.
     *
     * @return Stream reader the cursor uses for getting XML events
     */
    public final XMLStreamReader2 getStreamReader() {
        return _getStreamReader();
    }

    /**
     * Method to access starting Location of event (as defined by Stax
     * specification)
     * that this cursor points to.
     * Method can only be called if the
     * cursor is valid (as per {@link #readerAccessible}); if not,
     * an exception is thrown
     *
     * @return Location of the event this cursor points to
     */
    public Location getCursorLocation()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getCursorLocation");
        }
        return _context.getEventLocation();
    }

    /**
     * Method to access Location that the underlying stream reader points
     * to.
     *
     * @return Location of the event the underlying stream reader points
     *   to (independent of whether this cursor points to that event)
     */
    public Location getStreamLocation()
    {
        return _context.getStreamLocation();
    }

    /**
     * Same as calling {@link #getCursorLocation}
     *
     * @deprecated
     */
    @Deprecated
    public Location getLocation()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getLocation");
        }
        return _context.getStreamLocation();
    }

    /*
    /**********************************************************************
    /* Public API, accessing document text content
    /**********************************************************************
     */

    /**
     * Method that can be used when this cursor points to a textual
     * event; something for which {@link XMLStreamReader#getText} can
     * be called. Note that it does not advance the cursor, or combine
     * multiple textual events.
     *
     * @return Textual content of the current event that this cursor
     *   points to, if any
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (possibly including event type not being of textual
     *   type, see Stax 1.0 specs for details); or if this cursor does
     *   not currently point to an event.
     */
    public String getText()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getText");
        }
        return _streamReader.getText();
    }

    /**
     * Method that can collect all text contained within START_ELEMENT
     * currently pointed by this cursor. Collection is done recursively
     * through all descendant text (CHARACTER, CDATA; optionally SPACE) nodes,
     * ignoring nodes of other types. After collecting text, cursor
     * will be positioned at the END_ELEMENT matching initial START_ELEMENT
     * and thus needs to be advanced to access the next sibling event.
     *
     * @param includeIgnorable Whether text for events of type SPACE should
     *   be ignored in the results or not. If false, SPACE events will be
     *   skipped; if true, white space will be included in results.
     */
    public String collectDescendantText(boolean includeIgnorable)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("collectDescendantText");
        }
        SMFilter f = includeIgnorable
            ? SMFilterFactory.getTextOnlyFilter()
            : SMFilterFactory.getNonIgnorableTextFilter();
        SMInputCursor childIt = descendantCursor(f);

        /* Cursor should only return actual text nodes, so no type
         * checks are needed, except for checks for EOF. But we can
         * also slightly optimize things, by avoiding StringBuilder
         * construction if there's just one node.
         */
        if (childIt.getNext() == null) {
            return "";
        }
        String text = childIt.getText(); // has to be a text event
        if (childIt.getNext() == null) {
            return text;
        }

        XMLStreamReader2 sr = childIt._getStreamReader();
        int size = text.length() + sr.getTextLength()+ 20;
        StringBuilder sb = new StringBuilder(Math.max(size, 100));
        sb.append(text);
        do {
            // Let's assume char array access is more efficient...
            sb.append(sr.getTextCharacters(), sr.getTextStart(),
                      sr.getTextLength());
        } while (childIt.getNext() != null);

        return sb.toString();
    }

    /**
     * Same as calling {@link #collectDescendantText(boolean)} with 'false':
     * that is, do not include ignorable white space (as determined by DTD
     * or Schema) in the result text.
     *<p>
     * Note: it is not common to have have ignorable white space; it usually
     * results from indentation, but its detection requires DTD/schema-aware
     * processing
     *
     * @since 2.0.0
     */
    public final String collectDescendantText() throws XMLStreamException
    {
        return collectDescendantText(false);
    }

    /**
     * Method similar to {@link #collectDescendantText}, but will write
     * the text to specified Writer instead of collecting it into a
     * String.
     *
     * @param w Writer to use for outputting text found
     * @param includeIgnorable Whether text for events of type SPACE should
     *   be ignored in the results or not. If false, SPACE events will be
     *   skipped; if true, white space will be included in results.
     */
    public void processDescendantText(Writer w, boolean includeIgnorable)
        throws IOException, XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("processDescendantText");
        }
        SMFilter f = includeIgnorable
            ? SMFilterFactory.getTextOnlyFilter()
            : SMFilterFactory.getNonIgnorableTextFilter();
        SMInputCursor childIt = descendantCursor(f);

        // Any text in there?
        XMLStreamReader2 sr = childIt._getStreamReader();
        while (childIt.getNext() != null) {
            /* 'true' indicates that we are not to lose the text contained
             * (can call getText() multiple times, idempotency). While this
             * may not be as efficient as allowing content to be discarded,
             * let's play it safe. Another method could be added for
             * the alternative (fast but dangerous) behaviour as needed.
             */
            sr.getText(w, true);
        }
    }

    /*
    /**********************************************************************
    /* Public API, accessing current element information
    /**********************************************************************
     */

    public QName getQName()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getQName");
        }
        return _streamReader.getName();
    }

    /**
     * For events with fully qualified names (START_ELEMENT, END_ELEMENT,
     * ATTRIBUTE, NAMESPACE) returns the local component of the full
     * name; for events with only non-qualified name (PROCESSING_INSTRUCTION,
     * entity and notation declarations, references) returns the name, and
     * for other events, returns null.
     *
     * @return Local component of the name
     */
    public String getLocalName()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getLocalName");
        }
        switch (getCurrEventCode()) {
        case XMLStreamConstants.START_ELEMENT:
        case XMLStreamConstants.END_ELEMENT:
        case XMLStreamConstants.ENTITY_REFERENCE:
            return _streamReader.getLocalName();
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
            return _streamReader.getPITarget();
        case XMLStreamConstants.DTD:
            {
                DTDInfo dtd = _streamReader.getDTDInfo();
                return (dtd == null) ? null : dtd.getDTDRootName();
            }
        }

        return null;
    }

    /**
     * Method for accessing namespace prefix of the START_ELEMENT this
     * cursor points to.
     *
     * @return Prefix of currently pointed-to START_ELEMENT,
     *   if it has one; "" if none
     *
     * @throws XMLStreamException if cursor does not point to START_ELEMENT
     */
    public String getPrefix()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getPrefix");
        }
        String prefix = _streamReader.getPrefix();
        // some impls may return null instead, let's convert
        return (prefix == null) ? "" : prefix;
    }

    /**
     * Method for accessing namespace URI of the START_ELEMENT this
     * cursor points to.
     *
     * @return Namespace URI of currently pointed-to START_ELEMENT,
     *   if it has one; "" if none
     *
     * @throws XMLStreamException if cursor does not point to START_ELEMENT
     */
    public String getNsUri()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getNsUri");
        }
        String uri = _streamReader.getNamespaceURI();
        // some impls may return null instead, let's convert
        return (uri == null) ? "" : uri;
    }

    /**
     * Returns a String representation of either the fully-qualified name
     * (if the event has full name) or the local name (if event does not
     * have full name but has local name); or if no name available, throws
     * stream exception.
     */
    public String getPrefixedName()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getPrefixedName");
        }
        return _streamReader.getPrefixedName();
    }

    /**
     * Method for verifying whether current named event (one for which
     * {@link #getLocalName} can be called)
     * has the specified local name or not.
     *
     * @return True if the local name associated with the event is
     *   as expected
     */
    public boolean hasLocalName(String expName)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("hasName");
        }
        if (expName == null) {
            throw new IllegalArgumentException("Can not pass null name to method");
        }
        String name = getLocalName();
        return (name != null) && expName.equals(name);
    }

    /**
     * Method for verifying whether current named event (one for which
     * {@link #getLocalName} can be called) has the specified
     * fully-qualified name or not.
     * Both namespace URI and local name must match for the result
     * to be true.
     *
     * @return True if the fully-qualified name associated with the event is
     *   as expected
     */
    public boolean hasName(String expNsURI, String expLN)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("hasName");
        }

        int type = getCurrEventCode();
        String uri;
        String ln;

        switch (type) {
        case XMLStreamConstants.START_ELEMENT:
        case XMLStreamConstants.END_ELEMENT:
            ln = _streamReader.getLocalName();
            uri = _streamReader.getNamespaceURI();
            break;

        case XMLStreamConstants.ENTITY_REFERENCE:
            ln = _streamReader.getLocalName();
            uri = null;
            break;
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
            ln = _streamReader.getPITarget();
            uri = null;
            break;
        case XMLStreamConstants.DTD:
            {
                DTDInfo dtd = _streamReader.getDTDInfo();
                ln = (dtd == null) ? null : dtd.getDTDRootName();
            }
            uri = null;
            break;
        default:
            return false;
        }

        if (ln == null || !ln.equals(expLN)) {
            return false;
        }
        if (expNsURI == null || expNsURI.length() == 0) { // no namespace
            return (uri == null) || (uri.length() == 0);
        }

        return (uri != null) && expNsURI.equals(uri);
    }

    /**
     * Equivalent to calling {@link #hasName(String, String)} with
     * namespace URI and local name contained in the argument QName
     *
     * @param qname Name to compare name of current event (if any)
     *   against.
     * 
     * @since 2.0
     */
    public boolean hasName(QName qname)
        throws XMLStreamException
    {
        return hasName(qname.getNamespaceURI(), qname.getLocalPart());
    }

    /**
     * Method for accessing information regarding event this
     * cursor points to, as an instance of {@link XMLEvent}.
     * Depending on underlying input source this may be constructed
     * from scratch (for example, if the input cursor was not
     *  constructed from
     * {@link javax.xml.stream.XMLEventReader}), or accessed
     * from the underlying event reader.
     *<p>
     * Calling this method does not advance the underlying stream
     * or cursor itself.
     *<p>
     * Note, too, that it is ok to call this method at any time:
     * if the cursor is not in valid state for accessing information
     * null will be returned.
     *
     * @return Information about currently pointed-to input stream
     *   event, if we are pointing to one; null otherwise
     *
     * @since 2.0.0
     */
    public XMLEvent asEvent()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            return null;
        }
        return _context.currentAsEvent();
    }

    /*
    /**********************************************************************
    /* Public API, accessing current element's attribute information
    /**********************************************************************
     */

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and which will return number of attributes with values for the
     * start element. This includes both explicit attribute values and
     * possible implied default values (when DTD support is enabled
     * by the underlying stream reader).
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT)
     */
    public int getAttrCount()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrCount");
        }
        return _streamReader.getAttributeCount();
    }

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and which will return index of specified attribute, if it
     * exists for this element. If not, -1 is returned to denote "not found".
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT)
     */
    public int findAttrIndex(String uri, String localName)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrCount");
        }
        // As of stax2 v3.0 (TypedXMLStreamReader), we have this method:
        return _streamReader.getAttributeIndex(uri, localName);
    }

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and returns fully qualified name
     * of the attribute at specified index.
     * Index has to be between [0, {@link #getAttrCount}[; otherwise
     * {@link IllegalArgumentException} will be thrown.
     * 
     * @param index Index of the attribute
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT),
     *   or if invalid attribute 
     * @throws IllegalArgumentException if attribute index is invalid
     *   (less than 0 or greater than the last valid index
     *   [getAttributeCount()-1])
     */
    public QName getAttrName(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrName");
        }
        return _streamReader.getAttributeName(index);
    }

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and returns local name
     * of the attribute at specified index.
     * Index has to be between [0, {@link #getAttrCount}[; otherwise
     * {@link IllegalArgumentException} will be thrown.
     * 
     * @param index Index of the attribute
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT),
     *   or if invalid attribute 
     * @throws IllegalArgumentException if attribute index is invalid
     *   (less than 0 or greater than the last valid index
     *   [getAttributeCount()-1])
     */
    public String getAttrLocalName(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrLocalName");
        }
        return _streamReader.getAttributeLocalName(index);
    }

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and returns namespace prefix
     * of the attribute at specified index (if it has any), or
     * empty String if attribute has no prefix (does not belong to
     * a namespace).
     * Index has to be between [0, {@link #getAttrCount}[; otherwise
     * {@link IllegalArgumentException} will be thrown.
     * 
     * @param index Index of the attribute
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT),
     *   or if invalid attribute 
     * @throws IllegalArgumentException if attribute index is invalid
     *   (less than 0 or greater than the last valid index
     *   [getAttributeCount()-1])
     */
    public String getAttrPrefix(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrPrefix");
        }
        String prefix = _streamReader.getAttributePrefix(index);
        // some impls may return null instead, let's convert
        return (prefix == null) ? "" : prefix;
    }

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and returns namespace URI
     * of the attribute at specified index (non-empty String if it has
     * one, and empty String if attribute does not belong to a namespace)
     * Index has to be between [0, {@link #getAttrCount}[; otherwise
     * {@link IllegalArgumentException} will be thrown.
     * 
     * @param index Index of the attribute
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT),
     *   or if invalid attribute 
     * @throws IllegalArgumentException if attribute index is invalid
     *   (less than 0 or greater than the last valid index
     *   [getAttributeCount()-1])
     */
    public String getAttrNsUri(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrNsUri");
        }
        String uri = _streamReader.getAttributeNamespace(index);
        // some impls may return null instead, let's convert
        return (uri == null) ? "" : uri;
    }

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and returns unmodified textual value
     * of the attribute at specified index (non-empty String if it has
     * one, and empty String if attribute does not belong to a namespace)
     * Index has to be between [0, {@link #getAttrCount}[; otherwise
     * {@link IllegalArgumentException} will be thrown.
     * 
     * @param index Index of the attribute
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT),
     *   or if invalid attribute 
     * @throws IllegalArgumentException if attribute index is invalid
     *   (less than 0 or greater than the last valid index
     *   [getAttributeCount()-1])
     */
    public String getAttrValue(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttributeValue");
        }
        return _streamReader.getAttributeValue(index);
    }

    /**
     * Convenience accessor method to access an attribute that is
     * not in a namespace (has no prefix). Equivalent to
     * calling {@link #getAttrValue(String,String)} with
     * 'null' for 'namespace URI' argument
     */
    public String getAttrValue(String localName)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttributeValue");
        }
        /* If we are to believe StAX specs, null would mean "do not
         * check namespace" -- that's pretty much never what anyone
         * really wants (or, at least should use), so let's pass
         * "" to indicate "no namespace"
         */
        /* 16-Jun-2008, tatu: Alas, Sun sjsxp doesn't seem to work
         *   well if we do pass "" instead of null! Since Woodstox
         *   works ok with both, let's use null -- specs are irrelevant
         *   if no implementation follows this particular quirk.
         */
        //return _streamReader.getAttributeValue("", localName);
        return _streamReader.getAttributeValue(null, localName);
    }

    /**
     * Method that can be called when this cursor points to START_ELEMENT,
     * and returns unmodified textual value
     * of the specified attribute (if element has it), or null if
     * element has no value for such attribute.
     * 
     * @param namespaceURI Namespace URI for the attribute, if any;
     *   empty String or null if none.
     * @param localName Local name of the attribute to access (in
     *   namespace-aware mode: in non-namespace-aware mode, needs to
     *   be the full name)
     *
     * @throws XMLStreamException if either the underlying parser has
     *   problems (cursor not valid or not pointing to START_ELEMENT),
     *   or if invalid attribute 
     */
    public String getAttrValue(String namespaceURI, String localName)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrValue");
        }
        return _streamReader.getAttributeValue(namespaceURI, localName);
    }

    /*
    /**********************************************************************
    /* Public API, Typed Access API for attributes
    /**********************************************************************
     */

    /**
     * Method for accessing value of specified attribute as boolean.
     * Method will only succeed if the attribute value is a valid
     * boolean, as specified by XML Schema specification (and hence
     * is accessible via Stax2 Typed Access API).
     *
     * @param index Index of attribute to access
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of boolean
     * @throws IllegalArgumentException If given attribute index is invalid
     */
    public boolean getAttrBooleanValue(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrBooleanValue");
        }
        // from Stax2 v3.0 Typed Access API:
        return _streamReader.getAttributeAsBoolean(index);
    }

    /**
     * Method for accessing value of specified attribute as boolean.
     * If attribute value is not a valid boolean
     * (as specified by XML Schema specification), will instead
     * return specified "default value".
     *
     * @param index Index of attribute to access
     * @param defValue Value to return if attribute value exists but
     *   is not a valid boolean value
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of boolean.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public boolean getAttrBooleanValue(int index, boolean defValue)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrBooleanValue");
        }
        // not the most efficient way, but should work:
        try {
            return _streamReader.getAttributeAsBoolean(index);
        } catch (TypedXMLStreamException e) {
            return defValue;
        }
    }

    /**
     * Method for accessing value of specified attribute as integer.
     * Method will only succeed if the attribute value is a valid
     * integer, as specified by XML Schema specification (and hence
     * is accessible via Stax2 Typed Access API).
     *
     * @param index Index of attribute to access
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of integer.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public int getAttrIntValue(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrIntValue");
        }
        // from Stax2 v3.0 Typed Access API:
        return _streamReader.getAttributeAsInt(index);
    }

    /**
     * Method for accessing value of specified attribute as integer.
     * If attribute value is not a valid integer
     * (as specified by XML Schema specification), will instead
     * return specified "default value".
     *
     * @param index Index of attribute to access
     * @param defValue Value to return if attribute value exists but
     *   is not a valid integer value
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of integer.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public int getAttrIntValue(int index, int defValue)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrIntValue");
        }
        // not the most efficient way, but should work:
        try {
            // from Stax2 v3.0 Typed Access API:
            return _streamReader.getAttributeAsInt(index);
        } catch (TypedXMLStreamException e) {
            return defValue;
        }
    }

    /**
     * Method for accessing value of specified attribute as long.
     * Method will only succeed if the attribute value is a valid
     * long, as specified by XML Schema specification (and hence
     * is accessible via Stax2 Typed Access API).
     *
     * @param index Index of attribute to access
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of long.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public long getAttrLongValue(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrLongValue");
        }
        return _streamReader.getAttributeAsLong(index);
    }

    /**
     * Method for accessing value of specified attribute as long.
     * If attribute value is not a valid long
     * (as specified by XML Schema specification), will instead
     * return specified "default value".
     *
     * @param index Index of attribute to access
     * @param defValue Value to return if attribute value exists but
     *   is not a valid long value
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of long.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public long getAttrLongValue(int index, long defValue)
        throws  XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrLongValue");
        }
        try {
            return _streamReader.getAttributeAsLong(index);
        } catch (TypedXMLStreamException e) {
            return defValue;
        }
    }

    /**
     * Method for accessing value of specified attribute as double.
     * Method will only succeed if the attribute value is a valid
     * double, as specified by XML Schema specification (and hence
     * is accessible via Stax2 Typed Access API).
     *
     * @param index Index of attribute to access
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of double.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public double getAttrDoubleValue(int index)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrDoubleValue");
        }
        return _streamReader.getAttributeAsDouble(index);
    }

    /**
     * Method for accessing value of specified attribute as double.
     * If attribute value is not a valid double
     * (as specified by XML Schema specification), will instead
     * return specified "default value".
     *
     * @param index Index of attribute to access
     * @param defValue Value to return if attribute value exists but
     *   is not a valid double value
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of double.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public double getAttrDoubleValue(int index, double defValue)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrDoubleValue");
        }
        try {
            return _streamReader.getAttributeAsDouble(index);
        } catch (TypedXMLStreamException e) {
            return defValue;
        }
    }

    /**
     * Method for accessing value of specified attribute as an
     * Enum value of specified type, if content non-empty.
     * If it is empty, will return null. And if non-empty value
     * is not equal to name() of one of Enum values, will throw
     * a {@link TypedXMLStreamException} to indicate the problem.
     *
     * @param index Index of attribute to access
     *
     * @throws XMLStreamException If specified attribute can not be
     *   accessed (due to cursor state), or if attribute value
     *   is not a valid textual representation of double.
     * @throws IllegalArgumentException If given attribute index
     *   is invalid
     */
    public <T extends Enum<T>> T getAttrEnumValue(int index, Class<T> enumType)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrEnumValue");
        }
        String value = _streamReader.getAttributeValue(index).trim();
        if (value.length() == 0) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException iae) {
            throw _constructTypedException(value, iae, "Invalid enumeration value '"+value+"'; not one of values of "+enumType.getName());
        }
    }

    /*
    /**********************************************************************
    /* Deprecated data access
    /**********************************************************************
     */

    /**
     * @deprecated Use combination of {@link #findAttrIndex} and
     *   {@link #getAttrIntValue(int)} instead.
     */
    @Deprecated
    public int getAttrIntValue(String uri, String localName)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrIntValue");
        }
        return getAttrIntValue(findAttrIndex(uri, localName));
    }

    /**
     * @deprecated Use combination of {@link #findAttrIndex} and
     *   {@link #getAttrIntValue(int,int)} instead.
     */
    @Deprecated
    public int getAttrIntValue(String uri, String localName, int defValue)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible("getAttrIntValue");
        }
        return getAttrIntValue(findAttrIndex(uri, localName), defValue);
    }

    /*
    /**********************************************************************
    /* Public API, Typed Access API for element values
    /**********************************************************************
     */

    /**
     * Method that can collect text <b>directly</b> contained within
     * START_ELEMENT currently pointed by this cursor.
     * This is different from {@link #collectDescendantText} in that
     * it does NOT work for mixed content
     * (child elements are not allowed:
     * comments and processing instructions are allowed and ignored
     * if encountered).
     * If any ignorable white space (as per schema, dtd or so) is encountered,
     * it will be ignored.
     *<p>
     * The main technical difference to  {@link #collectDescendantText} is
     * that this method tries to make use of Stax2 v3.0 Typed Access API,
     * if available, and can thus be more efficient.
     *
     * @throws XMLStreamException if content is not accessible; may also
     *  be thrown if child elements are encountered.
     */
    public String getElemStringValue()
        throws XMLStreamException
    {
        _verifyElemAccess("getElemStringValue");
        /* 14-Feb-2009, tatu: stream reader should point at matching
         *   END_ELEMENT after 'getElementXxx' call; and we must change
         *   our current event from START_ELEMENT to something else
         *   (otherwise we'll try to skip a sub-tree with next getNext()).
         *   Not sure if END_ELEMENT is the best choice, but seems to work ok.
         */
        String str = _streamReader.getElementText();
        _currEvent = SMEvent.END_ELEMENT;
        return str;
    }

    /**
     * Method that can collect text <b>directly</b> contained within
     * START_ELEMENT currently pointed by this cursor and convert
     * it to a boolean value.
     * For method to work, the value must be legal textual representation of
     *<b>boolean</b>
     * data type as specified by W3C Schema (as well as Stax2 Typed
     * Access API).
     * Element also can not contain mixed content (child elements;
     * comments and processing instructions are allowed and ignored
     * if encountered).
     *
     * @throws XMLStreamException if content is not accessible or
     *    convertible to required return type
     */
    public boolean getElemBooleanValue()
        throws XMLStreamException
    {
        _verifyElemAccess("getElemBooleanValue");
        // need to change curr event (see comments for getElemStringValue)
        _currEvent = SMEvent.END_ELEMENT;
        return _streamReader.getElementAsBoolean();
    }

    /**
     * Similar to {@link #getElemBooleanValue()}, but instead of failing
     * on invalid value, returns given default value.
     */
    public boolean getElemBooleanValue(boolean defValue)
        throws XMLStreamException
    {
        _verifyElemAccess("getElemBooleanValue");
        _currEvent = SMEvent.END_ELEMENT;
        // not optimal, but should work:
        try {
            return _streamReader.getElementAsBoolean();
        } catch (TypedXMLStreamException tse) {
            _ensureEndElement();
            return defValue;
        }
    }

    /**
     * Method that can collect text <b>directly</b> contained within
     * START_ELEMENT currently pointed by this cursor and convert
     * it to a int value.
     * For method to work, the value must be legal textual representation of
     *<b>int</b>
     * data type as specified by W3C Schema (as well as Stax2 Typed
     * Access API).
     * Element also can not contain mixed content (child elements;
     * comments and processing instructions are allowed and ignored
     * if encountered).
     *
     * @throws XMLStreamException if content is not accessible or
     *    convertible to required return type
     */
    public int getElemIntValue()
        throws XMLStreamException
    {
        _verifyElemAccess("getElemIntValue");
        _currEvent = SMEvent.END_ELEMENT;
        return _streamReader.getElementAsInt();
    }

    /**
     * Similar to {@link #getElemIntValue()}, but instead of failing
     * on invalid value, returns given default value.
     */
    public int getElemIntValue(int defValue)
        throws XMLStreamException
    {
        _verifyElemAccess("getElemIntValue");
        _currEvent = SMEvent.END_ELEMENT;
        try {
            return _streamReader.getElementAsInt();
        } catch (TypedXMLStreamException tse) {
            _ensureEndElement();
            return defValue;
        }
    }

    /**
     * Method that can collect text <b>directly</b> contained within
     * START_ELEMENT currently pointed by this cursor and convert
     * it to a long value.
     * For method to work, the value must be legal textual representation of
     *<b>long</b>
     * data type as specified by W3C Schema (as well as Stax2 Typed
     * Access API).
     * Element also can not contain mixed content (child elements;
     * comments and processing instructions are allowed and ignored
     * if encountered).
     *
     * @throws XMLStreamException if content is not accessible or
     *    convertible to required return type
     */
    public long getElemLongValue()
        throws XMLStreamException
    {
        _verifyElemAccess("getElemLongValue");
        _currEvent = SMEvent.END_ELEMENT;
        return _streamReader.getElementAsLong();
    }

    /**
     * Similar to {@link #getElemLongValue()}, but instead of failing
     * on invalid value, returns given default value.
     */
    public long getElemLongValue(long defValue)
        throws XMLStreamException
    {
        _verifyElemAccess("getElemLongValue");
        _currEvent = SMEvent.END_ELEMENT;
        try {
            return _streamReader.getElementAsLong();
        } catch (TypedXMLStreamException tse) {
            _ensureEndElement();
            return defValue;
        }
    }

    /**
     * Method that can collect text <b>directly</b> contained within
     * START_ELEMENT currently pointed by this cursor and convert
     * it to a double value.
     * For method to work, the value must be legal textual representation of
     *<b>double</b>
     * data type as specified by W3C Schema (as well as Stax2 Typed
     * Access API).
     * Element also can not contain mixed content (child elements;
     * comments and processing instructions are allowed and ignored
     * if encountered).
     *
     * @throws XMLStreamException if content is not accessible or
     *    convertible to required return type
     */
    public double getElemDoubleValue()
        throws XMLStreamException
    {
        _verifyElemAccess("getElemDoubleValue");
        _currEvent = SMEvent.END_ELEMENT;
        return _streamReader.getElementAsDouble();
    }

    /**
     * Similar to {@link #getElemDoubleValue()}, but instead of failing
     * on invalid value, returns given default value.
     */
    public double getElemDoubleValue(double defValue)
        throws XMLStreamException
    {
        _verifyElemAccess("getElemDoubleValue");
        _currEvent = SMEvent.END_ELEMENT;
        try {
            return _streamReader.getElementAsDouble();
        } catch (TypedXMLStreamException tse) {
            _ensureEndElement();
            return defValue;
        }
    }

    /**
     * Method that can collect text <b>directly</b> contained within
     * START_ELEMENT currently pointed by this cursor and convert
     * it to one of enumerated values of given type, if textual
     * value non-type, and otherwise to null.
     * If a non-empty value that is not one of legal enumerated values
     * is encountered, a {@link TypedXMLStreamException} is thrown.
     *<p>
     * Element also can not contain mixed content (child elements;
     * comments and processing instructions are allowed and ignored
     * if encountered).
     *
     * @throws XMLStreamException if content is not accessible or
     *    convertible to required return type
     * @throws TypedXMLStreamException if element value is non-empty
     *   and not one of allowed values for the enumeration type
     */
    public <T extends Enum<T>> T getElemEnumValue(Class<T> enumType)
        throws XMLStreamException
    {
        _verifyElemAccess("getElemEnumValue");
        _currEvent = SMEvent.END_ELEMENT;
        String value = _streamReader.getElementText().trim();
        if (value.length() == 0) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException iae) {
            throw _constructTypedException(value, iae, "Invalid enumeration value '"+value+"'; not one of values of "+enumType.getName());
        }
    }

    /**
     * Helper method called by getElemXxxValue methods to ensure that
     * the state is appropriate for the call
     */
    private final void _verifyElemAccess(String method)
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            throw _notAccessible(method);
        }
        if (getCurrEvent() != SMEvent.START_ELEMENT) {
            throw _wrongState(method, SMEvent.START_ELEMENT);
        }
    }

    /**
     * This method can be called to ensure that this cursor gets to
     * point to END_ELEMENT that closes typed element that has been
     * read; usually this works as expected, but during type conversion
     * exceptions handling may not work as expected
     */
    private final void _ensureEndElement()
        throws XMLStreamException
    {
        /* !!! 14-Feb-2009, tatu: not sure what exactly to do; so for now
         *   let's rather just assert we are at END_ELEMENT
         */
        int code = _streamReader.getEventType();
        if (code != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException("Excepted state to be END_ELEMENT, got "+eventObjectByEventId(code), _streamReader.getLocation());
        }
    }

    /*
    /**********************************************************************
    /* Public API, accessing extra application data
    /**********************************************************************
     */

    /**
     * Method for accessing application-provided data set previously
     * by a {@link #setData} call.
     */
    public Object getData() {
        return mData;
    }

    /**
     * Method for assigning per-cursor application-managed data,
     * readable using {@link #getData}.
     */
    public void setData(Object o) {
        mData = o;
    }

    /*
    /**********************************************************************
    /* Public API, iteration
    /**********************************************************************
     */

    /**
     * Main iterating method. Will try to advance the cursor to the
     * next visible event (visibility defined by the filter cursor
     * is configured with, if any), and return event type.
     * If no such events are available, will return null.
     *<p>
     * Note that one side-effect of calling this method is to invalidate
     * the child cursor, if one was active. This is done by iterating over
     * any events child cursor (and its descendants if any) might
     * expose.
     *
     * @return Type of event (from {@link XMLStreamConstants}, such as
     *   {@link XMLStreamConstants#START_ELEMENT}, if a new node was
     *   iterated over; <code>null</code> when there are no more
     *   nodes this cursor can iterate over.
     *
     * @throws XMLStreamException If there are underlying parsing
     *   problems.
     */
    public abstract SMEvent getNext()
        throws XMLStreamException;

    /**
     * Method that does what {@link #getNext()} does, but instead of
     * returning resulting event type, returns <b>this</b> cursor.
     * The main purpose of this method is to allow for chaining
     * calls. This is especially useful right after constructing
     * a root level cursor, and needing to advance it to point to
     * the root element. As such, a common idiom is:
     *<pre>
     * SMInputCursor positionedRoot = smFactory.rootElementCursor(file).advance();
     *</pre>
     * which both constructs the root element cursor, and positions it
     * over the root element. Can be similarly used with other kinds of
     * cursors as well, of course
     *
     * @since 2.0
     */
    public final SMInputCursor advance()
        throws XMLStreamException
    {
        getNext();
        return this;
    }

    /**
     * Method that will create a new nested cursor for iterating
     * over all (immediate) child nodes of the start element this cursor
     * currently points to that are passed by the specified filter.
     * If cursor does not point to a start element,
     * it will throw {@link IllegalStateException}; if it does not support
     * concept of child cursors, it will throw
     * {@link UnsupportedOperationException}
     *
     * @param f Filter child cursor is to use for filtering out
     *    'unwanted' nodes; may be null if no filtering is to be done
     *
     * @throws IllegalStateException If cursor can not be created due
     *   to the state cursor is in.
     * @throws UnsupportedOperationException If cursor does not allow
     *   creation of child cursors.
     */
    public SMInputCursor childCursor(SMFilter f)
        throws XMLStreamException
    {
        if (_state != State.ACTIVE) {
            if (_state == State.HAS_CHILD) {
                throw new IllegalStateException("Child cursor already requested.");
            }
            throw new IllegalStateException("Can not iterate children: cursor does not point to a start element (state "+getStateDesc()+")");
        }
        if (_currEvent != SMEvent.START_ELEMENT) {
            throw new IllegalStateException("Can not iterate children: cursor does not point to a start element (pointing to "+_currEvent+")");
        }

        _childCursor = constructChildCursor(f);
        _state = State.HAS_CHILD;
        return _childCursor;
    }

    /**
     * Method that will create a new nested cursor for iterating
     * over all (immediate) child nodes of the start element this cursor
     * currently points to.
     * If cursor does not point to a start element,
     * it will throw {@link IllegalStateException}; if it does not support
     * concept of child cursors, it will throw
     * {@link UnsupportedOperationException}
     *
     * @throws IllegalStateException If cursor can not be created due
     *   to the state cursor is in.
     * @throws UnsupportedOperationException If cursor does not allow
     *   creation of child cursors.
     */
    public final SMInputCursor childCursor()
        throws XMLStreamException
    {
        return childCursor(null);
    }

    /**
     * Method that will create a new nested cursor for iterating
     * over all the descendant (children and grandchildren) nodes of
     * the start element this cursor currently points to
     * that are accepted by the specified filter.
     * If cursor does not point to a start element,
     * it will throw {@link IllegalStateException}; if it does not support
     * concept of descendant cursors, it will throw
     * {@link UnsupportedOperationException}
     *
     *
     * @param f Filter child cursor is to use for filtering out
     *    'unwanted' nodes; may be null if no filtering is to be done
     *
     * @throws IllegalStateException If cursor can not be created due
     *   to the state cursor is in (or for some cursors, if they never
     *   allow creating such cursors)
     * @throws UnsupportedOperationException If cursor does not allow
     *   creation of descendant cursors.
     */
    public SMInputCursor descendantCursor(SMFilter f)
        throws XMLStreamException
    {
        if (_state != State.ACTIVE) {
            if (_state == State.HAS_CHILD) {
                throw new IllegalStateException("Child cursor already requested.");
            }
            throw new IllegalStateException("Can not iterate children: cursor does not point to a start element (state "+getStateDesc()+")");
        }
        if (_currEvent != SMEvent.START_ELEMENT) {
            throw new IllegalStateException("Can not iterate children: cursor does not point to a start element (pointing to "+_currEvent+")");
        }

        _childCursor = constructDescendantCursor(f);
        _state = State.HAS_CHILD;
        return _childCursor;
    }

    /**
     * Method that will create a new nested cursor for iterating
     * over all the descendant (children and grandchildren) nodes of
     * the start element this cursor currently points to.
     * If cursor does not point to a start element,
     * it will throw {@link IllegalStateException}; if it does not support
     * concept of descendant cursors, it will throw
     * {@link UnsupportedOperationException}
     *
     * @throws IllegalStateException If cursor can not be created due
     *   to the state cursor is in (or for some cursors, if they never
     *   allow creating such cursors)
     * @throws UnsupportedOperationException If cursor does not allow
     *   creation of descendant cursors.
     */
    public final SMInputCursor descendantCursor()
        throws XMLStreamException
    {
        return descendantCursor(null);
    }

    /**
     * Convenience method; equivalent to 
     *<code>childCursor(SMFilterFactory.getElementOnlyFilter());</code>
     */
    public final SMInputCursor childElementCursor()
        throws XMLStreamException
    {
        return childCursor(SMFilterFactory.getElementOnlyFilter());
    }

    /**
     * Convenience method; equivalent to 
     *<code>childCursor(SMFilterFactory.getElementOnlyFilter(elemName));</code>
     * Will only return START_ELEMENT and END_ELEMENT events, whose element
     * name matches given qname.
     */
    public final SMInputCursor childElementCursor(QName elemName)
        throws XMLStreamException
    {
        return childCursor(SMFilterFactory.getElementOnlyFilter(elemName));
    }

    /**
     * Convenience method; equivalent to 
     *<code>childCursor(SMFilterFactory.getElementOnlyFilter(elemName));</code>
     * Will only return START_ELEMENT and END_ELEMENT events, whose element's
     * local name matches given local name, and that does not belong to
     * a namespace.
     */
    public final SMInputCursor childElementCursor(String elemLocalName)
        throws XMLStreamException
    {
        return childCursor(SMFilterFactory.getElementOnlyFilter(elemLocalName));
    }

    /**
     * Convenience method; equivalent to 
     *<code>descendantCursor(SMFilterFactory.getElementOnlyFilter());</code>
     */
    public final SMInputCursor descendantElementCursor()
        throws XMLStreamException
    {
        return descendantCursor(SMFilterFactory.getElementOnlyFilter());
    }

    /**
     * Convenience method; equivalent to 
     *<code>descendantCursor(SMFilterFactory.getElementOnlyFilter(elemName));</code>
     * Will only return START_ELEMENT and END_ELEMENT events, whose element
     * name matches given qname.
     */
    public final SMInputCursor descendantElementCursor(QName elemName)
        throws XMLStreamException
    {
        return descendantCursor(SMFilterFactory.getElementOnlyFilter(elemName));
    }

    /**
     * Convenience method; equivalent to 
     *<code>descendantCursor(SMFilterFactory.getElementOnlyFilter(elemLocalName));</code>.
     * Will only return START_ELEMENT and END_ELEMENT events, whose element
     * local name matches given local name, and that do not belong to a
     * namespace
     */
    public final SMInputCursor descendantElementCursor(String elemLocalName)
        throws XMLStreamException
    {
        return descendantCursor(SMFilterFactory.getElementOnlyFilter(elemLocalName));
    }

    /**
     * Convenience method; equivalent to 
     *<code>childCursor(SMFilterFactory.getMixedFilter());</code>
     */
    public final SMInputCursor childMixedCursor()
        throws XMLStreamException
    {
        return childCursor(SMFilterFactory.getMixedFilter());
    }

    /**
     * Convenience method; equivalent to 
     *<code>descendantCursor(SMFilterFactory.getMixedFilter());</code>
     */
    public final SMInputCursor descendantMixedCursor()
        throws XMLStreamException
    {
        return descendantCursor(SMFilterFactory.getMixedFilter());
    }

    /*
    /**********************************************************************
    /* Public API, convenience methods for exception construction
    /**********************************************************************
     */

    /**
     * Method for constructing stream exception with given message,
     * and location that matches that of the underlying stream
     *<b>regardless of whether this cursor is valid</b> (that is,
     * will indicate location of the stream which may differ from
     * where this cursor was last valid)
     */
    public XMLStreamException constructStreamException(String msg)
    {
        // !!! TODO: use StaxMate-specific sub-classes of XMLStreamException?
        return new XMLStreamException(msg, getStreamLocation());
    }

    /**
     * Method for constructing and throwing stream exception with given
     * message. Equivalent to throwing exception that
     * {@link #constructStreamException} constructs and returns.
     */
    public void throwStreamException(String msg)
        throws XMLStreamException
    {
        throw constructStreamException(msg);
    }

    /*
    /**********************************************************************
    /* Public API, dev-readable descs
    /**********************************************************************
     */

    /**
     * Method that generates developer-readable description of
     * the logical path of the event this cursor points to,
     * assuming that <b>element tracking</b> is enabled.
     * If it is, a path description will be constructed; if not,
     * result will be "." ("unspecified current location").
     *<p>
     * Note: while results look similar to XPath expressions,
     * they are not accurate (or even valid) XPath. 
     * This is partly because of filtering, and partly because
     * of differences between element/node index calculation.
     * The idea is to make it easier to get reasonable idea
     * of logical location, in addition to physical input location.
     */
    public String getPathDesc()
    {
        /* Need to start with parent, since current element may
         * or may not exist (depeneding on traversal)?
         */
        SMElementInfo parent = getParentTrackedElement();
        // Not tracking, or not just yet advanced?
        if (parent == null && getElementTracking() == Tracking.NONE) {
            return ".";
        }
        StringBuilder sb = new StringBuilder(100);

        appendPathDesc(sb, parent, true);

        /* Let's indicate index of the current node; whether to indicate
         * via element or node index depend on whether it's a start/end
         * element (and one for which we have info) or not
         */
        SMElementInfo curr = getTrackedElement();
        if (curr != null && getCurrEvent() == SMEvent.START_ELEMENT) {
            appendPathDesc(sb, _trackedElement, false);
        } else {
            sb.append("/*[n").append(getNodeCount()).append(']');
        }
        return sb.toString();
    }

    private static void appendPathDesc(StringBuilder sb, SMElementInfo info,
                                       boolean recursive)
    {
        if (info == null) {
            return;
        }
        if (recursive) {
            appendPathDesc(sb, info.getParent(), true);
        }
        sb.append('/');
        String prefix = info.getPrefix();
        if (prefix != null && prefix.length() > 0) {
            sb.append(prefix);
            sb.append(':');
        }
        sb.append(info.getLocalName());
        // and let's indicate relative element-index of the element
        sb.append("[e").append(info.getElementIndex()).append(']');
    }

    protected String getCurrEventDesc() {
        return (_currEvent == null) ? "[null]" : _currEvent.toString();
    }

    /**
     * Overridden implementation will just display description of
     * the event this cursor points to (or last pointed to, if not
     * valid)
     */
    @Override
        public String toString() {
        return "[Cursor that point(s/ed) to: "+getCurrEventDesc()+"]";
    }

    /*
    /**********************************************************************
    /* Methods sub-classes need or can override
    /* to customize behaviour:
    /**********************************************************************
     */

    /**
     * Method cursor calls when it needs to track element state information;
     * if so, it calls this method to take a snapshot of the element.
     *<p>
     * Note caller already suppresses calls so that this method is only
     * called when information needs to be preserved. Further, previous
     * element is only passed if such linkage is to be preserved (reason
     * for not always doing it is the increased memory usage).
     *<p>
     * Finally, note that this method does NOT implement
     * {@link ElementInfoFactory}, as its signature does not include the
     * cursor argument, as that's passed as this pointer already.
     */
    protected SMElementInfo constructElementInfo(SMElementInfo parent,
                                                 SMElementInfo prevSibling)
        throws XMLStreamException
    {
        if (mElemInfoFactory != null) {
            return mElemInfoFactory.constructElementInfo(this, parent, prevSibling);
        }
        XMLStreamReader2 sr = _streamReader;
        return new DefaultElementInfo(parent, prevSibling,
                                      sr.getPrefix(), sr.getNamespaceURI(), sr.getLocalName(),
                                      _nodeCount-1, _elemCount-1, getParentCount());
    }

    /**
     * Abstract method that concrete sub-classes implement, and is used
     * for all instantiation of child cursors by this cursor instance.
     *<p>
     * Note that custom cursor implementations can be used by overriding
     * this method.
     */
    protected abstract SMInputCursor constructChildCursor(SMFilter f)
        throws XMLStreamException;

    /**
     * Abstract method that concrete sub-classes implement, and is used
     * for all instantiation of descendant cursors by this cursor instance.
     *<p>
     * Note that custom cursor implementations can be used by overriding
     * this method.
     */
    protected abstract SMInputCursor constructDescendantCursor(SMFilter f)
        throws XMLStreamException;
}
