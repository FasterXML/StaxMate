package org.codehaus.staxmate.in;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.staxmate.util.ArrayMaker;

public enum SMEvent
{
    // First, document start/end event:

    START_DOCUMENT(XMLStreamConstants.START_DOCUMENT),
    END_DOCUMENT(XMLStreamConstants.END_DOCUMENT),
        
        // Then start/end tag

    START_ELEMENT(XMLStreamConstants.START_ELEMENT),
    END_ELEMENT(XMLStreamConstants.END_ELEMENT),

        // Textual events

    TEXT(XMLStreamConstants.CHARACTERS),
    CDATA(XMLStreamConstants.CDATA),
    IGNORABLE_WS(XMLStreamConstants.SPACE),

        // Other first-class XML events:

    COMMENT(XMLStreamConstants.COMMENT),
    PROCESSING_INSTR(XMLStreamConstants.PROCESSING_INSTRUCTION),

        // DTD-related events (entities, notations)

    DOCTYPE_DECL(XMLStreamConstants.DTD),
    ENTITY_DECL(XMLStreamConstants.ENTITY_DECLARATION),
    ENTITY_REF(XMLStreamConstants.ENTITY_REFERENCE),
    NOTATION_DECL(XMLStreamConstants.NOTATION_DECLARATION),

        /* Other auxiliary events specific by StAX specs
         * (but not usually directly returned by parsers)
         */

    ATTRIBUTE(XMLStreamConstants.ATTRIBUTE),
    NAMESPACE_DECL(XMLStreamConstants.NAMESPACE),

        /**
         * This is a placeholder event which should never be encountered during
         * normal operation. It is only used if an underlying event is of
         * unrecognized type, ie. application-specific extension StaxMate is
         * not aware of.
         */
    UNKNOWN(0)
    ;

    /*
    //////////////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////////////
     */

    /**
     * This is the underlying Stax 1.0 event constant matching this
     * StaxMate event enum.
     */
    private final int mEventType;

    SMEvent(int evtType)
    {
        mEventType = evtType;
    }

    /*
    //////////////////////////////////////////////////////////////////
    // Public API, accessors:
    //////////////////////////////////////////////////////////////////
     */

    /**
     * Set of events for which {@link #isTextualEvent} returns true
     */
    final private static int MASK_TEXTUAL_EVENT = 
        (1 << XMLStreamConstants.CHARACTERS)
        | (1 << XMLStreamConstants.CDATA)
        | (1 << XMLStreamConstants.SPACE)
    ;

    /**
     * Set of events for which {@link #hasText} returns true
     */
    final private static int MASK_HAS_TEXT = 
        // first pure textual events
        MASK_TEXTUAL_EVENT
        // then various declarations and references
        | (1 << XMLStreamConstants.ENTITY_DECLARATION)
        | (1 << XMLStreamConstants.ENTITY_REFERENCE)
        | (1 << XMLStreamConstants.NOTATION_DECLARATION)
        // and then other main-level elements with textual part:
        | (1 << XMLStreamConstants.DTD)
        | (1 << XMLStreamConstants.COMMENT)
        | (1 << XMLStreamConstants.PROCESSING_INSTRUCTION)
        /* ... also, types StaxMate does not use, but that should
         * have textual value if they were used:
         */
        | (1 << XMLStreamConstants.ATTRIBUTE)
        | (1 << XMLStreamConstants.NAMESPACE)
    ;

    /**
     * Set of events for which {@link #hasQName} returns true;
     * generally anything for which real QName could be constructed.
     */
    final private static int MASK_HAS_QNAME = 
        // Elements have QNames:
        (1 << XMLStreamConstants.START_ELEMENT)
        | (1 << XMLStreamConstants.END_ELEMENT)
        // And if attributes/ns-decls were used, they too would have it:
        | (1 << XMLStreamConstants.ATTRIBUTE)
        | (1 << XMLStreamConstants.NAMESPACE)
    ;

