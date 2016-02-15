// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ux;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.v.moments.R;
import io.v.moments.ifc.Moment;
import io.v.moments.ifc.MomentFactory;
import io.v.moments.lib.DiscoveredList;
import io.v.moments.lib.FileUtil;
import io.v.moments.lib.Id;
import io.v.moments.lib.ObservedList;
import io.v.moments.lib.PermissionManager;
import io.v.moments.model.AdConverterMoment;
import io.v.moments.model.AdvertiserFactory;
import io.v.moments.model.BitMapper;
import io.v.moments.model.Config;
import io.v.moments.model.MomentAdCampaign;
import io.v.moments.model.MomentFactoryImpl;
import io.v.moments.model.StateStore;
import io.v.moments.model.Toaster;
import io.v.moments.v23.ifc.Advertiser;
import io.v.moments.v23.ifc.Scanner;
import io.v.moments.v23.ifc.V23Manager;
import io.v.moments.v23.impl.V23ManagerImpl;
import io.v.v23.security.Blessings;

/**
 * This app allows the user to take photos and advertise them on the network.
 * Other instances of the app will scan for and display such photos.
 *
 * This app is an example of running, advertising and scanning for multiple
 * services.
 *
 * A photo and its ancillary data (id, author, caption, date, ordinal number,
 * etc.) are called a Moment.  There are _local_ moments, created locally, and
 * _remote_ moments, found via discovery.  Local moments can be advertised so
 * that remote devices can discover and request them.  Remote moments are not
 * re-advertised.
 *
 * Local moments are persistent, in that the user must delete them to get rid of
 * them.  Remote moments are pulled in over the network, and not officially
 * retained between runs, though remote photo data may be left in local storage
 * as a simple cache. The moment's id is used to invalidate the cache.
 *
 * Every local moment is served by its own service.  This egregious use of
 * services allows for easy creation of multiple scan targets to exercise
 * discovery code. The involvement of a photo encourages the use of an RPC since
 * adding a (very large) photo to an advertisement as an attribute noticeably
 * increases the time taken between clicking 'advertise' on one device and
 * seeing any evidence of the advertisement on another device.
 *
 * TODO: when reloading from prefs, don't change advertise or scan state. only
 * do that when reloading from bundle. TODO: ScannerImpl should handle the
 * Update parsing currently done by DiscoveredList. TODO: unit tests. TODO: Add
 * version number to prefs, ignore and overwrite state if old version (to avoid
 * need to manually wipe data to avoid crashes).
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // Android Marshmallow permissions list.
    private static final String[] PERMS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
    };
    // A string that unambiguously identifies the phone in human readable form.
    private static final String DEVICE_SIGNATURE = Build.MODEL + " " + Build.SERIAL;
    // For Marshmallow permissions.
    private final PermissionManager mPermissionManager
            = new PermissionManager(this, RequestCode.PERMISSIONS, PERMS);
    // Use a serial executor to assure serial execution, e.g. toggling a switch
    // on and off without a race condition.
    private final ExecutorService mSerialExecutor = Executors.newSingleThreadExecutor();
    // Use a pool when order isn't important.
    private final ExecutorService mPoolExecutor = Executors.newCachedThreadPool();
    // For changes to UX.
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    // For discovery, serving and behaving as a client.
    private final V23Manager mV23Manager = V23ManagerImpl.Singleton.get();

    // See wireUxToDataModel for discussion of the following.
    private StateStore mStateStore;
    private AdvertiserFactory mAdvertiserFactory;
    private Scanner mScanner;
    private ScanSwitchHolder mScanSwitchHolder;
    private MomentFactory mMomentFactory;
    private BitMapper mBitMapper;
    private ObservedList<Moment> mLocalMoments;
    private DiscoveredList<Moment> mRemoteMoments;
    private Id mCurrentPhotoId;
    private boolean mShouldBeScanning = false;

    /**
     * The number used in a moment's file name is called the moments 'ordinal'
     * number.  At the time of writing local moments are never individually
     * deleted - either they are all kept or all deleted when app data is
     * deleted. Therefore, the _next_ local ordinal is just an array size
     * increment.
     *
     * Remote (discovered) moments, however, come and go individually, so one
     * cannot use, say, the size of the current list of remote moments to
     * determine the next ordinal number, as one might use a number already in
     * use, and display the wrong photo.
     *
     * When the phone is rotated, discovery scanning stops, then restarts.  So
     * known moments are immediately 're-'discovered.  To avoid reacquiring
     * data, retain and use a cache of known remote moments.  When a discovery
     * is made, check the map, and if there's a hit, there's no need to contact
     * the remote service.  The cache can also be used to compute the 'next'
     * ordinal number for remote services.
     */
    private Map<Id, Moment> mRemoteMomentCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logState("onCreate");

        setContentView(R.layout.activity_main);

        // This will look in prefs for a Vanadium blessing, and if not
        // found will leave this activity (onStop likely to be called) and
        // return via a start intent (not via onActivityResult).
        mV23Manager.init(this, onBlessings());

        wireUxToDataModel();
        initializeOrRestore(savedInstanceState);
    }

    private FutureCallback<Blessings> onBlessings() {
        return new FutureCallback<Blessings>() {
            @Override
            public void onSuccess(Blessings b) {
                Log.d(TAG, "Got blessings!");
                if (!mPermissionManager.haveAllPermissions()) {
                    Log.d(TAG, "Obtaining permissions");
                    mPermissionManager.obtainPermission();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d(TAG, "Failure to get blessings, nothing will work.", t);
            }
        };
    }

    /**
     * This method builds the app's object graph, and intentionally has no
     * branches that depend on state loaded from the app's prefs or instance
     * bundle. State is loaded after the wiring is complete, to make phone
     * rotation easy.
     */
    private void wireUxToDataModel() {
        // Stores remote moments by Id to avoid having to wait for re-discovery.
        mRemoteMomentCache = new HashMap<>();

        // Compresses byte data, converts byte[] to bitmap, manages file storage.
        mBitMapper = Config.makeBitmapper(this);

        // Makes moments.  Each moment needs a bitmapper to read its BitMaps.
        mMomentFactory = new MomentFactoryImpl(mBitMapper);

        // Makes advertisers.  Needs v23Manager to do advertising.
        mAdvertiserFactory = new AdvertiserFactory(mV23Manager, mMomentFactory);

        // Local moments, with photos taken by the local device.
        mLocalMoments = new ObservedList<>();

        // Converts advertisements to 'remote' moments.  Needs v23Manager to
        // make RPCs, needs mMomentFactory to make moments, needs a thread pool
        // for making RPCs to get photos, needs mHandler to post photos on the
        // UX thread, fills the cache with remote moments.
        AdConverterMoment converter = new AdConverterMoment(
                mV23Manager, mMomentFactory, mPoolExecutor,
                mHandler, mRemoteMomentCache);

        // The list of remote (discovered) moments.  Pass mAdvertiserFactory
        // as a container of Id's to reject from discovery (because they
        // represent local moments).
        mRemoteMoments = new DiscoveredList<>(converter, mAdvertiserFactory, mHandler);

        Toaster toaster = new Toaster(this);

        mScanner = mV23Manager.makeScanner(MomentAdCampaign.QUERY);
        mScanSwitchHolder = new ScanSwitchHolder(
                toaster, mScanner, mRemoteMoments);

        // Stores app state to bundles, preferences, etc.  The mMomentFactory
        // needed to recreate moments.
        mStateStore = new StateStore(
                getSharedPreferences(
                        nameOfSharedPrefs(), Context.MODE_PRIVATE),
                mMomentFactory);

        // Tell the converter where to place remote moments.
        converter.setList(mRemoteMoments);

        // The adapter allows remote and local moment lists to 'stack' in a
        // RecyclerView.  mAdvertiserFactory is used to generate advertisers
        // for local moments when a user wants to advertise them.  The
        // serialExecutor is used to start/stop advertisements in the UX.
        MomentAdapter adapter = new MomentAdapter(
                mRemoteMoments, mLocalMoments, toaster, mAdvertiserFactory);

        // Lets the adapter speed up a bit.
        adapter.setHasStableIds(true);

        // Hook the adapter to a view, and begin observing changes to the
        // moment lists.
        RecyclerView view = configureRecyclerView();
        view.setAdapter(adapter);
        adapter.beginObserving();

        // Expose the scan switch on the action bar.
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Expose the camera button.
        setFabClickHandler(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
    }

    private void setFabClickHandler(View.OnClickListener listener) {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(listener);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        logState("onRestart");
    }

    @Override
    public void onPause() {
        super.onPause();
        logState("onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        logState("onStop");
    }

    /**
     * Before calling this, must have permission to read/write to storage, to
     * read/write photo data. Don't need camera permission, since a new activity
     * is started to actually take the photo.
     */
    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mCurrentPhotoId = Id.makeRandom();
        Integer ordinal = mLocalMoments.size() + 1;
        Uri uri = mBitMapper.getCameraPhotoUri(ordinal);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, RequestCode.CAPTURE_IMAGE);
    }

    /**
     * On new permissions, assume it's a fresh install and wipe the working
     * directory.   File formats and naming might have changed.  This is not
     * good for permanent photo management obviously.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] results) {
        logState("onRequestPermissionsResult");
        if (mPermissionManager.granted(requestCode, permissions, results)) {
            FileUtil.initializeDirectory(
                    Config.getWorkingDirectory(this));
            return;
        }
        toast(getString(R.string.need_permissions));
    }

    /**
     * A more realistic example would obtain the phone owner's name or email
     * address from, say, the contacts list.  Instead using a device
     * identifier.
     */
    public String getAuthorName() {
        return DEVICE_SIGNATURE;
    }

    /**
     * Could prompt the user for this; using a label derived from the id
     * instead.
     */
    public String getCaption(int index) {
        return "Generated caption " + index;
    }

    @Override
    protected void onStart() {
        super.onStart();
        logState("onStart");
        if (mLocalMoments.isEmpty()) {
            Log.d(TAG, "Loading moments from prefs.");
            mStateStore.prefsLoad(mLocalMoments);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        logState("onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logState("onDestroy");
        if (mScanner.isScanning()) {
            mScanner.stop();
        }
        stopAllAdvertising();
        mV23Manager.shutdown();
        Log.d(TAG, "Destruction complete.");
        Log.d(TAG, " ");
        Log.d(TAG, " ");
    }

    private void stopAllAdvertising() {
        int count = 0;
        for (Advertiser advertiser : mAdvertiserFactory.allAdvertisers()) {
            if (advertiser.isAdvertising()) {
                try {
                    advertiser.stop();
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Stopped advertising " + advertiser.toString());
            } else {
                Log.d(TAG, "A moment was not advertising");
            }
        }
        Log.d(TAG, "Stopped " + count + " advertisements.");
    }

    private RecyclerView configureRecyclerView() {
        RecyclerView view = (RecyclerView) findViewById(R.id.all_moments);
        view.setLayoutManager(makeLayoutManager());
        // Add some lines between items
        view.addItemDecoration(new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        // This makes it faster, but at a cost.
        view.setHasFixedSize(true);
        return view;
    }

    private LinearLayoutManager makeLayoutManager() {
        LinearLayoutManager mgr = new LinearLayoutManager(this);
        mgr.setOrientation(LinearLayoutManager.VERTICAL);
        mgr.scrollToPosition(0);
        return mgr;
    }


    private void toast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        logState("onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_scan);
        SwitchCompat sw = (SwitchCompat) MenuItemCompat.getActionView(item);
        mScanSwitchHolder.setSwitch(sw);
        if (mShouldBeScanning && !sw.isChecked()) {
            sw.setChecked(true);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        logState("onActivityResult");
        if (requestCode == RequestCode.CAPTURE_IMAGE) {
            if (resultCode == RESULT_OK) {
                processCapturedPhoto();
            }
        }
    }

    private void processCapturedPhoto() {
        int ordinal = mLocalMoments.size() + 1;
        final Moment moment = mMomentFactory.make(
                mCurrentPhotoId, ordinal,
                getAuthorName(), getCaption(ordinal));
        Log.d(TAG, "pcp:     moment = " + moment);
        Log.d(TAG, "pcp:  list size = " + mLocalMoments.size());
        mLocalMoments.push(moment);
        mSerialExecutor.submit(new Runnable() {
            @Override
            public void run() {
                mBitMapper.dealWithCameraResult(
                        mLocalMoments, moment);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        logState("onSaveInstanceState");
        b.putBoolean(B.SHOULD_BE_SCANNING, mScanner.isScanning());
        mStateStore.bundleSave(b, mRemoteMomentCache.values());
        mStateStore.prefsSave(mLocalMoments);
    }

    private void initializeOrRestore(Bundle b) {
        mStateStore.prefsLoad(mLocalMoments);
        if (b == null) {
            Log.d(TAG, "No bundle passed, starting fresh.");
            mRemoteMomentCache.clear();
            return;
        }
        Log.d(TAG, "Reloading from bundle.");
        mShouldBeScanning = b.getBoolean(B.SHOULD_BE_SCANNING, false);
        mStateStore.bundleLoad(b, mRemoteMomentCache);
    }

    private String nameOfSharedPrefs() {
        return getClass().getPackage() +
                "." + getString(R.string.photo_file_prefix);
    }

    private void logState(String state) {
        Log.d(TAG, state + " --------------------------------------------");
    }

    private static class RequestCode {
        static final int PERMISSIONS = 1001;
        static final int CAPTURE_IMAGE = 1003;
    }

    private static class B {
        static final String SHOULD_BE_SCANNING = "SHOULD_BE_SCANNING";
    }

}
