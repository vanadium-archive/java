// This file was auto-generated by the veyron vdl tool.
// Source: base.vdl
package com.veyron2.vdl.test_base;


@com.veyron2.vdl.VeyronService(serviceWrapper=com.veyron2.vdl.test_base.gen_impl.ServiceAServiceWrapper.class)
public interface ServiceAService  {

    
    
    public void methodA1(final com.veyron2.ipc.ServerContext context) throws com.veyron2.ipc.VeyronException;

    
    
    public java.lang.String methodA2(final com.veyron2.ipc.ServerContext context, final int a, final java.lang.String b) throws com.veyron2.ipc.VeyronException;

    
    
    public java.lang.String methodA3(final com.veyron2.ipc.ServerContext context, final int a, com.veyron2.vdl.Stream<java.lang.Void, com.veyron2.vdl.test_base.Scalars> stream) throws com.veyron2.ipc.VeyronException;

    
    
    public void methodA4(final com.veyron2.ipc.ServerContext context, final int a, com.veyron2.vdl.Stream<java.lang.Integer, java.lang.String> stream) throws com.veyron2.ipc.VeyronException;

}