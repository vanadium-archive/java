package com.veyron2;

import com.veyron2.ipc.Client;
import com.veyron2.ipc.VeyronException;

import org.joda.time.Duration;

import java.util.Map;

/**
 * Options stores various options used by the Veyron runtime.
 */
public class Options {
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
    public static String CLIENT = "com.veyron2.RUNTIME";

    /**
     * Generic method that returns the option of the provided type with the provided key.
     *
     * @param options          the options list, stored as (key,value) pairs.
     * @param key              key of the option we want to return.
     * @param type             type of the option we want to return.
     * @return                 an option in the options list with the provided key.
     * @throws VeyronException if the option isn't of the provided type.
     */
    public static <T> T getOption(Map<String, Object> options, String key, Class<T> type)
        throws VeyronException {
        if (options == null) return null;
        final Object opt = options.get(key);
        if (opt == null) return null;
        if (!type.isAssignableFrom(opt.getClass())) {
            throw new VeyronException(String.format("Expected type %s for option %s", type, key));
        }
        return type.cast(opt);
    }

    /**
     * Generic method that associates the option value with the provided key.
     *
     * @param options the options list, stored as (key,value) pairs.
     * @param key     key of the option we are setting.
     * @param value   an option we are setting.
     */
    public static void setOption(Map<String, Object> options, String key, Object value) {
        if (options == null) return;
        options.put(key, value);
    }

    /**
     * Returns the call timeout option value from the provided list of options.
     *
     * @param  options         the options list, stored as (key,value) pairs.
     * @return                 value of the call timeout option.
     * @throws VeyronException if the option isn't of the expected type.
     */
    public static Duration getCallTimeout(Map<String, Object> options) throws VeyronException {
        return getOption(options, CALL_TIMEOUT, Duration.class);
    }
    /**
     * Returns the VDL interface path option value from the provided list of options.
     *
     * @param  options         the options list, stored as (key,value) pairs.
     * @return                 value of the VDL interface path option.
     * @throws VeyronException if the option isn't of the expected type.
     */
    public static String getVDLInterfacePath(Map<String, Object> options) throws VeyronException {
        return getOption(options, VDL_INTERFACE_PATH, String.class);
    }
    /**
     * Returns the runtime option value from the provided list of options.
     *
     * @param  options         the options list, stored as (key,value) pairs.
     * @return                 value of the runtime option.
     * @throws VeyronException if the option isn't of the expected type.
     */
    public static Runtime getRuntime(Map<String, Object> options) throws VeyronException {
        return getOption(options, RUNTIME, Runtime.class);
    }
    /**
     * Returns the client option value from the provided list of options.
     *
     * @param  options         the options list, stored as (key,value) pairs.
     * @return                 value of the client option.
     * @throws VeyronException if the option isn't of the expected type.
     */
    public static Client getClient(Map<String, Object> options) throws VeyronException {
        return getOption(options, CLIENT, Client.class);
    }

    /**
     * Sets the call timeout option.
     *
     * @param options the options list, stored as (key, value) pairs.
     * @param value   value of the call timeout option.
     */
    public static void setCallTimeout(Map<String, Object> options, Duration value) {
        setOption(options, CALL_TIMEOUT, value);
    }

    /**
     * Sets the VDL interface path option.
     *
     * @param options the options list, stored as (key, value) pairs.
     * @param value   value of the VDL interface path option.
     */
    public static void setVDLInterfacePath(Map<String, Object> options, String value) {
        setOption(options, CALL_TIMEOUT, value);
    }

    /**
     * Sets the runtime option.
     *
     * @param options the options list, stored as (key, value) pairs.
     * @param value   value of the runtime option.
     */
    public static void setRuntime(Map<String, Object> options, Runtime value) {
        setOption(options, CALL_TIMEOUT, value);
    }

    /**
     * Sets the client option.
     *
     * @param options the options list, stored as (key, value) pairs.
     * @param value   value of the client option.
     */
    public static void setClient(Map<String, Object> options, Client value) {
        setOption(options, CALL_TIMEOUT, value);
    }
}