package com.veyron2;

/**
 * OptionDefs defines commonly used options in the Veyron runtime.
 */
public class OptionDefs {
    /**
     * A key for an option of type {@link org.joda.time.Duration} that denotes a call timeout.
     */
    public static String CALL_TIMEOUT = "com.veyron2.CALL_TIMEOUT";
    /**
     * A key for an option of type {@link java.lang.String} that specifies the VDL interface path.
     * NOTE(spetrovic): This option is temporary and will be removed soon after we switch
     * Java to encoding/decoding from vom.Value objects.
     */
    public static String VDL_INTERFACE_PATH = "com.veyron2.VDL_INTERFACE_PATH";

    /**
     * A key for an option of type {@link com.veyron2.Runtime} that specifies a Runtime.
     */
    public static String RUNTIME = "com.veyron2.RUNTIME";

    /**
     * A key for an option of type {@link com.veyron2.ipc.Client} that specifies a Client.
     */
    public static String CLIENT = "com.veyron2.CLIENT";
}