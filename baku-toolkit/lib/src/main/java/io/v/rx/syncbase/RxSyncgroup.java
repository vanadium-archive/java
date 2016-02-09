// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.baku.toolkit.R;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action2;

@Accessors(prefix = "m")
@AllArgsConstructor
public abstract class RxSyncgroup {
    public static final SyncgroupMemberInfo
            DEFAULT_SYNCGROUP_MEMBER_INFO = new SyncgroupMemberInfo();

    /**
     * @see io.v.baku.toolkit.ErrorReporter#onError(int, Throwable)
     */
    protected final Action2<Integer, Throwable> mOnError;

    public abstract Observable<?> rxJoin();

    /**
     * It is not generally necessary to unsubscribe explicitly from this subscription since the
     * lifecycle of the Syncbase client is generally tied to a Baku Activity.
     */
    public Subscription join() {
        return rxJoin().subscribe(x -> {
        }, t -> mOnError.call(R.string.err_syncgroup_join, t));
    }
}
