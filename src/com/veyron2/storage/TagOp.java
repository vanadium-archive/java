
// This file was auto-generated by the veyron vdl tool.
// Source: types.vdl
package com.veyron2.storage;


/**
 * TagOp is an operation to perform with an ACL.
 */
public final class TagOp { 
		private byte value;

	public TagOp(byte value) { 
		this.value = value;
	}
	public byte getValue() { return this.value; }

	public void setValue(byte value) { this.value = value; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final TagOp other = (TagOp)obj;
		if (this.value != other.value) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (int)value;
		return result;
	}
}