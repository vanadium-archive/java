package io.veyron.veyron.veyron2.vdl;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import android.os.Parcel;
import android.os.Parcelable;

import io.veyron.veyron.veyron2.VeyronException;

import java.io.Serializable;

/**
 * Any represents the VDL Any type, which specifies a VDL object of an arbitrary type.
 */
public class Any implements Parcelable, Serializable {
    static final long serialVersionUID = 0L;

	private String data;  // non-null
	private Gson gson;  // non-null

	public Any(String data) {
		this.data = data;
		this.gson = JSONUtil.getGsonBuilder().create();
	}

	/**
	 * Decodes the object into the provided VDL type.
	 *
	 * @param  type            a VDL type that the object should be decoded into.
	 * @return                 the object of the provided VDL type.
	 * @throws VeyronException if the object couldn't be decoded into the provided VDL type.
	 */
	public Object decode(TypeToken<?> type) throws VeyronException {
		try {
			return gson.fromJson(this.data, type.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format("Error decoding JSON data %s into type %s: %s",
				this.data, type.toString(), e.getMessage()));
		}
	}

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final Any other = (Any)obj;
		return this.data.equals(other.data);
	}
	@Override
	public int hashCode() {
		return this.data.hashCode();
	}
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.data);
	}
	public static final Parcelable.Creator<Any> CREATOR = new Parcelable.Creator<Any>() {
		@Override
		public Any createFromParcel(Parcel in) {
			return new Any(in);
		}
		@Override
		public Any[] newArray(int size) {
			return new Any[size];
		}
	};
	private Any(Parcel in) {
		this(in.readString());
	}
}