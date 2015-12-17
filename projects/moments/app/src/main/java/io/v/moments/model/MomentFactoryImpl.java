// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.content.SharedPreferences;
import android.os.Bundle;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.Id;
import io.v.v23.discovery.Attributes;

public class MomentFactoryImpl implements MomentFactory {
    private static final DateTimeFormatter FMT =
            DateTimeFormat.forPattern("yyyyMMdd_HHmmss");

    private final BitMapper mBitMapper;

    public MomentFactoryImpl(BitMapper bitMapper) {
        if (bitMapper == null) {
            throw new IllegalStateException("Null bitMapper");
        }
        mBitMapper = bitMapper;
    }

    @Override
    public Attributes makeAttributes(Moment moment) {
        Attributes attr = new Attributes();
        attr.put(F.AUTHOR.toString(), moment.getAuthor());
        attr.put(F.CAPTION.toString(), moment.getCaption());
        attr.put(F.DATE.toString(), FMT.print(moment.getCreationTime()));
        return attr;
    }

    @Override
    public Moment make(Id id, int index, String author, String caption) {
        return new MomentImpl(
                mBitMapper, id, index, author, caption, DateTime.now());
    }

    @Override
    public Moment makeFromAttributes(Id id, int ordinal, Attributes attr) {
        return new MomentImpl(
                mBitMapper, id, ordinal,
                attr.get(F.AUTHOR.toString()),
                attr.get(F.CAPTION.toString()),
                FMT.parseDateTime(attr.get(F.DATE.toString())));
    }

    @Override
    public void toBundle(Bundle b, String prefix, Moment m) {
        KeyMaker km = new KeyMaker(prefix);
        b.putString(km.get(F.ID), m.getId().toString());
        b.putInt(km.get(F.ORDINAL), m.getOrdinal());
        b.putString(km.get(F.AUTHOR), m.getAuthor());
        b.putString(km.get(F.CAPTION), m.getCaption());
        b.putLong(km.get(F.DATE), m.getCreationTime().getMillis());
        b.putBoolean(km.get(F.ADVERTISING), m.shouldBeAdvertising());
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
                new DateTime(b.getLong(km.get(F.DATE), 0)));
    }

    @Override
    public void toPrefs(SharedPreferences.Editor editor, String prefix, Moment m) {
        KeyMaker km = new KeyMaker(prefix);
        editor.putString(km.get(F.ID), m.getId().toString());
        editor.putInt(km.get(F.ORDINAL), m.getOrdinal());
        editor.putString(km.get(F.AUTHOR), m.getAuthor());
        editor.putString(km.get(F.CAPTION), m.getCaption());
        editor.putLong(km.get(F.DATE), m.getCreationTime().getMillis());
        editor.putBoolean(km.get(F.ADVERTISING), m.shouldBeAdvertising());
    }

    @Override
    public Moment fromPrefs(SharedPreferences p, String prefix) {
        KeyMaker km = new KeyMaker(prefix);
        return new MomentImpl(
                mBitMapper,
                Id.fromString(p.getString(km.get(F.ID), "")),
                p.getInt(km.get(F.ORDINAL), 0),
                p.getString(km.get(F.AUTHOR), ""),
                p.getString(km.get(F.CAPTION), ""),
                new DateTime(p.getLong(km.get(F.DATE), 0)));
    }

    private class KeyMaker {
        private final String mPrefix;

        KeyMaker(String prefix) {
            mPrefix = prefix;
        }

        String get(F field) {
            return mPrefix + field.toString();
        }
    }
}
