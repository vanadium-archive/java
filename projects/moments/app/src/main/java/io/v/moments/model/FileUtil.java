// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.model;

import java.io.File;

/**
 * Some generic file utilities.
 */
public class FileUtil {

    /**
     * Is the given argument a directory, and if so, can one create or remove
     * files in this directory?
     */
    public static boolean isUsableDirectory(File dir) {
        // For directories, "execute" means one can create or
        // delete new files in the directory (but not necessarily
        // get a full list of the files contained therein).
        return dir.isDirectory() && dir.canExecute();
    }

    /**
     * Make sure argument directory exists and delete anything it contains.
     */
    public static void initializeDirectory(File dir) {
        if (dir.exists()) {
            FileUtil.rmMinusF(dir);
        }
        if (!dir.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create directory " + dir);
        }
    }

    /**
     * Delete the given file, and if it's a directory, delete its contents too.
     */
    public static void rmMinusF(File path) {
        if (!path.exists()) {
            throw new IllegalArgumentException(
                    "non-existent path: " + path.getAbsolutePath());
        }
        File[] contents = path.listFiles();
        if (contents != null) {
            for (File f : contents) {
                rmMinusF(f);
            }
        }
        if (!path.delete()) {
            throw new IllegalStateException(
                    "unable to delete " + path.getAbsolutePath());
        }
    }
}
