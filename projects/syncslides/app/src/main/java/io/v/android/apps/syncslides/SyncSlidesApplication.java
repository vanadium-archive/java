// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Application;

/**
 * Subclass the main application object so that we can store global state.
 * http://www.devahead.com/blog/2011/06/extending-the-android-application-class-and-dealing-with-singleton/
 */
public class SyncSlidesApplication extends Application {
    private DB db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new FakeDB(getApplicationContext());
    }

    public DB getDb() {
        return db;
    }
}
