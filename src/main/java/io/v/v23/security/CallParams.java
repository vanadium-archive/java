package io.v.v23.security;

import org.joda.time.DateTime;

import io.v.v23.context.VContext;
import io.v.v23.vdl.VdlValue;

/**
 * CallParams stores security-call creation parameters.  Here is an example of a simple
 * call creation:
 * <code>
 *   ...
 *   final Call call = Security.newCall(new CallParams()
 *           .withLocalPrincipal(Security.newPrincipal())
 *           .withMethodName("test")
 *           .withTimestamp(DateTime.now());
 *   ...
 * <code>
 *
 * CallParams form a tree where derived params are children of the params from which they
 * were derived.  Children inherit all the properties of their parent except for the property being
 * replaced (the principal/method/timestamp in the example above).
 */
public class CallParams {
    final CallParams parent;

    private DateTime timestamp;
    private String method;
    private VdlValue[] methodTags;
    private String suffix;
    private String localEndpoint;
    private String remoteEndpoint;
    private Principal principal;
    private Blessings localBlessings;
    private Blessings remoteBlessings;
    private io.v.v23.context.VContext context;

    public CallParams() {
        this.parent = null;
    }

    private CallParams(CallParams parent) {
        this.parent = parent;
    }

    /**
     * Returns a child of the current params with the given timestamp attached.
     *
     * @param  time timestamp
     * @return      a child of the current params with the given timestamp attached
     */
    public CallParams withTimestamp(DateTime time) {
        final CallParams ret = new CallParams(this);
        ret.timestamp = time;
        return ret;
    }
    /**
     * Returns a child of the current params with the given method name attached.
     *
     * @param  method method name
     * @return      a child of the current params with the given method name attached
     */
    public CallParams withMethod(String method) {
        final CallParams ret = new CallParams(this);
        ret.method = method;
        return ret;
    }
    /**
     * Returns a child of the current params with the given method tags attached.
     *
     * @param  tags method tags
     * @return      a child of the current params with the given method tags attached
     */
    public CallParams withMethodTags(VdlValue... tags) {
        final CallParams ret = new CallParams(this);
        ret.methodTags = tags;
        return ret;
    }
    /**
     * Returns a child of the current params with the given veyron name suffix attached.
     *
     * @param  suffix veyron name suffix
     * @return      a child of the current params with the given veyron name suffix attached
     */
    public CallParams withSuffix(String suffix) {
        final CallParams ret = new CallParams(this);
        ret.suffix = suffix;
        return ret;
    }
    /**
     * Returns a child of the current params with the given local endpoint attached.
     *
     * @param  endpoint local endpoint
     * @return      a child of the current params with the given local endpoint attached
     */
    public CallParams withLocalEndpoint(String endpoint) {
        final CallParams ret = new CallParams(this);
        ret.localEndpoint = endpoint;
        return ret;
    }
    /**
     * Returns a child of the current params with the given remote endpoint attached.
     *
     * @param  endpoint remote endpoint
     * @return      a child of the current params with the given remote endpoint attached
     */
    public CallParams withRemoteEndpoint(String endpoint) {
        final CallParams ret = new CallParams(this);
        ret.remoteEndpoint = endpoint;
        return ret;
    }
    /**
     * Returns a child of the current params with the given local principal attached.
     *
     * @param  principal local principal
     * @return      a child of the current params with the given local principal attached
     */
    public CallParams withLocalPrincipal(Principal principal) {
        final CallParams ret = new CallParams(this);
        ret.principal = principal;
        return ret;
    }
    /**
     * Returns a child of the current params with the given local blessings attached.
     *
     * @param  blessings local blessings
     * @return      a child of the current params with the given local blessings attached
     */
    public CallParams withLocalBlessings(Blessings blessings) {
        final CallParams ret = new CallParams(this);
        ret.localBlessings = blessings;
        return ret;
    }
    /**
     * Returns a child of the current params with the given remote blessings attached.
     *
     * @param  blessings remote blessings
     * @return      a child of the current params with the given remote blessings attached
     */
    public CallParams withRemoteBlessings(Blessings blessings) {
        final CallParams ret = new CallParams(this);
        ret.remoteBlessings = blessings;
        return ret;
    }
    /**
     * Returns a child of the current params with the given Vanadium context attached.
     *
     * @param  context Vanadium context
     * @return         a child of the current params with the given Vanadium context attached
     */
    public CallParams withContext(VContext context) {
        final CallParams ret = new CallParams(this);
        ret.context = context;
        return ret;
    }
    /**
     * Returns a timestamp attached to the params, or {@code null} if no timestamp is attached.
     *
     * @return timestamp attached to the params
     */
    public DateTime getTimestamp() {
        if (this.timestamp != null) return this.timestamp;
        if (this.parent != null) return this.parent.getTimestamp();
        return null;
    }
    /**
     * Returns a method name attached to the params, or {@code null} if no method name is attached.
     *
     * @return method name attached to the params
     */
    public String getMethod() {
        if (this.method != null) return this.method;
        if (this.parent != null) return this.parent.getMethod();
        return null;
    }
    /**
     * Returns method tags attached to the params, or {@code null} if no method tags are attached.
     *
     * @return method tags attached to the params
     */
    public VdlValue[] getMethodTags() {
        if (this.methodTags != null) return this.methodTags;
        if (this.parent != null) return this.parent.getMethodTags();
        return null;
    }
    /**
     * Returns a veyron suffix attached to the params, or {@code null} if no veyron suffix is
     * attached.
     *
     * @return veyron suffix attached to the params
     */
    public String getSuffix() {
        if (this.suffix != null) return this.suffix;
        if (this.parent != null) return this.parent.getSuffix();
        return null;
    }
    /**
     * Returns a local endpoint attached to the params, or {@code null} if no local endpoint is
     * attached.
     *
     * @return local endpoint attached to the params
     */
    public String getLocalEndpoint() {
        if (this.localEndpoint != null) return this.localEndpoint;
        if (this.parent != null) return this.parent.getLocalEndpoint();
        return null;
    }
    /**
     * Returns a remote endpoint attached to the params, or {@code null} if no remote endpoint
     * is attached.
     *
     * @return remote endpoint attached to the params
     */
    public String getRemoteEndpoint() {
        if (this.remoteEndpoint != null) return this.remoteEndpoint;
        if (this.parent != null) return this.parent.getRemoteEndpoint();
        return null;
    }
    /**
     * Returns a local principal attached to the params, or {@code null} if no local principal is
     * attached.
     *
     * @return local principal attached to the params
     */
    public Principal getLocalPrincipal() {
        if (this.principal != null) return this.principal;
        if (this.parent != null) return this.parent.getLocalPrincipal();
        return null;
    }
    /**
     * Returns local blessings attached to the params, or {@code null} if no local blessings are
     * attached.
     *
     * @return local blessings attached to the params
     */
    public Blessings getLocalBlessings() {
        if (this.localBlessings != null) return this.localBlessings;
        if (this.parent != null) return this.parent.getLocalBlessings();
        return null;
    }
    /**
     * Returns remote blessings attached to the params, or {@code null} if no remote blessings are
     * attached.
     *
     * @return remote blessings attached to the params
     */
    public Blessings getRemoteBlessings() {
        if (this.remoteBlessings != null) return this.remoteBlessings;
        if (this.parent != null) return this.parent.getRemoteBlessings();
        return null;
    }
    /**
     * Returns Vanadium context attached to the params, or {@code null} if no Vanadium context is
     * attached.
     *
     * @return Vanadium context attached to the params
     */
    public VContext getContext() {
        if (this.context != null) return this.context;
        if (this.parent != null) return this.parent.getContext();
        return null;
    }

}