// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;

/**
 * Represents a question asked by the audience.
 */
public class Question implements Parcelable {
    private String mId;
    private String mName;
    /**
     * Time at which the question was asked in ms since the epoch.  Stored as
     * a long rather than DateTime because it is easier to serialize this way.
     */
    private long mTime;

    /**
     * @param id a uuid
     * @param name the name of the questioner
     * @param time the time at which the question was asked in ms since the epoch
     */
    public Question(String id, String name, long time) {
        mId = id;
        mName = name;
        mTime = time;
    }

    private Question(Parcel source) {
        mId = source.readString();
        mName = source.readString();
        mTime = source.readLong();
    }

    /**
     * Returns the unique id for the question.
     */
    public String getId() {
        return mId;
    }

   /**
     * Returns the name of the questioner.
     */
    public String getName() {
        return mName;

    }

    /**
     * Returns the time at which the question was asked.
     */
    public DateTime getTime() {
        return new DateTime(mTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeLong(mTime);
    }

    public static final Parcelable.Creator<Question> CREATOR =
            new Parcelable.Creator<Question>() {

                @Override
                public Question createFromParcel(Parcel source) {
                    return new Question(source);
                }

                @Override
                public Question[] newArray(int size) {
                    return new Question[size];
                }
            };
}
