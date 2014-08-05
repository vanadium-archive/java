// This file was auto-generated by the veyron vdl tool.
// Source(s):  advanced.vdl
package com.veyron2.vdl.test_arith.gen_impl;

/* Client stub for interface: AdvancedMath. */
public final class AdvancedMathStub implements com.veyron2.vdl.test_arith.AdvancedMath {
    private static final java.lang.String vdlIfacePathOpt = "com.veyron2.vdl.test_arith.AdvancedMath";
    private final com.veyron2.ipc.Client client;
    private final java.lang.String veyronName;

    
    
    
    private final com.veyron2.vdl.test_arith.gen_impl.TrigonometryStub trigonometryStub;
    
    
    private final com.veyron2.vdl.test_arith.exp.gen_impl.ExpStub expStub;
    

    public AdvancedMathStub(final com.veyron2.ipc.Client client, final java.lang.String veyronName) {
        this.client = client;
        this.veyronName = veyronName;
        
        
        this.trigonometryStub = new com.veyron2.vdl.test_arith.gen_impl.TrigonometryStub(client, veyronName);
         
        this.expStub = new com.veyron2.vdl.test_arith.exp.gen_impl.ExpStub(client, veyronName);
         
    }

    // Methods from interface AdvancedMath.





    @Override
    public double exp(final com.veyron2.ipc.Context context, final double x) throws com.veyron2.ipc.VeyronException {
        
        return this.expStub.exp(context, x);
    }
    @Override
    public double exp(final com.veyron2.ipc.Context context, final double x, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        return  this.expStub.exp(context, x, veyronOpts);
    }

    @Override
    public double cosine(final com.veyron2.ipc.Context context, final double angle) throws com.veyron2.ipc.VeyronException {
        
        return this.trigonometryStub.cosine(context, angle);
    }
    @Override
    public double cosine(final com.veyron2.ipc.Context context, final double angle, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        return  this.trigonometryStub.cosine(context, angle, veyronOpts);
    }

    @Override
    public double sine(final com.veyron2.ipc.Context context, final double angle) throws com.veyron2.ipc.VeyronException {
        
        return this.trigonometryStub.sine(context, angle);
    }
    @Override
    public double sine(final com.veyron2.ipc.Context context, final double angle, com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        
        return  this.trigonometryStub.sine(context, angle, veyronOpts);
    }


}
