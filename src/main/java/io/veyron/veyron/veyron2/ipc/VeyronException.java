package io.veyron.veyron.veyron2.ipc;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * VeyronException is an exception raised by the Veyron runtime in an event of
 * an unexpected error. It contains an error message and optionally a non-empty
 * unique error identifier which allows for stable error checking across
 * different error messages and different address spaces. By convention, the
 * format for the ID is "PKGPATH.NAME" - e.g., ErrIDFoo defined in the
 * "veyron2/verror" package has id "veyron2/verror.ErrIDFoo". (Note that dots in
 * Java package names are replaced with slashes.) All messages and ids are
 * non-null (but can be empty).
 *
 * For comparison between VeyronExceptions, we follow the following set of
 * rules: 1) Two exceptions, at least one of which has a non-empty ID, are equal
 * iff their IDs are equal, regardless of the message strings. 2) Two exceptions
 * with empty IDs are equal iff their messages are equal.
 */
// TODO(spetrovic): Move this class into package "io.veyron.veyron.veyron2".
public class VeyronException extends Exception implements Parcelable, Serializable {
    private static final long serialVersionUID = -3917496574141933784L;

    private final String id; // always non-null (can be empty)

    public VeyronException() {
        super("");
        this.id = "";
    }

    public VeyronException(String msg) {
        super(msg == null ? "" : msg);
        this.id = "";
    }

    public VeyronException(String msg, String id) {
        super(msg == null ? "" : msg);
        this.id = id == null ? "" : id;
    }

    /**
     * Returns the ID associated with this exception or null if no ID has been
     * associated.
     *
     * @return String the ID associated with the exception
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
        return this.getMessage().equals(other.getMessage());
    }

    @Override
    public int hashCode() {
        // Prefix with id_ and msg_, so that id doesn't end up matching
        // msg and vice versa.
        if (!this.id.isEmpty()) {
            return ("id_" + this.id).hashCode();
        }
        return ("msg_" + this.getMessage()).hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.getMessage());
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