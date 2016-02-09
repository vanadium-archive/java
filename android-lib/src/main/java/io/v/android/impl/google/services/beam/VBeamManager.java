// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.services.beam;

import android.app.Activity;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.interfaces.ECPublicKey;

import io.v.android.v23.VBeam;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

/**
 * Internal implementation of {@link VBeam} for an activity.
 */
public class VBeamManager implements NfcAdapter.CreateNdefMessageCallback {

    static final String EXTERNAL_DOMAIN = "io.v.android.vbeam";
    static final String EXTERNAL_TYPE = "vbs";
    private static final String EXTERNAL_PATH = "/" + EXTERNAL_DOMAIN + ":" + EXTERNAL_TYPE;
    private static final String TAG = "VBeamManager";
    private final VBeamServer server;
    private final VContext context;
    private final String packageName;

    /**
     * Key for the payload bytes in a VBeam intent.
     */
    public static final String EXTRA_VBEAM_PAYLOAD = "io.v.intent.vbeam.payload";

    /**
     * Starts the VBeam server and registers the activity to send NDEF messages.
     * @param context
     * @param activity
     * @param creator
     * @throws VException
     */
    public VBeamManager(VContext context, Activity activity, VBeam.IntentCreator creator)
            throws VException {
        this.server = new VBeamServer(creator);
        this.packageName = activity.getApplicationContext().getPackageName();
        this.context = V.withNewServer(
                context, "", this.server, VSecurity.newAllowEveryoneAuthorizer());
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        try {
            String requestID = server.newRequest();

            byte[] payload;
            try {
                Data data = new Data();
                data.secret = requestID;
                data.key = V.getPrincipal(context).publicKey();
                data.name = getEndpointName();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(data);
                oos.flush();
                payload = bos.toByteArray();
                oos.close();
                bos.close();
            } catch (IOException e) {
                Log.e(TAG, "unable to encode ndef message", e);
                return null;
            }
            return new NdefMessage(
                    new NdefRecord[]{
                            NdefRecord.createExternal(EXTERNAL_DOMAIN, EXTERNAL_TYPE, payload),
                            NdefRecord.createApplicationRecord(packageName)
                    }
            );
        } catch (Throwable t) {
            Log.e(TAG, "createNdefMessage failed", t);
            throw t;
        }
    }

    static Data decodeMessage(NdefMessage m) {
        for (NdefRecord r : m.getRecords()) {
            Uri uri = r.toUri();
            if (uri == null || !EXTERNAL_PATH.equals(uri.getPath())) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    StringBuilder description = new StringBuilder("ignoring ");
                    if (uri == null) {
                        description.append(r);
                    } else {
                        description.append(uri.getPath());
                        description.append(" != ");
                        description.append(EXTERNAL_PATH);
                    }
                    Log.d(TAG, description.toString());
                }
                continue;
            }
            byte[] payload = r.getPayload();
            if (payload != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(payload);
                ObjectInputStream ois = null;
                try {
                    ois = new ObjectInputStream(bis);
                    Object d = ois.readObject();
                    if (d != null && d instanceof Data) {
                        return (Data)d;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "decodeMessage", e);
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "decodeMessage", e);
                }
            }
        }
        return null;
    }

    private String getEndpointName() {
        Endpoint[] endpoints = V.getServer(context).getStatus().getEndpoints();
        return endpoints[0].name();
    }

    static class Data implements Serializable {
        String name;
        String secret;
        ECPublicKey key;
    }
}
