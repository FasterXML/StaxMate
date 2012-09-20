package org.codehaus.staxmate.in;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.typed.TypedXMLStreamException;

/**
 * Abstract base class that contains non-public methods to be used by
 * public sub-classes ({@link SMInputCursor} and its sub-classes).
 *
 * @author Tatu Saloranta
 */
abstract class CursorBase
{
    /*
    /**********************************************************************
    /* Constants, initial cursor state
    /**********************************************************************
     */

    // // // Constants for the cursor state

    /**
     * State constants are used for keeping track of state of individual
     * cursors.
     */
    public enum State {
        /**
         * Initial means that the cursor has been constructed, but hasn't
         * yet been advanced. No data can be accessed yet, but the cursor
         * can be advanced.
         */
        INITIAL,

        /**
         * Active means that cursor's data is valid and can be accessed;
         * plus it can be advanced as well.
         */
        ACTIVE,

        /**
         * Status that indicates that although cursor would be active, there
         * is a child cursor active which means that this cursor can not
         * be used to access data: only the innermost child cursor can.
         * It can still be advanced, however.
         */
        HAS_CHILD,

        /**
         * Closed cursors are ones that do not point to accessible data, nor
         * can be advanced any further.
         */
        CLOSED
    }

    /*
    /**********************************************************************
    /* Constants, other
    /**********************************************************************
     */

    /**
     * This is the mapping array, indexed by Stax 1.0 event type integer
     * code, value being matching {@link SMEvent} enumeration value.
     */
    private final static SMEvent[] EVENTS_BY_IDS =
        SMEvent.constructIdToEventMapping();

    /*
    /**********************************************************************
    /* Iteration state
    /**********************************************************************
     */

    protected final SMInputContext _context;

    /**
     * Underlying stream reader used. It will either be a native
     * {@link XMLStreamReader2} instance, or a regular (Stax 1.0)
     * {@link javax.xml.stream.XMLStreamReader} wrapped in an
     * adapter.
     */
    protected final XMLStreamReader2 _streamReader;

    /**
     * Depth the underlying stream reader had when this cursor was
     * created (which is the number of currently open parent elements).
     * 0 only for root cursor.
     */
    protected final int _baseDepth;

    /**
     * Current state of the cursor.
     */
    protected State _state = State.INITIAL;

    /**
     * Event that this cursor currently points to, if valid, or
     * it last pointed to if not (including null if cursor has not
     * yet been advanced).
     */
    protected SMEvent _currEvent = null;

    /**
     * Number of nodes iterated over by this cursor, including the
     * current one.
     */
    protected int _nodeCount = 0;

    /**
     * Number of start elements iterated over by this cursor, including the
     * current one.
     */
    protected int _elemCount = 0;

    /**
     * Element that was last "tracked"; element over which cursor was
     * moved, and of which state has been saved for further use. At this
     * point, it can be null if no elements have yet been iterater over.
     * Alternatively, if it's not null, it may be currently pointed to
     * or not; if it's not, either child cursor is active, or this
     * cursor points to a non-start-element node.
     */
    protected SMElementInfo _trackedElement = null;

    /**
     * Element that the parent of this cursor tracked (if any),
     * when this cursor was created.
     */
    protected SMElementInfo _parentTrackedElement = null;

    /**
     * Cursor that has been opened for iterating child nodes of the
     * start element node this cursor points to. Needed to keep
     * cursor hierarchy synchronized, independent of which ones are
     * traversed.
     */
    protected SMInputCursor _childCursor = null;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * @param baseDepth Base depth (number of enclosing open start elements)
     *   of the underlying stream at point when this cursor was instantiated
     */
    protected CursorBase(SMInputContext ctxt, int baseDepth)
    {
        _context = ctxt;
        _streamReader = ctxt.getStreamReader();
        _baseDepth = baseDepth;
    }

    /*
    /**********************************************************************
    /* Methods we need from sub-class
    /**********************************************************************
     */

    /**
     * @return Developer-readable description of the event this cursor
     *    currently points to.
     */
    protected abstract String getCurrEventDesc();

    /**
     * @return True if this cursor iterates over root level of
     *   the underlying stream reader
     */
    public abstract boolean isRootCursor();

    public abstract XMLStreamException constructStreamException(String msg);

    public abstract void throwStreamException(String msg)
        throws XMLStreamException;

    /*
    /**********************************************************************
    /* Methods sub-classes need or can override
    /* to customize behaviour:
    /**********************************************************************
     */

    /**
     * This method is needed by flattening cursors when they
     * have child cursors: if so, they can determine their
     * depth relative to child cursor's base parent count
     * (and can not check stream -- as it may have moved --
     * nor want to have separate field to track this information)
     */
    protected final int getBaseParentCount() {
        return _baseDepth;
    }

