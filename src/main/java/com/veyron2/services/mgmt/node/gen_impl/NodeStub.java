// This file was auto-generated by the veyron vdl tool.
// Source(s):  node.vdl
package com.veyron2.services.mgmt.node.gen_impl;

/* Client stub for interface: Node. */
public final class NodeStub implements com.veyron2.services.mgmt.node.Node {
    private static final java.lang.String vdlIfacePathOpt = "com.veyron2.services.mgmt.node.Node";
    private final com.veyron2.ipc.Client client;
    private final java.lang.String veyronName;

    
    
    
    private final com.veyron2.services.mgmt.node.gen_impl.ApplicationStub applicationStub;
    

    public NodeStub(final com.veyron2.ipc.Client client, final java.lang.String veyronName) {
        this.client = client;
        this.veyronName = veyronName;
        
        
        this.applicationStub = new com.veyron2.services.mgmt.node.gen_impl.ApplicationStub(client, veyronName);
         
    }

    // Methods from interface Node.


    
    public com.veyron2.services.mgmt.node.Description describe(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        return describe(context, null);
    }
    
    public com.veyron2.services.mgmt.node.Description describe(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        // Add VDL path option.
        // NOTE(spetrovic): this option is temporary and will be removed soon after we switch
        // Java to encoding/decoding from vom.Value objects.
        if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
        if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
            veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, NodeStub.vdlIfacePathOpt);
        }

        
        // Start the call.
        final java.lang.Object[] inArgs = new java.lang.Object[]{  };
        final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.veyronName, "describe", inArgs, veyronOpts);

        // Finish the call.
        
        

         
        final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{
            
            new com.google.common.reflect.TypeToken<com.veyron2.services.mgmt.node.Description>() {
                private static final long serialVersionUID = 1L;
            },
            
        };
        final java.lang.Object[] results = call.finish(resultTypes);
         
        return (com.veyron2.services.mgmt.node.Description)results[0];
         

         

        
    }

    
    public boolean isRunnable(final com.veyron2.ipc.Context context, final com.veyron2.services.mgmt.binary.Description Description) throws com.veyron2.ipc.VeyronException {
        return isRunnable(context, Description, null);
    }
    
    public boolean isRunnable(final com.veyron2.ipc.Context context, final com.veyron2.services.mgmt.binary.Description Description, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        // Add VDL path option.
        // NOTE(spetrovic): this option is temporary and will be removed soon after we switch
        // Java to encoding/decoding from vom.Value objects.
        if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
        if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
            veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, NodeStub.vdlIfacePathOpt);
        }

        
        // Start the call.
        final java.lang.Object[] inArgs = new java.lang.Object[]{ Description };
        final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.veyronName, "isRunnable", inArgs, veyronOpts);

        // Finish the call.
        
        

         
        final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{
            
            new com.google.common.reflect.TypeToken<java.lang.Boolean>() {
                private static final long serialVersionUID = 1L;
            },
            
        };
        final java.lang.Object[] results = call.finish(resultTypes);
         
        return (java.lang.Boolean)results[0];
         

         

        
    }

    
    public void reset(final com.veyron2.ipc.Context context, final long Deadline) throws com.veyron2.ipc.VeyronException {
         reset(context, Deadline, null);
    }
    
    public void reset(final com.veyron2.ipc.Context context, final long Deadline, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        // Add VDL path option.
        // NOTE(spetrovic): this option is temporary and will be removed soon after we switch
        // Java to encoding/decoding from vom.Value objects.
        if (veyronOpts == null) veyronOpts = new com.veyron2.Options();
        if (!veyronOpts.has(com.veyron2.OptionDefs.VDL_INTERFACE_PATH)) {
            veyronOpts.set(com.veyron2.OptionDefs.VDL_INTERFACE_PATH, NodeStub.vdlIfacePathOpt);
        }

        
        // Start the call.
        final java.lang.Object[] inArgs = new java.lang.Object[]{ Deadline };
        final com.veyron2.ipc.Client.Call call = this.client.startCall(context, this.veyronName, "reset", inArgs, veyronOpts);

        // Finish the call.
        
        

        
        final com.google.common.reflect.TypeToken<?>[] resultTypes = new com.google.common.reflect.TypeToken<?>[]{};
        call.finish(resultTypes);
         

        
    }




    @Override
    public void revert(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.revert(context);
    }
    @Override
    public void revert(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.revert(context, veyronOpts);
    }

    @Override
    public void suspend(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.suspend(context);
    }
    @Override
    public void suspend(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.suspend(context, veyronOpts);
    }

    @Override
    public void updateTo(final com.veyron2.ipc.Context context, final java.lang.String Name) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.updateTo(context, Name);
    }
    @Override
    public void updateTo(final com.veyron2.ipc.Context context, final java.lang.String Name, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.updateTo(context, Name, veyronOpts);
    }

    @Override
    public java.lang.String install(final com.veyron2.ipc.Context context, final java.lang.String Name) throws com.veyron2.ipc.VeyronException {
        
        return this.applicationStub.install(context, Name);
    }
    @Override
    public java.lang.String install(final com.veyron2.ipc.Context context, final java.lang.String Name, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        return  this.applicationStub.install(context, Name, veyronOpts);
    }

    @Override
    public void refresh(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.refresh(context);
    }
    @Override
    public void refresh(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.refresh(context, veyronOpts);
    }

    @Override
    public void restart(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.restart(context);
    }
    @Override
    public void restart(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.restart(context, veyronOpts);
    }

    @Override
    public void resume(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.resume(context);
    }
    @Override
    public void resume(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.resume(context, veyronOpts);
    }

    @Override
    public java.util.ArrayList<java.lang.String> start(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
        return this.applicationStub.start(context);
    }
    @Override
    public java.util.ArrayList<java.lang.String> start(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        return  this.applicationStub.start(context, veyronOpts);
    }

    @Override
    public void stop(final com.veyron2.ipc.Context context, final long Deadline) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.stop(context, Deadline);
    }
    @Override
    public void stop(final com.veyron2.ipc.Context context, final long Deadline, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.stop(context, Deadline, veyronOpts);
    }

    @Override
    public void uninstall(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.uninstall(context);
    }
    @Override
    public void uninstall(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.uninstall(context, veyronOpts);
    }

    @Override
    public void update(final com.veyron2.ipc.Context context) throws com.veyron2.ipc.VeyronException {
        
         this.applicationStub.update(context);
    }
    @Override
    public void update(final com.veyron2.ipc.Context context, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
          this.applicationStub.update(context, veyronOpts);
    }


}
