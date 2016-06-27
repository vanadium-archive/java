// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Permissions {
    public static class Tags {
        public static final String ADMIN = "Admin";
        public static final String READ = "Read";
        public static final String RESOLVE = "Resolve";
        public static final String WRITE = "Write";
    }

    public static final String IN = "In";
    public static final String NOT_IN = "NotIn";

    public byte[] json;

    public Permissions() {} // Needed for JNI. Avoid using this directly.

    public Permissions(byte[] json) {
        this.json = json;
    }

    public Permissions(Map copyFrom) {
        this.json = new JSONObject(copyFrom).toString().getBytes();
    }

    /**
     * Parses the JSON string and returns a map describing the permissions.
     * <p/>
     * Example:
     * <p/>
     * <pre>
     * {
     *      "Admin":{"In":["..."]},
     *      "Write":{"In":["..."]},
     *      "Read":{"In":["..."]},
     *      "Resolve":{"In":["..."]},
     *      "Debug":{"In":["..."]}
     * }
     * </pre>
     *
     * @return map describing the permissions
     */
    public Map<String, Map<String, Set<String>>> parse() {
        Map<String, Map<String, Set<String>>> permissions = new HashMap<>();

        try {
            JSONObject jsonObject = new JSONObject(new String(this.json));
            for (Iterator<String> iter = jsonObject.keys(); iter.hasNext(); ) {
                String tag = iter.next();
                permissions.put(tag, parseAccessList(jsonObject.getJSONObject(tag)));
            }
        } catch (JSONException e) {
            // TODO(razvanm): Should we do something else? Logging?
            throw new IllegalArgumentException("Permissions parsing failure", e);
        }

        // Initialize all the parts we missed out on.
        initializeAccessList(permissions, Tags.ADMIN);
        initializeAccessList(permissions, Tags.WRITE);
        initializeAccessList(permissions, Tags.READ);
        initializeAccessList(permissions, Tags.RESOLVE);

        return permissions;
    }

    private static void initializeAccessList(Map<String, Map<String, Set<String>>> permissions, String tag) {
        if (permissions.get(tag) == null) {
            permissions.put(tag, new HashMap<String, Set<String>>());
        }
        if (permissions.get(tag).get(IN) == null) {
            permissions.get(tag).put(IN, new HashSet<String>());
        }
    }

    private static Map<String, Set<String>> parseAccessList(JSONObject jsonObject)
            throws JSONException {
        Map<String, Set<String>> accessList = new HashMap<>();
        for (Iterator<String> iter = jsonObject.keys(); iter.hasNext(); ) {
            String type = iter.next();
            if (type.equals(NOT_IN)) {
                // Skip. getJSONArray will fail since type's corresponding value is often null.
            } else {
                accessList.put(type, parseBlessingPatternList(jsonObject.getJSONArray(type)));
            }
        }
        return accessList;
    }

    private static Set<String> parseBlessingPatternList(JSONArray jsonArray) throws JSONException {
        Set<String> blessings = new HashSet<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            blessings.add(jsonArray.getString(i));
        }
        return blessings;
    }
}