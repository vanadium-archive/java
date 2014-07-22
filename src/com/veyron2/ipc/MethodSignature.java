
// This file was auto-generated by the veyron vdl tool.
// Source: signature.vdl
package com.veyron2.ipc;

import com.veyron2.wiretype.TypeID;
import java.util.ArrayList;

/**
 * MethodSignature represents the structure for passing around method
 * signatures. This is usually sent in a ServiceSignature.
 */
public final class MethodSignature { 
		private ArrayList<MethodArgument> inArgs; // Positional Argument information.
		private ArrayList<MethodArgument> outArgs;
		private TypeID inStream; // Type of streaming arguments (or TypeIDInvalid if none). The type IDs here use the definitions in ServiceSigature.TypeDefs.
		private TypeID outStream;

	public MethodSignature(ArrayList<MethodArgument> inArgs, ArrayList<MethodArgument> outArgs, TypeID inStream, TypeID outStream) { 
		this.inArgs = inArgs;
		this.outArgs = outArgs;
		this.inStream = inStream;
		this.outStream = outStream;
	}
	public ArrayList<MethodArgument> getInArgs() { return this.inArgs; }
	public ArrayList<MethodArgument> getOutArgs() { return this.outArgs; }
	public TypeID getInStream() { return this.inStream; }
	public TypeID getOutStream() { return this.outStream; }

	public void setInArgs(ArrayList<MethodArgument> inArgs) { this.inArgs = inArgs; }
	public void setOutArgs(ArrayList<MethodArgument> outArgs) { this.outArgs = outArgs; }
	public void setInStream(TypeID inStream) { this.inStream = inStream; }
	public void setOutStream(TypeID outStream) { this.outStream = outStream; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final MethodSignature other = (MethodSignature)obj;
		if (!(this.inArgs.equals(other.inArgs))) return false;
		if (!(this.outArgs.equals(other.outArgs))) return false;
		if (!(this.inStream.equals(other.inStream))) return false;
		if (!(this.outStream.equals(other.outStream))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (inArgs == null ? 0 : inArgs.hashCode());
		result = prime * result + (outArgs == null ? 0 : outArgs.hashCode());
		result = prime * result + (inStream == null ? 0 : inStream.hashCode());
		result = prime * result + (outStream == null ? 0 : outStream.hashCode());
		return result;
	}
}
