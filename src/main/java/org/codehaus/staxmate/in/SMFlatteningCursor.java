package org.codehaus.staxmate.in;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * Default implementation of generic flat (non-scoped) cursor; cursor
 * that traverse all descendants (children and grandchildren) of a start
 * element.
 *<p>
 * Differences to {@link SMHierarchicCursor} are:
 * <ul>
 *  <li>Flat cursors return {@link XMLStreamConstants#END_ELEMENT} nodes (except
 *    for the one that closes the outermost level), unless
 *    filtered out by the filter, whereas the nested cursor automatically
 *    leaves those out.
 *   </li>
 *  <li>Flat cursors can not have child/descendant cursors
 *   </li>
 * </ul> 
 *
 * @author Tatu Saloranta
 */
public class SMFlatteningCursor
    extends SMInputCursor
{

    /*
    ////////////////////////////////////////////
    // Life cycle, configuration
    ////////////////////////////////////////////
     */

    public SMFlatteningCursor(SMInputContext ctxt,SMInputCursor parent, SMFilter f)
    {
        super(ctxt, parent, f);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, accessing cursor state information
    ///////////////////////////////////////////////////
     */

    public int getParentCount()
    {
        /* First things first: if we have a child cursor, we can not
         * ask stream, since its depth depends on how far child
         * cursor has travelled. But it does know its base parent
         * count, which has to be one bigger than our parent count
         * at time when child cursor was created, which is the latest
         * node this cursor traversed over.
         */
        if (_childCursor != null) {
            return _childCursor.getBaseParentCount();
        }
        // No event yet, or we are closed? base depth is ok then
        if (_currEvent == null) {
            return _baseDepth;
        }

        /* Otherwise, stream's count can be used. However, it'll be
         * off by one for both START_ELEMENT and END_ELEMENT.
         */
        int depth = _streamReader.getDepth();
        if (_currEvent == SMEvent.START_ELEMENT
            || _currEvent == SMEvent.END_ELEMENT) {
            --depth;
        }
        return depth;
    }

    /*
    ////////////////////////////////////////////
    // Public API, iterating
    ////////////////////////////////////////////
     */

    public SMEvent getNext()
        throws XMLStreamException
    {
        if (_state == State.CLOSED) {
            return null;
        }

        /* If there is a child cursor, it has to be traversed
         * through
         */
        if (_state == State.HAS_CHILD) {
            // After this, we'll be located at END_ELEMENT
            rewindPastChild();
            _state = State.ACTIVE;
        } else if (_state == State.INITIAL) {
            _state = State.ACTIVE;
        } // nothing to do if we are active
        while (true) {
            int type;

            /* Root level has no end element; should always get END_DOCUMENT,
             * but let's be extra careful... (maybe there's need for fragment
             * cursors later on)
             */
            if (isRootCursor()) {
                if (!_streamReader.hasNext()) {
                    break;
                }
                type = _streamReader.next();
                /* Document end marker at root level is same as end
                 * element at inner levels...
                 */
                if (type == XMLStreamConstants.END_DOCUMENT) {
                    break;
                }
            } else {
                type = _streamReader.next();
            }

            ++_nodeCount;

            if (type == XMLStreamConstants.END_ELEMENT) {
                /* Base depth was depth at START_ELEMENT, Stax2.getDepth()
                 * will return identical value for END_ELEMENT (and
                 * <= used instead of < just for sanity checking)
                 */
                int depth = _streamReader.getDepth();
                if (depth <= _baseDepth) {
                    if (depth != _baseDepth) {
                        _throwWrongEndElem(_baseDepth, depth);
                    }
                    break;
                }
            } else if (type == XMLStreamConstants.START_ELEMENT) {
                ++_elemCount;

                /* !!! 24-Oct-2007, tatus: This sanity check really
                 *   shouldn't be needed any more... but let's leave
                 *   it for time being
                 */
            } else if (type == XMLStreamConstants.END_DOCUMENT) {
                throw new IllegalStateException("Unexpected END_DOCUMENT encountered (root = "+isRootCursor()+")");
            }

            SMEvent evt = eventObjectByEventId(type);
            _currEvent = evt;

            // Ok, are we interested in this event?
            if (mFilter != null && !mFilter.accept(evt, this)) {
                // Nope, let's just skip over

                // May still need to create the tracked element?
                if (type == XMLStreamConstants.START_ELEMENT) { 
                    if (mElemTracking == Tracking.ALL_SIBLINGS) {
                        _trackedElement = constructElementInfo
                            (_parentTrackedElement, _trackedElement);
                    }
                }
                continue;
            }

            // Need to update tracked element?
            if (type == XMLStreamConstants.START_ELEMENT
                && mElemTracking != Tracking.NONE) {
                SMElementInfo prev = (mElemTracking == Tracking.PARENTS) ?
                    null : _trackedElement;
                _trackedElement = constructElementInfo
                    (_parentTrackedElement, prev);
            }
            return evt;
        }

        // Ok, no more events
        _state = State.CLOSED;
        _currEvent = null;
        return null;
    }

    public SMInputCursor constructChildCursor(SMFilter f) {
        return new SMHierarchicCursor(_context, this, f);
    }

    public SMInputCursor constructDescendantCursor(SMFilter f) {
        return new SMFlatteningCursor(_context, this, f);
    }
}
