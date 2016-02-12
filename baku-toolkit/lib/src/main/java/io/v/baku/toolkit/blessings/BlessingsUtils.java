// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.JsonReader;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.v.android.v23.V;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.BlessingRoots;
import io.v.v23.security.Blessings;
import io.v.v23.security.CryptoUtil;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.security.access.Tag;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import java8.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Common utilities for blessing and ACL management. In the future, we may want to factor these out
 * of Baku into the V23 Java libraries.
 */
@Slf4j
@UtilityClass
public class BlessingsUtils {
    public static final String
            PREF_BLESSINGS = "VanadiumBlessings",
            GLOBAL_BLESSING_ROOT_URL = "https://dev.v.io/auth/blessing-root";
    public static final Pattern DEV_V_IO_CLIENT_USER =
            Pattern.compile("dev\\.v\\.io:o:([^:]+):([^:]+).*");

    public static final AccessList OPEN_ACL = new AccessList(
            ImmutableList.of(new BlessingPattern("...")), ImmutableList.of());

    public static final ImmutableSet<Tag>
            DATA_TAGS = ImmutableSet.of(Constants.READ, Constants.WRITE, Constants.ADMIN),
            MOUNT_TAGS = ImmutableSet.of(Constants.READ, Constants.RESOLVE, Constants.ADMIN),
            SYNCGROUP_TAGS = ImmutableSet.of(Constants.READ, Constants.WRITE, Constants.RESOLVE,
                    Constants.ADMIN, Constants.DEBUG);

    public static final Permissions
            OPEN_DATA_PERMS = dataPermissions(OPEN_ACL),
            OPEN_MOUNT_PERMS = mountPermissions(OPEN_ACL);

    public static void writeSharedPrefs(final Context context, final Blessings blessings)
            throws VException {
        writeSharedPrefs(PreferenceManager.getDefaultSharedPreferences(context), PREF_BLESSINGS,
                blessings);
    }

    public static void writeSharedPrefs(final SharedPreferences prefs, final String key,
                                        final Blessings blessings) throws VException {
        prefs.edit().putString(key, VomUtil.encodeToString(blessings, Blessings.class)).apply();
    }

    public static Blessings readSharedPrefs(final Context context) throws VException {
        return readSharedPrefs(PreferenceManager.getDefaultSharedPreferences(context),
                PREF_BLESSINGS);
    }

    public static Blessings readSharedPrefs(final SharedPreferences prefs, final String key)
            throws VException {
        final String blessingsVom = prefs.getString(key, "");
        return Strings.isNullOrEmpty(blessingsVom) ? null : decodeBlessings(blessingsVom);
    }

    public static Blessings decodeBlessings(final String blessings) throws VException {
        return (Blessings) VomUtil.decodeFromString(blessings, Blessings.class);
    }

    public static Blessings decodeBlessings(final byte[] blessings) throws VException {
        return (Blessings) VomUtil.decode(blessings, Blessings.class);
    }

    public static Set<String> getBlessingNames(final VContext ctx, final Blessings blessings) {
        return ImmutableSet.copyOf(VSecurity.getBlessingNames(V.getPrincipal(ctx), blessings));
    }

    public static AccessList blessingsToAcl(final VContext ctx, final Blessings blessings) {
        return new AccessList(ImmutableList.copyOf(Collections2.transform(
                getBlessingNames(ctx, blessings),
                s -> new BlessingPattern(s))), //method reference confuses Android Studio
                ImmutableList.of());
    }

    /**
     * This method adds the given {@link BlessingPattern} to the given {@link AccessList} but does
     * not perform deduping or checking to make sure the new pattern is not in
     * {@link AccessList#getNotIn()}.
     */
    public static AccessList augmentAcl(final AccessList acl, final BlessingPattern newBlessing) {
        return new AccessList(ImmutableList.<BlessingPattern>builder()
                .addAll(acl.getIn())
                .add(newBlessing).build(),
                ImmutableList.of());
    }

    public static Stream<ClientUser> blessingsToClientUserStream(final VContext ctx,
                                                                 final Blessings blessings) {
        return StreamSupport.stream(getBlessingNames(ctx, blessings))
                .map(DEV_V_IO_CLIENT_USER::matcher)
                .filter(Matcher::matches)
                .map(m -> new ClientUser(m.group(1), m.group(2)));
    }

