// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import java.util.HashMap;
import java.util.Map;

import io.v.moments.ifc.IdSet;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.moments.v23.ifc.Advertiser;
import io.v.moments.v23.ifc.V23Manager;

/**
 * Makes moment advertisers.
 *
 * Keeps a record of all of them for the life of the app. This record used to
 * reject locally created advertisements when scanning, or to shut down all
 * advertising.
 */
public class AdvertiserFactory implements IdSet {
    private final V23Manager mV23Manager;
    private final Map<Id, Advertiser> mLocalAds = new HashMap<>();
    private final MomentFactory mFactory;

    public AdvertiserFactory(V23Manager v23Manager, MomentFactory factory) {
        if (v23Manager == null) {
            throw new IllegalArgumentException("Null v23Manager");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Null factory");
        }
        mV23Manager = v23Manager;
        mFactory = factory;
    }

    public Advertiser getOrMake(Moment moment) {
        if (contains(moment.getId())) {
            return mLocalAds.get(moment.getId());
        }
        Advertiser result = mV23Manager.makeAdvertiser(
                new MomentAdCampaign(moment, mFactory));
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
