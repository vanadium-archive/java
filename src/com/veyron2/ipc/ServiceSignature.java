
// This file was auto-generated by the veyron vdl tool.
// Source: signature.vdl
package com.veyron2.ipc;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ServiceSignature represents the signature of the service. This includes type information needed
 * to resolve the method argument types.
 * TODO(bprosnitz) Rename this and move it to wiretype.
 */
public final class ServiceSignature { 
		private ArrayList<Object> typeDefs; // A slice of wiretype structures form the type definition.
		private HashMap<String, MethodSignature> methods;

	public ServiceSignature(ArrayList<Object> typeDefs, HashMap<String, MethodSignature> methods) { 
		this.typeDefs = typeDefs;
		this.methods = methods;
	}
	public ArrayList<Object> getTypeDefs() { return this.typeDefs; }
	public HashMap<String, MethodSignature> getMethods() { return this.methods; }

	public void setTypeDefs(ArrayList<Object> typeDefs) { this.typeDefs = typeDefs; }
	public void setMethods(HashMap<String, MethodSignature> methods) { this.methods = methods; }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final ServiceSignature other = (ServiceSignature)obj;
		if (!(this.typeDefs.equals(other.typeDefs))) return false;
		if (!(this.methods.equals(other.methods))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (typeDefs == null ? 0 : typeDefs.hashCode());
		result = prime * result + (methods == null ? 0 : methods.hashCode());
		return result;
	}
}
