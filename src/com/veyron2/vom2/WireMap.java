
// This file was auto-generated by the veyron vdl tool.
// Source: wiretype.vdl
package com.veyron2.vom2;


/**
 * WireMap represents a map type definition.
 */
public final class WireMap { 
		private String name;
		private TypeID key;
		private TypeID elem;

	public WireMap(String name, TypeID key, TypeID elem) { 
		this.name = name;
		this.key = key;
		this.elem = elem;
	}
	public String getName() { return this.name; }
	public TypeID getKey() { return this.key; }
	public TypeID getElem() { return this.elem; }

	public void setName(String name) { this.name = name; }
	public void setKey(TypeID key) { this.key = key; }
	public void setElem(TypeID elem) { this.elem = elem; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final WireMap other = (WireMap)obj;
		if (!(this.name.equals(other.name))) return false;
		if (!(this.key.equals(other.key))) return false;
		if (!(this.elem.equals(other.elem))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (key == null ? 0 : key.hashCode());
		result = prime * result + (elem == null ? 0 : elem.hashCode());
		return result;
	}
}