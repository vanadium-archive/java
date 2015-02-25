package io.v.v23.verror;

import io.v.v23.context.VContext;
import io.v.v23.i18n.Language;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlType;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * VException provides a generic mechanism for capturing errors that occurred in the Vanadium
 * environment, both in the core Vanadium code as well as the user-defined clients and servers.
 *
 * Each exception has an identifier associated with it that uniquely represents an error.
 * Two exceptions are equal iff their identifiers are equal, regardless of the associated
 * messages.  This allows the user to throw exceptions with different messages (e.g., multiple
 * languages) yet have the caller interpret them all as the same error.
 *
 * To define a new error identifier, for example {@code "someNewError"}, client code is
 * expected to declare a variable like this:
 * <code>
 *     final IDAction someNewError = VException.register(
 *             "my/package/name.someNewError",
 *             VException.ActionCode.NO_RETRY,
 *             "{1} {2} English text for new error");
 * </code>
 *
 * Error identifier strings should start with the package path to ensure uniqueness.  Note that the
 * package paths are separated with {@code "/"} delimiter; this is a chosen convention to make
 * the error uniquely identifiable across various programming languages.
 *
 * The purpose of an action code is so clients not familiar with an error can retry appropriately.
 * Errors are registered with their English text, but the text for other languages can subsequently
 * be added to the default i18n {@code Catalog} using the returned error identifier.
 *
 * Exceptions are given parameters when created.  Conventionally, the first parameter
 * is the name of the component (typically server or binary name), and the second is the name of the
 * operation (such as an RPC or sub-command) that encountered the error.  Other parameters typically
 * identify the object(s) on which the error occurred.  This convention is normally applied by
 * {@code make()}, which fetches the language, component name and operation name from the
 * current context:
 * <code>
 *      final VException e = VException.make(someNewError, ctx, "object_on_which_error_occurred");
 * </code>
 *
 * The {@code explicitMake()} method can be used to specify these things explicitly:
 * <code>
 *      final VException e = VException.explicitMake(
 *              someNewError, "en", "my_component", "op_name", "procedure_name", "object_name");
 * </code>
 *
 * If the language, component and/or operation name are unknown, use the empty string.
 *
 * Because of the convention for the first two parameters, error messages in the catalog typically
 * look like this (at least for left-to-right languages):
 * <code>
 *      {1} {2} The new error {_}
 * </code>
 *
 * The tokens <code>{1}</code>, </code>{2}</code>, etc.  refer to the first and second positional
 * parameters respectively, while <code>{_}</code> is replaced by the positional parameters not
 * explicitly referred to elsewhere in the message.  Thus, given the parameters above, this would
 * lead to the output:
 * <code>
 *      my_component op_name The new error object_name
 * </code>
 *
 * If a substring is of the form <code>{:<number>}, {<number>:}, {:<number>:}, {:_}, {_:}, or {:_:}
 * </code>, and the corresponding parameters are not the empty string, the parameter is preceded by
 * {@code ": "} or followed by {@code ":"} or both, respectively.
 */
public class VException extends Exception {
    private static VContext defaultContext = null;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * ActionCode represents the action expected to be performed by a typical client receiving
     * an error that perhaps it does not understand.
     */
    public static enum ActionCode {
        NO_RETRY         (0),
        RETRY_CONNECTION (1),
        RETRY_REFETCH    (2),
        RETRY_BACKOFF    (3);

        public static ActionCode fromValue(int value) {
            switch (value) {
                case 0: return NO_RETRY;
                case 1: return RETRY_CONNECTION;
                case 2: return RETRY_REFETCH;
                case 3: return RETRY_BACKOFF;
                default: return NO_RETRY;
            }
        }

        private final int value;

        private ActionCode(int value) {
            this.value = value;
        }
        public int getValue() { return this.value; }
    }

