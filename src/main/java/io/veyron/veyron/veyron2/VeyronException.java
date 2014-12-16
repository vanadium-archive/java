package io.veyron.veyron.veyron2;

import android.os.Parcel;
import android.os.Parcelable;

import io.veyron.veyron.veyron2.vdl.GeneratedFromVdl;

import java.io.Serializable;

/**
 * VeyronException is an exception raised by the Veyron runtime in an event of an unexpected error.
 * It contains an error message and optionally a non-empty unique error identifier which allows for
 * stable error checking across different error messages and different address spaces.
 * By convention, the format for the identifier is {@literal "PKGPATH.NAME"} - e.g., ERROR_ID_FOO
 * defined in the "veyron2.verror" package has id "veyron2/verror/ERROR_ID_FOO". (Note that dots in
 * Java package names are replaced with slashes.) All messages and ids are non-{@code null} (but can
 * be empty).
 *
 * For comparison between {@code VeyronException}s, we follow the following set of rules:
 *     1) Two exceptions, at least one of which has a non-empty id, are equal iff their ids are
 *        equal, regardless of the message strings.
 *     2) Two exceptions with empty IDs are equal iff their messages are equal.
 */
public class VeyronException extends Exception implements Parcelable, Serializable {
	private static final long serialVersionUID = -3917496574141933784L;

	@GeneratedFromVdl(name = "Id")
	private final String id; // always non-null (can be empty)
	@GeneratedFromVdl(name = "Msg")
	private final String msg;  // always non-null (can be empty)

	public VeyronException() {
		this(null, null);
	}

	public VeyronException(String msg) {
		this(msg, null);
	}

	public VeyronException(String msg, String id) {
		super(msg == null ? "" : msg);
		this.id = id == null ? "" : id;
		this.msg = msg == null ? "" : msg;
	}

	/**
	 * Returns the ID associated with this exception or null if no ID has been associated.
	 *
	 * @return the ID associated with the exception
	 */
	public String getID() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof VeyronException))
			return false;

		final VeyronException other = (VeyronException) obj;
		// Compare ids.
		if (!this.id.isEmpty() || !other.id.isEmpty()) {
			return this.id.equals(other.id);
		}
		// Compare messages.
		return this.msg.equals(other.msg);
	}

	@Override
	public int hashCode() {
		// Prefix with id_ and msg_, so that id doesn't end up matching
		// msg and vice versa.
		if (!this.id.isEmpty()) {
			return ("id_" + this.id).hashCode();
		}
		return ("msg_" + this.msg).hashCode();
	}

	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.msg);
		out.writeString(this.id);
	}
	public static final Parcelable.Creator<VeyronException> CREATOR =
			new Parcelable.Creator<VeyronException>() {
		@Override
		public VeyronException createFromParcel(Parcel in) {
			return new VeyronException(in);
		}
		@Override
		public VeyronException[] newArray(int size) {
			return new VeyronException[size];
		}
	};
	private VeyronException(Parcel in) {
		this(in.readString(), in.readString());
	}
}