
// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron2.services.security.access;

import com.veyron2.security.LabelSet;
import com.veyron2.security.PrincipalPattern;
import java.util.HashMap;

/**
 * Entries describes a set of principals and groups (of principals).
 */
public final class Entries { 
	// Principals specifies the type of access being granted or revoked to any
// Identity that matches the PrincipalPattern.  If multiple patterns match
// an Identity, the server will iterate through them to find one that
// contains the desired label.
	private HashMap<PrincipalPattern, LabelSet> principals;
	// Groups specifies the type of access being granted or revoked to all
// members of the group.
	private HashMap<Group, LabelSet> groups;

	public Entries(HashMap<PrincipalPattern, LabelSet> principals, HashMap<Group, LabelSet> groups) { 
		this.principals = principals;
		this.groups = groups;
	}
	public HashMap<PrincipalPattern, LabelSet> getPrincipals() { return this.principals; }
	public HashMap<Group, LabelSet> getGroups() { return this.groups; }

	public void setPrincipals(HashMap<PrincipalPattern, LabelSet> principals) { this.principals = principals; }
	public void setGroups(HashMap<Group, LabelSet> groups) { this.groups = groups; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final Entries other = (Entries)obj;
		if (!(this.principals.equals(other.principals))) return false;
		if (!(this.groups.equals(other.groups))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (principals == null ? 0 : principals.hashCode());
		result = prime * result + (groups == null ? 0 : groups.hashCode());
		return result;
	}
}