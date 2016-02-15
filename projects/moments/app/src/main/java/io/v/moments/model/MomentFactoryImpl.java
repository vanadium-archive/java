// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.content.SharedPreferences;
import android.os.Bundle;

import org.joda.time.DateTime;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.v23.discovery.Attributes;

import static io.v.moments.ifc.Moment.AdState;

public class MomentFactoryImpl implements MomentFactory {

    private final BitMapper mBitMapper;

    public MomentFactoryImpl(BitMapper bitMapper) {
        if (bitMapper == null) {
            throw new IllegalArgumentException("Null bitMapper");
        }
        mBitMapper = bitMapper;
    }

    @Override
    public Moment make(Id id, int index, String author, String caption) {
        return new MomentImpl(
                mBitMapper, id, index, author, caption, DateTime.now(), AdState.OFF);
    }

    @Override
    public Moment fromAttributes(Id id, int ordinal, Attributes attr) {
        return new MomentImpl(
                mBitMapper, id, ordinal,
                attr.get(F.AUTHOR.toString()),
                attr.get(F.CAPTION.toString()),
                Moment.FMT.parseDateTime(attr.get(F.DATE.toString())),
                AdState.OFF);
    }

    @Override
    public Attributes toAttributes(Moment moment) {
        Attributes attr = new Attributes();
        attr.put(MomentFactory.F.AUTHOR.toString(), moment.getAuthor());
        attr.put(MomentFactory.F.CAPTION.toString(), moment.getCaption());
        attr.put(MomentFactory.F.DATE.toString(),
                Moment.FMT.print(moment.getCreationTime()));
        return attr;
    }

    @Override
    public void toBundle(Bundle b, String prefix, Moment m) {
        KeyMaker km = new KeyMaker(prefix);
        b.putString(km.get(F.ID), m.getId().toString());
        b.putInt(km.get(F.ORDINAL), m.getOrdinal());
        b.putString(km.get(F.AUTHOR), m.getAuthor());
        b.putString(km.get(F.CAPTION), m.getCaption());
        b.putLong(km.get(F.DATE), m.getCreationTime().getMillis());
        b.putBoolean(km.get(F.ADVERTISING), m.getDesiredAdState().equals(AdState.ON));
    }

    @Override
    public Moment fromBundle(Bundle b, String prefix) {
        KeyMaker km = new KeyMaker(prefix);
        return new MomentImpl(
                mBitMapper,
                Id.fromString(b.getString(km.get(F.ID), "")),
                b.getInt(km.get(F.ORDINAL), 0),
                b.getString(km.get(F.AUTHOR), ""),
                b.getString(km.get(F.CAPTION), ""),
                new DateTime(b.getLong(km.get(F.DATE), 0)),
                b.getBoolean(km.get(F.ADVERTISING)) ? AdState.ON : AdState.OFF);
    }

    @Override
    public void toPrefs(SharedPreferences.Editor editor, String prefix, Moment m) {
        KeyMaker km = new KeyMaker(prefix);
        editor.putString(km.get(F.ID), m.getId().toString());
        editor.putInt(km.get(F.ORDINAL), m.getOrdinal());
        editor.putString(km.get(F.AUTHOR), m.getAuthor());
        editor.putString(km.get(F.CAPTION), m.getCaption());
        editor.putLong(km.get(F.DATE), m.getCreationTime().getMillis());
        editor.putBoolean(km.get(F.ADVERTISING), m.getDesiredAdState().equals(AdState.ON));
    }

    @Override
    public Moment fromPrefs(SharedPreferences p, String prefix) {
        KeyMaker km = new KeyMaker(prefix);
        String idString = p.getString(km.get(F.ID), "");
        if (idString.isEmpty()) {
            throw new IllegalStateException("Empty id from prefs.");
        }
        return new MomentImpl(
                mBitMapper,
                Id.fromString(idString),
                p.getInt(km.get(F.ORDINAL), 0),
                p.getString(km.get(F.AUTHOR), ""),
                p.getString(km.get(F.CAPTION), ""),
                new DateTime(p.getLong(km.get(F.DATE), 0)),
                p.getBoolean(km.get(F.ADVERTISING), false) ? AdState.ON : AdState.OFF);
    }

    private static class KeyMaker {
        private final String mPrefix;

        KeyMaker(String prefix) {
            mPrefix = prefix;
        }

        String get(F field) {
            return mPrefix + field.toString();
        }
    }
}
