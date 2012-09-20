package org.codehaus.staxmate.util;

import java.lang.reflect.Array;

/**
 * Simple helper class used to create compact array presentations where
 * the eventual size is not initially known.
 */
public class ArrayMaker
{
    private Object[] mData = new Object[16];

    private int mLastIndex = -1;

    public ArrayMaker() { }

    public void addEntry(int index, Object value)
    {
        if (index >= mData.length) {
            int size = mData.length;
            size += size;
            if (size <= index) {
                size = index + size;
            }
            mData = new Object[size];
        }
        mData[index] = value;
        if (index > mLastIndex) {
            mLastIndex = index;
        }
    }

    /**
     * @param result Array in which results are to be added (if it's
     *   big enough); or if not big enough, one that indicates type of
     *   the array to create. If null, a generic Object[] is created,
     *   used and returned.
     */
    public Object[] toArray(Object[] result)
    {
        int size = (mLastIndex < 0) ? 0 : mLastIndex;

        if (result == null) { // no type, have to use base Object...
            result = new Object[size];
        } else if (result.length < mLastIndex) {
            result = (Object[]) Array.newInstance(result.getClass().getComponentType(), size);
        }
        if (size > 0) {
            System.arraycopy(mData, 0, result, 0, size);
        }
        return result;
    }
}

