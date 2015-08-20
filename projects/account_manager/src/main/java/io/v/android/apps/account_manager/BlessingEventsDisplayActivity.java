// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;

import java.util.List;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.VCertificate;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Lists all the blessings given to a particular principal.
 */
public class BlessingEventsDisplayActivity extends PreferenceActivity {
    public static final String TAG = "BlessingEventsDisplay"; // 23 character limit
    public static final String EXTRA_PUBLIC_KEY = "PUBLIC_KEY";

    VContext mBaseContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseContext = V.init(this);

        SharedPreferences blessingsLog =
                getSharedPreferences(BlessActivity.LOG_BLESSINGS, MODE_PRIVATE);
        String principalPublicKey =
                getIntent().getStringExtra(EXTRA_PUBLIC_KEY);

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        int n = blessingsLog.getInt(principalPublicKey, 0);

        for (int i = Math.max(0, n - BlessActivity.MAX_BLESSINGS_FOR_PRINCIPAL); i < n; i++)
        {
            String key = principalPublicKey + "_" + i;
            String encoded = blessingsLog.getString(key, "");
            try {
                // Recover the certificate chains that were given out.
                List<List<VCertificate>> certChains =
                        blessingsFromEvent(BlessingEvent.decode(encoded));
                for (List<VCertificate> certChain: certChains) {
                    String name = certificateChainName(certChain);
                    String certChainVom = null;
                    try {
                        certChainVom = VomUtil.encodeToString(certChain,
                                new TypeToken<List<VCertificate>>(){}.getType());
                    } catch(VException e) {
                        String msg = "Couldn't serialize certificate chain: " + e;
                        android.util.Log.e(TAG, msg);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                    Preference currentPreference = new Preference(this);
                    currentPreference.setSummary(name);
                    currentPreference.setEnabled(true);

                    Intent intent = new Intent();
                    intent.setPackage("io.v.android.apps.account_manager");
                    intent.setClassName("io.v.android.apps.account_manager",
                            "io.v.android.apps.account_manager.BlessingDisplayActivity");
                    intent.setAction("io.v.android.apps.account_manager.DISPLAY_BLESSING");
                    intent.putExtra(BlessingDisplayActivity.EXTRA_CERTIFICATE_CHAIN_VOM,
                            certChainVom);
                    currentPreference.setIntent(intent);
                    prefScreen.addPreference(currentPreference);
                }
            } catch (Exception e) {
                String msg = "BlessingEvent not found or improperly serialized.";
                android.util.Log.e(TAG, msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }
        setPreferenceScreen(prefScreen);
    }

    private List<List<VCertificate>> blessingsFromEvent(BlessingEvent event) throws VException {
        // Get the blessings that were extended.
        String blessingsVom = event.getBlessingsVom();
        Blessings blessings = (Blessings) VomUtil.decodeFromString(blessingsVom,
                Blessings.class);
        List<List<VCertificate>> certChains = blessings.getCertificateChains();

        // Recreate the certificate that the blessings were extended with.
        VCertificate cert = new VCertificate();
        cert.setExtension(event.getNameExtension());
        cert.setPublicKey(event.getPublicKey().getEncoded());
        cert.setCaveats(event.getCaveats());

        // Recreate the blessing that was given to the remote end.
        for (List<VCertificate> certChain: certChains) {
            certChain.add(cert);
        }
        return certChains;
    }

    private String certificateChainName(List<VCertificate> certChain) {
        String name = "";
        int size = certChain.size();
        for (int j = 0; j < (size - 1); j++) {
            name += certChain.get(j).getExtension() + "/";
        }
        name += certChain.get(size - 1).getExtension();
        return name;
    }
}