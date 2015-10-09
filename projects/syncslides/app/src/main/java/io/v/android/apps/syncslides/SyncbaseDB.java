// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.File;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class SyncbaseDB implements DB {

    private static final String TAG = "SyncbaseDB";
    /**
     * The intent result code for when we get blessings from the account manager.
     * The value must not conflict with any other blessing result codes.
     */
    private static final int BLESSING_REQUEST = 200;
    private static final String SYNCBASE_APP = "syncslides";
    private static final String SYNCBASE_DB = "syncslides";
    private static final String DECKS_TABLE = "Decks";

    private Permissions mPermissions;
    private Context mContext;
    private VContext vContext;
    private Table mDecks;

    SyncbaseDB(Context context) {
        mContext = context;
    }

    @Override
    public void init(Activity activity) {
        if (vContext != null) {
            // Already initialized.
            return;
        }
        vContext = V.init(mContext);
        try {
            vContext = V.withListenSpec(
                    vContext, V.getListenSpec(vContext).withProxy("proxy"));
        } catch (VException e) {
            handleError("Couldn't setup vanadium proxy: " + e.getMessage());
        }
        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        mPermissions = new Permissions(ImmutableMap.of(
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));
        getBlessings(activity);

    }

    private void getBlessings(Activity activity) {
        Blessings blessings = null;
        try {
            // See if there are blessings stored in shared preferences.
            blessings = BlessingsManager.getBlessings(mContext);
        } catch (VException e) {
            handleError("Error getting blessings from shared preferences " + e.getMessage());
        }
        if (blessings == null) {
            // Request new blessings from the account manager via an intent.  This intent
            // will call back to onActivityResult() which will continue with
            // configurePrincipal().
            refreshBlessings(activity);
            return;
        }
        configurePrincipal(blessings);
    }

    private void refreshBlessings(Activity activity) {
        Intent intent = BlessingService.newBlessingIntent(mContext);
        activity.startActivityForResult(intent, BLESSING_REQUEST);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLESSING_REQUEST) {
            try {
                byte[] blessingsVom = BlessingService.extractBlessingReply(resultCode, data);
                Blessings blessings = (Blessings) VomUtil.decode(blessingsVom, Blessings.class);
                BlessingsManager.addBlessings(mContext, blessings);
                Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show();
                configurePrincipal(blessings);
            } catch (BlessingCreationException e) {
                handleError("Couldn't create blessing: " + e.getMessage());
            } catch (VException e) {
                handleError("Couldn't derive blessing: " + e.getMessage());
            }
            return true;
        }
        return false;
    }

    private void configurePrincipal(Blessings blessings) {
        // TODO(kash): Probably better to do this not in the UI thread.
        try {
            VPrincipal p = V.getPrincipal(vContext);
            p.blessingStore().setDefaultBlessings(blessings);
            p.blessingStore().set(blessings, new BlessingPattern("..."));
            p.addToRoots(blessings);
        } catch (VException e) {
            handleError(String.format(
                    "Couldn't set local blessing %s: %s", blessings, e.getMessage()));
            return;
        }
        setupSyncbase(blessings);
    }

    // TODO(kash): Run this in an AsyncTask so it doesn't block the UI.
    private void setupSyncbase(Blessings blessings) {
        // Prepare the syncbase storage directory.
        File storageDir = new File(mContext.getFilesDir(), "syncbase");
        storageDir.mkdirs();

        try {
            vContext = SyncbaseServer.withNewServer(vContext, new SyncbaseServer.Params()
                    .withPermissions(mPermissions)
                    .withStorageRootDir(storageDir.getAbsolutePath()));
        } catch (SyncbaseServer.StartException e) {
            handleError("Couldn't start syncbase server");
            return;
        }
        try {
            Server syncbaseServer = V.getServer(vContext);
            String serverName = "/" + syncbaseServer.getStatus().getEndpoints()[0];
            SyncbaseService service = Syncbase.newService(serverName);
            SyncbaseApp app = service.getApp(SYNCBASE_APP);
            if (!app.exists(vContext)) {
                app.create(vContext, mPermissions);
            }
            Database db = app.getNoSqlDatabase(SYNCBASE_DB, null);
            if (!db.exists(vContext)) {
                db.create(vContext, mPermissions);
            }
            mDecks = db.getTable(DECKS_TABLE);
            if (!mDecks.exists(vContext)) {
                mDecks.create(vContext, mPermissions);
            }
            // Test that we can put and get.
            // TODO(kash): Replace this with the real code.
            mDecks.put(vContext, "dummy", new Integer(1), Integer.class);
            Integer result = (Integer) mDecks.get(vContext, "dummy", Integer.class);
            handleError("got result " + result);
        } catch (VException e) {
            handleError("Couldn't setup syncbase service: " + e.getMessage());
            return;
        }
    }

    @Override
    public void askQuestion(String identity) {

    }

    @Override
    public void getQuestionerList(String deckId, QuestionerListener callback) {

    }

    @Override
    public DeckList getDecks() {
        return null;
    }

    @Override
    public SlideList getSlides(String deckId) {
        return null;
    }

    @Override
    public void getSlides(String deckId, SlidesCallback callback) {

    }

    private void handleError(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }

}
