package io.v.moments.model;

import android.app.Activity;
import android.widget.Toast;

/**
 * Wrapper allowing limited access to an activity.
 */
public class Toaster {

    private final Activity mActivity;

    public Toaster(Activity activity) {
        mActivity = activity;
    }

    public boolean isDestroyed() {
        return mActivity.isDestroyed();
    }

    /**
     * Must be called from UX thread.
     */
    public void toast(String msg) {
        Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();
    }
}
