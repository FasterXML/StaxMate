import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.out.*;

public class TestOutputBuffered2
{
    private void test()
        throws XMLStreamException
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        //final String NS_URI1 = "http://foo";

        doc.setIndentation("\n ", 1, 2);
        //SMNamespace ns = doc.getNamespace(NS_URI1, "p1");
        SMOutputElement smoutput = doc.addElement("MyRoot");
        //SMBufferedElement smbuf = smoutput.createBufferedElement(ns,"Child1");
        SMBufferedElement smbuf = smoutput.createBufferedElement(null, "Child1");
        smbuf.addAttribute("status","active");
        smoutput.addAndReleaseBuffered(smbuf);
        //smoutput.addBuffered(smbuffrag);

        doc.closeRoot();

        // Uncomment for debugging:
        System.out.println("Result:");
        System.out.println(sw.toString());
    }

    public static void main(String[] args)
        throws XMLStreamException
    {
        new TestOutputBuffered2().test();
    }
}
