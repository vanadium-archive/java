// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ux;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;

import java.util.concurrent.CancellationException;

import io.v.moments.R;
import io.v.moments.v23.ifc.Advertiser;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.ifc.Moment.Style;
import io.v.moments.model.Toaster;

import static io.v.moments.ifc.Moment.AdState;

/**
 * Holds the views comprising a Moment for a RecyclerView.
 */
public class MomentHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "MomentHolder";
    private final TextView mAuthorTextView;
    private final TextView mCaptionTextView;
    private final SwitchCompat mAdvertiseButton;
    private final ImageView mImageView;
    private final Context mContext;
    private final Toaster mToaster;

    public MomentHolder(View itemView, Context context, Toaster toaster) {
        super(itemView);
        mContext = context;
        mAuthorTextView = (TextView) itemView.findViewById(R.id.moment_author);
        mCaptionTextView = (TextView) itemView.findViewById(R.id.moment_caption);
        mAdvertiseButton = (SwitchCompat) itemView.findViewById(R.id.advertise_button);
        mImageView = (ImageView) itemView.findViewById(R.id.moment_image);
        mToaster = toaster;
    }

    /**
     * Modifies the view to reflect the state of moment.
     */
    public void bind(Moment moment, Advertiser advertiser) {
        final Kind kind = advertiser == null ? Kind.REMOTE : Kind.LOCAL;
        Log.d(TAG, "Binding " + kind + " " + moment);
        mAuthorTextView.setText(moment.getAuthor());
        mCaptionTextView.setText(moment.getCaption());
        if (moment.hasPhoto(kind, Style.THUMB)) {
            mImageView.setImageBitmap(moment.getPhoto(kind, Style.THUMB));
            mImageView.setOnClickListener(showPhoto(moment, kind));
        }
        if (moment.hasPhoto(kind, Style.FULL)) {
            if (!moment.hasPhoto(kind, Style.THUMB)) {
                throw new IllegalStateException(Style.THUMB.toString() +
                        " should be ready if " + Style.FULL + " is ready.");
            }
        }
        if (kind.equals(Kind.REMOTE)) {
            mAdvertiseButton.setVisibility(View.INVISIBLE);
        } else {
            mAdvertiseButton.setText("");
            mAdvertiseButton.setVisibility(View.VISIBLE);
            mAdvertiseButton.setEnabled(true);
            mAdvertiseButton.setOnCheckedChangeListener(
                    toggleAdvertising(moment, advertiser));
            mAdvertiseButton.setChecked(
                    moment.getDesiredAdState().equals(AdState.ON));
        }
    }

    /**
     * Fires an activity that will try to show a large photo, failing over to
     * show a blown up version of a thumbnail.
     */
    private View.OnClickListener showPhoto(
            final Moment moment, final Kind kind) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = ShowPhotoActivity.makeIntent(
                        mContext, moment.getOrdinal(), kind);
                mContext.startActivity(intent);
            }
        };
    }

    private CompoundButton.OnCheckedChangeListener toggleAdvertising(
            final Moment moment, final Advertiser advertiser) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                if (isChecked) {
                    if (!advertiser.isAdvertising()) {
                        advertiser.start(
                                makeAdvertiseStartCallback(moment),
                                makeAdvertiseStopCallback(moment));
                    } else {
                        Log.d(TAG, "Advertiser already on.");
                    }
                } else {
                    if (advertiser.isAdvertising()) {
                        advertiser.stop();
                    } else {
                        Log.d(TAG, "Advertiser already off.");
                    }
                }
            }
        };
    }

    private FutureCallback<Void> makeAdvertiseStartCallback(final Moment moment) {
        return new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "start:onSuccess:" + moment);
                moment.setDesiredAdState(AdState.ON);
                if (!mAdvertiseButton.isChecked()) {
                    mAdvertiseButton.setChecked(true);
                }
                mToaster.toast("Advertising " + moment.getCaption());
            }

            @Override
            public void onFailure(final Throwable t) {
                cleanUpPostStop(moment);
                mToaster.toast("Failure to start advertising " + moment.getCaption());
                Log.d(TAG, "Failure to start advertising " + moment, t);
            }
        };
    }

    /**
     * Verify that advertising is off and that the UX reflects that fact.
     */
    private void cleanUpPostStop(final Moment moment) {
        Log.d(TAG, "cleanUpPostStop");
        if (mToaster.isDestroyed()) {
            Log.d(TAG, "The activity is dead, no UX to fix.");
            return;
        }
        moment.setDesiredAdState(AdState.OFF);
        if (mAdvertiseButton.isChecked()) {
            Log.d(TAG, "Advertising off, must cleanup UX.");
            mAdvertiseButton.setChecked(false);
        }
    }

    private FutureCallback<Void> makeAdvertiseStopCallback(final Moment moment) {
        return new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                cleanUpPostStop(moment);
            }

            @Override
            public void onFailure(final Throwable t) {
                cleanUpPostStop(moment);
                if (t instanceof CancellationException) {
                    // At the time of writing, the only way advertising ends
                    // is by throwing this exception, so this is actually
                    // a non-exceptional success case.
                } else {
                    Log.d(TAG, "Failure to gracefully stop advertising.", t);
                }
            }
        };
    }
}
