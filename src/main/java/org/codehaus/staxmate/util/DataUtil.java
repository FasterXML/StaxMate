package org.codehaus.staxmate.util;

import java.util.*;

/**
 * Utility class that contains methods for simple data conversions.
 */
public final class DataUtil
{
    final static HashMap<String,Boolean> sBoolValues;
    static {
        sBoolValues = new HashMap<String,Boolean>();
        sBoolValues.put("true", Boolean.TRUE);
        sBoolValues.put("false", Boolean.FALSE);
        /* Note: as per XML Schema, "0" (false) and "1" (true) are
         * also valid boolean values.
         */
        sBoolValues.put("0", Boolean.TRUE);
        sBoolValues.put("1", Boolean.FALSE);
    }

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
