// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.naming;

import com.google.common.base.CharMatcher;

/**
 * Utilities for dealing with Vanadium names.
 */
public class NamingUtil {
    private NamingUtil() {
    }

    /**
     * Takes an address and a relative name and returns a rooted or relative name.
     * <p>
     * If a valid address is supplied then the returned name will always be a rooted name (i.e.
     * starting with {@code /}), otherwise it may be relative. {@code address} should not start
     * with a {@code /} and if it does, that prefix will be stripped.
     */
    public static String joinAddressName(String address, String name) {
        address = CharMatcher.is('/').trimLeadingFrom(address);
        if (address.isEmpty()) {
            return clean(name);
        }
        if (name.isEmpty()) {
            return clean("/" + address);
        }
        return clean("/" + address + "/" + name);
    }

    /**
     * Reduces multiple adjacent slashes to a single slash and removes any trailing slash.
     */
    public static String clean(String name) {
        CharMatcher slashMatcher = CharMatcher.is('/');
        name = slashMatcher.collapseFrom(name, '/');
        if ("/".equals(name)) {
            return name;
        }
        return slashMatcher.trimTrailingFrom(name);
    }
}
