
// This file was auto-generated by the veyron vdl tool.
// Source: wiretype.vdl
package com.veyron2.vom2;


/**
 * WireArray represents an array type definition.
 */
public final class WireArray { 
		private String name;
		private TypeID elem;
		private long len;

	public WireArray(String name, TypeID elem, long len) { 
		this.name = name;
		this.elem = elem;
		this.len = len;
	}
	public String getName() { return this.name; }
	public TypeID getElem() { return this.elem; }
	public long getLen() { return this.len; }

	public void setName(String name) { this.name = name; }
	public void setElem(TypeID elem) { this.elem = elem; }
	public void setLen(long len) { this.len = len; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final WireArray other = (WireArray)obj;
		if (!(this.name.equals(other.name))) return false;
		if (!(this.elem.equals(other.elem))) return false;
		if (this.len != other.len) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (elem == null ? 0 : elem.hashCode());
		result = prime * result + Long.valueOf(len).hashCode();
		return result;
	}
}