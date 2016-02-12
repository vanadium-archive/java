// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.beam;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.Duration;

import io.v.android.v23.V;
import io.v.android.v23.VBeam;
import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.security.VSecurity;

/**
 * Handles the NDEF discovered intent on the receiver phone.
 * It contacts the VBeam server on the sending phone, then retrieves and starts the shared intent.
 */
public class BeamActivity extends Activity {
    private static final String TAG = "BeamActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        NdefMessage msgs[] = null;
        Intent intent = getIntent();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
        }
        if (msgs == null) {
            Log.d(TAG, "No ndef messages");
            finish();
            return;
        }
        VBeamManager.Data data = null;
        for (NdefMessage m : msgs) {
            data = VBeamManager.decodeMessage(m);
            if (data != null)
                break;
        }
        if (data == null) {
            Log.w(TAG, "Unable to deserialize data");
            finish();
            return;
        }
        Log.d(TAG, "connecting to " + data.name);
        VContext ctx = V.init(this).withTimeout(Duration.standardSeconds(2));
        Options opts = new Options();

        opts.set(OptionDefs.SERVER_AUTHORIZER, VSecurity.newPublicKeyAuthorizer(data.key));
        IntentBeamerClient client = IntentBeamerClientFactory.getIntentBeamerClient(data.name);
        ListenableFuture<IntentBeamerClient.GetIntentOut> out =
                client.getIntent(ctx, data.secret, opts);
        Futures.addCallback(out, new FutureCallback<IntentBeamerClient.GetIntentOut>() {
            @Override
            public void onSuccess(IntentBeamerClient.GetIntentOut result) {
                try {
                    Log.d(TAG, "got intent " + result.intentUri);
                    int flags = 0;
                    if (result.intentUri.startsWith("intent:")) {
                        flags = Intent.URI_INTENT_SCHEME;
                    } else {
                        flags = Intent.URI_ANDROID_APP_SCHEME;
                    }
                    Intent resultIntent = Intent.parseUri(result.intentUri, flags);
                    resultIntent.putExtra(VBeamManager.EXTRA_VBEAM_PAYLOAD, result.payload);
                    startActivity(resultIntent);
                    finish();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                finish();
            }
        });
    }
}
