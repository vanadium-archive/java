// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.BlessingStore;
import io.v.v23.security.Blessings;
import io.v.v23.security.VCertificate;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.vom.VomUtil;

/**
 * Prompts user to choose among his/her Vanadium Blessings.
 */
public class BlessingChooserActivity extends Activity {
    public static final String TAG = "BlessingChooserActivity";
    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    private static final int CREATE_BLESSING_REQUEST = 1;

    BlessingStore mBlessingStore = null;
    HashMap<Integer, Blessings> mBlessings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VContext context = V.init(this);
        VPrincipal principal = V.getPrincipal(context);
        mBlessingStore = principal.blessingStore();
        mBlessings = new HashMap<>();

        setContentView(R.layout.activity_blessing_chooser);
        if (hasBlessings()) {
            display();
        } else {
            // No Vanadium blessings available: prompt the user to fetch an blessing from his/her
            // identity service.
            createBlessings();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CREATE_BLESSING_REQUEST:
                if (resultCode != RESULT_OK && !hasBlessings()) {
                    // No blessing to display.  To avoid putting the user into a potentially
                    // infinite blessing creation loop, we reply with an error here.
                    replyWithError("Couldn't create a Vanadium blessing.");
                    return;
                }
                display();
        }
    }

    private void display() {
        if (hasBlessings()) {
            LinearLayout blessingsView = (LinearLayout) findViewById(R.id.chooser_blessings);
            blessingsView.removeAllViews();

            Map<BlessingPattern, Blessings> peerMap = mBlessingStore.peerBlessings();

            for (BlessingPattern pattern: peerMap.keySet()) {
                for (List<VCertificate> certChain: peerMap.get(pattern).getCertificateChains()) {
                    ArrayList<List<VCertificate>> certChains = new ArrayList<List<VCertificate>>();
                    certChains.add(certChain);
                    Blessings blessing = new Blessings(certChains);
                    mBlessings.put(blessingsView.getChildCount(), blessing);

                    LinearLayout blessingView =
                            (LinearLayout)
                                    getLayoutInflater().inflate(R.layout.chooser_blessing, null);
                    CheckedTextView view =
                            (CheckedTextView) blessingView.findViewById(R.id.chooser_blessing);
                    view.setText(name(certChain));
                    blessingsView.addView(blessingView);
                }
            }
        } else {
            replyWithError("Couldn't find newly obtained blessings?!");
        }
    }

    // Starts the blessing creation activity.
    private void createBlessings() {
        startActivityForResult(new Intent(this, AccountActivity.class), CREATE_BLESSING_REQUEST);
    }

    private boolean hasBlessings() {
        return !mBlessingStore.peerBlessings().values().isEmpty();
    }

    public void onBlessingSelected(View v) {
        CheckedTextView blessingView = (CheckedTextView) v.findViewById(R.id.chooser_blessing);
        blessingView.setChecked(!blessingView.isChecked());
    }

    public void onOK(View v) {
        LinearLayout blessingsView = (LinearLayout) findViewById(R.id.chooser_blessings);
        ArrayList<Blessings> selectedBlessings = new ArrayList<>();
        for (int i = 0; i < blessingsView.getChildCount(); ++i) {
            CheckedTextView blessingView =
                    (CheckedTextView)
                            blessingsView.getChildAt(i).findViewById(R.id.chooser_blessing);
            if (blessingView.isChecked()) {
                selectedBlessings.add(mBlessings.get(i));
            }
        }
        if (selectedBlessings.isEmpty()) {
            Toast.makeText(this, "Must select a blessing.", Toast.LENGTH_LONG).show();
            return;
        }

        replyWithSuccess(selectedBlessings.toArray(new Blessings[selectedBlessings.size()]));
    }

    public void onCancel(View v) {
        replyWithError("User canceled blessing selection.");
    }

    public void onAdd(View v) {
        createBlessings();
    }

    private void replyWithSuccess(Blessings[] blessings) {
        Intent intent = new Intent();
        try {
            Blessings union = VSecurity.unionOfBlessings(blessings);
            intent.putExtra(REPLY, VomUtil.encodeToString(union, Blessings.class));
            setResult(RESULT_OK, intent);
            finish();
        } catch (Exception e) {
            replyWithError("Could not serialize blessings: " + e.getMessage());
        }

    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Error choosing blessings: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    private static String name(List<VCertificate> certChain) {
        String name = "";
        for (VCertificate certificate: certChain) {
            name += certificate.getExtension() + "/";
        }
        return name;
    }
}
