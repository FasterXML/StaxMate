import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.*;

public class TestTypedInput
{
    public static void main(String[] args)
        throws IOException, XMLStreamException
    {
        XMLInputFactory f0 = SMInputFactory.getGlobalXMLInputFactory();
        System.out.println("[note: Input factory == "+f0.getClass()+"]");
        SMInputFactory f = new SMInputFactory(f0);
        String XML =
            "<root><a>true</a><b>false</b></root>"
            ;
        SMInputCursor rc = f.rootElementCursor(new StringReader(XML)).advance();
        SMInputCursor c2 = rc.childElementCursor().advance();
        System.out.println("First child: "+c2.getQName());
        System.out.println("boolean: "+c2.getElemBooleanValue());
        System.out.println("Next token -> "+c2.getNext());
        System.out.println("Second child: "+c2.getQName());
        System.out.println("boolean: "+c2.getElemBooleanValue());
System.out.println("DEBUG: curr -> "+c2.getCurrEvent());
System.out.println("DEBUG: name -> "+c2.getStreamReader().getName());
        System.out.println("Next token (c2) -> "+c2.getNext());
System.out.println("DEBUG: curr -> "+rc.getCurrEvent());
System.out.println("DEBUG: name -> "+rc.getStreamReader().getName());
        System.out.println("Next token (root) -> "+rc.getNext());
    }
}