    /**
     * IDAction combines a unique identifier for errors with an {@code ActionCode}.  The identifier
     * allows stable error checking across different error messages and different address spaces.
     *
     * By convention the format for the identifier is "PKGPATH.NAME" - e.g. {@code errIDFoo} defined
     * in the {@code io.v.v23.verror} package has id
     * {@code io.v.v23.verror.errIDFoo}.  It is unwise ever to create two {@code IDAction}s
     * that associate different {@code ActionCode}s with the same id.
    */
    public static class IDAction {
        private final String id;
        private final ActionCode action;

        public IDAction(String id, ActionCode action) {
            this.id = id == null ? "" : id;
            this.action = action;
        }

        public String getID() { return this.id; }

        public ActionCode getAction() { return this.action; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (this.getClass() != obj.getClass()) return false;
            final IDAction other = (IDAction) obj;
            if (!this.id.equals(other.id)) return false;
            if (this.action != other.action) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = 1;
            final int prime = 31;
            result = prime * result + id.hashCode();
            result = prime * result + action.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("{ID: %s, Action: %s}", this.id, this.action);
        }
    }

    /**
     * Register returns an {@code IDAction} with the given identifier and action fields and
     * inserts a message into the default i18n catalogue in US English.  Other languages can be
     * added by directly modifying the catalogue.
     *
     * @param  id          error identifier
     * @param  action      error action
     * @param  englishText english message associated with the error
     * @return             {@code IDAction} with the given identifier and action fields
     */
    public static IDAction register(String id, ActionCode action, String englishText) {
        Language.getDefaultCatalog().setWithBase("en-US", id, englishText);
        return new IDAction(id, action);
    }

    /*
     * Returns an error with the given identifier and an error string in the chosen language.
     * The component and operation name are included the first and second parameters of the error.
     * Other parameters are taken from {@code params}. The parameters are formatted into the message
     * according to the format described in the {@code VException} documentation.
     *
     * The provided parameters need to be VOM-encodeable.  This means that they have to be of one
     * of the supported VOM types, which includes all Java primitive types, all VDL types, and
     * Lists/Maps/Sets of those.
     *
     * @param  idAction      error identifier
     * @param  language      error language, in IETF format
     * @param  componentName error component name
     * @param  opName        error operation name
     * @param  params        error message parameters
     * @return               an error with the given identifier and an error string in the given
     *                       language
     */
    public static VException explicitMake(IDAction idAction, String language, String componentName,
        String opName, Serializable... params) {
        final Type[] paramTypes = new Type[params.length];
        for (int i = 0; i < params.length; ++i) {
            paramTypes[i] = params[i].getClass();
        }
        return explicitMake(idAction, language, componentName, opName, paramTypes, params);
    }

    /**
     * Just like {@code explicitMake} but explicitly provides the types for all parameters.  This is
     * necessary as parameters may need to be VOM-encoded (to be shipped across the wire), and Java
     * doesn't always have the ability to deduce the right type from the value (e.g., generic
     * parameters like List<String> or Map<String, Integer>).
     *
     * @param  idAction      error identifier
     * @param  language      error language, in IETF format
     * @param  componentName error component name
     * @param  opName        error operation name
     * @param  paramTypes    error message parameter types
     * @param  params        error message parameters
     * @return               an error with the given identifier and an error string in the given
     *                       language
     */
    public static VException explicitMake(IDAction idAction, String language, String componentName,
        String opName, Type[] paramTypes, Serializable[] params) {
        if (paramTypes == null) {
            paramTypes = new Type[0];
        }
        if (params == null) {
            params = new Serializable[0];
        }

        // Append componentName and opName to params.
        final Serializable[] newParams = new Serializable[params.length + 2];
        final Type[] newParamTypes = new Type[paramTypes.length + 2];
        newParams[0] = componentName;
        newParamTypes[0] = String.class;
        newParams[1] = opName;
        newParamTypes[1] = String.class;
        System.arraycopy(params, 0, newParams, 2, params.length);
        System.arraycopy(paramTypes, 0, newParamTypes, 2, params.length);
        final String msg = Language.getDefaultCatalog().format(
                    language, idAction.getID(), (Object[]) newParams);
        return new VException(idAction, msg, newParams, newParamTypes);
    }

