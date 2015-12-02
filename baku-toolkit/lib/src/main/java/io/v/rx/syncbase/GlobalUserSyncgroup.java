// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import io.v.baku.toolkit.BakuActivityMixin;
import io.v.baku.toolkit.BakuActivityTrait;
import io.v.baku.toolkit.BlessingsUtils;
import io.v.baku.toolkit.R;
import io.v.baku.toolkit.VAndroidContextMixin;
import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.services.syncbase.nosql.TableRow;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Syncgroup;
import io.v.v23.verror.NoExistException;
import io.v.v23.verror.VException;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action2;
import rx.schedulers.Schedulers;

@Accessors(prefix = "m")
@AllArgsConstructor
@Builder(builderClassName = "Builder")
@Slf4j
public class GlobalUserSyncgroup {
    public static final String
            DEFAULT_SYNCGROUP_HOST_NAME = "usersync",
            DEFAULT_SYNCGROUP_SUFFIX = "user",
            DEFAULT_RENDEZVOUS_MOUNT = "sgmt";
    public static final SyncgroupMemberInfo
            DEFAULT_SYNCGROUP_MEMBER_INFO = new SyncgroupMemberInfo();

    public static GlobalUserSyncgroup forActivity(final BakuActivityTrait t) {
        return builder().bakuActivityTrait(t).build();
    }

    public static GlobalUserSyncgroup forActivity(final BakuActivityMixin m) {
        return forActivity(m.getBakuActivityTrait());
    }

    /*
    As of Lombok IntelliJ 0.9.6, @Builder exhibits a few bugs interacting with @Accessors (Gradle
    build is fine).

    https://github.com/mplushnikov/lombok-intellij-plugin/issues/151
     */
    public static class Builder {
        private String sgSuffix = DEFAULT_SYNCGROUP_SUFFIX;
        private Function<String, String> descriptionForUsername = u -> "User syncgroup for " + u;
        private Function<AccessList, Permissions> permissionsForUserAcl =
                BlessingsUtils::syncgroupPermissions;
        private List<TableRow> prefixes = new ArrayList<>();
        private SyncgroupMemberInfo memberInfo = DEFAULT_SYNCGROUP_MEMBER_INFO;

        /**
         * This is an additive setter to {@link #prefixes(List)}.
         */
        public Builder prefix(final TableRow prefix) {
            prefixes.add(prefix);
            return this;
        }

        /**
         * This is an additive setter to {@link #prefixes(List)}.
         */
        public Builder prefix(final String tableName, final String rowPrefix) {
            return prefix(new TableRow(tableName, rowPrefix));
        }

        /**
         * This is an additive setter to {@link #prefixes(List)}.
         */
        public Builder prefix(final String tableName) {
            return prefix(tableName, "");
        }

        /**
         * This is a composite setter for:
         * <ul>
         * <li>{@code vContext}</li>
         * <li>{@code rxBlessings}</li>
         * <li>{@code syncHostLevel}</li> (as a new
         * {@link UserAppSyncHost#UserAppSyncHost(Context)})
         * <li>{@code onError}</li>
         * </ul>
         * and should be called prior to any overrides for those fields.
         */
        public Builder vActivityTrait(final VAndroidContextTrait<?> t) {
            return vContext(t.getVContext())
                    .rxBlessings(t.getBlessingsProvider().getRxBlessings())
                    .syncHostLevel(new UserAppSyncHost(t.getAndroidContext()))
                    .onError(t.getErrorReporter()::onError);
        }

        /**
         * In addition to those fields in {@link #vActivityTrait(VAndroidContextTrait)}, this
         * additionally sets:
         * <ul>
         * <li>{@code syncbase}</li>
         * <li>{@code db}</li>
         * <li>and adds to {@code prefixes}</li>
         * </ul>
         */
        public Builder bakuActivityTrait(final BakuActivityTrait t) {
            return vActivityTrait(t.getVAndroidContextTrait())
                    .syncbase(t.getSyncbase())
                    .db(t.getSyncbaseDb())
                    .prefix(t.getSyncbaseTableName());
        }

        /**
         * A convenience setter for {@link #bakuActivityTrait(BakuActivityTrait)} via
         * {@link VAndroidContextMixin}.
         */
        public Builder activity(final BakuActivityMixin activity) {
            return bakuActivityTrait(activity.getBakuActivityTrait());
        }
    }

    private final VContext mVContext;
    private final Observable<Blessings> mRxBlessings;
    private final SyncHostLevel mSyncHostLevel;
    private final String mSgSuffix;
    private final RxSyncbase mSyncbase;
    private final RxDb mDb;
    private final Function<String, String> mDescriptionForUsername;
    private final Function<AccessList, Permissions> mPermissionsForUserAcl;
    private final List<TableRow> mPrefixes;
    private final SyncgroupMemberInfo mMemberInfo;
    /**
     * @see io.v.baku.toolkit.ErrorReporter#onError(int, Throwable)
     */
    private final Action2<Integer, Throwable> mOnError;

    private SyncgroupSpec createSpec(final String username, final AccessList userAcl) {
        return new SyncgroupSpec(mDescriptionForUsername.apply(username),
                mPermissionsForUserAcl.apply(userAcl), mPrefixes,
                mSyncHostLevel.getRendezvousTableNames(username), false);
    }

    private Observable<Object> createOrJoinSyncgroup(final Database db, final String sgName,
                                                     final SyncgroupSpec spec) {
        return Observable.create(s -> {
            final Syncgroup sg = db.getSyncgroup(sgName);
            try {
                sg.join(mVContext, mMemberInfo);
                log.info("Joined syncgroup " + sgName);
            } catch (final NoExistException e) {
                try {
                    sg.create(mVContext, spec, mMemberInfo);
                    log.info("Created syncgroup " + sgName);
                } catch (final VException e2) {
                    s.onError(e2);
                    return;
                }
            } catch (final VException e) {
                s.onError(e);
                return;
            }
            s.onNext(null);
            s.onCompleted();
        });
    }

    private Observable<Object> createOrJoinSyncgroup(final String username, final AccessList acl) {
        final String sgHost = mSyncHostLevel.getSyncgroupHostName(username);
        final String sgName = RxSyncbase.syncgroupName(sgHost, mSgSuffix);
        final SyncgroupSpec spec = createSpec(username, acl);

        final Observable<Object> mount = SgHostUtil.ensureSyncgroupHost(
                mVContext, mSyncbase.getRxServer(), sgHost).share();

        return mDb.getObservable()
                .observeOn(Schedulers.io())
                .switchMap(db -> Observable.merge(mount.first().ignoreElements().concatWith(
                        createOrJoinSyncgroup(db, sgName, spec)), mount));
    }

    public Subscription join() {
        return Observable.switchOnNext(mRxBlessings
                .map(b -> {
                    final AccessList acl = BlessingsUtils.blessingsToAcl(mVContext, b);
                    final List<Observable<?>> createOrJoins =
                            BlessingsUtils.blessingsToUsernameStream(mVContext, b)
                                    .distinct()
                                    .map(u -> createOrJoinSyncgroup(u, acl))
                                    .collect(Collectors.toList());
                    if (createOrJoins.isEmpty()) {
                        throw new NoSuchElementException("GlobalUserSyncgroup requires a " +
                                "username; no username blessings found. Blessings: " + b);
                    }
                    return Observable.merge(createOrJoins);
                }))
                .subscribe(x -> {
                }, t -> mOnError.call(R.string.err_syncgroup_join, t));
    }
}