    /**
     * Method called to skim through the content that the child
     * cursor(s) are pointing to, end return once next call to
     * XMLStreamReader2.next() will return the next event
     * this cursor should see.
     */
    protected final void rewindPastChild()
        throws XMLStreamException
    {
        final SMInputCursor child = _childCursor;
        _childCursor = null;

        child.invalidate();

        /* Base depth to match is always known by the child in question,
         * so let's ask it (hierarchic cursor parent also knows it)
         */
        final int endDepth = child.getBaseParentCount();
        final XMLStreamReader2 sr = _streamReader;

        for (int type = sr.getEventType(); true; type = sr.next()) {
            if (type == XMLStreamConstants.END_ELEMENT) {
                int depth = sr.getDepth();
                if (depth > endDepth) {
                    continue;
                }
                if (depth != endDepth) { // sanity check
                    _throwWrongEndElem(endDepth, depth);
                }
                break;
            } else if (type == XMLStreamConstants.END_DOCUMENT) {
                /* 18-Nov-2010, This check causes unit test failures, esp. 3 for "TestTyped",
                 *    for "non-stax2-native" parser (Sjsxp).
                 *    It should not; but commenting out following check can help...
                 */
                /*
                if (this.isRootCursor()) {
                    break;
                }
                */
                _throwUnexpectedEndDoc();
            }
        }
    }


    /**
     * Method called by the parent cursor, to indicate it has to
     * traverse over xml content and that child cursor as well
     * as all of its descendant cursors (if any) are to be
     * considered invalid.
     */
    protected void invalidate()
        throws XMLStreamException
    {
        _state = State.CLOSED;
        _currEvent = null;

        // child cursor(s) to delegate skipping to?
        if (_childCursor != null) {
            _childCursor.invalidate();
            _childCursor = null;
        }
    }

    /*
    /**********************************************************************
    /* Package methods
    /**********************************************************************
     */

    /**
     * Internal method (but available to sub-classes) that allows
     * access to the underlying stream reader.
     */
    protected final XMLStreamReader2 _getStreamReader() {
        return _streamReader;
    }

    /**
     *<p>
     * Note: no checks are done regarding validity of passed-in
     * type.
     *
     * @return {@link SMEvent} matching given type
     */
    protected final static SMEvent eventObjectByEventId(int type)
    {
        return EVENTS_BY_IDS[type];
    }

    /**
     * Internal method for throwing a stream exception that indicates
     * that given method can not be called because the cursor does
     * not point to event of expected type. This can be either because
     * cursor is invalid (doesn't point to any event), or because
     * it points to "wrong" event type. Distinction is reflected
     * in the exception message.
     */
    protected XMLStreamException _notAccessible(String method)
        throws XMLStreamException
    {
        if (_childCursor != null) {
            return constructStreamException("Can not call '"+method+"(): cursor does not point to a valid node, as it has an active open child cursor.");
        }
        return constructStreamException("Can not call '"+method+"(): cursor does not point to a valid node (curr event "+getCurrEventDesc()+"; cursor state "
                                        +getStateDesc()+")");
    }

    protected XMLStreamException _wrongState(String method, SMEvent expState)
        throws XMLStreamException
    {
        return constructStreamException("Can not call '"+method+"()' when cursor is not positioned over "+expState+" but "+currentEventStr()); 
    }

    protected String getStateDesc() {
        return _state.toString();
    }

    /**
     * Method for constructing human-readable description of the event
     * this cursor points to (if cursor valid) or last pointed to (if
     * not valid; possibly null if cursor has not yet been advanced).
     *
     * @return Human-readable description of the underlying Stax event
     *   this cursor points to.
     */
    protected String currentEventStr()
    {
        return (_currEvent == null) ? "null" : _currEvent.toString();
    }

    void _throwUnexpectedEndDoc()
        throws XMLStreamException
    {
        throw new IllegalStateException("Unexpected END_DOCUMENT encountered (root = "+isRootCursor()
                +"; reader impl "+_getStreamReader().getClass().getName()+"); location: "
                +_streamReader.getLocation());
    }

    void _throwWrongEndElem(int expDepth, int actDepth)
        throws IllegalStateException
    {
        throw new IllegalStateException("Expected to encounter END_ELEMENT with depth >= "+expDepth+", got "+actDepth);
    }

    TypedXMLStreamException _constructTypedException(String value, IllegalArgumentException rootCause, String msg)
    {
        return new TypedXMLStreamException(value, msg, _getStreamReader().getLocation(), rootCause);
    }

    @Override
    public String toString() {
        return "{" + getClass().getName()+": "+_state+", curr evt: "
            +_currEvent+"}";
    }
}
