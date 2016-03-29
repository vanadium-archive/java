// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.pingpongapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.InputChannels;
import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.VIterable;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.services.syncbase.nosql.TableRow;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Syncgroup;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import static io.v.v23.VFutures.sync;

public class MainActivity extends AppCompatActivity {
    // Syncgroup-related names for where the mounttable is and the syncgroup name's suffix.
    public static final String MT_PREFIX = "/ns.dev.v.io:8101/tmp/benchmark/pingpong/";
    public static final String SG_SUFFIX = "/s0/%%sync/sg";

    // Syncbase-related names for the app/db/tb hierarchy and where to write data.
    public static final String APP_NAME = "app";
    public static final String DB_NAME = "db";
    public static final String TB_NAME = "table";
    public static final String SYNC_PREFIX = "prefix";

    // Allow each asynchronous operation to take at most this amount of time.
    private static final long TEST_TIMEOUT = 5;
    private static final TimeUnit TEST_TIMEUNIT = TimeUnit.SECONDS;

    // Fixed parameters for the app.
    private static final String TAG = "PingPong"; // Debug tag for Log.d
    private static final String TEST_ID = "AndroidPingPongTest";
    private static final int NUM_TIMES = 100;
    // Log success/failure once blessings are received.
    private static final FutureCallback<Blessings> ON_BLESSINGS = new FutureCallback<Blessings>() {
        @Override
        public void onSuccess(Blessings b) {
            Log.d(TAG, "Got blessings!");
        }

        @Override
        public void onFailure(Throwable t) {
            Log.d(TAG, "Failure to get blessings, nothing will work.", t);
        }
    };

    // The V context tracked by the app.
    VContext mBaseContext;
    // Values that can be changed by the app.
    private int mPeerID = 0;
    private Thread mTestThread;
    private boolean mUsingProxy = false;

    public static Permissions openPermissions() {
        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        return new Permissions(ImmutableMap.of(
                Constants.RESOLVE.getValue(), acl,
                Constants.DEBUG.getValue(), acl,
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));
    }

    private String getMountPoint(String testID, int peerID) {
        return MT_PREFIX + testID + "/s" + peerID;
    }

    private String getSyncgroupName(String testID) {
        return MT_PREFIX + testID + SG_SUFFIX;
    }

    // Helper to show default text before the test starts.
    private void setPreTestText() {
        String suffix = mUsingProxy ? " with proxy" : " w/o proxy";
        String prefix = mPeerID == 0 ? "Pinger: Hello World!" : "Ponger: Hello World!";
        updateText(prefix + suffix);
    }

    // Helper to change the text shown by the debug TextView.
    private void updateText(String data) {
        TextView tv = (TextView) findViewById(R.id.debug_text);
        tv.setText(data);
    }

    // Callback (from button press). Changes the text color between pinger and ponger.
    public void pingpongToggle(View view) {
        if (mTestThread == null) {
            Log.d(TAG, "Toggling peer ID!");
            mPeerID = 1 - mPeerID;
            setPreTestText();
            TextView tv = (TextView) findViewById(R.id.debug_text);
            tv.setTextColor(mPeerID == 0 ? Color.BLACK : Color.BLUE);
        }
    }

    // Callback (from button press). If not pressed yet, the proxy will be added to the listen spec.
    public void useProxy(View view) {
        // If we don't proxy, we can't communicate across networks.
        if (!mUsingProxy) {
            Log.d(TAG, "Using the proxy!");
            try {
                mBaseContext = V.withListenSpec(mBaseContext,
                        V.getListenSpec(mBaseContext).withProxy("proxy"));
                mUsingProxy = true;
                setPreTestText();
                ((Button) findViewById(R.id.useproxybutton)).setText(R.string.using_proxy);

            } catch (final VException e) {
                Log.d(TAG, e.toString());
            }
        }
    }

