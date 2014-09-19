package io.veyron.veyron.veyron2;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.util.HashMap;
import java.util.Map;

/**
 * Options is a holder for options used by various Veyron methods.  Each option has a String key
 * and an arbitrary option value.
 */
public class Options {
    private Map<String, Object> options = new HashMap<String, Object>();

    /**
     * Returns true iff the option with the provided key exists.  (Note that if the option exists
     * but its value is null, this method will return true.)
     *
     * @param  key key of the option whose existance is checked.
     * @return     true iff the option with the above key exists.
     */
    public boolean has(String key) {
        return this.options.containsKey(key);
    }

    /**
     * Returns the option with the provided key.
     *
     * @param  key key of the option we want to get.
     * @return     an option with the provided key, or null if no such option exists.
     */
    public Object get(String key) {
        return this.options.get(key);
    }

    /**
     * Returns the option of the provided type with the given key.
     *
     * @param key              key of the option we want to get.
     * @param type             type of the option we want to get.
     * @return                 an option with the provided key, or null if no such option exists.
     * @throws VeyronException if the option isn't of the provided type.
     */
    public <T> T get(String key, Class<T> type) throws VeyronException {
        final Object opt = this.options.get(key);
        if (opt == null) return null;
        if (!type.isAssignableFrom(opt.getClass())) {
            throw new VeyronException(String.format("Expected type %s for option %s", type, key));
        }
        return type.cast(opt);
    }

    /**
     * Associates option value with the provided key.  If an option with the
     * same key already exist, its value will be overwritten.
     *
     * @param key     key of the option we are setting.
     * @param value   an option we are setting.
     */
    public void set(String key, Object value) {
        this.options.put(key, value);
    }

    /**
     * Removes the option with a given key.  A no-op if the option doesn't exist.
     *
     * @param key a key of an option to be removed.
     */
    public void remove(String key) {
        this.options.remove(key);
    }
}