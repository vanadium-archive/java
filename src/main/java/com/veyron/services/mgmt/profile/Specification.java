// This file was auto-generated by the veyron vdl tool.
// Source: profile.vdl
package com.veyron.services.mgmt.profile;

/**
 * type Specification struct{Arch veyron2/services/mgmt/build.Architecture string;Description string;Format veyron2/services/mgmt/build.Format string;Libraries set[veyron/services/mgmt/profile.Library struct{Name string;MajorVersion string;MinorVersion string}];Label string;OS veyron2/services/mgmt/build.OperatingSystem string} 
 * Specification is how we represent a profile internally. It should
 * provide enough information to allow matching of binaries to nodes.
 **/
public final class Specification {
    
    
      private com.veyron2.services.mgmt.build.Architecture arch;
    
      private java.lang.String description;
    
      private com.veyron2.services.mgmt.build.Format format;
    
      private java.util.HashSet<com.veyron.services.mgmt.profile.Library> libraries;
    
      private java.lang.String label;
    
      private com.veyron2.services.mgmt.build.OperatingSystem oS;
    

    
    public Specification(final com.veyron2.services.mgmt.build.Architecture arch, final java.lang.String description, final com.veyron2.services.mgmt.build.Format format, final java.util.HashSet<com.veyron.services.mgmt.profile.Library> libraries, final java.lang.String label, final com.veyron2.services.mgmt.build.OperatingSystem oS) {
        
            this.arch = arch;
        
            this.description = description;
        
            this.format = format;
        
            this.libraries = libraries;
        
            this.label = label;
        
            this.oS = oS;
        
    }

    
    
    public com.veyron2.services.mgmt.build.Architecture getArch() {
        return this.arch;
    }
    public void setArch(com.veyron2.services.mgmt.build.Architecture arch) {
        this.arch = arch;
    }
    
    public java.lang.String getDescription() {
        return this.description;
    }
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    
    public com.veyron2.services.mgmt.build.Format getFormat() {
        return this.format;
    }
    public void setFormat(com.veyron2.services.mgmt.build.Format format) {
        this.format = format;
    }
    
    public java.util.HashSet<com.veyron.services.mgmt.profile.Library> getLibraries() {
        return this.libraries;
    }
    public void setLibraries(java.util.HashSet<com.veyron.services.mgmt.profile.Library> libraries) {
        this.libraries = libraries;
    }
    
    public java.lang.String getLabel() {
        return this.label;
    }
    public void setLabel(java.lang.String label) {
        this.label = label;
    }
    
    public com.veyron2.services.mgmt.build.OperatingSystem getOS() {
        return this.oS;
    }
    public void setOS(com.veyron2.services.mgmt.build.OperatingSystem oS) {
        this.oS = oS;
    }
    

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final Specification other = (Specification)obj;

        
        
        if (this.arch == null) {
            if (other.arch != null) {
                return false;
            }
        } else if (!this.arch.equals(other.arch)) {
            return false;
        }
         
        
        
        if (this.description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!this.description.equals(other.description)) {
            return false;
        }
         
        
        
        if (this.format == null) {
            if (other.format != null) {
                return false;
            }
        } else if (!this.format.equals(other.format)) {
            return false;
        }
         
        
        
        if (this.libraries == null) {
            if (other.libraries != null) {
                return false;
            }
        } else if (!this.libraries.equals(other.libraries)) {
            return false;
        }
         
        
        
        if (this.label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!this.label.equals(other.label)) {
            return false;
        }
         
        
        
        if (this.oS == null) {
            if (other.oS != null) {
                return false;
            }
        } else if (!this.oS.equals(other.oS)) {
            return false;
        }
         
         
        return true;
    }
    @Override
    public int hashCode() {
        int result = 1;
        final int prime = 31;
        
        result = prime * result + (arch == null ? 0 : arch.hashCode());
        
        result = prime * result + (description == null ? 0 : description.hashCode());
        
        result = prime * result + (format == null ? 0 : format.hashCode());
        
        result = prime * result + (libraries == null ? 0 : libraries.hashCode());
        
        result = prime * result + (label == null ? 0 : label.hashCode());
        
        result = prime * result + (oS == null ? 0 : oS.hashCode());
        
        return result;
    }
}