    // Prepares an asynchronous fetch of blessings using the user account.
    // On some Android versions, this will open an account picker.
    private void setUpBlessings() {
        Log.d(TAG, "Attempting to get blessings.");

        Futures.addCallback(BlessingsManager.getBlessings(mBaseContext, this, "BlessingsKey", true),
                ON_BLESSINGS);
    }

    // Folder where Syncbase will store its data.
    private String getDataDir() {
        return getApplicationContext().getApplicationInfo().dataDir + "/" + TEST_ID;
    }

    /**
     * Callback (from button press). If the test hasn't run yet, starts the test.
     * The protocol is:
     * 1. Create app hierarchy.
     * 2. Create syncgroup if pinger, else as ponger, join the syncgroup.
     * 3. If pinger, write some data to the store.
     * 4. Watch from the beginning of time. This watch loop will have the pinger write on odd watch
     * changes. and the ponger write when it sees even watch changes.
     * 5. After enough watches have been seen, the watch loop ends, and the statistics are printed.
     * The start time and end time are set during the watch loop.
     * We allow 1 round of free sync in order to initialize before the start time is recorded.
     *
     * @param view
     */
    public void startTestPingPong(final View view) {
        if (mTestThread == null) {
            ((Button) findViewById(R.id.pingpongbutton)).setText(R.string.running_ping_pong);

            Log.d(TAG, "Starting Syncbase Ping Pong Test!");
            mTestThread = new Thread() {
                long startTime;
                long endTime;

                // Count starts at -2. (The first ping and first pong are not counted in the test.)
                int count = -2;

                @Override
                public void run() {
                    try {
                        testPingPong();
                    } catch (ExecutionException | InterruptedException | TimeoutException |
                            VException e) {
                        Log.d(TAG, e.toString());
                        mBaseContext.cancel();
                    }
                }

                public void testPingPong() throws InterruptedException, ExecutionException,
                        TimeoutException, VException {

                    Log.d(TAG, "Preparing Syncbase Server");
                    helpUpdateText("Preparing Syncbase Server");

                    // Create the syncbase server...
                    try {
                        mBaseContext = SyncbaseServer.withNewServer(mBaseContext,
                                new SyncbaseServer.Params()
                                        .withPermissions(openPermissions())
                                        .withStorageRootDir(getDataDir())
                                        .withName(getMountPoint(TEST_ID, mPeerID)));
                    } catch (SyncbaseServer.StartException e) {
                        Log.d(TAG, e.toString());
                    }

                    Server syncbaseServer = io.v.v23.V.getServer(mBaseContext);


                    Log.d(TAG, "Preparing Syncbase Client");
                    helpUpdateText("Preparing Syncbase Client");

                    Log.d(TAG, Arrays.toString(syncbaseServer.getStatus().getEndpoints()));

                    // And the syncbase client, app, db, and table...
                    SyncbaseService service = Syncbase.newService(
                            "/" + syncbaseServer.getStatus().getEndpoints()[0]);

                    SyncbaseApp app = service.getApp(APP_NAME);
                    final Database db = app.getNoSqlDatabase(DB_NAME, null);
                    final Table tb = db.getTable(TB_NAME);
                    Log.d(TAG, "app exists?");
                    if (!syncWithTimeout(app.exists(mBaseContext))) {
                        Log.d(TAG, "app create");
                        syncWithTimeout(app.create(mBaseContext, openPermissions()));
                    }
                    Log.d(TAG, "db exists?");
                    if (!syncWithTimeout(db.exists(mBaseContext))) {
                        Log.d(TAG, "db create");
                        syncWithTimeout(db.create(mBaseContext, openPermissions()));
                    }
                    Log.d(TAG, "tb exists?");
                    if (!syncWithTimeout(tb.exists(mBaseContext))) {
                        Log.d(TAG, "tb create");
                        syncWithTimeout(tb.create(mBaseContext, openPermissions()));
                    }

                    Log.d(TAG, "I am peer " + mPeerID);
                    helpUpdateText("I am peer " + mPeerID);

                    // If you're peer 0, you should create the syncgroup. Otherwise, join it.
                    String sgName = getSyncgroupName(TEST_ID);
                    String mtPointName = MT_PREFIX + "/" + TEST_ID;
                    Syncgroup group = db.getSyncgroup(sgName);

                    SyncgroupMemberInfo memberInfo = new SyncgroupMemberInfo();
                    memberInfo.setSyncPriority((byte) 3);
                    if (mPeerID == 0) {
                        Log.d(TAG, "Creating Syncgroup" + sgName);
                        helpUpdateText("Creating Syncgroup");

                        SyncgroupSpec spec = new SyncgroupSpec(
                                TEST_ID, openPermissions(),
                                ImmutableList.of(new TableRow(TB_NAME, SYNC_PREFIX)),
                                ImmutableList.of(mtPointName), false);
                        syncWithTimeout(group.create(mBaseContext, spec, memberInfo));
                    } else {
                        Log.d(TAG, "Joining Syncgroup" + sgName);
                        helpUpdateText("Joining Syncgroup");
                        syncWithTimeout(group.join(mBaseContext, memberInfo));
                    }

                    // Now that we've all joined the syncgroup...
                    // We should synchronize our times.
                    Log.d(TAG, "We are now ready to time sync!");
                    helpUpdateText("We are now ready to time sync!");

                    // 0 will send to 1, and 1 responds via watch.
                    if (mPeerID == 0) {
                        writeData(tb);
                    }

                    watchForChanges(db, tb);

                    Log.d(TAG, "We finished!");
                    helpUpdateText("We finished!");
                    double delta = (endTime - startTime) / (NUM_TIMES * 1000000.);
                    Log.d(TAG, "Average Time: " + delta + " ms per ping pong iteration\n");
                    helpUpdateText("Average Time: " + delta + "ms per ping pong iteration!");
                }

                // Posts an action through the view to be run on the main thread.
                // This is the only way we're allowed to change the UI from this Thread.
                private void helpUpdateText(final String t) {
                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            updateText(t);
                        }
                    });
                }

