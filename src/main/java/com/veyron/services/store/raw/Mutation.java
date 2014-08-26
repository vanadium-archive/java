// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron.services.store.raw;

/**
 * type Mutation struct{ID veyron2/storage.ID [16]byte;PriorVersion veyron/services/store/raw.Version uint64;Version veyron/services/store/raw.Version;IsRoot bool;Value any;Dir []veyron2/storage.DEntry struct{Name string;ID veyron2/storage.ID}} 
 * Mutation represents an update to an entry in the store, and contains enough
 * information for a privileged service to replicate the update elsewhere.
 **/
public final class Mutation {
    
    
      private com.veyron2.storage.ID iD;
    
      private com.veyron.services.store.raw.Version priorVersion;
    
      private com.veyron.services.store.raw.Version version;
    
      private boolean isRoot;
    
      private java.lang.Object value;
    
      private java.util.ArrayList<com.veyron2.storage.DEntry> dir;
    

    
    public Mutation(final com.veyron2.storage.ID iD, final com.veyron.services.store.raw.Version priorVersion, final com.veyron.services.store.raw.Version version, final boolean isRoot, final java.lang.Object value, final java.util.ArrayList<com.veyron2.storage.DEntry> dir) {
        
            this.iD = iD;
        
            this.priorVersion = priorVersion;
        
            this.version = version;
        
            this.isRoot = isRoot;
        
            this.value = value;
        
            this.dir = dir;
        
    }

    
    
    public com.veyron2.storage.ID getID() {
        return this.iD;
    }
    public void setID(com.veyron2.storage.ID iD) {
        this.iD = iD;
    }
    
    public com.veyron.services.store.raw.Version getPriorVersion() {
        return this.priorVersion;
    }
    public void setPriorVersion(com.veyron.services.store.raw.Version priorVersion) {
        this.priorVersion = priorVersion;
    }
    
    public com.veyron.services.store.raw.Version getVersion() {
        return this.version;
    }
    public void setVersion(com.veyron.services.store.raw.Version version) {
        this.version = version;
    }
    
    public boolean getIsRoot() {
        return this.isRoot;
    }
    public void setIsRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }
    
    public java.lang.Object getValue() {
        return this.value;
    }
    public void setValue(java.lang.Object value) {
        this.value = value;
    }
    
    public java.util.ArrayList<com.veyron2.storage.DEntry> getDir() {
        return this.dir;
    }
    public void setDir(java.util.ArrayList<com.veyron2.storage.DEntry> dir) {
        this.dir = dir;
    }
    

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final Mutation other = (Mutation)obj;

        
        
        if (this.iD == null) {
            if (other.iD != null) {
                return false;
            }
        } else if (!this.iD.equals(other.iD)) {
            return false;
        }
         
        
        
        if (this.priorVersion == null) {
            if (other.priorVersion != null) {
                return false;
            }
        } else if (!this.priorVersion.equals(other.priorVersion)) {
            return false;
        }
         
        
        
        if (this.version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!this.version.equals(other.version)) {
            return false;
        }
         
        
        
        if (this.isRoot != other.isRoot) {
            return false;
        }
         
        
        
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
         
        
        
        if (this.dir == null) {
            if (other.dir != null) {
                return false;
            }
        } else if (!this.dir.equals(other.dir)) {
            return false;
        }
         
         
        return true;
    }
    @Override
    public int hashCode() {
        int result = 1;
        final int prime = 31;
        
        result = prime * result + (iD == null ? 0 : iD.hashCode());
        
        result = prime * result + (priorVersion == null ? 0 : priorVersion.hashCode());
        
        result = prime * result + (version == null ? 0 : version.hashCode());
        
        result = prime * result + java.lang.Boolean.valueOf(isRoot).hashCode();
        
        result = prime * result + (value == null ? 0 : value.hashCode());
        
        result = prime * result + (dir == null ? 0 : dir.hashCode());
        
        return result;
    }
}