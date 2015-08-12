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

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.List;

import io.v.v23.android.V;
import io.v.v23.security.Caveat;
import io.v.v23.security.VCertificate;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Displays a blessing (i.e., a single certificate chain).
 */
public class BlessingDisplayActivity extends PreferenceActivity  {
    public static final String TAG = "BlessingDisplayActivity";
    public static final String EXTRA_CERTIFICATE_CHAIN_VOM = "EXTRA_CERTIFICATE_CHAIN_VOM";
    public static final String EXTRA_BLESSING_PATTERN = "EXTRA_BLESSING_PATTERN";

    private static final String PEERS_TITLE = "Peers";
    private static final String CERTIFICATES_TITLE = "Certificates";
    private static final String CAVEATS_TITLE = "Caveats";

    DateTime mExpiryTime = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        V.init(this);

        Bundle extras = getIntent().getExtras();

        String certChainVom =
                extras.getString(EXTRA_CERTIFICATE_CHAIN_VOM);
        String pattern =
                extras.getString(EXTRA_BLESSING_PATTERN);
        if (pattern == null) {
            pattern = "Pattern not found";
        }

        List<VCertificate> certChain = null;
        try {
            certChain = (List<VCertificate>) VomUtil.decodeFromString(certChainVom,
                    new TypeToken<List<VCertificate>>(){}.getType());
        } catch (Exception e) {
            handleError("Couldn't display blessings: " + e);
        }

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        ListView listView = new ListView(this);
        prefScreen.bind(listView);

        PreferenceCategory peersCategory = new PreferenceCategory(this);
        peersCategory.setTitle(PEERS_TITLE);
        prefScreen.addPreference(peersCategory);

        Preference patternPreference = new Preference(this);
        patternPreference.setTitle(pattern);
        patternPreference.setEnabled(false);
        peersCategory.addPreference(patternPreference);

        PreferenceCategory certificatesCategory = new PreferenceCategory(this);
        certificatesCategory.setTitle(CERTIFICATES_TITLE);
        prefScreen.addPreference(certificatesCategory);

        PreferenceCategory caveatsCategory = new PreferenceCategory(this);
        caveatsCategory.setTitle(CAVEATS_TITLE);
        prefScreen.addPreference(caveatsCategory);

        for (VCertificate certificate: certChain) {
            Preference currentPreference = new Preference(this);
            currentPreference.setSummary(certificate.getExtension());
            currentPreference.setEnabled(true);

            String certificateVom = null;
            try {
                certificateVom = VomUtil.encodeToString(certificate, VCertificate.class);
            } catch (Exception e) {
                handleError("Couldn't serialize certificate: " + e);
            }

            Intent intent = new Intent();
            intent.setPackage("io.v.android.apps.account_manager");
            intent.setClassName("io.v.android.apps.account_manager",
                    "io.v.android.apps.account_manager.CertificateDisplayActivity");
            intent.setAction("io.v.android.apps.account_manager.DISPLAY_CERTIFICATE");
            intent.putExtra(CertificateDisplayActivity.EXTRA_CERTIFICATE_VOM, certificateVom);
            currentPreference.setIntent(intent);

            certificatesCategory.addPreference(currentPreference);

            for (Caveat caveat: certificate.getCaveats()) {
                if (caveat.getId().equals(io.v.v23.security.Constants.EXPIRY_CAVEAT.getId())) {
                    updateExpiryTime(caveat);
                } else {
                    String caveatDescription = CertificateDisplayActivity.caveatText(caveat);
                    Preference caveatPreference = new Preference(this);
                    caveatPreference.setSummary(caveatDescription);
                    caveatPreference.setEnabled(true);
                    caveatsCategory.addPreference(caveatPreference);
                }
            }
        }
        if (mExpiryTime != null) {
            Preference expiryPref = new Preference(this);
            expiryPref.setSummary("Expiry Caveat: " +
                    mExpiryTime.toString(DateTimeFormat.mediumDateTime()));
            expiryPref.setEnabled(true);
            caveatsCategory.addPreference(expiryPref);
        }
        setPreferenceScreen(prefScreen);
    }

    private void updateExpiryTime(Caveat caveat) {
        try {
            DateTime expiry = CertificateDisplayActivity.expiryCaveatPayload(caveat);
            if (mExpiryTime == null) {
                mExpiryTime = expiry;
            } else if (expiry.isBefore(mExpiryTime)) {
                mExpiryTime = expiry;
            }
        } catch (VException e) {
            handleError("Could not get expiry time: " + e.getMessage());
            return;
        }
    }

    private void handleError(String error) {
        String msg = "Blessing display error: " + error;
        android.util.Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
