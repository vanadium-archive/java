// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.example.suharshs.vango;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.joda.time.Duration;

import io.v.android.inspectors.RemoteInspectors;
import io.v.android.security.BlessingsManager;
import io.v.android.util.Vango;
import io.v.android.v23.V;
import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;

public class RunKeyActivity extends AppCompatActivity {
    private static final String TAG = "RunKeyActivity";
    private static final String BLESSINGS_KEY = "blessings";

    private RemoteInspectors mRemoteInspectors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_key);

        Intent intent = getIntent();
        final String key = intent.getStringExtra(MainActivity.EXTRA_KEY);

        TextView textView = (TextView)findViewById(R.id.text_run_key_status);
        textView.setText(String.format("Running go function keyed by '%s'", key));

        final VContext ctx = V.init(this, new Options()
                .set(OptionDefs.LOG_VLEVEL, 0)
                .set(OptionDefs.LOG_VMODULE, "*=0"));
        try {
            mRemoteInspectors = new RemoteInspectors(ctx);
        } catch (VException e) {
            Log.e(TAG, "Failed to enable remote inspection: " + e.toString());
        }
        Futures.addCallback(BlessingsManager.getBlessings(ctx, this, BLESSINGS_KEY, true),
                new FutureCallback<Blessings>() {
                    @Override
                    public void onSuccess(Blessings b) {
                        Log.v(TAG, "Received blessings " + b.toString());
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                (new Vango()).run(ctx, key);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.d(TAG, "Failed to get blessings", t);
                    }
                });
    }


    /** Called when the user clicks the Remote Inspect button */
    public void runRemoteInspect(View view) {
        String email;
        try {
            email = mRemoteInspectors.invite("helper", Duration.standardDays(7));
        } catch (VException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
            return;
        }
        // This whole app lives to be thrown away after testing, so don't worry about
        // using resource ids for the strings.
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Please look at vango");
        intent.putExtra(Intent.EXTRA_TEXT, email);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No email client installed", Toast.LENGTH_LONG).show();
        }
    }
}