    /**
     * Just like {@code explicitMake} but obtains the language, component name, and operation
     * name from the given context.  If the provided context is {@code null}, default values
     * are used.
     *
     * @param  idAction error identifier
     * @param  ctx      context
     * @param  params   error message parameters
     * @return          an error with the given identifier and an error string in the inferred
     *                  language
     */
    public static VException make(IDAction idAction, VContext ctx, Serializable... params) {
        final Type[] paramTypes = new Type[params.length];
        for (int i = 0; i < params.length; ++i) {
            paramTypes[i] = params[i] == null ? String.class : params[i].getClass();
        }
        return make(idAction, ctx, paramTypes, params);
    }

    /**
     * Just like {@code make} but explicitly provides the types for all parameters.  This is
     * necessary as parameters may need to be VOM-encoded (to be shipped across the wire), and Java
     * doesn't always have the ability to deduce the right type from the value (e.g., generic
     * parameters like {@code List<String>} or {@code Map<String, Integer>}).
     *
     * @param  idAction   error identifier
     * @param  ctx        context
     * @param  paramTypes error message parameter types
     * @param  params     error message parameters
     * @return            an error with the given identifier and an error string in the inferred
     *                    language
     */
    public static VException make(
        IDAction idAction, VContext ctx, Type[] paramTypes, Serializable[] params) {
        final String language = languageFromContext(ctx);
        final String componentName = componentNameFromContext(ctx);
        // TODO(spetrovic): Implement the opName code.
        final String opName = "";
        return explicitMake(idAction, language, componentName, opName, paramTypes, params);
    }

    /**
     * Sets the default context that is used whenever a user passes in a {@code null} context
     * to {@code make} methods above.
     *
     * @param ctx default context
     */
    public static void setDefaultContext(VContext ctx) {
        lock.writeLock().lock();
        defaultContext = ctx;
        lock.writeLock().unlock();
    }

    /**
     * Returns a child of the given context that has the provided component name attached to it.
     *
     * @param  base          base context
     * @param  componentName a component name that is to be attached
     * @return               a child of the given context that has the provided component name
     *                       attached to it
     */
    public static VContext contextWithComponentName(VContext base, String componentName) {
        return base.withValue(new ComponentNameKey(), componentName);
    }

    private static String componentNameFromContext(VContext ctx) {
        if (ctx == null) {
            lock.readLock().lock();
            ctx = defaultContext;
            lock.readLock().unlock();
        }
        String componentName = "";
        if (ctx != null) {
            final Object value = ctx.value(new ComponentNameKey());
            if (value != null && value instanceof String) {
                componentName = (String) value;
            }
        }
        if (componentName.isEmpty()) {
            componentName = System.getProperty("program.name", "");
        }
        if (componentName.isEmpty()) {
            componentName = System.getProperty("user.name", "");
        }
        return componentName;
    }

    private static String languageFromContext(VContext ctx) {
        if (ctx == null) {
            lock.readLock().lock();
            ctx = defaultContext;
            lock.readLock().unlock();
        }
        String language = "";
        if (ctx != null) {
            language = Language.languageFromContext(ctx);
        }
        if (language.isEmpty()) {
            language = "en-US";
        }
        return language;
    }

    private static VdlType[] convertParamTypes(Type[] types) {
        if (types == null) {
            return null;
        }
        final VdlType[] vdlTypes = new VdlType[types.length];
        for (int i = 0; i < types.length; ++i) {
            try {
                vdlTypes[i] = Types.getVdlTypeFromReflect(types[i]);
            } catch (IllegalArgumentException e) {
                System.err.println(String.format(
                        "Couldn't determine VDL type for param reflect type %s.  This param will " +
                        "be dropped if ever VOM-encoded", types[i]));
                vdlTypes[i] = null;
            }
        }
        return vdlTypes;
    }

    private final IDAction id;  // non-null
    private final Serializable[] params;  // non-null
    private final VdlType[] paramTypes;  // non-null, same length as params

    public VException(String msg) {
        this(make(Errors.UNKNOWN, null, msg));
    }

    public VException(IDAction id, String msg, Serializable[] params, Type[] paramTypes) {
        this(id, msg, params, convertParamTypes(paramTypes));
    }

