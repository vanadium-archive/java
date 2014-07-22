
// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron2.services.mounttable;

import java.util.ArrayList;

/**
 * MountEntry represents a given name mounted in the mounttable.
 */
public final class MountEntry { 
	// Name is the mounted name.
	private String name;
	// Servers (if present) specifies the mounted names.
	private ArrayList<MountedServer> servers;

	public MountEntry(String name, ArrayList<MountedServer> servers) { 
		this.name = name;
		this.servers = servers;
	}
	public String getName() { return this.name; }
	public ArrayList<MountedServer> getServers() { return this.servers; }

	public void setName(String name) { this.name = name; }
	public void setServers(ArrayList<MountedServer> servers) { this.servers = servers; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final MountEntry other = (MountEntry)obj;
		if (!(this.name.equals(other.name))) return false;
		if (!(this.servers.equals(other.servers))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (servers == null ? 0 : servers.hashCode());
		return result;
	}
}
