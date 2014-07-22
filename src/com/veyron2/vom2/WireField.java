
// This file was auto-generated by the veyron vdl tool.
// Source: wiretype.vdl
package com.veyron2.vom2;


/**
 * WireField represents a field in a struct.
 */
public final class WireField { 
		private String name;
		private TypeID type;

	public WireField(String name, TypeID type) { 
		this.name = name;
		this.type = type;
	}
	public String getName() { return this.name; }
	public TypeID getType() { return this.type; }

	public void setName(String name) { this.name = name; }
	public void setType(TypeID type) { this.type = type; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final WireField other = (WireField)obj;
		if (!(this.name.equals(other.name))) return false;
		if (!(this.type.equals(other.type))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
		return result;
	}
}
