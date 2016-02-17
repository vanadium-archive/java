// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.example.vbeam.vbeamexample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.URI;
import java.nio.charset.Charset;

import io.v.android.v23.V;
import io.v.android.v23.VBeam;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

public class MainActivity extends AppCompatActivity implements VBeam.CreateBeamIntentCallback {
    VContext ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ctx = V.init(this);
        try {
            //Note: Customize the listen spec to use a specific address with beam:
            //ctx = V.withListenSpec(ctx, new ListenSpec(new ListenSpec.Address("bt", "/0"), "", null));
            VBeam.setBeamIntentCallback(ctx, this, this);
        } catch (VException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                for (int i = 0; i < rawMsgs.length; i++) {
                    for (NdefRecord r : ((NdefMessage) rawMsgs[i]).getRecords()) {
                        System.out.println("record " + r.toString() + "("+r.toUri() + ")");
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public ListenableFuture<Pair<String, byte[]>> createIntent(VContext context, ServerCall call) {
        String blessing = "anonymous";
        String[] names = VSecurity.getRemoteBlessingNames(ctx, call.security());
        if (names.length > 0) {
            blessing = names[0];
        }
        byte[] payload = ("Hello " + blessing).getBytes(Charset.forName("utf-8"));
        Intent intent = new Intent(this, GotBeamActivity.class);
        intent.setPackage(getApplicationContext().getPackageName());
        System.out.println("APP_SCHEME: " + intent.toUri(Intent.URI_ANDROID_APP_SCHEME));
        System.out.println("INTENT_SCHEME: " + intent.toUri(Intent.URI_INTENT_SCHEME));
        return Futures.immediateFuture(new Pair<>(intent.toUri(Intent.URI_INTENT_SCHEME), payload));
    }
}
