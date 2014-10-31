package io.veyron.veyron.veyron2;

/**
 * OptionDefs defines commonly used options in the Veyron runtime.
 */
public class OptionDefs {
    /**
     * A key for an option of type {@link java.lang.String} that specifies the VDL interface path.
     * NOTE(spetrovic): This option is temporary and will be removed soon after we switch
     * Java to encoding/decoding from vom.Value objects.
     */
    public static String VDL_INTERFACE_PATH = "io.veyron.veyron.veyron2.VDL_INTERFACE_PATH";

    /**
     * A key for an option of type {@link io.veyron.veyron.veyron2.Runtime} that
     * specifies a Runtime.
     */
    public static String RUNTIME = "io.veyron.veyron.veyron2.RUNTIME";

    /**
     * A key for an option of type {@link io.veyron.veyron.veyron2.ipc.Client} that
     * specifies a Client.
     */
    public static String CLIENT = "io.veyron.veyron.veyron2.CLIENT";

    /**
     * A key for an option of type {@link io.veyron.veyron.veyron2.security.Principal} that
     * specifies a Principal for the Runtime.
     */
    public static String RUNTIME_PRINCIPAL = "io.veyron.veyron.veyron2.RUNTIME_PRINCIPAL";
}