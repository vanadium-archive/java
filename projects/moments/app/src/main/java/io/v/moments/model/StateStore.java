// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.Collection;
import java.util.Map;

import io.v.moments.lib.Id;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.ObservedList;

/**
 * Stores and loads app state from Android "shared preferences". This is the
 * state that should exist from app execution to app execution.
 */
public class StateStore {
    private final SharedPreferences mPrefs;
    private final MomentFactory mMomentFactory;

    public StateStore(SharedPreferences prefs, MomentFactory momentFactory) {
        mPrefs = prefs;
        mMomentFactory = momentFactory;
    }

    private String inZeroes(int index) {
        return String.format("%03d", index);
    }

    public void prefsSave(ObservedList<Moment> moments) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.clear();
        saveMomentsToPrefs(editor, moments);
        editor.commit();
    }

    public void prefsLoad(ObservedList<Moment> moments) {
        loadMomentsFromPrefs(moments);
    }

    private void saveMomentsToPrefs(SharedPreferences.Editor editor, Iterable<Moment> moments) {
        int count = 0;
        for (Moment moment : moments) {
            count++;
            mMomentFactory.toPrefs(editor, makeMomentPrefix(count), moment);
        }
        editor.putInt(Field.NUM_MOMENTS.toString(), count);
    }

    public void bundleSave(Bundle b, Collection<Moment> moments) {
        int count = 0;
        for (Moment moment : moments) {
            count++;
            mMomentFactory.toBundle(b, makeMomentPrefix(count), moment);
        }
        b.putInt(Field.NUM_MOMENTS.toString(), count);
    }

    public void bundleLoad(Bundle b, Map<Id, Moment> map) {
        map.clear();
        int size = b.getInt(Field.NUM_MOMENTS.toString(), 0);
        for (int i = 1; i <= size; i++) {
            Moment moment = mMomentFactory.fromBundle(b, makeMomentPrefix(i));
            map.put(moment.getId(), moment);
        }
    }

    private String makeMomentPrefix(int i) {
        return Field.MOMENT_ITEM.toString() + inZeroes(i) + "_";
    }

    private void loadMomentsFromPrefs(ObservedList<Moment> moments) {
        int size = mPrefs.getInt(Field.NUM_MOMENTS.toString(), 0);
        for (int i = size; i >= 1; i--) {
            moments.push(mMomentFactory.fromPrefs(mPrefs, makeMomentPrefix(i)));
        }
    }

    private enum Field {NUM_MOMENTS, MOMENT_ITEM}
}
