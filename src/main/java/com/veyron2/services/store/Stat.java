// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron2.services.store;

/**
 * type Stat struct{ID veyron2/storage.ID [16]byte;MTimeNS int64;Attrs []any} 
 * Stat provides information about an entry in the store.
 * 
 * TODO(jyh): Specify versioning more precisely.
 **/
public final class Stat {
    
    
      private com.veyron2.storage.ID iD;
    
      private long mTimeNS;
    
      private java.util.ArrayList<java.lang.Object> attrs;
    

    
    public Stat(final com.veyron2.storage.ID iD, final long mTimeNS, final java.util.ArrayList<java.lang.Object> attrs) {
        
            this.iD = iD;
        
            this.mTimeNS = mTimeNS;
        
            this.attrs = attrs;
        
    }

    
    
    public com.veyron2.storage.ID getID() {
        return this.iD;
    }
    public void setID(com.veyron2.storage.ID iD) {
        this.iD = iD;
    }
    
    public long getMTimeNS() {
        return this.mTimeNS;
    }
    public void setMTimeNS(long mTimeNS) {
        this.mTimeNS = mTimeNS;
    }
    
    public java.util.ArrayList<java.lang.Object> getAttrs() {
        return this.attrs;
    }
    public void setAttrs(java.util.ArrayList<java.lang.Object> attrs) {
        this.attrs = attrs;
    }
    

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final Stat other = (Stat)obj;

        
        
        if (this.iD == null) {
            if (other.iD != null) {
                return false;
            }
        } else if (!this.iD.equals(other.iD)) {
            return false;
        }
         
        
        
        if (this.mTimeNS != other.mTimeNS) {
            return false;
        }
         
        
        
        if (this.attrs == null) {
            if (other.attrs != null) {
                return false;
            }
        } else if (!this.attrs.equals(other.attrs)) {
            return false;
        }
         
         
        return true;
    }
    @Override
    public int hashCode() {
        int result = 1;
        final int prime = 31;
        
        result = prime * result + (iD == null ? 0 : iD.hashCode());
        
        result = prime * result + java.lang.Long.valueOf(mTimeNS).hashCode();
        
        result = prime * result + (attrs == null ? 0 : attrs.hashCode());
        
        return result;
    }
}