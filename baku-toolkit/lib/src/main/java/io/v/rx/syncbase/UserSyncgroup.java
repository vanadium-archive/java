// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import android.content.Context;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.baku.toolkit.blessings.ClientUser;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.services.syncbase.nosql.TableRow;
import java8.util.function.Function;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import lombok.Value;
import lombok.experimental.Accessors;
import rx.Observable;
import rx.functions.Action2;

// TODO(rosswang): Generalize this to other possible syncgroup strategies.
@Accessors(prefix = "m")
public abstract class UserSyncgroup extends RxSyncgroup {
    public static final String DEFAULT_SYNCGROUP_SUFFIX = "user";

    public static class Builder {
        protected VContext mVContext;
        protected Observable<Blessings> mRxBlessings;
        protected SyncHostLevel mSyncHostLevel;
        protected String mSgSuffix = DEFAULT_SYNCGROUP_SUFFIX;
        protected RxDb mDb;
        protected Function<String, String> mDescriptionForUsername = u -> "User syncgroup for " + u;
        protected Function<AccessList, Permissions> mPermissionsForAcl =
                BlessingsUtils::syncgroupPermissions;
        protected List<TableRow> mPrefixes = new ArrayList<>();
        protected SyncgroupMemberInfo mMemberInfo = DEFAULT_SYNCGROUP_MEMBER_INFO;
        protected Action2<Integer, Throwable> mOnError;

        // helper for constructing a default UserAppSyncHost if needed
        protected Context mAndroidContext;

        public Builder vContext(final VContext vContext) {
            mVContext = vContext;
            return this;
        }

        public Builder rxBlessings(final Observable<Blessings> rxBlessings) {
            mRxBlessings = rxBlessings;
            return this;
        }

        public Builder syncHostLevel(final SyncHostLevel syncHostLevel) {
            mSyncHostLevel = syncHostLevel;
            mAndroidContext = null;
            return this;
        }

        public Builder sgSuffix(final String sgSuffix) {
            mSgSuffix = sgSuffix;
            return this;
        }

        public Builder db(final RxDb db) {
            mDb = db;
            return this;
        }

        public Builder descriptionForUsername(final Function<String, String> descriptionForUsername) {
            mDescriptionForUsername = descriptionForUsername;
            return this;
        }

        public Builder permissionsForAcl(final Function<AccessList, Permissions> permissionsForAcl) {
            mPermissionsForAcl = permissionsForAcl;
            return this;
        }

        /**
         * This setter is not additive.
         */
        public Builder prefixes(final List<TableRow> prefixes) {
            mPrefixes.clear();
            mPrefixes.addAll(prefixes);
            return this;
        }

        /**
         * This setter is not additive.
         */
        public Builder prefixes(final TableRow... prefixes) {
            mPrefixes.clear();
            mPrefixes.addAll(Arrays.asList(prefixes));
            return this;
        }

        /**
         * This is an additive setter.
         */
        public Builder prefix(final TableRow prefix) {
            mPrefixes.add(prefix);
            return this;
        }

        /**
         * This is an additive setter.
         */
        public Builder prefix(final String tableName, final String rowPrefix) {
            return prefix(new TableRow(tableName, rowPrefix));
        }

        /**
         * This is an additive setter.
         */
        public Builder prefix(final String tableName) {
            return prefix(tableName, "");
        }

        public Builder memberInfo(final SyncgroupMemberInfo memberInfo) {
            mMemberInfo = memberInfo;
            return this;
        }

        public Builder onError(final Action2<Integer, Throwable> onError) {
            mOnError = onError;
            return this;
        }

        /**
         * This is a composite setter for:
         * <ul>
         * <li>{@code vContext}</li>Context
         * <li>{@code rxBlessings}</li>
         * <li>{@code onError}</li>
         * </ul>
         * and should be called prior to any overrides for those fields.
         */
        public Builder activity(final VAndroidContextTrait<?> t) {
            mAndroidContext = t.getAndroidContext();
            return vContext(t.getVContext())
                    .rxBlessings(t.getBlessingsProvider().getRxBlessings())
                    .onError(t.getErrorReporter()::onError);
        }

        /**
         * In addition to those fields in {@link #activity(VAndroidContextTrait)}, this
         * additionally sets:
         * <ul>
         * <li>{@code db}</li>
         * <li>and adds to {@code prefixes}</li>
         * </ul>
         */
        public Builder activity(final BakuActivityTrait<?> t) {
            return activity(t.getVAndroidContextTrait())
                    .db(t.getSyncbaseDb())
                    .prefix(t.getSyncbaseTableName());
        }

        protected Parameters buildParameters(final Supplier<SyncHostLevel> defaultSyncHost) {
            return new Parameters(mVContext, mRxBlessings,
                    mSyncHostLevel == null ? defaultSyncHost.get() : mSyncHostLevel, mSgSuffix, mDb,
                    mDescriptionForUsername, mPermissionsForAcl, ImmutableList.copyOf(mPrefixes),
                    mMemberInfo, mOnError);
        }

        public UserCloudSyncgroup buildCloud() {
            return new UserCloudSyncgroup(buildParameters(() -> ClientLevelCloudSync.DEFAULT));
        }

        public UserPeerSyncgroup buildPeer() {
            return new UserPeerSyncgroup(buildParameters(
                    () -> new UserAppSyncHost(mAndroidContext)));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Value
    public static class Parameters {
        VContext mVContext;
        Observable<Blessings> mRxBlessings;
        SyncHostLevel mSyncHostLevel;
        String mSgSuffix;
        RxDb mDb;
        Function<String, String> mDescriptionForUsername;
        Function<AccessList, Permissions> mPermissionsForAcl;
        ImmutableList<TableRow> mPrefixes;
        SyncgroupMemberInfo mMemberInfo;
        Action2<Integer, Throwable> mOnError;
    }

    protected final Parameters mParams;

    public UserSyncgroup(final Parameters params) {
        super(params.getOnError());
        mParams = params;
    }

    private SyncgroupSpec createSpec(final ClientUser clientUser, final AccessList acl) {
        return new SyncgroupSpec(
                mParams.getDescriptionForUsername().apply(clientUser.getUsername()),
                mParams.getPermissionsForAcl().apply(acl), mParams.getPrefixes(),
                mParams.getSyncHostLevel().getRendezvousTableNames(clientUser), false);
    }

    protected abstract Observable<?> rxJoin(final String sgHost, final String sgName,
                                            final SyncgroupSpec spec);

    private Observable<?> rxJoin(final ClientUser clientUser, final AccessList acl) {
        final String sgHost = mParams.getSyncHostLevel().getSyncgroupHostName(clientUser);
        final String sgName = RxSyncbase.syncgroupName(sgHost, mParams.getSgSuffix());
        final SyncgroupSpec spec = createSpec(clientUser, acl);

        return rxJoin(sgHost, sgName, spec);
    }

    @Override
    public Observable<?> rxJoin() {
        return Observable.switchOnNext(mParams.getRxBlessings()
                .map(b -> {
                    final AccessList acl = BlessingsUtils.blessingsToAcl(mParams.getVContext(), b);
                    final List<Observable<?>> joins =
                            BlessingsUtils.blessingsToClientUserStream(mParams.getVContext(), b)
                                    .distinct()
                                    .map(cu -> rxJoin(cu, acl))
                                    .collect(Collectors.toList());
                    if (joins.isEmpty()) {
                        throw new NoSuchElementException("UserSyncgroup requires a username; no " +
                                "username blessings found. Blessings: " + b);
                    }
                    return Observable.merge(joins);
                }));
    }
}
