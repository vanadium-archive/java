// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.v.v23.VFutures;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

public class Collection {
    private final Database mDatabase;
    private final io.v.v23.syncbase.Collection mVCollection;

    protected Collection(Database database, io.v.v23.syncbase.Collection vCollection, boolean createIfMissing) {
        if (createIfMissing) {
            try {
                VFutures.sync(vCollection.create(Syncbase.getVContext(), null));
            } catch (ExistException e) {
                // Collection already exists.
            } catch (VException e) {
                throw new RuntimeException("Failed to create collection", e);
            }
        }
        mDatabase = database;
        mVCollection = vCollection;
    }

    public Id getId() {
        return new Id(mVCollection.id());
    }

    // Shortcut for Database.getSyncgroup(c.getId()), helpful for the common case of one syncgroup
    // per collection.
    public Syncgroup getSyncgroup() {
        return mDatabase.getSyncgroup(getId());
    }

    // TODO(sadovsky): Maybe add scan API, if developers aren't satisfied with watch.

    // TODO(sadovsky): Revisit this API, which was copied from io.v.v23.syncbase. For example, would
    // the signature "public <T> T get(String key)" be preferable?
    public Object get(String key, Type type) {
        try {
            return VFutures.sync(mVCollection.getRow(key).get(Syncbase.getVContext(), type));
        } catch (VException e) {
            throw new RuntimeException("get failed: " + key, e);
        }
    }

    public boolean exists(String key) {
        try {
            return VFutures.sync(mVCollection.getRow(key).exists(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("exists failed: " + key, e);
        }
    }

    // TODO(sadovsky): Only needed for the current (old) version of io.v.v23.syncbase, which does
    // not include fredq's change to the put() API.
    private static Type getType(Object object) {
        Type superclassType = object.getClass().getGenericSuperclass();
        if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
            return null;
        }
        return ((ParameterizedType) superclassType).getActualTypeArguments()[0];
    }

    public <T> void put(String key, T value) {
        try {
            VFutures.sync(mVCollection.put(Syncbase.getVContext(), key, value, getType(value)));
        } catch (VException e) {
            throw new RuntimeException("put failed: " + key, e);
        }
    }

    public void delete(String key) {
        try {
            VFutures.sync(mVCollection.getRow(key).delete(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("delete failed: " + key, e);
        }
    }

    // FOR ADVANCED USERS. The following methods manipulate the AccessList for this collection, but
    // not for associated syncgroups.
    public AccessList getAccessList() {
        throw new RuntimeException("Not implemented");
    }

    public void updateAccessList(AccessList delta) {
        throw new RuntimeException("Not implemented");
    }
}
