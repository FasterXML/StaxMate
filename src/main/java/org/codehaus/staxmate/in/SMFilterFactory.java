package org.codehaus.staxmate.in;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;

/**
 * Simple factory that can be used to get instances of simple default
 * filters.
 *
 * @author Tatu Saloranta
 */
public final class SMFilterFactory
    implements XMLStreamConstants // just to use type consts
{
    final static SimpleFilter sNonIgnorableTextFilter;
    static {
        sNonIgnorableTextFilter = new SimpleFilter
            ((1 << CDATA) | (1 << CHARACTERS));
    }

    final static SimpleFilter sTextOnlyFilter;
    static {
        sTextOnlyFilter = sNonIgnorableTextFilter.extend((1 << SPACE), 0);
    }

    /**
     * Filter that only passes through element events. Since end events
     * are in general only passed by flattening iterators, this won't
     * mean that nested iterators get end element events.
     */
    final static SimpleFilter sElementOnlyFilter;
    static {
        sElementOnlyFilter = new SimpleFilter
            ((1 << START_ELEMENT) | (1 << END_ELEMENT));
    }

    final static SimpleFilter sMixedFilter;
    static {
        sMixedFilter = sTextOnlyFilter.extend
            (((1 << START_ELEMENT) | (1 << END_ELEMENT)), 0);
    }


    public final static SMFilter getTextOnlyFilter() {
        return sTextOnlyFilter;
    }

    public final static SMFilter getNonIgnorableTextFilter() {
        return sNonIgnorableTextFilter;
    }

    public final static SMFilter getElementOnlyFilter() {
        return sElementOnlyFilter;
    }

    public final static SMFilter getElementOnlyFilter(QName elemName) {
        return new ElementFilter(elemName);
    }

    public final static SMFilter getElementOnlyFilter(String elemLocalName) {
        return new ElementFilter(elemLocalName);
    }

    /**
     * @return Filter that will pass element events as well as all
     *    text events (including ignorable white space).
     */
    public final static SMFilter getMixedFilter() {
        return sMixedFilter;
    }
}
