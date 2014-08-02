// This file was hand-crafted by spetrovic@.
// Source: types.vdl
package com.veyron2.security;

/**
 * type Signature struct{Hash veyron2/security.Hash string;R []byte;S []byte} 
 * Signature represents an ECDSA signature.
 **/
public final class Signature {
    
    
      private com.veyron2.security.Hash hash;
    
      private byte[] r;
    
      private byte[] s;
    

    
    public Signature(final com.veyron2.security.Hash hash, final byte[] r, final byte[] s) {
        
            this.hash = hash;
        
            this.r = r;
        
            this.s = s;
        
    }

    
    
    public com.veyron2.security.Hash getHash() {
        return this.hash;
    }
    public void setHash(com.veyron2.security.Hash hash) {
        this.hash = hash;
    }
    
    public byte[] getR() {
        return this.r;
    }
    public void setR(byte[] r) {
        this.r = r;
    }
    
    public byte[] getS() {
        return this.s;
    }
    public void setS(byte[] s) {
        this.s = s;
    }
    

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final Signature other = (Signature)obj;

        
        
        if (this.hash == null) {
            if (other.hash != null) {
                return false;
            }
        } else if (!this.hash.equals(other.hash)) {
            return false;
        }
         
        
        
        if (this.r == null) {
            if (other.r != null) {
                return false;
            }
        } else if (!this.r.equals(other.r)) {
            return false;
        }
         
        
        
        if (this.s == null) {
            if (other.s != null) {
                return false;
            }
        } else if (!this.s.equals(other.s)) {
            return false;
        }
         
         
        return true;
    }
    @Override
    public int hashCode() {
        int result = 1;
        final int prime = 31;
        
        result = prime * result + (hash == null ? 0 : hash.hashCode());
        
        result = prime * result + (r == null ? 0 : r.hashCode());
        
        result = prime * result + (s == null ? 0 : s.hashCode());
        
        return result;
    }
}