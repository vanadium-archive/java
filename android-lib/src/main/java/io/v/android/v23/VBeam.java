// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.v23;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.android.impl.google.services.beam.VBeamManager;
import io.v.v23.context.VContext;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.Call;
import io.v.v23.verror.VException;

/**
 * Enables sending authenticated data over Android Beam.
 * <p>
 * To support Beam in an activity you register an {@link io.v.android.v23.VBeam.IntentCreator}.
 * When the user initiates an NFC tap, the framework will call
 * {@link io.v.android.v23.VBeam.IntentCreator#createIntent(VContext, ServerCall)}. You should
 * return an intent encapsulating the data you wish to share.
 * <p>
 * Internally this starts a Vanadium service on this phone. After tapping the app will start on
 * the destination phone and contact the sender. You may then inspect the credentials the app
 * is using on the destination phone and decide what data to send. For example, you may
 * add their blessing to an ACL and then send the name of the object you are trying to share.
 * <p>
 * Beaming will fail if the app is in the foreground on both phones. The destination
 * phone should not have the app open.
 * <p>
 * If the app is not installed on the destination phone, it will open in the Play store.
 */
public class VBeam {
    /**
     * A callback to be invoked when Beam has been inititaed to another device.
     */
    public interface IntentCreator {
        /**
         * Create the intent to transfer to the destination phone.
         * <p>
         * This runs on the sender phone. The credentials for the receiver phone are available
         * in {@code call}. For example you can use
         * {@link io.v.v23.security.VSecurity#getRemoteBlessingNames(VContext, Call)} to find the
         * receiver's blessings.
         * <p>
         * You should return an intent URI (as returned by {@link Intent#toUri(int)}), and
         * optionally a byte array payload. This intent will be started on the receiver phone.
         */
        ListenableFuture<Pair<String, byte[]>> createIntent(VContext context, ServerCall call);
    }

    /**
     * Set a callback that dynamically generates an Intent to send using Android Beam.
     * <p>
     * You should call this method during your Activity's onCreate(). You should cancel the context
     * in your Activity's onDestroy(), but not before.
     * <p>
     * Do not mix calls to VBeam and NfcAdapter for the same activity.
     */
    public static boolean setBeamIntentCallback(VContext context,
                                                Activity activity,
                                                IntentCreator creator) throws VException {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc == null)
            return false;
        if (creator == null) {
            nfc.setNdefPushMessageCallback(null, activity);
            return true;
        }

        final VBeamManager vBeamManager = new VBeamManager(context, activity, creator);
        nfc.setNdefPushMessageCallback(vBeamManager, activity);
        return true;
    }

    private VBeam() { }  // static

    /**
     * Returns the {@code byte[]} payload from an intent sent using VBeam.
     * Call this on the receiving device to retrieve your payload.
     */
    public static byte[] getBeamPayload(Intent intent) {
        return intent.getByteArrayExtra(VBeamManager.EXTRA_VBEAM_PAYLOAD);
    }
}
