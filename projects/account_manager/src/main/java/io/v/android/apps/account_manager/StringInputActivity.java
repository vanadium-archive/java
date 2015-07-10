// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Prompts user to choose among potentially many Vanadium accounts.
 */
public class StringInputActivity extends Activity {
    public static final String TAG = "StringInputActivity";

    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_string_input);
    }

    public void onOK(View v) {
        LinearLayout inputView = (LinearLayout) findViewById(R.id.input_layout);
        TextView textView = (EditText) findViewById(R.id.nameEditText);
        String name = textView.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Must enter non-empty name.", Toast.LENGTH_LONG).show();
            return;
        } else {
            replyWithSuccess(name);
        }
    }

    public void onCancel(View v) {
        replyWithError("User canceled account selection.");
    }

    private void replyWithSuccess(String name) {
        Intent intent = new Intent();
        intent.putExtra(REPLY, name);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Account choosing error: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
