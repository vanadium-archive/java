// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security.access;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.context.VContextImpl;
import io.v.v23.security.Authorizer;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.BlessingRoots;
import io.v.v23.security.Blessings;
import io.v.v23.security.Call;
import io.v.v23.security.CallParams;
import io.v.v23.security.Constants;
import io.v.v23.security.Principal;
import io.v.v23.security.Security;
import io.v.v23.security.Signer;
import io.v.v23.security.access.internal.MyObjectServerWrapper;
import io.v.v23.security.access.internal.MyTag;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

import java.security.interfaces.ECPublicKey;
import java.util.List;

/**
 * Tests the implementation of {@code TaggedACLAuthorizer}.
 */
public class PermissionsAuthorizerTest extends TestCase {
    private static class AuthorizeTestdata {
        Principal pServer;
        Principal pClient;
        Blessings server;
        Permissions acl = null;
        List<TestCase> accept;
        List<TestCase> deny;

        static class TestCase {
            final String method;
            final Blessings client;

            TestCase(String method, Blessings client) {
                this.method = method;
                this.client = client;
            }
        }

        Blessings blessings(String... names) throws VException {
            final Blessings[] parts = new Blessings[names.length];
            for (int i = 0; i < names.length; ++i) {
                parts[i] = this.pClient.blessSelf(names[i]);
            }
            return Security.unionOfBlessings(parts);
        }

        void runAuthorize(String method, Blessings client, VContext context) throws VException {
            final Authorizer authorizer = PermissionsAuthorizer.create(this.acl, MyTag.class);
            final Call call = Security.newCall(new CallParams()
                    .withLocalPrincipal(this.pServer)
                    .withLocalBlessings(this.server)
                    .withRemoteBlessings(client)
                    .withMethod(method)
                    .withMethodTags(getMethodTags(method)));
            authorizer.authorize(context, call);
        }
    }

    public void testAuthorize() throws VException {
        final VContext context = V.init();
        final AuthorizeTestdata test = new AuthorizeTestdata();
        test.pServer = newPrincipal();
        test.pClient = newPrincipal();
        test.server = test.pServer.blessSelf("server");
        test.acl = new Permissions(ImmutableMap.of(
                "R", new AccessList(
                        ImmutableList.of(Constants.ALL_PRINCIPALS),
                        null),
                "W", new AccessList(
                        ImmutableList.of(
                                new BlessingPattern("ali/family"),
                                new BlessingPattern("bob"),
                                new BlessingPattern("che/$")),
                        ImmutableList.of(
                                "bob/acquaintances")),
                "X", new AccessList(
                        ImmutableList.of(
                                new BlessingPattern("ali/family/boss/$"),
                                new BlessingPattern("superman/$")),
                        null)
                ));
        test.accept = ImmutableList.<AuthorizeTestdata.TestCase>builder()
                .add(new AuthorizeTestdata.TestCase("get", null))
                .add(new AuthorizeTestdata.TestCase("get", test.blessings("ali")))
                .add(new AuthorizeTestdata.TestCase("get",
                        test.blessings("bob/friend", "che/enemy")))

                .add(new AuthorizeTestdata.TestCase("put", test.blessings("ali/family/mom")))
                .add(new AuthorizeTestdata.TestCase("put", test.blessings("bob/friends")))
                // granted because of "che"
                .add(new AuthorizeTestdata.TestCase("put",
                        test.blessings("bob/acquantainces/carol", "che")))

                .add(new AuthorizeTestdata.TestCase("resolve", test.blessings("superman")))
                .add(new AuthorizeTestdata.TestCase("resolve", test.blessings("ali/family/boss")))

                .build();
        test.deny = ImmutableList.<AuthorizeTestdata.TestCase>builder()
                // Nobody is denied access to "get"
                .add(new AuthorizeTestdata.TestCase("put", test.blessings(
                        "ali", "bob/acquaintances", "bob/acquaintances/dave", "che/friend",
                        "dave")))
                .add(new AuthorizeTestdata.TestCase("resolve", test.blessings(
                        "ali", "ali/friend", "ali/family", "ali/family/friend",
                        "alice/family/boss/friend", "superman/friend")))
                // Since there are no tags on the noTags method, it has an
                // empty ACL.  No client will have access.
                .add(new AuthorizeTestdata.TestCase("noTags", test.blessings(
                        "ali", "ali/family/boss", "bob", "che", "superman")))
                .build();

        for (final AuthorizeTestdata.TestCase testCase : test.accept) {
            try {
                test.runAuthorize(testCase.method, testCase.client, context);
            } catch (VException e) {
                fail(String.format("Access denied for method %s to %s: %s",
                        testCase.method, testCase.client, e.getMessage()));
            }
        }

        for (final AuthorizeTestdata.TestCase testCase : test.deny) {
            try {
                test.runAuthorize(testCase.method, testCase.client, context);
                fail(String.format(
                        "Access granted for method %s to %s", testCase.method, testCase.client));
            } catch (VException e) {
                // OK
            }
        }
    }

    public void testSelfRPCs() throws Exception {
        final VContext context = V.init();
        final Principal p = newPrincipal();
        final Blessings client = p.blessSelf("client");
        final Blessings server = p.blessSelf("server");
        final Authorizer authorizer = PermissionsAuthorizer.create(new Permissions(ImmutableMap.of(
                        "R", new AccessList(ImmutableList.of(new BlessingPattern("nobody/$")), null))),
                MyTag.class);
        for (final String testCase : new String[]{ "put", "get", "resolve", "noTags", "allTags" }) {
            final Call call = Security.newCall(new CallParams()
                    .withLocalPrincipal(p)
                    .withLocalBlessings(server)
                    .withRemoteBlessings(client)
                    .withMethod(testCase)
                    .withMethodTags(getMethodTags(testCase)));
            try {
                authorizer.authorize(context, call);
            } catch (VException e) {
                throw new Exception(String.format("Access denied for method %s", testCase), e);
            }
        }
    }

    private static Principal newPrincipal() throws VException {
        final Signer signer = Security.newInMemorySigner();
        return Security.newPrincipal(signer, null, new TrustAllRoots());
    }

    private static VdlValue[] getMethodTags(String method) throws VException {
        final MyObjectServerWrapper s = new MyObjectServerWrapper(null);
        return s.getMethodTags(null, method);
    }

    private static class TrustAllRoots implements BlessingRoots {
        @Override
        public void add(ECPublicKey root, BlessingPattern pattern) throws VException {}
        @Override
        public void recognized(ECPublicKey root, String blessing) throws VException {}
        @Override
        public String debugString() { return TrustAllRoots.class.toString(); }
    }
}