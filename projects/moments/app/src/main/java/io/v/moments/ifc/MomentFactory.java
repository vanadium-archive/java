// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import android.content.SharedPreferences;
import android.os.Bundle;

import io.v.moments.lib.Id;
import io.v.v23.discovery.Attributes;

/**
 * Makes moments, converts them to and from other formats.
 */
public interface MomentFactory {
    Moment make(Id id, int index, String author, String caption);

    void toPrefs(SharedPreferences.Editor editor, String prefix, Moment m);

    Moment fromPrefs(SharedPreferences p, String prefix);

    void toBundle(Bundle b, String prefix, Moment m);

    Moment fromBundle(Bundle b, String prefix);

    Attributes toAttributes(Moment moment);

    Moment fromAttributes(Id id, int ordinal, Attributes attr);

    enum F {
        DATE, AUTHOR, CAPTION, ORDINAL, ID, ADVERTISING
    }
}