    /**
     * This method finds and parses all blessings of the form dev.v.io/o/....
     */
    public static Set<ClientUser> blessingsToClientUsers(
            final VContext ctx, final Blessings blessings) {
        return blessingsToClientUserStream(ctx, blessings).collect(Collectors.toSet());
    }

    public static String userMount(final String username) {
        return NamingUtil.join("users", username);
    }

    public static String clientMount(final String clientId) {
        return NamingUtil.join("tmp", "clients", clientId);
    }

    public static Stream<String> blessingsToClientMountStream(final VContext ctx,
                                                              final Blessings blessings) {
        return blessingsToClientUserStream(ctx, blessings)
                .map(cu -> BlessingsUtils.clientMount(cu.getClientId()));
    }

    public static Set<String> blessingsToClientMounts(final VContext ctx,
                                                      final Blessings blessings) {
        return blessingsToClientMountStream(ctx, blessings).collect(Collectors.toSet());
    }

    public static Permissions homogeneousPermissions(final Set<Tag> tags, final AccessList acl) {
        return new Permissions(Maps.toMap(Collections2.transform(tags, Tag::getValue), x -> acl));
    }

    public static Permissions dataPermissions(final AccessList acl) {
        return homogeneousPermissions(DATA_TAGS, acl);
    }

    public static Permissions mountPermissions(final AccessList acl) {
        return homogeneousPermissions(MOUNT_TAGS, acl);
    }

    public static Permissions syncgroupPermissions(final AccessList acl) {
        return homogeneousPermissions(SYNCGROUP_TAGS, acl);
    }

    /**
     * TODO(rosswang): This probably won't be best practice in the long run, but we'll need it until
     * we can bless the cloud Syncbase instance remotely.
     */
    public static Permissions cloudSyngroupPermissions(final AccessList userAcl,
                                                       final BlessingPattern sgHostBlessing) {
        final AccessList cloudAcl = augmentAcl(userAcl, sgHostBlessing);
        return new Permissions(ImmutableMap.of(
                Constants.ADMIN.getValue(), cloudAcl,
                Constants.READ.getValue(), userAcl,
                Constants.WRITE.getValue(), userAcl,
                Constants.RESOLVE.getValue(), userAcl,
                Constants.DEBUG.getValue(), userAcl
        ));
    }

    /**
     * Standard blessing handling for Vanadium applications:
     * <ul>
     * <li>Provide the given blessings when anybody connects to us.</li>
     * <li>Provide these blessings when we connect to other services (for example, when we talk
     * to the mounttable).</li>
     * <li>Trust these blessings and all the "parent" blessings.</li>
     * </ul>
     */
    public static void assumeBlessings(final VContext vContext, final Blessings blessings)
            throws VException {
        log.info("Assuming blessings: " + blessings);
        final VPrincipal principal = V.getPrincipal(vContext);
        principal.blessingStore().setDefaultBlessings(blessings);
        principal.blessingStore().set(blessings, new BlessingPattern("..."));
        VSecurity.addToRoots(principal, blessings);
    }

    public static void addGlobalBlessingRoots(final VContext vContext)
            throws IOException, VException {
        final URL url;
        try {
            url = new URL(GLOBAL_BLESSING_ROOT_URL);
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }

        ECPublicKey publicKey = null;
        final List<BlessingPattern> names = new ArrayList<>();

        try (final JsonReader json = new JsonReader(
                new InputStreamReader(url.openStream(), StandardCharsets.US_ASCII))) {
            json.beginObject();
            while (json.hasNext()) {
                final String name = json.nextName();
                if ("publicKey".equals(name)) {
                    final String strKey = json.nextString();
                    final byte[] binKey = Base64.decode(strKey.getBytes(StandardCharsets.US_ASCII),
                            Base64.URL_SAFE);
                    publicKey = CryptoUtil.decodeECPublicKey(binKey);
                } else if ("names".equals(name)) {
                    json.beginArray();
                    while (json.hasNext()) {
                        names.add(new BlessingPattern(json.nextString()));
                    }
                    json.endArray();
                } else {
                    json.skipValue();
                }
            }
        }

        if (publicKey != null) {
            final BlessingRoots roots = V.getPrincipal(vContext).roots();
            for (final BlessingPattern name : names) {
                log.info("Adding global blessing root " + name);
                roots.add(publicKey, name);
            }
        } else {
            log.warn("No global blessing roots found");
        }
    }
}
