package org.codehaus.staxmate.in;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.ri.evt.Stax2EventAllocatorImpl;

/**
 * Class that encapsulates details about context in which StaxMate input
 * is done. The most important of the details is the stream reader to use
 * (since that is eventually invoked to do the real output), and its
 * properties.
 */
public final class SMInputContext
{
    final XMLStreamReader2 _streamReader;

    protected XMLEventAllocator _allocator;

    public SMInputContext(XMLStreamReader2 sr)
    {
        _streamReader = sr;
    }

    public XMLEvent currentAsEvent()
        throws XMLStreamException
    {
        if (_allocator == null) {
            _allocator = new Stax2EventAllocatorImpl();
        }
        return _allocator.allocate(_streamReader);
    }

    protected XMLStreamReader2 getStreamReader() { return _streamReader; }

    /*
    /**********************************************************************
    /* Public API: access to location information
    /**********************************************************************
     */

    public int getDepth()
    {
        return _streamReader.getDepth();
    }

    /**
     * Method for accessing starting location of the currently
     * pointed-to event, within input stream. 
     */
    public Location getEventLocation()
    {
        // Let's try to get actual exact location via Stax2 first:
        LocationInfo li = _streamReader.getLocationInfo();
        if (li != null) {
            Location loc = li.getStartLocation();
            if (loc != null) {
                return loc;
            }
        }
        // If not, fall back to regular method
        return _streamReader.getLocation();
    }

    /**
     * Method for accessing the currently pointed-to location
     * within input stream. May be useful for indicating error
     * location, for example.
     */
    public Location getStreamLocation()
    {
        // Let's try to get actual exact location via Stax2 first:
        LocationInfo li = _streamReader.getLocationInfo();
        if (li != null) {
            Location loc = li.getCurrentLocation();
            if (loc != null) {
                return loc;
            }
        }
        // If not, fall back to regular method
        return _streamReader.getLocation();
    }
}

