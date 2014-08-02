// This file was auto-generated by the veyron vdl tool.
// Source(s):  service.vdl
package com.veyron2.services.store;

/* Factory for binding to TransactionRoot interfaces. */
public final class TransactionRootFactory {
    public static TransactionRoot bind(final java.lang.String name) throws com.veyron2.ipc.VeyronException {
        return bind(name, null);
    }
    public static TransactionRoot bind(final java.lang.String name, final com.veyron2.Options veyronOpts) throws com.veyron2.ipc.VeyronException {
        com.veyron2.ipc.Client client = null;
        if (veyronOpts != null && veyronOpts.get(com.veyron2.OptionDefs.CLIENT) != null) {
            client = veyronOpts.get(com.veyron2.OptionDefs.CLIENT, com.veyron2.ipc.Client.class);
        } else if (veyronOpts != null && veyronOpts.get(com.veyron2.OptionDefs.RUNTIME) != null) {
            client = veyronOpts.get(com.veyron2.OptionDefs.RUNTIME, com.veyron2.Runtime.class).getClient();
        } else {
            client = com.veyron2.RuntimeFactory.defaultRuntime().getClient();
        }
        return new com.veyron2.services.store.gen_impl.TransactionRootStub(client, name);
    }
}
