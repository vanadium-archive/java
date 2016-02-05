// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import java.util.HashMap;
import java.util.Map;

import io.v.moments.ifc.Advertiser;
import io.v.moments.ifc.IdSet;
import io.v.moments.ifc.Moment;
import io.v.moments.lib.Id;
import io.v.moments.lib.V23Manager;

/**
 * Makes advertisers.  Keeps a record of all of them for the life of the app.
 * Can use this record to reject local advertisements when scanning, or to shut
 * down all advertising.
 */
public class AdvertiserFactory implements IdSet {
    private final V23Manager mV23Manager;
    private final Map<Id, Advertiser> mLocalAds = new HashMap<>();

    public AdvertiserFactory(V23Manager v23Manager) {
        mV23Manager = v23Manager;
    }

    public Advertiser getOrMake(Moment moment) {
        if (contains(moment.getId())) {
            return mLocalAds.get(moment.getId());
        }
        Advertiser result = new AdvertiserImpl(mV23Manager, moment);
        mLocalAds.put(moment.getId(), result);
        return result;
    }

    public boolean contains(Id id) {
        return mLocalAds.containsKey(id);
    }

    public Iterable<Advertiser> allAdvertisers() {
        return mLocalAds.values();
    }
}
