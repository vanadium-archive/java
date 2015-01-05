package io.v.core.veyron2;

/**
 * OptionDefs defines commonly used options in the Veyron runtime.
 */
public class OptionDefs {
    /**
     * A key for an option of type {@link io.v.core.veyron2.VRuntimeImpl} that
     * specifies a runtime implementation.
     */
    public static String RUNTIME = "io.v.core.veyron2.RUNTIME";

    /**
     * A key for an option of type {@link io.v.core.veyron2.ipc.Client} that
     * specifies a client.
     */
    public static String CLIENT = "io.v.core.veyron2.CLIENT";

    /**
     * A key for an option of type {@link io.v.core.veyron2.security.Principal} that
     * specifies a principal to be assigned to the runtime.
     */
    public static String RUNTIME_PRINCIPAL = "io.v.core.veyron2.RUNTIME_PRINCIPAL";

    /**
     * A key for an option of type {@link io.v.core.veyron2.ipc.ListenSpec} that specifies
     * a default listen spec to be used by the servers when the client passes in a {@code null}
     * listen spec to their {@code listen()} calls.
     */
    public static String DEFAULT_LISTEN_SPEC = "io.v.core.veyron2.DEFAULT_LISTEN_SPEC";
}