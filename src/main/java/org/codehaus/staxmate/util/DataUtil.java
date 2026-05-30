package org.codehaus.staxmate.util;

/**
 * Utility class that contains methods for simple data conversions.
 */
public final class DataUtil
{
    private DataUtil() { }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    public static String ensureNotEmpty(String value)
    {
        value = trim(value);
        if (value == null) {
            throw new IllegalArgumentException("Missing/empty value");
        }
        return value;
    }

    public static String trim(String value)
    {
        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                return value;
            }
        }
        return null;
    }
}
