package io.v.core.veyron2.security;

import org.joda.time.DateTime;

/**
 * VContextParams stores security-context creation parameters.  Here is an example of a simple
 * context creation:
 * <code>
 *   ...
 *   final VContext ctx = Security.newContext(new VContextParams()
 *       .withLocalPrincipal(Security.newPrincipal())
 *       .withMethodName("test")
 *       .withTimestamp(DateTime.now());
 *   ...
 * <code>
 *
 * VContextParams form a tree where derived params are children of the params from which they
 * were derived.  Children inherit all the properties of their parent except for the property being
 * replaced (the principal/method/timestamp in the example above).
 */
public class VContextParams {
    final VContextParams parent;

    private DateTime timestamp;
    private String method;
    private Object[] methodTags;
    private String suffix;
    private String localEndpoint;
    private String remoteEndpoint;
    private Principal principal;
    private Blessings localBlessings;
    private Blessings remoteBlessings;

    public VContextParams() {
        this.parent = null;
    }

    private VContextParams(VContextParams parent) {
        this.parent = parent;
    }

    /**
     * Returns a child of the current params with the given timestamp attached.
     *
     * @param  time timestamp
     * @return      a child of the current params with the given timestamp attached.
     */
    public VContextParams withTimestamp(DateTime time) {
        final VContextParams ret = new VContextParams(this);
        ret.timestamp = time;
        return ret;
    }
    /**
     * Returns a child of the current params with the given method name attached.
     *
     * @param  method method name
     * @return      a child of the current params with the given method name attached.
     */
    public VContextParams withMethod(String method) {
        final VContextParams ret = new VContextParams(this);
        ret.method = method;
        return ret;
    }
    /**
     * Returns a child of the current params with the given method tags attached.
     *
     * @param  tags method tags
     * @return      a child of the current params with the given method tags attached.
     */
    public VContextParams withMethodTags(Object[] tags) {
        final VContextParams ret = new VContextParams(this);
        ret.methodTags = tags;
        return ret;
    }
    /**
     * Returns a child of the current params with the given veyron name suffix attached.
     *
     * @param  suffix veyron name suffix
     * @return      a child of the current params with the given veyron name suffix attached.
     */
    public VContextParams withSuffix(String suffix) {
        final VContextParams ret = new VContextParams(this);
        ret.suffix = suffix;
        return ret;
    }
    /**
     * Returns a child of the current params with the given local endpoint attached.
     *
     * @param  endpoint local endpoint
     * @return      a child of the current params with the given local endpoint attached.
     */
    public VContextParams withLocalEndpoint(String endpoint) {
        final VContextParams ret = new VContextParams(this);
        ret.localEndpoint = endpoint;
        return ret;
    }
    /**
     * Returns a child of the current params with the given remote endpoint attached.
     *
     * @param  endpoint remote endpoint
     * @return      a child of the current params with the given remote endpoint attached.
     */
    public VContextParams withRemoteEndpoint(String endpoint) {
        final VContextParams ret = new VContextParams(this);
        ret.remoteEndpoint = endpoint;
        return ret;
    }
    /**
     * Returns a child of the current params with the given local principal attached.
     *
     * @param  principal local principal
     * @return      a child of the current params with the given local principal attached.
     */
    public VContextParams withLocalPrincipal(Principal principal) {
        final VContextParams ret = new VContextParams(this);
        ret.principal = principal;
        return ret;
    }
    /**
     * Returns a child of the current params with the given local blessings attached.
     *
     * @param  blessings local blessings
     * @return      a child of the current params with the given local blessings attached.
     */
    public VContextParams withLocalBlessings(Blessings blessings) {
        final VContextParams ret = new VContextParams(this);
        ret.localBlessings = blessings;
        return ret;
    }
    /**
     * Returns a child of the current params with the given remote blessings attached.
     *
     * @param  blessings remote blessings
     * @return      a child of the current params with the given remote blessings attached.
     */
    public VContextParams withRemoteBlessings(Blessings blessings) {
        final VContextParams ret = new VContextParams(this);
        ret.remoteBlessings = blessings;
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
    public Object[] getMethodTags() {
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
}