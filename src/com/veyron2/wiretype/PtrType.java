
// This file was auto-generated by the veyron vdl tool.
// Source: wiretype.vdl
package com.veyron2.wiretype;

import java.util.ArrayList;

/**
 * WARNING: DEPRECATED
 * TODO(bprosnitz) Remove this.
 * PtrType represents a pointer; a value referencing an underlying Elem value.
 */
public final class PtrType { 
		private TypeID elem;
		private String name;
		private ArrayList<String> tags;

	public PtrType(TypeID elem, String name, ArrayList<String> tags) { 
		this.elem = elem;
		this.name = name;
		this.tags = tags;
	}
	public TypeID getElem() { return this.elem; }
	public String getName() { return this.name; }
	public ArrayList<String> getTags() { return this.tags; }

	public void setElem(TypeID elem) { this.elem = elem; }
	public void setName(String name) { this.name = name; }
	public void setTags(ArrayList<String> tags) { this.tags = tags; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final PtrType other = (PtrType)obj;
		if (!(this.elem.equals(other.elem))) return false;
		if (!(this.name.equals(other.name))) return false;
		if (!(this.tags.equals(other.tags))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (elem == null ? 0 : elem.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (tags == null ? 0 : tags.hashCode());
		return result;
	}
}
