package io.veyron.veyron.veyron2.vom2;

import org.junit.Assert;

import io.veyron.veyron.veyron2.vdl.VdlType;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TestUtil {
    static String bytesToHexString(byte[] dat) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : dat) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    static byte[] hexStringToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new RuntimeException("Hex strings must be multiples of 2 in length");
        }
        int outLen = hex.length() / 2;
        byte[] dat = new byte[outLen];
        for (int i = 0; i < outLen; i++) {
            dat[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return dat;
    }

    static void matchHexString(final String description, final String expectedExpression,
            final String actual) {
        String[] parts = expectedExpression.split("[\\[\\]]");
        String remainder = actual;
        for (String part : parts) {
            String[] possibilities = part.split(",");
            Set<String> toSee = new HashSet<String>(Arrays.asList(possibilities));
            while (!toSee.isEmpty()) {
                boolean foundMatch = false;
                for (String possibility : possibilities) {
                    if (remainder.startsWith(possibility)) {
                        remainder = remainder.substring(possibility.length());
                        toSee.remove(possibility);
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    break;
                }
            }
            if (!toSee.isEmpty()) {
                if (toSee.size() == 1) {
                    String nextExpected = toSee.iterator().next();
                    int maxMatch = 0;
                    for (; maxMatch < nextExpected.length() && maxMatch < remainder.length(); maxMatch++) {
                        if (nextExpected.charAt(maxMatch) != remainder.charAt(maxMatch)) {
                            break;
                        }
                    }
                    remainder = remainder.substring(maxMatch);
                }
                Assert.fail(description + " Hex string match failed. Matched up to: "
                        + actual.substring(0, actual.length() - remainder.length())
                        + ". Remainder: " + remainder);
                return;
            }
        }
        if (remainder.length() > 0) {
            Assert.fail(description + " Hex string match failed. Matched up to: "
                    + actual.substring(0, actual.length() - remainder.length()) + ". Remainder: "
                    + remainder);
        }
    }

    static String encode(VdlType type, Object value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = new BinaryEncoder(out);
        encoder.encodeValue(type, value);
        return TestUtil.bytesToHexString(out.toByteArray());
    }
}
