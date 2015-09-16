// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.slide_presenter;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VCertificate;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.SyncGroupMemberInfo;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseServerParams;
import io.v.v23.syncbase.SyncbaseServerStartException;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Row;
import io.v.v23.syncbase.nosql.Stream;
import io.v.v23.syncbase.nosql.SyncGroup;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class SlidePresenterActivity extends Activity {
    private static final String TAG = "SlidePresenter";

    private static final int BLESSING_REQUEST = 1;
    private static final String SYNCBASE_APP_NAME = "slidepresenter";
    private static final String SYNCBASE_DB_NAME = "slidepresenter";
    private static final String SYNCBASE_TABLE_NAME = "slidepresenter";
    private static final String SYNCBASE_SLIDE_NUM_ROW_NAME = "slideNumber";
    private static final String SYNCGROUP_NAME = "syncgroup";
    private static final String SYNCBASE_MOUNTTABLE = "/ns.dev.v.io:8101";
    private static final Slide[] SLIDES;

    static {
        int[] slideDrawables = new int[]{R.drawable.slide1, R.drawable.slide2, R.drawable.slide3,
                R.drawable.slide4, R.drawable.slide5, R.drawable.slide6, R.drawable.slide7};
        SLIDES = new Slide[slideDrawables.length];
        for (int i = 0; i < slideDrawables.length; ++i) {
            SLIDES[i] = new Slide(slideDrawables[i], "Notes for slide " + (i + 1));
        }
    }

    private VContext mBaseContext = null;
    private Permissions mPermissions = null;
    private Row mSlideNumRow = null;
    private SyncGroup mSyncGroup = null;
    private Stream<WatchChange> mChangeStream = null;
    private volatile int mSlideNum = 0;
    private volatile boolean mSynced = true;

    private enum Role {
        Lecturer, Moderator, Audience
    }

    private Role role = Role.Audience;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (role == Role.Lecturer) {
            setContentView(R.layout.activity_slidelecturer);
        } else if (role == Role.Audience) {
            setContentView(R.layout.activity_slideaudience);
        }
        mBaseContext = V.init(this);
        try {
            mBaseContext = V.withListenSpec(
                    mBaseContext, V.getListenSpec(mBaseContext).withProxy("proxy"));
        } catch (VException e) {
            handleError("Couldn't setup vanadium proxy: " + e.getMessage());
        }
        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        mPermissions = new Permissions(ImmutableMap.of(
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));
        getBlessings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSyncGroup != null) {
            try {
                mSyncGroup.leave(mBaseContext);
            } catch (VException e) {
                Log.e(TAG, "Couldn't leave syncgroup: " + e.getMessage());
            }
        }
        if (mChangeStream != null) {
            try {
                mChangeStream.cancel();
            } catch (VException e) {
                Log.e(TAG, "Couldn't cancel change stream: " + e.getMessage());
            }
        }
        Server syncbaseServer = V.getServer(mBaseContext);
        if (syncbaseServer != null) {
            try {
                syncbaseServer.stop();
            } catch (VException e) {
                Log.e(TAG, "Couldn't stop syncbase server: " + e.getMessage());
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.textView).setVisibility(View.INVISIBLE);
            ((ImageView) findViewById(R.id.slideView)).setScaleType(ImageView.ScaleType.FIT_XY);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            findViewById(R.id.textView).setVisibility(View.VISIBLE);
            ((ImageView) findViewById(R.id.slideView)).setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        if (role == Role.Audience) {
            findViewById(R.id.prevButton).setVisibility(View.INVISIBLE);
            findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESSING_REQUEST:
                try {
                    byte[] blessingsVom = BlessingService.extractBlessingReply(resultCode, data);
                    Blessings blessings = (Blessings) VomUtil.decode(blessingsVom, Blessings.class);
                    BlessingsManager.addBlessings(this, blessings);
                    Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
                    getBlessings();
                } catch (BlessingCreationException e) {
                    handleError("Couldn't create blessing: " + e.getMessage());
                } catch (VException e) {
                    handleError("Couldn't derive blessing: " + e.getMessage());
                }
                return;
        }
    }

    private void refreshBlessings() {
        Intent intent = BlessingService.newBlessingIntent(this);
        startActivityForResult(intent, BLESSING_REQUEST);
    }

    private void getBlessings() {
        Blessings blessings = null;
        try {
            // See if there are blessings stored in shared preferences.
            blessings = BlessingsManager.getBlessings(this);
        } catch (VException e) {
            String msg = "Error getting blessings from shared preferences " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            android.util.Log.e(TAG, msg);
        }
        if (blessings == null) {
            // Request new blessings from the account manager.  If successful, this will eventually
            // trigger another call to this method, with BlessingsManager.getBlessings() returning
            // non-null blessings.
            refreshBlessings();
            return;
        }
        try {
            VPrincipal p = V.getPrincipal(mBaseContext);
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

    private void setupSyncbase(Blessings blessings) {
        // Prepare the syncbase storage directory.
        File storageDir = new File(this.getFilesDir(), "syncbase");
        // Remove the syncbase directory as syncgroups aren't yet restartable: using an
        // existing directory messes up the sync.
        deleteFileRecursively(storageDir);
        storageDir.mkdirs();

        String email = emailFromBlessings(blessings);
        if (email.isEmpty()) {
            handleError("Couldn't get email from blessings: " + blessings);
            return;
        }
        try {
            mBaseContext = Syncbase.withNewServer(mBaseContext, new SyncbaseServerParams()
                    .withPermissions(mPermissions)
                    .withStorageRootDir(storageDir.getAbsolutePath()));
        } catch (SyncbaseServerStartException e) {
            handleError("Couldn't start syncbase server");
            return;
        }
        try {
            Server syncbaseServer = V.getServer(mBaseContext);
            String serverName = "/" + syncbaseServer.getStatus().getEndpoints()[0];
            SyncbaseService service = Syncbase.newService(serverName);
            SyncbaseApp app = service.getApp(SYNCBASE_APP_NAME);
            if (!app.exists(mBaseContext)) {
                app.create(mBaseContext, mPermissions);
            }
            Database db = app.getNoSqlDatabase(SYNCBASE_DB_NAME, null);
            if (!db.exists(mBaseContext)) {
                db.create(mBaseContext, mPermissions);
            }
            Table table = db.getTable(SYNCBASE_TABLE_NAME);
            if (!table.exists(mBaseContext)) {
                table.create(mBaseContext, mPermissions);
            }
            mSlideNumRow = table.getRow(SYNCBASE_SLIDE_NUM_ROW_NAME);
            if (!mSlideNumRow.exists(mBaseContext)) {
                mSlideNumRow.put(mBaseContext, 0, Integer.class);
            }
            mChangeStream = db.watch(mBaseContext, SYNCBASE_TABLE_NAME,
                    SYNCBASE_SLIDE_NUM_ROW_NAME, db.getResumeMarker(mBaseContext));
            new SlideChangeAsyncTask(mChangeStream).execute();
            String sgName = NamingUtil.join(SYNCBASE_MOUNTTABLE,
                    "users", email, "slidePresenterSync", "desktop", "@@sync", SYNCGROUP_NAME);
            mSyncGroup = db.getSyncGroup(sgName);
            mSyncGroup.join(mBaseContext, new SyncGroupMemberInfo((byte) 0));
        } catch (VException e) {
            handleError("Couldn't setup syncbase service: " + e.getMessage());
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setSlideNum(int slideNum) {
        final int slide =
                slideNum < 0 ? 0 : (slideNum >= SLIDES.length ? SLIDES.length - 1 : slideNum);
        mSlideNum = slide;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ImageView) findViewById(R.id.slideView))
                        .setImageResource(SLIDES[slide].getSlideDrawableId());

                ((ImageView) findViewById(R.id.currSlide))
                        .setImageResource(SLIDES[slide].getSlideDrawableId());

                if (slide > 0) {
                    ((ImageView) findViewById(R.id.prevSlide))
                            .setImageResource(SLIDES[slide - 1].getSlideDrawableId());
                } else {
                    ((ImageView) findViewById(R.id.prevSlide)).setImageResource(0);
                }

                if (slide < SLIDES.length - 1) {
                    ((ImageView) findViewById(R.id.nextSlide))
                            .setImageResource(SLIDES[slide + 1].getSlideDrawableId());
                } else {
                    ((ImageView) findViewById(R.id.nextSlide)).setImageResource(0);
                }
                if (role == Role.Lecturer || role == Role.Moderator) {
                    ((TextView) findViewById(R.id.textView)).setText(SLIDES[slide].getSlideNotes());
                }
                findViewById(R.id.slideView).invalidate();
            }
        });
    }

    public void updateSyncbase(int slideNum) {
        try {
            mSlideNumRow.put(mBaseContext, slideNum, Integer.class);
        } catch (VException e) {
            handleError("Couldn't write to syncbase database: " + e.getMessage());
            // Normally, updating the row would trigger the call to refreshDisplay()
            // (via SlideChangeAsyncTask), but since the update failed we have to do it manually.
            setSlideNum(slideNum);
        }
    }

    public void nextSlide(View v) {
        if (role == Role.Lecturer || role == Role.Moderator) {
            // This update will trigger a call to setSlideNum().
            updateSyncbase(mSlideNum + 1);
        } else if (role == Role.Audience) {
            setSlideNum(mSlideNum + 1);
        }
    }

    public void prevSlide(View v) {
        if (role == Role.Lecturer || role == Role.Moderator) {
            // This update will trigger a call to setSlideNum().
            updateSyncbase(mSlideNum - 1);  // this will trigger a call to setSlideNum()
        } else if (role == Role.Audience) {
            setSlideNum(mSlideNum - 1);
        }
    }

    public void syncClick(View v) {
        mSynced = !mSynced;
        if (mSynced) {
            // Immediately read the slide number from syncbase, as it may have been
            // updated during the un-synced phase.
            try {
                setSlideNum((Integer) mSlideNumRow.get(mBaseContext, Integer.class));
            } catch (VException e) {
                handleError("Couldn't read from syncbase database: " + e.getMessage());
            }
            findViewById(R.id.prevButton).setVisibility(View.INVISIBLE);
            findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
            ((Button) findViewById(R.id.syncButton)).setText("Unsync");
        } else {
            findViewById(R.id.prevButton).setVisibility(View.VISIBLE);
            findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.syncButton)).setText("Sync");
        }
    }

    private void handleError(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private static void deleteFileRecursively(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                deleteFileRecursively(child);
            }
        }
        f.delete();
    }

    private static String emailFromBlessings(Blessings blessings) {
        for (List<VCertificate> chain : blessings.getCertificateChains()) {
            for (VCertificate certificate : Lists.reverse(chain)) {
                if (certificate.getExtension().contains("@")) {
                    for (String part : NamingUtil.split(certificate.getExtension())) {
                        if (part.contains("@")) {
                            return part;
                        }
                    }
                }
            }
        }
        return "";
    }

    private class SlideChangeAsyncTask extends AsyncTask<Void, Void, Void> {
        private Stream<WatchChange> changeStream;

        SlideChangeAsyncTask(Stream<WatchChange> stream) {
            changeStream = stream;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                for (WatchChange change : changeStream) {
                    if (!change.getTableName().equals(SYNCBASE_TABLE_NAME)) {
                        handleError("Wrong change table name: " + change.getTableName() + ", wanted: " +
                                SYNCBASE_TABLE_NAME);
                        continue;
                    }
                    if (!change.getRowName().equals(SYNCBASE_SLIDE_NUM_ROW_NAME)) {
                        handleError("Wrong change row name: " + change.getRowName() + ", wanted: " +
                                SYNCBASE_SLIDE_NUM_ROW_NAME);
                        continue;
                    }
                    if (!change.getChangeType().equals(ChangeType.PUT_CHANGE)) {
                        handleError("Expected only PUT_CHANGEs, got: " + change.getChangeType());
                        continue;
                    }
                    try {
                        int slideNum =
                                (Integer) VomUtil.decode(change.getVomValue(), Integer.class);
                        if (mSynced || !change.isFromSync()) {
                            setSlideNum(slideNum);
                        }
                    } catch (VException e) {
                        handleError("Couldn't decode slide number value: " + e.getMessage());
                        continue;
                    }


                }
            } catch (RuntimeException e) {
                handleError("Error reading from the change stream: slide numbers will no longer " +
                        "be kept in sync: " + e.getMessage());
            }
            return null;
        }
    }

    private static class Slide {
        int slideDrawableId;
        String slideNotes;

        public Slide() {
            slideDrawableId = 0;
            slideNotes = "";
        }

        public Slide(int _slideDrawableId) {
            slideDrawableId = _slideDrawableId;
        }

        public Slide(int _slideDrawableId, String _slideNotes) {
            slideDrawableId = _slideDrawableId;
            slideNotes = _slideNotes;
        }

        public int getSlideDrawableId() {
            return slideDrawableId;
        }

        public String getSlideNotes() {
            return slideNotes;
        }
    }
}
