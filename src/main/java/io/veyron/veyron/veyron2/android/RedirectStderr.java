
package io.veyron.veyron.veyron2.android;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Application;
import android.util.Log;

/**
 * RedirectStderr redirects standard error to a file named stderr.log in the app's internal store.
 * On initialization, it will check for previously written stderr messages and output them to logcat
 * under the tag "VeyronNativeLog".
 * When go panics, it outputs the panic message to stderr and RedirectStderr enables us to log these
 * panic messages to logcat.
 * TODO(bprosnitz) Make a more elegant panic catcher
 */
public final class RedirectStderr {
    /**
     * Start redirecting android's stderr
     */
    public static void Start() {
        dumpOldStderrLog();
        redirectStderr();
    }

    /**
     * Get the context of the currently running application. (Undocumented API).
     *
     * @return the application context
     */
    private static Application getApplication() {
        try {
            final Class<?> activityThreadClass = Class
                    .forName("android.app.ActivityThread");
            final Method method = activityThreadClass
                    .getMethod("currentApplication");
            final Application app = (Application) method.invoke(null,
                    (Object[]) null);
            if (app == null) {
                Log.e("RedirectStderrGetApplication",
                        "Application context was null");
            }
            return app;
        } catch (final Exception e) {
            Log.e("RedirectStderrGetApplication", e.toString());
        }
        return null;
    }

    /**
     * Redirects stderr to the file specified by fileno.
     *
     * @param fileno the file descriptor number
     */
    private static native void nativeStart(int fileno);

    /**
     * Redirects stderr of the veyron JNI code to a file. (Assumes a specific
     * implementation of FileDescriptor).
     */
    private static void redirectStderr() {
        Application app = getApplication();
        if (app == null) {
            return;
        }

        try {
            FileOutputStream fos = app.openFileOutput("stderr.log",
                    android.content.Context.MODE_PRIVATE);
            FileDescriptor fd = fos.getFD();
            Field fld = fd.getClass().getDeclaredField("descriptor");
            fld.setAccessible(true);
            int fileno = fld.getInt(fd);
            nativeStart(fileno);
        } catch (FileNotFoundException e) {
            Log.e("RedirectStderrGetFD", e.toString());
        } catch (IOException e) {
            Log.e("RedirectStderrOpenLog", e.toString());
            return;
        } catch (NoSuchFieldException e) {
            Log.e("RedirectStderrDescriptor",
                    "Failed to get descriptor field. It is highly likely that you are using a different version of java. Report this bug. "
                            + e);
            return;
        } catch (IllegalAccessException e) {
            Log.e("RedirectStderrGetField", e.toString());
        } catch (IllegalArgumentException e) {
            Log.e("RedirectStderrGetField", e.toString());
        }
    }

    /**
     * Dumps the previously stored stderr log to the android logging system.
     */
    private static void dumpOldStderrLog() {
        Application app = getApplication();
        if (app == null) {
            return;
        }

        FileInputStream fis;
        try {
            fis = app.openFileInput("stderr.log");
        } catch (FileNotFoundException e) {
            return;
        }

        BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
        while (true) {
            String line;
            try {
                line = bf.readLine();
            } catch (IOException e) {
                Log.e("DumpOldStderr", e.toString());
                break;
            }
            if (line == null) {
                break;
            }

            Log.i("VeyronNativeLog", line);
        }

        try {
            bf.close();
        } catch (IOException e) {
        }

        app.deleteFile("stderr.log");
    }
}
