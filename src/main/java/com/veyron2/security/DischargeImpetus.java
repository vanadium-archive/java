// This file was auto-generated by the veyron vdl tool.
// Source: types.vdl
package com.veyron2.security;

/**
 * type DischargeImpetus struct{Server any;Method string;Arguments []any} 
 * DischargeImpetus encapsulates the motivation for a discharge being sought.
 * 
 * These values are reported by the holder of a PublicID with ThirdPartyCaveats when
 * requesting a ThirdPartyDischarge. The third-party issuing discharges thus cannot safely
 * assume that all values are provided, or that they are provided honestly.
 * 
 * Implementations of services that issue discharges are encouraged to add caveats to the
 * discharge that bind the discharge to the impetus, thereby rendering the discharge unsuable
 * for any other purpose.
 **/
public final class DischargeImpetus {
    
    
      private java.lang.Object server;
    
      private java.lang.String method;
    
      private java.util.ArrayList<java.lang.Object> arguments;
    

    
    public DischargeImpetus(final java.lang.Object server, final java.lang.String method, final java.util.ArrayList<java.lang.Object> arguments) {
        
            this.server = server;
        
            this.method = method;
        
            this.arguments = arguments;
        
    }

    
    
    public java.lang.Object getServer() {
        return this.server;
    }
    public void setServer(java.lang.Object server) {
        this.server = server;
    }
    
    public java.lang.String getMethod() {
        return this.method;
    }
    public void setMethod(java.lang.String method) {
        this.method = method;
    }
    
    public java.util.ArrayList<java.lang.Object> getArguments() {
        return this.arguments;
    }
    public void setArguments(java.util.ArrayList<java.lang.Object> arguments) {
        this.arguments = arguments;
    }
    

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final DischargeImpetus other = (DischargeImpetus)obj;

        
        
        if (this.server == null) {
            if (other.server != null) {
                return false;
            }
        } else if (!this.server.equals(other.server)) {
            return false;
        }
         
        
        
        if (this.method == null) {
            if (other.method != null) {
                return false;
            }
        } else if (!this.method.equals(other.method)) {
            return false;
        }
         
        
        
        if (this.arguments == null) {
            if (other.arguments != null) {
                return false;
            }
        } else if (!this.arguments.equals(other.arguments)) {
            return false;
        }
         
         
        return true;
    }
    @Override
    public int hashCode() {
        int result = 1;
        final int prime = 31;
        
        result = prime * result + (server == null ? 0 : server.hashCode());
        
        result = prime * result + (method == null ? 0 : method.hashCode());
        
        result = prime * result + (arguments == null ? 0 : arguments.hashCode());
        
        return result;
    }
}