package io.veyron.veyron.veyron2;

/**
 * OptionDefs defines commonly used options in the Veyron runtime.
 */
public class OptionDefs {
    /**
     * A key for an option of type {@link org.joda.time.Duration} that denotes a call timeout.
     */
    public static String CALL_TIMEOUT = "io.veyron.veyron.veyron2.CALL_TIMEOUT";
    /**
     * A key for an option of type {@link java.lang.String} that specifies the VDL interface path.
     * NOTE(spetrovic): This option is temporary and will be removed soon after we switch
     * Java to encoding/decoding from vom.Value objects.
     */
    public static String VDL_INTERFACE_PATH = "io.veyron.veyron.veyron2.VDL_INTERFACE_PATH";

    /**
     * A key for an option of type {@link io.veyron.veyron.veyron2.Runtime} that specifies a Runtime.
     */
    public static String RUNTIME = "io.veyron.veyron.veyron2.RUNTIME";

    /**
     * A key for an option of type {@link io.veyron.veyron.veyron2.ipc.Client} that specifies a Client.
     */
    public static String CLIENT = "io.veyron.veyron.veyron2.CLIENT";

    /**
     * A key for an option of type {@link io.veyron.veyron.veyron2.security.PrivateID} that specifies the
     * identity to be used by the runtime.
     */
    public static String RUNTIME_ID = "io.veyron.veyron.veyron2.RUNTIME_ID";

    /**
     * A key for an option of type {@link io.veyron.veyron.veyron2.security.PublicIDStore} that specifies
     * the public id store to be used by the runtime.
     */
    public static String RUNTIME_PUBLIC_ID_STORE = "io.veyron.veyron.veyron2.RUNTIME_PUBLIC_ID_STORE";
}