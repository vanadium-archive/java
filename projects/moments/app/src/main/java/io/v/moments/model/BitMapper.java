// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.ifc.Moment.Style;
import io.v.moments.lib.ObservedList;

/**
 * Photo bitmap manipulator.
 *
 * Generates file names for bitmaps, encodes them for writing to storage, reads
 * them back from storage, generates thumbnails from full size photos, etc.
 */
public class BitMapper {
    private static final String FILE_EXT = ".jpg";
    private final Point mDisplaySize;
    private final int mThumbWidth;
    private final Bitmap mPlaceholderBitmap;
    private final Handler mHandler;
    private final File mWorkingDir;

    public BitMapper(File dir, Handler handler, Point displaySize, int thumbWidth) {
        mDisplaySize = displaySize;
        mHandler = handler;
        mThumbWidth = thumbWidth;
        mPlaceholderBitmap = Bitmap.createBitmap(
                mThumbWidth, mThumbWidth, Bitmap.Config.ALPHA_8);
        mWorkingDir = dir;
    }

    public void checkIoPermissions() {
        if (!mWorkingDir.exists()) {
            throw new IllegalArgumentException("Unable to see dir " + mWorkingDir);
        }
        if (!FileUtil.isUsableDirectory(mWorkingDir)) {
            throw new IllegalArgumentException("Unable to use dir " + mWorkingDir);
        }
    }

    static String inZeroes(int index) {
        return String.format("%04d", index);
    }

    private Bitmap readScaled(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        Point source = new Point(options.outWidth, options.outHeight);
        int sampleSize = 1;
        if (source.y > mDisplaySize.y || source.x > mDisplaySize.x) {
            sampleSize = (source.x > source.y) ?
                    Math.round((float) source.y / (float) mDisplaySize.y) :
                    Math.round((float) source.x / (float) mDisplaySize.x);
        }
        options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }

    public Bitmap readFullWithFallback(int index, Kind kind) {
        try {
            if (Config.DO_FULL_SIZE_TOO) {
                return readBitmap(makeFileName(index, kind, Style.FULL));
            }
        } catch (Exception e) {
            // Print stack, and fall back to reading the thumb.
            e.printStackTrace();
        }
        return readBitmap(makeFileName(index, kind, Style.THUMB));
    }

    private Bitmap makeThumb(Bitmap original) {
        float scale = (float) mThumbWidth / (float) original.getWidth();
        int height = Math.round(scale * (float) original.getHeight());
        return Bitmap.createScaledBitmap(original, mThumbWidth, height, true);
    }

    public Uri getCameraPhotoUri(int ordinal) {
        return Uri.fromFile(makeCameraPhotoFile(ordinal));
    }

    public File makeCameraPhotoFile(int ordinal) {
        return makeFileName(ordinal, Name.CAMERA);
    }

    public File makeFileName(int ordinal, Kind kind, Style style) {
        return makeFileName(ordinal, kind + Name.D + style);
    }

    private File makeFileName(int ordinal, String filePrefix) {
        String name = filePrefix + Name.D + inZeroes(ordinal) + FILE_EXT;
        return new File(mWorkingDir.getPath() + File.separator + name);
    }

    private Bitmap makeBitmap(byte[] data) {
        if (data == null || data.length < 1) {
            return mPlaceholderBitmap;
        }
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public boolean exists(int ordinal, Kind kind, Style style) {
        return makeFileName(ordinal, kind, style).exists();
    }

    public Bitmap readBitmap(int ordinal, Kind kind, Style style) {
        return readBitmap(makeFileName(ordinal, kind, style));
    }

    private Bitmap readBitmap(File file) {
        if (!file.exists()) {
            // Use an uncheck exception here, rather than FileNotFoundException.
            throw new IllegalArgumentException("Unable to read " + file.toString());
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
        return bitmap;
    }

    private String dim(Bitmap bitmap) {
        return "(w=" + bitmap.getWidth() + ",h=" + bitmap.getHeight() + ")";
    }

    public void writeBitmap(int ordinal, Kind kind, Style style, Bitmap bitmap) {
        writeBitmap(makeFileName(ordinal, kind, style), bitmap);
    }

    public void writeBytes(int ordinal, Kind kind, Style style, byte[] bytes) {
        writeBitmap(makeFileName(ordinal, kind, style), makeBitmap(bytes));
    }

    private void writeBitmap(File file, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Runnable signalChange(
            final ObservedList<Moment> moments, final Moment moment) {
        return new Runnable() {
            @Override
            public void run() {
                moments.changeById(moment.getId());
            }
        };
    }

    /**
     * Read the raw photo, and store it scaled down to full size of display and
     * a smaller thumbnail.
     */
    public void dealWithCameraResult(
            final ObservedList<Moment> moments, final Moment moment) {
        final int index = moment.getOrdinal();
        // Use the following to read the raw image for some reason, e.g.
        // full scrolling (raw image too big to display on phone w/o scroll).
        // Bitmap full = MediaStore.Images.Media.getBitmap
        //    (mContext.getContentResolver(), uri);
        Bitmap full = readScaled(makeCameraPhotoFile(index));
        writeBitmap(
                makeFileName(index, Kind.LOCAL, Style.THUMB),
                makeThumb(full));
        mHandler.post(signalChange(moments, moment));
        if (Config.DO_FULL_SIZE_TOO) {
            writeBitmap(makeFileName(index, Kind.LOCAL, Style.FULL), full);
            mHandler.post(signalChange(moments, moment));
        }
    }

    private static class Name {
        static final String CAMERA = "camera";
        static final String D = "_";
    }
}
