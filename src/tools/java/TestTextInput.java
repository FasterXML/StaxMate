import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.*;

public class TestTextInput
{
    private TestTextInput() { }

    public void test(String filename)
        throws IOException, XMLStreamException
    {
        FileInputStream fin = new FileInputStream(filename);
        XMLInputFactory fact = SMInputFactory.getGlobalXMLInputFactory();
        XMLStreamReader sr = fact.createXMLStreamReader(fin);

        SMInputCursor crsr = SMInputFactory.rootElementCursor(sr);
        crsr.getNext();
        readThrough(crsr);

        sr.close();
        fin.close();
    }

    private void readThrough(SMInputCursor parentCrsr)
        throws XMLStreamException
    {
        QName pn = parentCrsr.getQName();
        System.out.println("<"+pn+">");
        //SMInputCursor crsr = parentCrsr.childCursor(SMFilterFactory.getTextOnlyFilter());
        SMInputCursor crsr = parentCrsr.descendantCursor(SMFilterFactory.getTextOnlyFilter());
        SMEvent type;
        while ((type = crsr.getNext()) != null) {
            System.out.print("["+type+"]");
            if (type.hasText()) {
                System.out.print(" '"+crsr.getText()+"'");
            }
            System.out.println();
        }
        System.out.println("</"+pn+">");
    }

    public static void main(String[] args)
        throws IOException, XMLStreamException
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [input-file]");
            System.exit(1);
        }
        new TestTextInput().test(args[0]);
    }
}
