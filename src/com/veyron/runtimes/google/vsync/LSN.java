
// This file was auto-generated by the veyron vdl tool.
// Source: vsync.vdl
package com.veyron.runtimes.google.vsync;


/**
 * LSN is the log sequence number.
 */
public final class LSN { 
		private long value;

	public LSN(long value) { 
		this.value = value;
	}
	public long getValue() { return this.value; }

	public void setValue(long value) { this.value = value; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final LSN other = (LSN)obj;
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
