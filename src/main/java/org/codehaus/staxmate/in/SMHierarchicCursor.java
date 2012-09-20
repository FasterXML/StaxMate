package org.codehaus.staxmate.in;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Default implementation of generic nested (scoped) cursor; cursor that only
 * traverses direct children of a single start element.
 * 
 * @author Tatu Saloranta
 */
public class SMHierarchicCursor
    extends SMInputCursor
{
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public SMHierarchicCursor(SMInputContext ctxt, SMInputCursor parent, SMFilter f)
    {
        super(ctxt, parent, f);
    }

    /*
    /**********************************************************************
    /* Public API, accessing cursor state information
    /**********************************************************************
     */

    public int getParentCount() {
        return _baseDepth;
    }

    /*
    /**********************************************************************
    /* Public API, iterating
    /**********************************************************************
     */

    public SMEvent getNext()
        throws XMLStreamException
    {
        if (_state == State.CLOSED) {
            return null;
        }
        // If there is a child cursor, it has to be traversed through
        if (_state == State.HAS_CHILD) {
            // After this, we'll be located at END_ELEMENT
            rewindPastChild();
            _state = State.ACTIVE;
        } else if (_state == State.INITIAL) {
            _state = State.ACTIVE;
        } else { // active
            // If we had a start element, need to skip the subtree...
            if (_currEvent == SMEvent.START_ELEMENT) {
                skipToEndElement();
            }
        }

        while (true) {
            int type;
            
            // Root level has no end element...
            if (isRootCursor()) {
                if (!_streamReader.hasNext()) {
                    break;
                }
                type = _streamReader.next();
                /* Document end marker at root level is same as end element
                 * at inner levels...
                 */
                if (type == XMLStreamConstants.END_DOCUMENT) {
                    break;
                }
            } else {
                type = _streamReader.next();
            }
            ++_nodeCount;
            if (type == XMLStreamConstants.END_ELEMENT) {
                break;
            }
            if (type == XMLStreamConstants.START_ELEMENT) {
                ++_elemCount;
            } else if (type == XMLStreamConstants.END_DOCUMENT) {
                // just a sanity check; shouldn't really be needed
                _throwUnexpectedEndDoc();
            }
            SMEvent evt = eventObjectByEventId(type);
            _currEvent = evt;
            
            // Ok, are we interested in this event?
            if (mFilter != null && !mFilter.accept(evt, this)) {
                /* Nope, let's just skip over; but we may still need to
                 * create the tracked element?
                 */
                if (type == XMLStreamConstants.START_ELEMENT) {
                    if (mElemTracking == Tracking.ALL_SIBLINGS) {
                        _trackedElement = constructElementInfo(_parentTrackedElement, _trackedElement);
                    }
                    skipToEndElement();
                }
                continue;
            }
            
            // Need to update tracked element?
            if (type == XMLStreamConstants.START_ELEMENT && mElemTracking != Tracking.NONE) {
                SMElementInfo prev = (mElemTracking == Tracking.PARENTS) ? null : _trackedElement;
                _trackedElement = constructElementInfo(_parentTrackedElement, prev);
            }
            return evt;
        }

        // Ok, no more events
        _state = State.CLOSED;
        _currEvent = null;
        return null;
    }

    public SMInputCursor constructChildCursor(SMFilter f)
    {
        return new SMHierarchicCursor(_context, this, f);
    }

    public SMInputCursor constructDescendantCursor(SMFilter f)
    {
        return new SMFlatteningCursor(_context, this, f);
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    /**
     * Method called when current event/token is START_ELEMENT, but
     * we are not interested in its contents (children). Hence, needs
     * to skip all intervening events/tokens until matching END_ELEMENT
     * is encountered.
     */
    protected void skipToEndElement()
        throws XMLStreamException
    {
        XMLStreamReader2 sr = _streamReader;
        /* Here we have two choices: first, depth of current START_ELEMENT should
         * match that of matching END_ELEMENT. Additionally, START_ELEMENT's depth
         * for hierarchic cursors must be baseDepth+1.
         */
        //int endDepth = sr.getDepth();
        int endDepth = _baseDepth+1;

        while (true) {
            int type = sr.next();
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
                /* This is just a sanity check, to give more meaningful
                 * error messages in case something weird happens
                 */
                _throwUnexpectedEndDoc();
            }
        }
    }
}
