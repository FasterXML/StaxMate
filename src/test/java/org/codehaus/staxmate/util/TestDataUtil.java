package org.codehaus.staxmate.util;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link DataUtil} helper methods.
 */
public class TestDataUtil
    extends TestCase
{
    public void testTrim()
    {
        // null and "empty" (after trimming) values become null
        assertNull(DataUtil.trim(null));
        assertNull(DataUtil.trim(""));
        assertNull(DataUtil.trim("   "));
        assertNull(DataUtil.trim("\t\n "));

        // non-empty values are trimmed of surrounding white space
        assertEquals("abc", DataUtil.trim("abc"));
        assertEquals("abc", DataUtil.trim("  abc  "));
        assertEquals("a b", DataUtil.trim("\t a b \n"));
    }

    public void testEnsureNotEmpty()
    {
        assertEquals("abc", DataUtil.ensureNotEmpty("abc"));
        assertEquals("abc", DataUtil.ensureNotEmpty("  abc "));
    }

    public void testEnsureNotEmptyFailsOnNull()
    {
        try {
            DataUtil.ensureNotEmpty(null);
            fail("Expected IllegalArgumentException for null value");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().indexOf("empty") >= 0);
        }
    }

    public void testEnsureNotEmptyFailsOnBlank()
    {
        try {
            DataUtil.ensureNotEmpty("   ");
            fail("Expected IllegalArgumentException for blank value");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().indexOf("empty") >= 0);
        }
    }
}
