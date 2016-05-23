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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.v.android.inspectors.RemoteInspectors;
import io.v.android.security.BlessingsManager;
import io.v.android.util.Vango;
import io.v.android.v23.V;
import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;

public class RunKeyActivity extends AppCompatActivity implements Vango.OutputWriter {
    private static final String
            TAG = "RunKeyActivity",
            BLESSINGS_KEY = "blessings",
            STARTED = "vangoFuncStarted",
            OUTPUT_DATA = "vangoOutputData",
            OUTPUT_NEXT = "vangoOutputNext";

    private RemoteInspectors mRemoteInspectors;

    // State for Vango.OutputWriter implementation
    private String[] mLines;
    private int mNextLine;
    private TextView mOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_key);

        Intent intent = getIntent();
        final String key = intent.getStringExtra(MainActivity.EXTRA_KEY);

        TextView textView = (TextView)findViewById(R.id.text_run_key_status);
        textView.setText(String.format("Running go function keyed by '%s'", key));

        mOutput = (TextView)findViewById(R.id.text_run_key_output);

        if (savedInstanceState != null) {
            mLines = (String[])savedInstanceState.getCharSequenceArray(OUTPUT_DATA);
            mNextLine = savedInstanceState.getInt(OUTPUT_NEXT);
            write(null);  // force a redraw of the output TextView.
            if (savedInstanceState.getBoolean(STARTED)) {
                return;
            }
        }

        final VContext ctx = V.init(this, new Options()
                .set(OptionDefs.LOG_VLEVEL, 0)
                .set(OptionDefs.LOG_VMODULE, "*=0"));
        try {
            mRemoteInspectors = new RemoteInspectors(ctx);
        } catch (VException e) {
            Log.e(TAG, "Failed to enable remote inspection: " + e.toString());
        }
        synchronized (this) {
            mLines = new String[10];
            mNextLine = 0;
        }
        Futures.addCallback(BlessingsManager.getBlessings(ctx, this, BLESSINGS_KEY, true),
                new FutureCallback<Blessings>() {
                    @Override
                    public void onSuccess(Blessings b) {
                        Log.v(TAG, "Received blessings " + b.toString());
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    (new Vango()).run(ctx, key, RunKeyActivity.this);
                                } catch (Exception e) {
                                    write(e.toString());
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.d(TAG, "Failed to get blessings", t);
                    }
                });
    }

    @Override
    public synchronized void write(String output) {
        // write() might be called with strings that do not constitute a full line.
        // If you see funny output, that might be the case, so fix that.
        // But until seen in the wild, ignoring the problem for Vango is fine.
        DateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.US);
        if (output != null) {
            mLines[(mNextLine++) % mLines.length] = fmt.format(new Date()) + " " + output.trim();
        }
        StringBuilder b  = new StringBuilder();
        for (int i = 0, idx = mNextLine % mLines.length; i < mLines.length; i++, idx++) {
            String l = mLines[idx % mLines.length];
            if (l == null) {
                continue;
            }
            b = b.append(l).append("\n\n");
        }
        final String text = b.toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOutput.setText(text);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(STARTED, true);
        savedInstanceState.putCharSequenceArray(OUTPUT_DATA, mLines);
        savedInstanceState.putInt(OUTPUT_NEXT, mNextLine);
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
