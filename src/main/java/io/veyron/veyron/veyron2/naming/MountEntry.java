package io.veyron.veyron.veyron2.naming;

import android.os.Parcel;
import android.os.Parcelable;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.vdl.GeneratedFromVdlName;

import java.io.Serializable;

/**
 * MountEntry represents a name mounted in the mounttable.
 */
public class MountEntry implements Parcelable, Serializable {
	static final long serialVersionUID = 0L;

	@GeneratedFromVdlName("Name")
	private final String name;
	@GeneratedFromVdlName("Servers")
	private final MountedServer[] servers;
	@GeneratedFromVdlName("Error")
	private final VeyronException error;

	public MountEntry(String name, MountedServer[] servers, VeyronException error) {
		this.name = name;
		this.servers = servers;
		this.error = error;
	}

	/**
	 * Returns the mounted name.
	 *
	 * @return the mounted name.
	 */
	public String getName() { return this.name; }

	/**
	 * Returns the list of servers mounted under the given name.
	 *
	 * @return the list of servers mounted under the given name.
	 */
	public MountedServer[] getServers() { return this.servers; }

	/**
	 * Returns an error (if any) that occurred fulfilling the request.
	 *
	 * @return an error that occurred fulfilling the request.
	 */
	public VeyronException getError() { return this.error; }

    @Override
    public int describeContents() {
        return 0;
    }
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.name);
		out.writeInt(this.servers != null ? this.servers.length : -1);
		if (this.servers != null) {
			out.writeTypedArray(this.servers, flags);
		}
		out.writeParcelable(this.error, flags);
	}
	public static final Parcelable.Creator<MountEntry> CREATOR =
			new Parcelable.Creator<MountEntry>() {
		@Override
		public MountEntry createFromParcel(Parcel in) {
			return new MountEntry(in);
		}
		@Override
		public MountEntry[] newArray(int size) {
			return new MountEntry[size];
		}
	};
	private MountEntry(Parcel in) {
		this.name = in.readString();
		final int size = in.readInt();
		if (size < 0) {
			this.servers = null;
		} else {
			this.servers = new MountedServer[size];
			in.readTypedArray(this.servers, MountedServer.CREATOR);
		}
		this.error = (VeyronException)in.readParcelable(VeyronException.class.getClassLoader());
	}
}