// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.ListView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

import io.v.v23.security.Constants;
import io.v.v23.security.VCertificate;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;
import io.v.v23.security.Caveat;
import io.v.v23.uniqueid.Id;

/**
 * Displays a single certificate.
 */
public class CertificateDisplayActivity extends PreferenceActivity  {
    public static final String TAG = "BlessingDetailsDisplay";
    public static final String EXTRA_CERTIFICATE_VOM = "EXTRA_CERTIFICATE_VOM";

    private static final String NAME_TITLE = "Name";
    private static final String PUBLIC_KEY_TITLE = "Public Key";
    private static final String CAVEATS_TITLE = "Caveats";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        byte[] certificateVom = getIntent().getByteArrayExtra(EXTRA_CERTIFICATE_VOM);
        VCertificate certificate = null;
        try {
            certificate = (VCertificate) VomUtil.decode(certificateVom, VCertificate.class);
        } catch (Exception e) {
            handleError("Couldn't display certificate: " + e);
            return;
        }

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        ListView listView = new ListView(this);
        prefScreen.bind(listView);

        PreferenceCategory nameCategory = new PreferenceCategory(this);
        nameCategory.setTitle(NAME_TITLE);
        prefScreen.addPreference(nameCategory);

        Preference namePref  = new Preference(this);
        namePref.setTitle(certificate.getExtension());
        namePref.setEnabled(false);
        nameCategory.addPreference(namePref);

        PreferenceCategory publicKeyCategory = new PreferenceCategory(this);
        publicKeyCategory.setTitle(PUBLIC_KEY_TITLE);
        prefScreen.addPreference(publicKeyCategory);

        // Display the fields of the certificate
        Preference publicKeyPreference = new Preference(this);
        publicKeyPreference.setSummary(
                VomUtil.bytesToHexString(certificate.getPublicKey()).toString());
        publicKeyPreference.setEnabled(false);
        publicKeyCategory.addPreference(publicKeyPreference);

        PreferenceCategory caveatsCategory = new PreferenceCategory(this);
        caveatsCategory.setTitle(CAVEATS_TITLE);
        prefScreen.addPreference(caveatsCategory);

        for (Caveat caveat: certificate.getCaveats()) {
            String caveatDescription = caveatText(caveat);
            Preference caveatPreference = new Preference(this);
            caveatPreference.setSummary(caveatDescription);
            caveatPreference.setEnabled(true);
            caveatsCategory.addPreference(caveatPreference);
        }

        setPreferenceScreen(prefScreen);
    }

    /**
     * Returns a human-readable description of a caveat.
     *
     * @param caveat        the caveat to be examined
     * @return              a human-readable description of the caveat
     */
    public static String caveatText(Caveat caveat) {
        Id caveatId = caveat.getId();
        if (caveatId.equals(Constants.CONST_CAVEAT.getId())) {
            return "Const Caveat";
        } else if (caveatId.equals(Constants.EXPIRY_CAVEAT.getId())) {
            String ret = "Expiry Caveat: ";
            try {
                DateTime expiry = expiryCaveatPayload(caveat);
                ret += expiry.toString(DateTimeFormat.mediumDateTime());
            } catch (VException e) {
                android.util.Log.e(TAG, "Error parsing expiry caveat payload: " + e);
                ret += "Could not get expiry time";
            }
            return ret;
        } else if (caveatId.equals(Constants.METHOD_CAVEAT.getId())) {
            String ret = "Method Caveat: ";
            try {
                List<String> methods = methodCaveatPayload(caveat);
                for (String name: methods) {
                    ret += name + "\n";
                }
            } catch (VException e) {
                android.util.Log.e(TAG, "Error parsing method caveat payload: " + e);
                ret += "Could not get method names";
            }
            return ret;
        } else if (caveatId.equals(Constants.PEER_BLESSINGS_CAVEAT.getId())) {
            return  "Peer Blessings Caveat";
        } else if (caveatId.equals(Constants.PUBLIC_KEY_THIRD_PARTY_CAVEAT.getId())) {
            return  "Public Key Third Party Caveat";
        } else {
            return  "Unknown Caveat";
        }
    }

    /**
     * Returns expiry time that the caveat restricts the blessing usage to.
     *
     * @param caveat            the caveat to be examined.
     * @return                  expiry time
     * @throws VException       if the given caveat is not a method caveat, or if there
     *                          was a problem getting the method names
     */
    public static DateTime expiryCaveatPayload(Caveat caveat) throws VException {
        Object param = VomUtil.decode(caveat.getParamVom());
        if (param == null) {
            param = new DateTime(0);
        }
        if (!(param instanceof DateTime)) {
            throw new VException(String.format(
                    "Caveat param %s of wrong type: want %s", param, DateTime.class));
        }
        return (DateTime) param;
    }

    /**
     * Returns method names that the caveat restricts the blessing usage to.
     *
     * @param caveat            the caveat to be examined.
     * @return                  list of methods that the blessing is valid for
     * @throws VException       if the given caveat is not a method caveat, or if there
     *                          was a problem getting the method names
     */
    public static List<String> methodCaveatPayload(Caveat caveat) throws VException {
        Object param = VomUtil.decode(caveat.getParamVom());
        if (param == null) {
            param = new ArrayList<String>();
        }
        if (!(param instanceof List<?>)) {
            throw new VException(String.format(
                    "Caveat param %s of wrong type: want List<?>", param));
        }
        try {
            return (List<String>) param;
        } catch (Exception e) {
            throw new VException(String.format(
                    "Caveat param %s of wrong type: want List<String>", param));
        }
    }

    private void handleError(String error) {
        String msg = "Caveat display error: " + error;
        android.util.Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
