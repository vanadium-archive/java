
// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron2.services.store;


/**
 * TransactionID is a transaction identifier.  The identifier is chosen by the
 * client.
 * 
 * TransactionIDs do not span store instances.  If you use the same
 * TransactionID with two different store instances, the transactions are
 * separate, and must be committed separately.  Don't do that, it will lead to
 * confusion.  Use fresh TransactionIDs for each store instance.
 * 
 * TODO(jyh): Consider using a larger identifier space to reduce chance of
 * collisions.
 */
public final class TransactionID { 
		private long value;

	public TransactionID(long value) { 
		this.value = value;
	}
	public long getValue() { return this.value; }

	public void setValue(long value) { this.value = value; }

	@Override
	public boolean equals(java.lang.Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		final TransactionID other = (TransactionID)obj;
		if (this.value != other.value) return false;
		return true;
	}
	@Override
	public int hashCode() {
		int result = 1;
		final int prime = 31;
		result = prime * result + Long.valueOf(value).hashCode();
		return result;
	}
}