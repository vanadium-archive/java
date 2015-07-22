// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Prompts user to input a string.
 */
public class TextInputActivity extends Activity {
    public static final String TAG = "BlessingExtensionInput";
    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";
    public static final String TITLE = "TITLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_string_input);
        String title = getIntent().getExtras().getString(TITLE);
        if (title != null) {
            this.setTitle(title);
        }
    }

    public void onOK(View v) {
        TextView textView = (EditText) findViewById(R.id.nameEditText);
        String text = textView.getText().toString();

        if (text.isEmpty()) {
            Toast.makeText(this, "Must enter non-empty text.", Toast.LENGTH_LONG).show();
            return;
        } else {
            replyWithSuccess(text);
        }
    }

    public void onCancel(View v) {
        replyWithError("User canceled text input.");
    }

    private void replyWithSuccess(String text) {
        Intent intent = new Intent();
        intent.putExtra(REPLY, text);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Text input error: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
