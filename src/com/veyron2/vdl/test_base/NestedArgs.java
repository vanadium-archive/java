
// This file was auto-generated by the veyron vdl tool.
// Source: base.vdl
package com.veyron2.vdl.test_base;


/**
 * NestedArgs is defined before Args; that's allowed in regular Go, and also
 * allowed in our vdl files.  The compiler will re-order dependent types to ease
 * code generation in other languages.
 */
public final class NestedArgs { 
		private Args args;

	public NestedArgs(Args args) { 
		this.args = args;
	}
	public Args getArgs() { return this.args; }

	public void setArgs(Args args) { this.args = args; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final NestedArgs other = (NestedArgs)obj;
		if (!(this.args.equals(other.args))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (args == null ? 0 : args.hashCode());
		return result;
	}
}