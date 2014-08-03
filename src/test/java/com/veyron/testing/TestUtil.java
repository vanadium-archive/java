
package com.veyron.testing;

import junit.framework.Assert;

public class TestUtil {
    public static <T> void assertArrayEquals(T[] arr1, T[] arr2) {
        assertArrayEquals(null, arr1, arr2);
    }

    public static <T> void assertArrayEquals(String message, T[] arr1, T[] arr2) {
        String messageWithSpace = "";
        if (message != null) {
            messageWithSpace = message + " ";
        }
        Assert.assertNotNull(messageWithSpace + "first array non-null", arr1);
        Assert.assertNotNull(messageWithSpace + "second array non-null", arr2);
        Assert.assertEquals(messageWithSpace + "arrays have equal length (lengths are "
                + arr1.length + " and " + arr2.length + ")", arr1.length, arr2.length);
        for (int i = 0; i < arr1.length; i++) {
            Assert.assertEquals(messageWithSpace + "" + i + "th array element equal (values are "
                    + arr1[i] + " and " + arr2[i] + ")", arr1[i], arr2[i]);
        }
    }
}
