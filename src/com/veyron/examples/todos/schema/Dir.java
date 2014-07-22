
// This file was auto-generated by the veyron vdl tool.
// Source: schema.vdl
package com.veyron.examples.todos.schema;


/**
 * Dir represents a directory.
 */
public final class Dir { 
	// TODO(jyh): The IDL does not recognize empty structs. Fix it and remove this
// useless field.
	private byte x;

	public Dir(byte x) { 
		this.x = x;
	}
	public byte getX() { return this.x; }

	public void setX(byte x) { this.x = x; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final Dir other = (Dir)obj;
		if (this.x != other.x) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (int)x;
		return result;
	}
}
