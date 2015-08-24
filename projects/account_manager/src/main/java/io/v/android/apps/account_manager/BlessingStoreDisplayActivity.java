package io.v.android.apps.account_manager;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.v.android.v23.V;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.BlessingStore;
import io.v.v23.security.Blessings;
import io.v.v23.security.VCertificate;
import io.v.v23.security.VPrincipal;
import io.v.v23.vom.VomUtil;

/**
 * Displays all the blessings in account manager's blessing store.
 */
public class BlessingStoreDisplayActivity extends PreferenceActivity  {
    public static final String TAG = "BlessingStoreDisplay";

    private static final String SIGNING_BLESSINGS_TITLE = "Identity Blessings";
    private static final String AUTHORIZATION_BLESSINGS_TITLE = "Authorization Blessings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VPrincipal principal = V.getPrincipal(V.init(this));
        BlessingStore blessingStore = principal.blessingStore();

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        ListView listView = new ListView(this);
        prefScreen.bind(listView);

        PreferenceCategory signingCat = new PreferenceCategory(this);
        signingCat.setTitle(SIGNING_BLESSINGS_TITLE);
        prefScreen.addPreference(signingCat);

        PreferenceCategory nonSigningCat = new PreferenceCategory(this);
        nonSigningCat.setTitle(AUTHORIZATION_BLESSINGS_TITLE);
        prefScreen.addPreference(nonSigningCat);

        for (Map.Entry<BlessingPattern, Blessings> entry: blessingStore.peerBlessings().entrySet()) {
            Blessings blessings = entry.getValue();
            for (List<VCertificate> certChain: blessings.getCertificateChains()) {
                String name = "";
                int size = certChain.size();
                for (int i = 0; i < (size - 1); i++) {
                    name += certChain.get(i).getExtension() + "/";
                }
                name += certChain.get(size - 1).getExtension();

                byte[] certChainVom = null;
                try {
                    certChainVom = VomUtil.encode(certChain,
                            new TypeToken<List<VCertificate>>(){}.getType());
                } catch(Exception e) {
                    String msg = "Couldn't serialize certificate chain: " + e;
                    android.util.Log.e(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    return;
                }

                Preference currentPreference = new Preference(this);
                currentPreference.setSummary(name);
                currentPreference.setEnabled(true);

                Intent intent = new Intent();
                intent.setPackage("io.v.android.apps.account_manager");
                intent.setClassName("io.v.android.apps.account_manager",
                        "io.v.android.apps.account_manager.BlessingDisplayActivity");
                intent.setAction(
                        "io.v.android.apps.account_manager.DISPLAY_BLESSING");
                intent.putExtra(BlessingDisplayActivity.EXTRA_CERTIFICATE_CHAIN_VOM, certChainVom);
                intent.putExtra(BlessingDisplayActivity.EXTRA_BLESSING_PATTERN,
                        entry.getKey().getValue());
                currentPreference.setIntent(intent);

                List<List<VCertificate>> b = new ArrayList<List<VCertificate>>();
                b.add(certChain);
                if (Blessings.create(b).signingBlessings().isEmpty()) {
                    nonSigningCat.addPreference(currentPreference);
                } else {
                    signingCat.addPreference(currentPreference);
                }

            }
        }
        if (signingCat.getPreferenceCount() <= 0) {
            Preference nonePref = new Preference(this);
            nonePref.setTitle("No Identity Blessings");
            nonePref.setEnabled(false);
            signingCat.addPreference(nonePref);
        }
        if (nonSigningCat.getPreferenceCount() <= 0) {
            Preference nonePref = new Preference(this);
            nonePref.setTitle("No Authorization Blessings");
            nonePref.setEnabled(false);
            nonSigningCat.addPreference(nonePref);
        }
        setPreferenceScreen(prefScreen);
    }
}
