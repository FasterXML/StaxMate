package org.codehaus.staxmate.out;

import java.io.*;

import org.codehaus.staxmate.SMOutputFactory;

/**
 * Tests for the heuristic indentation ("pretty-printing") feature,
 * enabled via {@link SMOutputContainer#setIndentation}.
 */
public class TestIndentation
    extends BaseWriterTest
{
    /**
     * Basic test: a nested element tree (no mixed content) should get
     * a newline plus increasing indentation for each level.
     */
    public void testNestedIndentation()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);
        // Newline + up to 30 spaces available; start at offset 1 (just the
        // newline), add 2 spaces per nesting level:
        doc.setIndentation("\n                              ", 1, 2);

        SMOutputElement root = doc.addElement("root");
        SMOutputElement branch = root.addElement("branch");
        branch.addElement("leaf");
        root.addElement("branch2");
        doc.closeRoot();

        String xml = trimDecl(sw.toString());
        assertEquals(
                "<root>\n"
                +"  <branch>\n"
                +"    <leaf/>\n"
                +"  </branch>\n"
                +"  <branch2/>\n"
                +"</root>",
                xml);
    }

    /**
     * When an element holds text (mixed content), indentation is
     * heuristically suppressed for that element so the text is not
     * altered.
     */
    public void testIndentationSuppressedByText()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);
        doc.setIndentation("\n                              ", 1, 2);

        SMOutputElement root = doc.addElement("root");
        SMOutputElement leaf = root.addElement("leaf");
        leaf.addCharacters("text");
        doc.closeRoot();

        String xml = trimDecl(sw.toString());
        // 'leaf' is indented (child of root), but its text content means
        // no indentation is added around the text:
        assertEquals(
                "<root>\n"
                +"  <leaf>text</leaf>\n"
                +"</root>",
                xml);
    }

    /**
     * Verifies that the per-level 'step' is honored (here: 1 char per level).
     */
    public void testIndentationStep()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        SMOutputDocument doc = createSimpleDoc(sw);
        doc.setIndentation("\n.........", 1, 1);

        SMOutputElement root = doc.addElement("root");
        SMOutputElement branch = root.addElement("branch");
        branch.addElement("leaf");
        doc.closeRoot();

        String xml = trimDecl(sw.toString());
        assertEquals(
                "<root>\n"
                +".<branch>\n"
                +"..<leaf/>\n"
                +".</branch>\n"
                +"</root>",
                xml);
    }

    /**
     * Strips the leading XML declaration (if any) so the body can be
     * compared regardless of how the underlying Stax impl emits the prolog.
     */
    private String trimDecl(String xml)
    {
        if (xml.startsWith("<?")) {
            int ix = xml.indexOf("?>");
            if (ix > 0) {
                xml = xml.substring(ix+2);
            }
        }
        return xml.trim();
    }
}
