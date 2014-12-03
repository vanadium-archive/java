package io.veyron.veyron.veyron.runtimes.google.ipc;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.vom2.BinaryDecoder;
import io.veyron.veyron.veyron2.vom2.BinaryEncoder;
import io.veyron.veyron.veyron2.vom2.ConversionException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Util contains various utilities used by the ipc package.
 */
class Util {
	/**
	 * VOM-encodes the provided value using a new VOM-encoder.
	 */
	static byte[] VomEncode(Object value, Type type) throws VeyronException {
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
	 * VOM-decodes the provided data using a new VOM-decoder.
	 */
	static Object VomDecode(byte[] data, Type type) throws VeyronException {
		final BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(data));
		try {
			return decoder.decodeValue(type);
		} catch (IOException e) {
			throw new VeyronException(e.getMessage());
		} catch (ConversionException e) {
			throw new VeyronException(e.getMessage());
		}
	}
}