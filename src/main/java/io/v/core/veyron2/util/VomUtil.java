package io.v.core.veyron2.util;

import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.vdl.VdlType;
import io.v.core.veyron2.vdl.VdlValue;
import io.v.core.veyron2.vom.BinaryDecoder;
import io.v.core.veyron2.vom.BinaryEncoder;
import io.v.core.veyron2.vom.ConversionException;

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
	 * @param  value           value to be encoded
	 * @param  type            type of the provided value
	 * @return                 VOM-encoded value as a byte array
	 * @throws VException      if there was an error encoding the value
	 */
	public static byte[] encode(Object value, Type type) throws VException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final BinaryEncoder encoder = new BinaryEncoder(out);
		try {
			encoder.encodeValue(type, value);
		} catch (IOException e) {
			throw new VException(e.getMessage());
		}
		return out.toByteArray();
	}

	/**
	 * VOM-encodes the provided value using a new VOM-encoder.
	 *
	 * @param  value           value to be encoded
	 * @param  type            type of the provided value
	 * @return                 VOM-encoded value as a byte array
	 * @throws VException      if there was an error encoding the value
	 */
	public static byte[] encode(Object value, VdlType type) throws VException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final BinaryEncoder encoder = new BinaryEncoder(out);
		try {
			encoder.encodeValue(type, value);
		} catch (IOException e) {
			throw new VException(e.getMessage());
		}
		return out.toByteArray();
	}

	/**
	 * VOM-encodes the provided value using a new VOM-encoder, returning a hex-encoded string.
	 *
	 * @param  value           value to be encoded
	 * @param  type            type of the provided value
	 * @return                 VOM-encoded value in a hex-string format
	 * @throws VException      if there was an error encoding the value
	 */
	public static String encodeToString(Object value, Type type) throws VException {
		final byte[] data = encode(value, type);
		return bytesToHexString(data);
	}

	/**
	 * VOM-encodes the provided VDL value using a new VOM-encoder.
	 *
	 * @param  value           VDL value to be encoded
	 * @return                 VOM-encoded value as a byte array
	 * @throws VException      if there was an error encoding the value
	 */
	public static byte[] encode(VdlValue value) throws VException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final BinaryEncoder encoder = new BinaryEncoder(out);
		try {
			encoder.encodeValue(value);
		} catch (IOException e) {
			throw new VException(e.getMessage());
		}
		return out.toByteArray();
	}


	/**
	 * VOM-decodes the provided data using a new VOM-decoder.
	 *
	 * @param  data            VOM-encoded data
	 * @param  type            type of the object that the data should be decoded into
	 * @return                 VOM-decoded object
	 * @throws VException      if there was an error decoding the data
	 */
	public static Object decode(byte[] data, Type type) throws VException {
		final BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(data));
		try {
			return decoder.decodeValue(type);
		} catch (IOException e) {
			throw new VException(e.getMessage());
		} catch (ConversionException e) {
			throw new VException(e.getMessage());
		}
	}

	/**
	 * VOM-decodes the provided data using a new VOM-decoder.  A best effort is made to deduce the
	 * type of the encoded data.
	 *
	 * @param  data            VOM-encoded data
	 * @return                 VOM-decoded object
	 * @throws VException      if there was an error decoding the data
	 */
	public static Object decode(byte[] data) throws VException {
		final BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(data));
		try {
			return decoder.decodeValue();
		} catch (IOException e) {
			throw new VException(e.getMessage());
		} catch (ConversionException e) {
			throw new VException(e.getMessage());
		}
	}

	/**
	 * VOM-decodes the provided data (stored as a hex string) using a new VOM-decoder.
	 *
	 * @param  hex             VOM-encoded data, stored as a hex string
	 * @param  type            type of the object that the data should be decoded into
	 * @return                 VOM-decoded object
	 * @throws VException      if there was an error decoding the data
	 */
	public static Object decodeFromString(String hex, Type type) throws VException {
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

	private static byte[] hexStringToBytes(String hex) throws VException {
		if (hex.length() % 2 != 0) {
			throw new VException("Hex strings must be multiples of 2 in length");
		}
		final int outLen = hex.length() / 2;
		final byte[] dat = new byte[outLen];
		for (int i = 0; i < outLen; i++) {
			dat[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return dat;
	}
}