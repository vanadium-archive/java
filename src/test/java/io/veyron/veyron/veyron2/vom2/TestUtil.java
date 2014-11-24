package io.veyron.veyron.veyron2.vom2;

import io.veyron.veyron.veyron2.vdl.VdlType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;

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

    static Object decode(byte[] bytes, Type targetType) throws Exception {
        BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(bytes));
        return decoder.decodeValue(targetType);
    }

    static String encode(VdlType type, Object value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = new BinaryEncoder(out);
        encoder.encodeValue(type, value);
        return TestUtil.bytesToHexString(out.toByteArray());
    }
}
