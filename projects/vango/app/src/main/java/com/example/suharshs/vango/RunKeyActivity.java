// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.example.suharshs.vango;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import io.v.android.security.BlessingsManager;
import io.v.android.util.Vango;
import io.v.android.v23.V;
import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;

public class RunKeyActivity extends AppCompatActivity {
    private static final String TAG = "RunKeyActivity";
    private static final String BLESSINGS_KEY = "blessings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_key);

        Intent intent = getIntent();
        final String key = intent.getStringExtra(MainActivity.EXTRA_KEY);

        TextView textView = new TextView(this);
        textView.setTextSize(25);
        textView.setTextColor(Color.parseColor("#0097A7"));
        textView.setText(String.format("Running go function keyed by '%s'", key));
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);
        layout.addView(textView);

        final VContext ctx = V.init(this, new Options()
                .set(OptionDefs.LOG_VLEVEL, 0)
                .set(OptionDefs.LOG_VMODULE, "*=0"));
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
}
