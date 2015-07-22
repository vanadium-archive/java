package io.v.android.apps.account_manager;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;
import java.util.List;
import java.util.Map;

import io.v.v23.android.V;
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
    public static final String CERTIFICATE_CHAIN_VOM = "CERTIFICATE_CHAIN_VOM";
    public static final String BLESSING_PATTERN = "BLESSING_PATTERN";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VPrincipal principal = V.getPrincipal(V.init(this));
        BlessingStore blessingStore = principal.blessingStore();

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        ListView listView = new ListView(this);
        prefScreen.bind(listView);

        for (Map.Entry<BlessingPattern, Blessings> entry: blessingStore.peerBlessings().entrySet()) {
            Blessings blessings = entry.getValue();

            for (List<VCertificate> certChain: blessings.getCertificateChains()) {
                String name = "";
                for (VCertificate certificate: certChain) {
                    name += certificate.getExtension() + "/";
                }

                String certChainVom = null;
                try {
                    certChainVom = VomUtil.encodeToString(certChain,
                            new TypeToken<List<VCertificate>>(){}.getType());
                } catch(Exception e) {
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
                intent.setAction(
                        "io.v.android.apps.account_manager.DISPLAY_BLESSING");
                intent.putExtra(CERTIFICATE_CHAIN_VOM, certChainVom);
                intent.putExtra(BLESSING_PATTERN, entry.getKey().getValue());
                currentPreference.setIntent(intent);

                prefScreen.addPreference(currentPreference);
            }
        }
        setPreferenceScreen(prefScreen);
    }
}
