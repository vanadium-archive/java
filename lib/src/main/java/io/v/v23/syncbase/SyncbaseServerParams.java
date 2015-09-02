package io.v.v23.syncbase;

import io.v.v23.rpc.ListenSpec;
import io.v.v23.security.access.Permissions;

/**
 * Parameters used when starting a syncbase service.  Here is an example of a simple
 * parameter creation:
 * <p><blockquote><pre>
 *     SyncbaseServerParams params = new SyncbaseServerParams()
 *           .withListenSpec(V.getListenSpec(ctx))
 *           .withName("test")
 *           .withStorageEngine(SyncbaseStorageEngine.LEVELDB);
 *     Syncbase.startServer(params);
 * </pre></blockquote><p>
 * {@link SyncbaseServerParams} form a tree where derived params are children of the params from
 * which they were derived.  Children inherit all the properties of their parent except for the
 * property being replaced (the listenSpec/name/storageEngine in the example above).
 */
public class SyncbaseServerParams {
    private SyncbaseServerParams parent = null;

    private Permissions permissions;
    private ListenSpec listenSpec;
    private String name;
    private String storageRootDir;
    private SyncbaseStorageEngine storageEngine;

    /**
     * Creates a new (and empty) {@link SyncbaseServerParams} object.
     */
    public SyncbaseServerParams() {
    }

    private SyncbaseServerParams(SyncbaseServerParams parent) {
        this.parent = parent;
    }

    /**
     * Returns a child of the current params with the given permissions.
     */
    public SyncbaseServerParams withPermissions(Permissions permissions) {
        SyncbaseServerParams ret = new SyncbaseServerParams(this);
        ret.permissions = permissions;
        return ret;
    }

    /**
     * Returns a child of the current params with the given {@link ListenSpec}.
     */
    public SyncbaseServerParams withListenSpec(ListenSpec spec) {
        SyncbaseServerParams ret = new SyncbaseServerParams(this);
        ret.listenSpec = spec;
        return ret;
    }

    /**
     * Returns a child of the current params with the given mount name.
     */
    public SyncbaseServerParams withName(String name) {
        SyncbaseServerParams ret = new SyncbaseServerParams(this);
        ret.name = name;
        return ret;
    }

    /**
     * Returns a child of the current params with the given storage root directory.
     */
    public SyncbaseServerParams withStorageRootDir(String rootDir) {
        SyncbaseServerParams ret = new SyncbaseServerParams(this);
        ret.storageRootDir = rootDir;
        return ret;
    }

    /**
     * Returns a child of the current params with the given storage engine.
     */
    public SyncbaseServerParams withStorageEngine(SyncbaseStorageEngine engine) {
        SyncbaseServerParams ret = new SyncbaseServerParams(this);
        ret.storageEngine = engine;
        return ret;
    }

    /**
     * Returns permissions that the syncbase service will be started with.
     */
    public Permissions getPermissions() {
        if (this.permissions != null) return this.permissions;
        if (this.parent != null) return this.parent.getPermissions();
        return null;
    }

    /**
     * Returns a {@ListenSpec} that the service will listen on.
     */

    public ListenSpec getListenSpec() {
        if (this.listenSpec != null) return this.listenSpec;
        if (this.parent != null) return this.parent.getListenSpec();
        return null;
    }

    /**
     * Returns a name that the service will mount itself on.
     */
    public String getName() {
        if (this.name != null) return this.name;
        if (this.parent != null) return this.parent.getName();
        return null;
    }

    /**
     * Returns a root directory for all of the service's storage files.
     */
    public String getStorageRootDir() {
        if (this.storageRootDir != null) return this.storageRootDir;
        if (this.parent != null) return this.parent.getStorageRootDir();
        return null;
    }

    /**
     * Returns a storage engine for the service.
     */
    public SyncbaseStorageEngine getStorageEngine() {
        if (this.storageEngine != null) return this.storageEngine;
        if (this.parent != null) return this.parent.getStorageEngine();
        return null;
    }
}
