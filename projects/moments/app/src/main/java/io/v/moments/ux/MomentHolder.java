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

import java.util.concurrent.ExecutorService;

import io.v.moments.R;
import io.v.moments.ifc.Advertiser;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.Moment.Kind;
import io.v.moments.ifc.Moment.Style;

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
    private final ExecutorService mExecutor;
    private final Handler mHandler;

    public MomentHolder(
            View itemView, Context context,
            ExecutorService executor, Handler handler) {
        super(itemView);
        mContext = context;
        mExecutor = executor;
        mHandler = handler;
        authorTextView = (TextView) itemView.findViewById(R.id.moment_author);
        captionTextView = (TextView) itemView.findViewById(R.id.moment_caption);
        advertiseButton = (SwitchCompat) itemView.findViewById(R.id.advertise_button);
        imageView = (ImageView) itemView.findViewById(R.id.moment_image);
    }

    /**
     * Modifies the view to reflect the state of moment.
     */
    public void bind(Moment moment, Kind kind, Advertiser advertiser) {
        Log.d(TAG, "Holder: binding " + kind + " " + moment);
        if (kind.equals(Kind.LOCAL)) {
            if (advertiser == null) {
                throw new IllegalStateException("Null advertiser with local moment.");
            }
        }
        // No advertiser means the moment is remote and should not be advertised.
        authorTextView.setText(moment.getAuthor());
        captionTextView.setText(moment.getCaption());
        if (moment.hasPhoto(kind, Style.THUMB)) {
            imageView.setImageBitmap(moment.getPhoto(kind, Style.THUMB));
            imageView.setOnClickListener(showPhoto(moment, kind));
        } else {
            Log.d(TAG, kind.toString() + " " + Style.THUMB + " not ready.");
        }
        if (moment.hasPhoto(kind, Style.FULL)) {
            if (!moment.hasPhoto(kind, Style.THUMB)) {
                throw new IllegalStateException(Style.THUMB.toString() +
                        " should be ready if " + Style.FULL + " is ready.");
            }
        } else {
            Log.d(TAG, kind.toString() + " " + Style.FULL + " not ready.");
        }
        if (kind.equals(Kind.REMOTE)) {
            advertiseButton.setVisibility(View.INVISIBLE);
        } else {
            advertiseButton.setText("");
            advertiseButton.setVisibility(View.VISIBLE);
            advertiseButton.setEnabled(true);
            advertiseButton.setChecked(advertiser.shouldBeAdvertising());
            advertiseButton.setOnCheckedChangeListener(
                    toggleAdvertising(moment, advertiser));
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
                Log.d(TAG, "Clicked moment.");
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
                    // TODO(jregan): eliminate this redundancy.
                    // The moment is persisted in prefs, the advertiser isn't.
                    // That wasn't done because remote moments never
                    // need an advertiser.
                    moment.setShouldBeAdvertising(true);
                    advertiser.setShouldBeAdvertising(true);
                    mExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                advertiser.advertiseStart();
                                toast("Started advertising.");
                            } catch (Exception e) {
                                e.printStackTrace();
                                toast("Had problem starting advertising.");
                            }
                        }
                    });
                } else {
                    moment.setShouldBeAdvertising(false);
                    advertiser.setShouldBeAdvertising(false);
                    mExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                advertiser.advertiseStop();
                                toast("Stopped advertising.");
                            } catch (Exception e) {
                                e.printStackTrace();
                                toast("Had problem stopping advertising.");
                            }
                        }
                    });
                }
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
