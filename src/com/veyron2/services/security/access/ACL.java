
// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron2.services.security.access;


/**
 * ACL (Access Control List) tracks which principals and groups have access to
 * an object and which principals and groups specifically do not have access
 * to an object.  For example:
 * ACL {
 * In {
 * Principals {
 * "user1": ["Read", "Write"],
 * "user2": ["Read"],
 * }
 * Groups {
 * Group{"google.com/engineering"}: ["Read"],
 * }
 * }
 * NotIn {
 * Principals {
 * "user1": ["Write"],
 * }
 * Groups {
 * Group{"google.com/eng-interns"}: ["Read", "Write", "Admin"],
 * }
 * }
 * }
 * NotIn subtracts privileges.  In this example, it says that "user1" has
 * only  "Read" access.  All of engineering has read access except for
 * engineering interns.
 * 
 * Principals can have multiple names.  As long as the principal has a name
 * that matches In and not NotIn, it is authorized. The reasoning is that the
 * principal can always hide a name if it wants to, so requiring all names to
 * satisfy the policy does not make sense.
 */
public final class ACL { 
	// In represents the set of principals and groups that can access the object
// only if they are not also present in NotIn.
	private Entries in;
	// NotIn represents the set of principals and groups that do not have access
// to the object.  It effectively subtracts permissions from In.
	private Entries notIn;

	public ACL(Entries in, Entries notIn) { 
		this.in = in;
		this.notIn = notIn;
	}
	public Entries getIn() { return this.in; }
	public Entries getNotIn() { return this.notIn; }

	public void setIn(Entries in) { this.in = in; }
	public void setNotIn(Entries notIn) { this.notIn = notIn; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final ACL other = (ACL)obj;
		if (!(this.in.equals(other.in))) return false;
		if (!(this.notIn.equals(other.notIn))) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + (in == null ? 0 : in.hashCode());
		result = prime * result + (notIn == null ? 0 : notIn.hashCode());
		return result;
	}
}
