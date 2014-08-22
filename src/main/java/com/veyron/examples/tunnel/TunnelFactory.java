// This file was auto-generated by the veyron vdl tool.
// Source(s):  tunnel.vdl
package com.veyron.examples.tunnel;

/* Factory for binding to Tunnel interfaces. */
public final class TunnelFactory {
    public static Tunnel bind(final java.lang.String name) throws com.veyron2.ipc.VeyronException {
        return bind(name, null);
    }
    public static Tunnel bind(final java.lang.String name, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        com.veyron2.ipc.Client client = null;
        if (veyronOpts != null && veyronOpts.get(com.veyron2.OptionDefs.CLIENT) != null) {
            client = veyronOpts.get(com.veyron2.OptionDefs.CLIENT, com.veyron2.ipc.Client.class);
        } else if (veyronOpts != null && veyronOpts.get(com.veyron2.OptionDefs.RUNTIME) != null) {
            client = veyronOpts.get(com.veyron2.OptionDefs.RUNTIME, com.veyron2.Runtime.class).getClient();
        } else {
            client = com.veyron2.RuntimeFactory.defaultRuntime().getClient();
        }
        return new com.veyron.examples.tunnel.gen_impl.TunnelStub(client, name);
    }
}