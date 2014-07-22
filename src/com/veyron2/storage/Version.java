
// This file was auto-generated by the veyron vdl tool.
// Source: types.vdl
package com.veyron2.storage;


/**
 * Version identifies the value in the store for a key at some point in time.
 * The version is a numeric identifier that is globally unique within the space
 * of a single ID, meaning that if two stores contain an entry with the same ID
 * and version, then the entries represent the same thing, at the same point in
 * time (as agreed upon by the two stores).
 */
public final class Version { 
		private long value;

	public Version(long value) { 
		this.value = value;
	}
	public long getValue() { return this.value; }

	public void setValue(long value) { this.value = value; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final Version other = (Version)obj;
		if (this.value != other.value) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + Long.valueOf(value).hashCode();
		return result;
	}
}