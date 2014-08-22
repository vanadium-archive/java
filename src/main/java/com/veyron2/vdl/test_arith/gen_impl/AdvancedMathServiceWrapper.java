// This file was auto-generated by the veyron vdl tool.
// Source(s):  advanced.vdl
package com.veyron2.vdl.test_arith.gen_impl;

public final class AdvancedMathServiceWrapper {

    private final com.veyron2.vdl.test_arith.AdvancedMathService service;



    
    private final com.veyron2.vdl.test_arith.gen_impl.TrigonometryServiceWrapper trigonometryWrapper;
    
    
    private final com.veyron2.vdl.test_arith.exp.gen_impl.ExpServiceWrapper expWrapper;
    

    public AdvancedMathServiceWrapper(final com.veyron2.vdl.test_arith.AdvancedMathService service) {
        this.service = service;
        
        
        this.trigonometryWrapper = new com.veyron2.vdl.test_arith.gen_impl.TrigonometryServiceWrapper(service);
        
        this.expWrapper = new com.veyron2.vdl.test_arith.exp.gen_impl.ExpServiceWrapper(service);
        
    }

    /**
     * Returns all tags associated with the provided method or null if the method isn't implemented
     * by this service.
     */
    public java.lang.Object[] getMethodTags(final com.veyron2.ipc.ServerCall call, final java.lang.String method) throws com.veyron2.ipc.VeyronException {
        
        if ("getMethodTags".equals(method)) {
            return new java.lang.Object[] {
                
            };
        }
        
        
        try {
            return this.trigonometryWrapper.getMethodTags(call, method);
        } catch (com.veyron2.ipc.VeyronException e) {}  // method not found.
        
        try {
            return this.expWrapper.getMethodTags(call, method);
        } catch (com.veyron2.ipc.VeyronException e) {}  // method not found.
        
        throw new com.veyron2.ipc.VeyronException("method: " + method + " not found");
    }

     
    



    public double exp(final com.veyron2.ipc.ServerCall call, final double x) throws com.veyron2.ipc.VeyronException {
        
        return  this.expWrapper.exp(call, x);
    }

    public double cosine(final com.veyron2.ipc.ServerCall call, final double angle) throws com.veyron2.ipc.VeyronException {
        
        return  this.trigonometryWrapper.cosine(call, angle);
    }

    public double sine(final com.veyron2.ipc.ServerCall call, final double angle) throws com.veyron2.ipc.VeyronException {
        
        return  this.trigonometryWrapper.sine(call, angle);
    }
 

}