
// This file was auto-generated by the veyron vdl tool.
// Source: wire.vdl
package com.veyron2.security.wire;

import com.veyron2.security.PrincipalPattern;
import java.util.ArrayList;

/**
 * Caveat represents a veyron2/security.ServiceCaveat.
 */
public final class Caveat { 
	// Service is a pattern identifying the services that the caveat encoded in Bytes
// is bound to.
	private PrincipalPattern service;
	// Bytes is a serialized representation of the embedded caveat.
	private ArrayList<Byte> bytes;

	public Caveat(PrincipalPattern service, ArrayList<Byte> bytes) { 
		this.service = service;
		this.bytes = bytes;
	}
	public PrincipalPattern getService() { return this.service; }
	public ArrayList<Byte> getBytes() { return this.bytes; }

	public void setService(PrincipalPattern service) { this.service = service; }
	public void setBytes(ArrayList<Byte> bytes) { this.bytes = bytes; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final Caveat other = (Caveat)obj;
		if (!(this.service.equals(other.service))) return false;
		if (!(this.bytes.equals(other.bytes))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (service == null ? 0 : service.hashCode());
		result = prime * result + (bytes == null ? 0 : bytes.hashCode());
		return result;
	}
}
