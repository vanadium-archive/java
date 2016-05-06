// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package com.example.suharshs.v23gorunner;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_KEY = "com.example.suharshs.v23gorunner.KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Called when the user clicks the the Run button */
    public void runKey(View view){
        Intent intent = new Intent(this, RunKeyActivity.class);
        EditText editText = (EditText) findViewById(R.id.key);
        String key = editText.getText().toString();
        intent.putExtra(EXTRA_KEY, key);
        startActivity(intent);
    }
}