    public VException(IDAction id, String msg, Serializable[] params, VdlType[] paramTypes) {
        super(msg);
        this.id = id;
        params = params != null ? params : new Serializable[0];
        paramTypes = paramTypes != null ? paramTypes : new VdlType[0];
        if (params.length != paramTypes.length) {
            System.err.println(String.format(
                    "Passed different number of types (%s) than parameters (%s) to VException. " +
                    "Some params may be dropped.", paramTypes, params));
            final int length =
                    params.length < paramTypes.length ? params.length : paramTypes.length;
            params = Arrays.copyOf(params, length);
            paramTypes = Arrays.copyOf(paramTypes, length);
        }
        this.params = params;
        this.paramTypes = paramTypes;
    }

    private VException(VException other) {
        this(other.id, other.getMessage(), other.params, other.paramTypes);
    }

    /**
     * Returns the error identifier associated with this exception.
     *
     * @return the error identifier associated with this exception
     */
    public String getID() {
        return this.id.getID();
    }

    /**
     * Returns the action associated with this exception.
     *
     * @return the action associated with this exception
     */
    public ActionCode getAction() {
        return this.id.getAction();
    }

    /**
     * Returns true iff the error identifier associated with this exception is equal to the provided
     * identifier.
     *
     * @param  id the error identifier we're comparing with this error
     * @return    true iff the error identifier associated with this exception is equal to the
     *            provided identifier.
     */
    public boolean is(String id) {
        return getID().equals(id);
    }

    /**
     * Returns true iff the error identifier associated with this exception is equal to the provided
     * identifier.
     *
     * @param  idAction the error identifier we're comparing with this error
     * @return          true iff the error identifier associated with this exception is equal to the
     *                  provided identifier.
     */
    public boolean is(IDAction idAction) {
        return is(idAction.getID());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        final VException other = (VException) obj;
        return this.getID().equals(other.getID());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("{IDAction: %s, Msg: \"%s\", Params: %s, ParamTypes: %s}", this.id,
                getMessage(), Arrays.toString(this.params), Arrays.toString(this.paramTypes));
    }

    /**
     * Returns true if this {@code VException} is deeply equal to the provided {@code Object}.
     * Unlike {@code equals()}, this method, in addition to comparing identifiers, also compares
     * action codes and parameters.
     *
     * @param  obj the other object we are testing for equality
     * @return     true if this {@code VException} is deeply equal to the other object
     */
    public boolean deepEquals(Object obj) {
        if (!equals(obj)) return false;
        final VException other = (VException) obj;
        // equals() has already compared the IDs.
        if (!getAction().equals(other.getAction())) return false;
        if (!Arrays.deepEquals(getParams(), other.getParams())) return false;
        return Arrays.equals(getParamTypes(), other.getParamTypes());
    }

    private static class ComponentNameKey {
        @Override
        public int hashCode() {
            return 0;
        }
    }

    /**
     * Returns the parameters associated with this exception.  The expectation is that the
     * first parameter will be a string that identifies the component (typically the server or
     * binary) where the error was generated, the second will be a string that identifies the
     * operation that encountered the error.  The remaining parameters typically identify the
     * object(s) on which the operation was acting.  A component passing on an error from another
     * may prefix the first parameter with its name, a colon and a space.
     *
     * @return the array of parameters associated with this exception
     */
    Serializable[] getParams() { return this.params; }

    /**
     * Returns the VDL types of all the parameters associated with this exception, in the same
     * order as {@code getParams()}.  This type information is necessary for VOM encoding as
     * Java doesn't always give us the ability to deduce the right type from the value (e.g.,
     * generic parameters like List<String> or Map<String, Integer>).
     *
     * NOTE: some types may be {@code null}, which means that the {@code VdlType} could not be
     * deduced from the Java reflect type.  In this case, the param will be dropped during the VOM
     * encoding.
     *
     * @return the array of VDL types of all the parameters associated with this exception.
     */
    VdlType[] getParamTypes() { return this.paramTypes; }
}