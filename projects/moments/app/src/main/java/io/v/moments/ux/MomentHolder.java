// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ux;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.v.moments.R;
import io.v.moments.ifc.Advertiser;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.ifc.Moment.Style;

import static io.v.moments.ifc.Moment.AdState;

/**
 * Holds the views comprising a Moment for a RecyclerView.
 */
public class MomentHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "MomentHolder";
    private final TextView authorTextView;
    private final TextView captionTextView;
    private final SwitchCompat advertiseButton;
    private final ImageView imageView;
    private final Context mContext;
    private final Handler mHandler;

    public MomentHolder(View itemView, Context context, Handler handler) {
        super(itemView);
        mContext = context;
        mHandler = handler;
        authorTextView = (TextView) itemView.findViewById(R.id.moment_author);
        captionTextView = (TextView) itemView.findViewById(R.id.moment_caption);
        advertiseButton = (SwitchCompat) itemView.findViewById(R.id.advertise_button);
        imageView = (ImageView) itemView.findViewById(R.id.moment_image);
    }

    /**
     * Modifies the view to reflect the state of moment.
     */
    public void bind(Moment moment, Advertiser advertiser) {
        final Kind kind = advertiser == null ? Kind.REMOTE : Kind.LOCAL;
        Log.d(TAG, "Binding " + kind + " " + moment);
        authorTextView.setText(moment.getAuthor());
        captionTextView.setText(moment.getCaption());
        if (moment.hasPhoto(kind, Style.THUMB)) {
            imageView.setImageBitmap(moment.getPhoto(kind, Style.THUMB));
            imageView.setOnClickListener(showPhoto(moment, kind));
        }
        if (moment.hasPhoto(kind, Style.FULL)) {
            if (!moment.hasPhoto(kind, Style.THUMB)) {
                throw new IllegalStateException(Style.THUMB.toString() +
                        " should be ready if " + Style.FULL + " is ready.");
            }
        }
        if (kind.equals(Kind.REMOTE)) {
            advertiseButton.setVisibility(View.INVISIBLE);
        } else {
            advertiseButton.setText("");
            advertiseButton.setVisibility(View.VISIBLE);
            advertiseButton.setEnabled(true);
            advertiseButton.setOnCheckedChangeListener(
                    toggleAdvertising(moment, advertiser));
            advertiseButton.setChecked(
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
                    handleStartAdvertising(moment, advertiser);
                } else {
                    handleStopAdvertising(moment, advertiser);
                }
            }
        };
    }

    private void handleStartAdvertising(final Moment moment, final Advertiser advertiser) {
        moment.setDesiredAdState(AdState.ON);
        advertiser.advertiseStart(makeAdvertiseCallback(moment));
    }

    private void handleStopAdvertising(final Moment moment, final Advertiser advertiser) {
        moment.setDesiredAdState(AdState.OFF);
        if (advertiser.isAdvertising()) {
            advertiser.advertiseStop();
            toast("Stopped advertising " + moment.getCaption());
        } else {
            Log.d(TAG, "handleStopAdvertising called, but not advertising.");
        }
    }

    private FutureCallback<ListenableFuture<Void>> makeAdvertiseCallback(final Moment moment) {
        return new FutureCallback<ListenableFuture<Void>>() {
            private void assureStopped() {
                moment.setDesiredAdState(AdState.OFF);
                if (advertiseButton.isChecked()) {
                    advertiseButton.setChecked(false);
                }
            }

            @Override
            public void onSuccess(ListenableFuture<Void> result) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        moment.setDesiredAdState(AdState.ON);
                        advertiseButton.setChecked(true);
                    }
                });
                Futures.addCallback(
                        result, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        assureStopped();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        assureStopped();
                                        if (t instanceof java.util.concurrent.CancellationException) {
                                            // At the time of writing, the only way advertising ends
                                            // is by throwing this exception, so this is actually
                                            // a non-exceptional success case.
                                        } else {
                                            Log.d(TAG, "Failure to gracefully stop advertising.", t);

                                        }
                                    }
                                });
                            }
                        }
                );
            }

            @Override
            public void onFailure(final Throwable t) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        assureStopped();
                        Log.d(TAG, "Failure to start advertising " + moment, t);
                    }
                });
            }
        };
    }

    private void toast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