    /**
     * Set of events for which {@link #hasLocalName} returns true.
     * It is a superset of those for which {@link #hasQName} returns
     * true, since things like processing instructions, declarations,
     * references, and even DOCTYPE declrations have identifiers that
     * can be thought of as local names.
     */
    final private static int MASK_HAS_LOCAL_NAME = 
        MASK_HAS_QNAME 
        // Ok, decls/refs first:
        | (1 << XMLStreamConstants.ENTITY_DECLARATION)
        | (1 << XMLStreamConstants.ENTITY_REFERENCE)
        | (1 << XMLStreamConstants.NOTATION_DECLARATION)
        // And PIs, DOCTYPE declaration have ids (target, root name) too:
        | (1 << XMLStreamConstants.PROCESSING_INSTRUCTION)
        | (1 << XMLStreamConstants.DTD)
        ;

    /**
     * @return Underlying Stax 1.0 event code (int) that matches this
     *   event enumeration object.
     */
    public int getEventCode() { return mEventType; }

    // Element-properties:

    /**
     * @return True, if the event is a start or end element event; false
     *    otherwise.
     */
    public boolean isElementEvent() {
        return (mEventType == XMLStreamConstants.START_ELEMENT
                || mEventType == XMLStreamConstants.END_ELEMENT);
    }

    // Textual-properties:

    /**
     * Textual events are events that consist of regular document text
     * content: ignorable white space, CDATA segments, and other text.
     * For these types, {@link XMLStreamReader#getText()} methods can
     * be called, and they can also be output using regular text
     * output methods.
     *<p>
     * Note that set of events for which this returns true is less than
     * that of {@link #hasText}; since this only includes "true" textual
     * events, not just events that have some associated text.
     */
    public boolean isTextualEvent() {
        return ((1 << mEventType) & MASK_TEXTUAL_EVENT) != 0;
    }

    /**
     * This method returns true if it would be ok to call
     * {@link org.codehaus.staxmate.in.SMInputCursor#getText()}
     * of the iterator object, when it is positioned over this
     * event.
     *<p>
     * Note that set of events for which this returns true is
     * bigger than for which {@link XMLStreamReader#hasText} returns
     * true; this because StaxMate has looser definition of contained
     * text. For example, true will be returned for Processing
     * Instructions, since the 'data' part of the processing instruction
     * is considered to be text by StaxMate.
     */
    public boolean hasText() {
        return ((1 << mEventType) & MASK_HAS_TEXT) != 0;
    }

    // Name-properties:

    /**
     * This method returns true if it would be ok to call
     * {@link org.codehaus.staxmate.in.SMInputCursor#getLocalName()}
     * of the iterator object, when it is positioned over this
     * event.
     */
    public boolean hasLocalName() {
        return ((1 << mEventType) & MASK_HAS_LOCAL_NAME) != 0;
    }

    /**
     * This method returns true if it would be ok to call
     * {@link org.codehaus.staxmate.in.SMInputCursor#getQName()}
     * of the iterator object, when it is positioned over this
     * event.
     */
    public boolean hasQName() {
        return ((1 << mEventType) & MASK_HAS_QNAME) != 0;
    }

    /*
    //////////////////////////////////////////////////////////////////
    // Package API
    //////////////////////////////////////////////////////////////////
     */

    /**
     * Method that will construct the mapping from event id int codes
     * (used by {@link XMLStreamReader} to actual {@link SMEvent}
     * enum values.
     */
    static SMEvent[] constructIdToEventMapping()
    {
        ArrayMaker arr = new ArrayMaker();

        SMEvent[] evts = new SMEvent[] {
            ATTRIBUTE,
            CDATA,
            TEXT,
            COMMENT,
            DOCTYPE_DECL,
            END_DOCUMENT,
            END_ELEMENT,
            ENTITY_DECL,
            ENTITY_REF,
            NAMESPACE_DECL,
            NOTATION_DECL,
            PROCESSING_INSTR,
            IGNORABLE_WS,
            START_DOCUMENT,
            START_ELEMENT,

            UNKNOWN
        };
        for (SMEvent evt : evts) {
            arr.addEntry(evt.getEventCode(), evt);
        }

        return (SMEvent[]) arr.toArray(new SMEvent[1]);
    }
}
