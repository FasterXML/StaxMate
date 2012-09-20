package org.codehaus.staxmate.in;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMFilterFactory;
import org.codehaus.staxmate.in.SMInputCursor;

/**
 * Set of test cases to verify that the nested cursors handle traversal
 * as expected, when only partially traversing cursors.
 *
 * @author egentry
 */
public class TestPartialTraversal
    extends ReaderTestBase
{
  SMInputCursor a_iterator;

  final static String XML =
"<?xml version='1.0' encoding='UTF-8'?>\n"
+"<A1>\n"
+"  <A1B1>\n"
+"    <A1B1C1>\n"
+"      <A1B1C1D1></A1B1C1D1>\n"
+"      <A1B1C1D2></A1B1C1D2>\n"
+"    </A1B1C1>\n"
+"    <A1B1C2>\n"
+"      <A1B1C2D1></A1B1C2D1>\n"
+"      <A1B1C2D2></A1B1C2D2>\n"
+"    </A1B1C2>\n"
+"  </A1B1>\n"
+"  <A1B2>\n"
+"    <A1B2C1> </A1B2C1>\n"
+"    <A1B2C2> </A1B2C2>\n"
+"    <A1B2C3> </A1B2C3>\n"
+"  </A1B2>\n"
+"</A1>"
     ;

  public void setUp() throws XMLStreamException
  {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLStreamReader parser = factory.createXMLStreamReader(new java.io.StringReader(XML));
    a_iterator = SMInputFactory.hierarchicCursor(parser, SMFilterFactory.getElementOnlyFilter());
  }

  // The c_iterator reads A1B1C1
  // b_iterator is moved and reads A1B1C2 should read A1B2
  public void testOneLevelDeepPartialChildIteration() throws Exception
  {
    assertGotNextElementNamed(a_iterator, "A1");

    SMInputCursor b_iterator = a_iterator.childElementCursor();

    assertGotNextElementNamed(b_iterator, "A1B1");

    SMInputCursor c_iterator = b_iterator.childElementCursor();
    assertGotNextElementNamed(c_iterator, "A1B1C1");

    assertGotNextElementNamed(b_iterator, "A1B2");
    assertNull(b_iterator.getNext());

    assertNull(a_iterator.getNext());
  }
  
  // So what to do?  Examination reveals that when you are between a start element and an element,
  // in a child iterator, a call to skipSubTree reaches and end element first, decrementing depth 
  // to -1 and returning.  The next start element is going to be the next child at the same level
  // IF it exists
  
  // Solution: in those cases, skipSubTree needs to be called with depth 1.
  // Modified code with patch0 to make skipSubTree be called with an integer.
  // This correctly catches the case above, but as we suspected, what if 
  // you had a child, but hadn't traversed it?
  // True enough, State.INITIAL in skipTree needed a call to skipSubTree(0);
  // Some additional restructuring as you could not enter the
  // 'if (_state == State.HAS_CHILD)' line due to changing _state = State.CLOSED two lines earlier
  //  These changes result in patch1.

  // Works initially...
  // Works with patch1
  public void testOneLevelDeepChildCursorAcquisition() throws Exception
  {
    assertGotNextElementNamed(a_iterator, "A1");

    SMInputCursor b_iterator = a_iterator.childElementCursor();

    assertGotNextElementNamed(b_iterator,"A1B1");

    // Edge case 1...
    // Get child cursor but leave in State.INITIAL...
    // Skip tree needs to start with initial depth value of 0...
    b_iterator.childElementCursor();
    
    assertGotNextElementNamed(b_iterator,"A1B2");
    assertNull(b_iterator.getNext());

    assertNull(a_iterator.getNext());
  }

  // Now however, in certain cases the pointer points too high.
  // It's fine to move C after d, but for some reason b breaks.
  // More checking and I'm not sure why, but my suspicion was that 
  // in skipTree, the HAS_CHILD State was falling through to skipSubTree(1)
  // and needed to be skipSubTree(0)
  // Added that change and everything works... so far...
  public void testTwoLevelDeepPartialCursorIteration() throws Exception
  {
    
    assertGotNextElementNamed(a_iterator,"A1");
    
    SMInputCursor b_iterator = a_iterator.childElementCursor();
    assertGotNextElementNamed(b_iterator,"A1B1");

    SMInputCursor c_iterator = b_iterator.childElementCursor();
    assertGotNextElementNamed(c_iterator,"A1B1C1");
    
    SMInputCursor d_iterator = c_iterator.childElementCursor();
    assertGotNextElementNamed(d_iterator,"A1B1C1D1");
    
    assertGotNextElementNamed(b_iterator,"A1B2");
    assertNull(b_iterator.getNext());
    
    assertNull(a_iterator.getNext());

  }
  
  // One more try just to be safe, two level up move, without moving the d_iterator
  public void testTwoLevelDeepChildCursorAcquisition() throws Exception
  {
    assertGotNextElementNamed(a_iterator,"A1");
    
    SMInputCursor b_iterator = a_iterator.childElementCursor();
    assertGotNextElementNamed(b_iterator,"A1B1");

    SMInputCursor c_iterator = b_iterator.childElementCursor();
    assertGotNextElementNamed(c_iterator,"A1B1C1");
    
    /*SMInputCursor d_iterator =*/ c_iterator.childElementCursor();
    
    assertGotNextElementNamed(b_iterator,"A1B2");
    assertNull(b_iterator.getNext());
    
    assertNull(a_iterator.getNext());
  }
  
  // It works, is everything right with the world!?
  
  public void assertGotNextElementNamed(SMInputCursor cursor, String name) throws Exception
  {
    assertNotNull(cursor.getNext());
    assertEquals(name, cursor.getLocalName());
  }


}