                private void watchForChanges(Database db, Table tb) {
                    try {
                        VIterable<WatchChange> watchStream = InputChannels.asIterable(
                                db.watch(mBaseContext, TB_NAME, SYNC_PREFIX)
                        );

                        Log.d(TAG, "Starting watch");
                        // 0 will send to 1, and vice versa.
                        for (final WatchChange wc : watchStream) {
                            String key = wc.getRowName();
                            String value = VomUtil.bytesToHexString(wc.getVomValue());
                            Log.d(TAG, "Watch: " + key + " " + value);
                            helpUpdateText("Watch: " + key + " " + value);
                            count++;

                            if (count == 0) {
                                startTime = System.nanoTime();
                            }
                            if (count == NUM_TIMES * 2) {
                                endTime = System.nanoTime();
                                break;
                            }
                            if ((count + 2) % 2 == mPeerID) {
                                writeData(tb);
                            }
                        }
                        Log.d(TAG, "Exiting watch");
                    } catch (TimeoutException | VException ve) {
                        Log.e(TAG, ve.toString());
                    }
                }

                private void writeData(Table tb) throws VException, TimeoutException {
                    String actualKey = SYNC_PREFIX + "/" + count;
                    Log.d(TAG, "I write " + count);
                    syncWithTimeout(tb.put(mBaseContext, actualKey, count, Integer.class));
                }
            };
            mTestThread.start();
        }
    }

    private <T> T syncWithTimeout(Future<T> f) throws TimeoutException, VException {
        return sync(f, TEST_TIMEOUT, TEST_TIMEUNIT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        // Initialize Vanadium as soon as we can.
        mBaseContext = V.init(getApplicationContext(), new Options()
                .set(OptionDefs.LOG_VLEVEL, 0)
                .set(OptionDefs.LOG_VMODULE, "vsync*=2"));

        // We also need (trusted) blessings in order to be able to communicate with others.
        setUpBlessings();
    }
}
