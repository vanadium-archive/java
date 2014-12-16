package io.veyron.veyron.veyron2.util;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.vom2.BinaryDecoder;
import io.veyron.veyron.veyron2.vom2.BinaryEncoder;
import io.veyron.veyron.veyron2.vom2.ConversionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * VomUtil contains utilities used by the ipc package.
 */
public class VomUtil {
	/**
	 * VOM-encodes the provided value using a new VOM-encoder.
	 *
	 * @param  value           value to be encoded.
	 * @param  type            type of the provided value.
	 * @return                 VOM-encoded value as a byte array.
	 * @throws VeyronException if there was an error encoding the value.
	 */
	public static byte[] encode(Object value, Type type) throws VeyronException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final BinaryEncoder encoder = new BinaryEncoder(out);
		try {
			encoder.encodeValue(type, value);
		} catch (IOException e) {
			throw new VeyronException(e.getMessage());
		}
		return out.toByteArray();
	}

	/**
	 * VOM-encodes the provided value using a new VOM-encoder, returning a hex-encoded string.
	 *
	 * @param  value           value to be encoded.
	 * @param  type            type of the provided value.
	 * @return                 VOM-encoded value in a hex-string format.
	 * @throws VeyronException if there was an error encoding the value.
	 */
	public static String encodeToString(Object value, Type type) throws VeyronException {
		final byte[] data = encode(value, type);
		return bytesToHexString(data);
	}

	/**
	 * VOM-decodes the provided data using a new VOM-decoder.
	 *
	 * @param  data            VOM-encoded data
	 * @param  type            type of the object that the data should be decoded into
	 * @return                 VOM-decoded object.
	 * @throws VeyronException if there was an error decoding the data.
	 */
	public static Object decode(byte[] data, Type type) throws VeyronException {
		final BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(data));
		try {
			return decoder.decodeValue(type);
		} catch (IOException e) {
			throw new VeyronException(e.getMessage());
		} catch (ConversionException e) {
			throw new VeyronException(e.getMessage());
		}
	}

	/**
	 * VOM-decodes the provided data (stored as a hex string) using a new VOM-decoder.
	 *
	 * @param  hex             VOM-encoded data, stored as a hex string
	 * @param  type            type of the object that the data should be decoded into
	 * @return                 VOM-decoded object.
	 * @throws VeyronException if there was an error decoding the data.
	 */
	public static Object decodeFromString(String hex, Type type) throws VeyronException {
		final byte[] data = hexStringToBytes(hex);
		return decode(data, type);
	}

	private static String bytesToHexString(byte[] data) {
		final StringBuilder builder = new StringBuilder();
		for (byte b : data) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}

	private static byte[] hexStringToBytes(String hex) throws VeyronException {
		if (hex.length() % 2 != 0) {
			throw new VeyronException("Hex strings must be multiples of 2 in length");
		}
		final int outLen = hex.length() / 2;
		final byte[] dat = new byte[outLen];
		for (int i = 0; i < outLen; i++) {
			dat[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return dat;
	}
}