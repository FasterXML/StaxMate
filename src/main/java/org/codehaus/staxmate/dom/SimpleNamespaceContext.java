//https://stackoverflow.com/questions/6390339/how-to-query-xml-using-namespaces-in-java-with-xpath
package org.codehaus.staxmate.dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;

public class SimpleNamespaceContext implements NamespaceContext {
    
    private final Map<String, String> PREF_MAP = new HashMap<String, String>();
    
    public SimpleNamespaceContext(final Map<String, String> prefMap) {
        PREF_MAP.putAll(prefMap);
    }
    
    public String getNamespaceURI(String prefix) {
        return PREF_MAP.get(prefix);
    }
    
    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }
    
    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
}
