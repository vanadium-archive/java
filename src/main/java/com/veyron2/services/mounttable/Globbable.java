// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package com.veyron2.services.mounttable;


public interface Globbable  {

    
    

    
    // Glob returns all matching entries at the given server.

    public com.veyron2.vdl.ClientStream<java.lang.Void,com.veyron2.services.mounttable.types.MountEntry, java.lang.Void> glob(final com.veyron2.ipc.Context context, final java.lang.String pattern) throws com.veyron2.ipc.VeyronException;
    public com.veyron2.vdl.ClientStream<java.lang.Void,com.veyron2.services.mounttable.types.MountEntry, java.lang.Void> glob(final com.veyron2.ipc.Context context, final java.lang.String pattern, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException;

}
