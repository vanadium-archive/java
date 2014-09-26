package io.veyron.veyron.veyron2.naming;

import com.google.gson.annotations.SerializedName;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.Duration;

import java.io.Serializable;

/**
 * MountedServer represents a server mounted under an object name.
 */
public class MountedServer implements Parcelable, Serializable {
	static final long serialVersionUID = 0L;

	@SerializedName("Server")
	private final String server;
	@SerializedName("TTL")
	private final Duration ttl;

	public MountedServer(String server, Duration ttl) {
		this.server = server;
		this.ttl = ttl;
	}

	/**
	 * Returns the server object address (OA): endpoint + suffix.
	 *
	 * @return a server object address (OA).
	 */
	public String getServer() { return this.server; }

	/**
	 * Returns the Time-To-Live after which the mount expires.
	 *
	 * @return a Time-To-Live after which the mount expires.
	 */
	public Duration getTTL() { return this.ttl; }

    @Override
    public int describeContents() {
        return 0;
    }
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.server);
		out.writeLong(this.ttl.getMillis());
	}
	public static final Parcelable.Creator<MountedServer> CREATOR =
			new Parcelable.Creator<MountedServer>() {
		@Override
		public MountedServer createFromParcel(Parcel in) {
			return new MountedServer(in);
		}
		@Override
		public MountedServer[] newArray(int size) {
			return new MountedServer[size];
		}
	};
	private MountedServer(Parcel in) {
		this(in.readString(), Duration.millis(in.readLong()));
	}
}