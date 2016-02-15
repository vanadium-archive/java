// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Environment;
import android.os.Handler;

import java.io.File;

import io.v.moments.R;

/**
 * Configuration.
 */
public class Config {
    // If true, app processes full size images in addition to thumbnails.
    // App can become slower as a result, e.g. a thumbnail transfer that would
    // be quick might be competing for bandwidth with a full size photo.
    public static final boolean DO_FULL_SIZE_TOO = true;
    /**
     * Parent directory to all app storage.
     */
    private static final File PHOTO_PARENT_DIR =
            Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);

    /**
     * Returns purported image display area.
     *
     * Area changes depending on rotation, and the change is more than just a
     * swap of {x, y}.  E.g. a nexus 9 in held in landscape reports (2048,
     * 1440), while held in portrait reports (1536, 1952). Presumably the nav
     * control bar bits are discounted.
     */
    private static Point getDisplaySize(Activity activity) {
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        return size;
    }

    public static File getWorkingDirectory(Context context) {
        return new File(
                PHOTO_PARENT_DIR,
                context.getString(R.string.photo_dir_name));
    }

    public static BitMapper makeBitmapper(Activity activity) {
        return new BitMapper(
                getWorkingDirectory(activity),
                new Handler(activity.getMainLooper()),
                getDisplaySize(activity),
                activity.getResources().getDimensionPixelSize(
                        R.dimen.moment_image_width)
        );
    }
}
