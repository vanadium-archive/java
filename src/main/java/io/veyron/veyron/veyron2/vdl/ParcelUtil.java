package io.veyron.veyron.veyron2.vdl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * ParcelUtil contains utility methods for parceling various VDL types.
 */
public class ParcelUtil {
	/**
	 * Writes the provided boolean value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, boolean val) {
		out.writeByte((byte)(val ? 1 : 0));
	}

	/**
	 * Writes the provided byte value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, byte val) {
		out.writeByte(val);
	}

	/**
	 * Writes the provided short integer value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, short val) {
		out.writeInt(val);
	}

	/**
	 * Writes the provided integer value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, int val) {
		out.writeInt(val);
	}

	/**
	 * Writes the provided long value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, long val) {
		out.writeLong(val);
	}

	/**
	 * Writes the provided float value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, float val) {
		out.writeFloat(val);
	}

	/**
	 * Writes the provided double value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, double val) {
		out.writeDouble(val);
	}

	/**
	 * Writes the provided Parcelable value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, Parcelable val) {
		out.writeParcelable(val, 0);
	}

	/**
	 * Writes the provided Object value into the parcel.
	 *
	 * @param out a parcel where the value will be written.
	 * @param val a value to be written into the parcel.
	 */
	public static void writeValue(Parcel out, Object val) {
		out.writeSerializable((Serializable)val);
	}

	/**
	 * Reads the boolean value from the parcel.
	 *
	 * @param  in     a parcel from which the value should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy instance of the value (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static boolean readValue(Parcel in, ClassLoader loader, boolean dummy) {
		return in.readByte() == 1;
	}

	/**
	 * Reads the byte value from the parcel.
	 *
	 * @param  in     a parcel from which the value should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy instance of the value (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static byte readValue(Parcel in, ClassLoader loader, byte dummy) {
		return in.readByte();
	}

	/**
	 * Reads the short integer value from the parcel.
	 *
	 * @param  in     a parcel from which the value should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy instance of the value (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static short readValue(Parcel in, ClassLoader loader, short dummy) {
		return (short)in.readInt();
	}

	/**
	 * Reads the integer value from the parcel.
	 *
	 * @param  in     a parcel from which the value should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy instance of the value (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static int readValue(Parcel in, ClassLoader loader, int dummy) {
		return in.readInt();
	}

	/**
	 * Reads the long value from the parcel.
	 *
	 * @param  in     a parcel from which the value should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy instance of the value (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static long readValue(Parcel in, ClassLoader loader, long dummy) {
		return in.readLong();
	}

	/**
	 * Reads the float value from the parcel.
	 *
	 * @param  in     a parcel from which the value should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy instance of the value (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static float readValue(Parcel in, ClassLoader loader, float dummy) {
		return in.readFloat();
	}

	/**
	 * Reads the double value from the parcel.
	 *
	 * @param  in     a parcel from which the value should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy instance of the value (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static double readValue(Parcel in, ClassLoader loader, double dummy) {
		return in.readDouble();
	}

	/**
	 * Reads the Parcelable value from the parcel.
	 *
	 * @param  in     a parcel from which the object should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy (possibly null) instance of the value
	 *                (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static Parcelable readValue(Parcel in, ClassLoader loader, Parcelable dummy) {
		return in.readParcelable(loader);
	}

	/**
	 * Reads the Object value from the parcel.
	 *
	 * @param  in     a parcel from which the object should be read.
	 * @param  loader a ClassLoader to be used while reading from the parcel.
	 * @param  dummy  a dummy (possibly null) instance of the value
	 *                (used only to help with method overloading).
	 * @return        a value that was read from the parcel.
	 */
	public static Object readValue(Parcel in, ClassLoader loader, Object dummy) {
		return in.readSerializable();
	}
}