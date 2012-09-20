import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.out.*;

public class TestOutputBuffered
{
    public void test()
        throws XMLStreamException
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        final String NS_URI1 = "http://foo";
        final String NS_PREFIX2 = "myns";
        final String NS_URI2 = "urn://hihhei";

        SMNamespace ns1 = doc.getNamespace(NS_URI1);
        SMNamespace ns2 = doc.getNamespace(NS_URI2, NS_PREFIX2);

        final String COMMENT_CONTENT = "Comment!";
        doc.addComment(COMMENT_CONTENT);

        SMOutputElement elem = doc.addElement(ns1, "root");
        final String TEXT_CONTENT1 = "Rock & Roll";
        elem.addCharacters(TEXT_CONTENT1);
        SMBufferedFragment frag = elem.createBufferedFragment();
        elem.addBuffered(frag);
        final String TEXT_CONTENT2 = "[FRAG";
        frag.addCharacters(TEXT_CONTENT2);
        final String COMMENT_CONTENT2 = "!!!";
        frag.addComment(COMMENT_CONTENT2);
        frag.addElement(ns1, "tag");
        SMOutputElement elem2 = elem.addElement("branch");
        elem2.addElement(ns2, "leaf");
        final String TEXT_CONTENT3 = "ment!]";
        frag.addCharacters(TEXT_CONTENT3);
        frag.release();
        elem.addElement(ns2, "leaf2");
        doc.closeRoot();

        // Uncomment for debugging:
        System.out.println("Result:");
        System.out.println(sw.toString());
    }

    public static void main(String[] args)
        throws XMLStreamException
    {
        new TestOutputBuffered().test();
    }
}
