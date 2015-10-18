// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.model;

import org.joda.time.DateTime;

/**
 * Represents a question asked by the audience.
 */
public class Question {
    private String mId;
    private String mFirstName;
    private String mLastName;
    private DateTime mTime;

    /**
     * @param id a uuid
     * @param firstName the first name of the questioner
     * @param lastName the last name of the questioner
     * @param time the time at which the question was asked
     */
    public Question(String id, String firstName, String lastName, DateTime time) {
        mId = id;
        mFirstName = firstName;
        mLastName = lastName;
        mTime = time;
    }

    /**
     * Returns the unique id for the question.
     */
    public String getId() {
        return mId;
    }

   /**
     * Returns the first name of the questioner.
     */
    public String getFirstName() {
        return mFirstName;

    }

    /**
     * Returns the last name of the questioner.
     */
    public String getLastName() {
        return mLastName;
    }

    /**
     * Returns the time at which the question was asked.
     */
    public DateTime getTime() {
        return mTime;
    }
}
