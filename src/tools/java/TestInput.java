import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.*;

public class TestInput
{
    private TestInput() { }

    public void test(String filename)
        throws IOException, XMLStreamException
    {
        FileInputStream fin = new FileInputStream(filename);
        XMLInputFactory fact = SMInputFactory.getGlobalXMLInputFactory();
        XMLStreamReader sr = fact.createXMLStreamReader(fin);

        SMInputCursor crsr = SMInputFactory.rootCursor(sr);
        crsr.getNext();
        readThrough(crsr, 0);

        sr.close();
        fin.close();
    }

    private void readThrough(SMInputCursor crsr, int indent)
        throws XMLStreamException
    {
        crsr.setElementTracking(SMInputCursor.Tracking.PARENTS);

        for (SMEvent type = crsr.getCurrEvent(); type != null; type = crsr.getNext()) {
            System.out.print("["+indent+"] "+type+" ");
            System.out.print(" (path = \""+crsr.getPathDesc()+"\")");
            String name = type.hasLocalName() ? crsr.getLocalName() : "NO-NAME";
            System.out.println(name);

            if (type == SMEvent.START_ELEMENT) {
                SMInputCursor kidCrsr = crsr.childCursor();
                //SMInputCursor kidCrsr = crsr.childElementCursor();
                if (kidCrsr.getNext() != null) {
                    readThrough(kidCrsr, indent+1);
                }
                System.out.println("[/"+indent+"] END_ELEMENT");
            }
        }
    }

    public static void main(String[] args)
        throws IOException, XMLStreamException
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [input-file]");
            System.exit(1);
        }
        new TestInput().test(args[0]);
    }
}
