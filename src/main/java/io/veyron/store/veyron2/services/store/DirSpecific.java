// This file was auto-generated by the veyron vdl tool.
// Source: service.vdl
package io.veyron.store.veyron2.services.store;


public interface DirSpecific extends io.veyron.store.veyron2.services.store.SyncGroup {

    
    

    
    // Make creates this directory and any ancestor directories that do not
// exist (i.e. equivalent to Unix's 'mkdir -p').  Make is idempotent.

    public void make(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException;
    public void make(final com.veyron2.ipc.Context context, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException;

    
    

    
    // GetSyncGroupNames returns the global names of all SyncGroups attached
// to this directory.

    public java.util.List<java.lang.String> getSyncGroupNames(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException;
    public java.util.List<java.lang.String> getSyncGroupNames(final com.veyron2.ipc.Context context, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException;

}