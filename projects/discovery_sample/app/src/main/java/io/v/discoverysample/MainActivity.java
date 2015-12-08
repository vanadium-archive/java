// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.discoverysample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.UiThreadExecutor;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.InputChannels;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.verror.CanceledException;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class MainActivity extends Activity {
    private static final Service SERVICE_TO_ADVERTISE = ServiceAdvert.make();
    private static final List<BlessingPattern> NO_PATTERNS = new ArrayList<>();
    private static final int BLESSINGS_REQUEST = 1;
    private Button chooseBlessingsButton;
    private Button scanButton;
    private Button advertiseButton;
    private Blessings blessings;
    private VDiscovery discovery;
    private VContext rootCtx;
    private CancelableVContext scanCtx;
    private CancelableVContext advCtx;
    private ScanHandlerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootCtx = V.init(this);
        discovery = V.getDiscovery(rootCtx);

        setContentView(R.layout.activity_main);
        chooseBlessingsButton = (Button) findViewById(R.id.blessingsButton);
        chooseBlessingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchBlessings();
            }
        });

        scanButton = (Button) findViewById(R.id.scanForService);
        scanButton.setEnabled(false);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipScan();
            }
        });

        advertiseButton = (Button) findViewById(R.id.advertiseButton);
        advertiseButton.setEnabled(false);
        advertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipAdvertise();
            }
        });

        adapter = new ScanHandlerAdapter(this);
        ListView devices = (ListView) findViewById(R.id.list_view);
        devices.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void flipAdvertise() {
        advertiseButton.setEnabled(false);
        if (advCtx == null) {  // not advertising
            advCtx = rootCtx.withCancel();
            Futures.addCallback(discovery.advertise(advCtx, SERVICE_TO_ADVERTISE, NO_PATTERNS),
                    new FutureCallback<ListenableFuture<Void>>() {
                        @Override
                        public void onSuccess(ListenableFuture<Void> result) {
                            // Advertising started.
                            advertiseButton.setText("Stop advertising");
                            advertiseButton.setEnabled(true);
                            Futures.transform(result, new Function<Void, Void>() {
                                @Override
                                public Void apply(Void input) {
                                    // Advertising done.
                                    advertiseButton.setText("Advertise");
                                    advertiseButton.setEnabled(true);
                                    return null;
                                }
                            }, UiThreadExecutor.INSTANCE);
                        }
                        @Override
                        public void onFailure(final Throwable t) {
                            if (!(t instanceof CanceledException)) {
                                // advertising wasn't stopped via advCtx.cancel()
                                String msg = "Couldn't start advertising: " + t.getMessage();
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                            advertiseButton.setText("Advertise");
                            advertiseButton.setEnabled(true);
                        }
                    }, UiThreadExecutor.INSTANCE);
        } else {  // advertising
            advCtx.cancel();
            advCtx = null;
        }
    }

    private void flipScan() {
        if (scanCtx == null) {  // not scanning
            scanButton.setText("Stop scan");
            scanCtx = rootCtx.withCancel();
            Futures.addCallback(InputChannels.withCallback(
                            discovery.scan(scanCtx, ServiceAdvert.QUERY), adapter,
                            UiThreadExecutor.INSTANCE),
                    new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scanButton.setText("Start scan");
                                    scanButton.setEnabled(true);
                                }
                            });
                        }
                        @Override
                        public void onFailure(Throwable t) {
                            if (!(t instanceof CanceledException)) {
                                // scanning wasn't stopped via scanCtx.cancel()
                                String msg = "Scan failed with error: " + t.getMessage();
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                            scanButton.setText("Start scan");
                            scanButton.setEnabled(true);
                        }
                    }, UiThreadExecutor.INSTANCE);
        } else {  // scanning
            scanButton.setEnabled(false);
            scanCtx.cancel();
            scanCtx = null;
        }
    }

    private void fetchBlessings() {
        Intent intent = BlessingService.newBlessingIntent(this);
        startActivityForResult(intent, BLESSINGS_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESSINGS_REQUEST:
                try {
                    // The Account Manager will pass us the blessings to use as
                    // an array of bytes. Use VomUtil to decode them...
                    byte[] blessingsVom =
                            BlessingService.extractBlessingReply(resultCode, data);
                    blessings = (Blessings) VomUtil.decode(blessingsVom, Blessings.class);
                    BlessingsManager.addBlessings(this, blessings);
                    Toast.makeText(this, "Success, ready to listen!",
                            Toast.LENGTH_SHORT).show();
                    scanButton.setEnabled(true);
                    advertiseButton.setEnabled(true);
                } catch (BlessingCreationException e) {
                    String msg = "Couldn't create blessing: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                } catch (VException e) {
                    String msg = "Couldn't store blessing: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    private static class ServiceAdvert {
        // Required type/interface name, probably a URL into a web-based
        // ontology.  Necessary for querying.
        private static final String INTERFACE_NAME = "v.io/x/ref.Thermostat";

        // Required service address(es).
        private static final String[] ADDRESSES = {"localhost:2001", "localhost:2002"};

        // Optional universally unique identifier of the service.
        // If omitted, the discovery service will invent a value.
        private static final String INSTANCE_UUID = "012";

        // Optional service name intended for humans.
        private static final String INSTANCE_NAME = "livingRoomThermostat";

        // Optional arbitrary key value pairs to supplement the service description.
        private static Attributes makeAttributes() {
            Attributes attrs = new Attributes();
            attrs.put("maxTemp", "451");
            return attrs;
        }

        // To limit scans to see only this service.
        static final String QUERY = "v.InterfaceName=\"" + INTERFACE_NAME + "\"";

        // Make a service description (not an actual service).
        static Service make() {
            return new Service(
                    INSTANCE_UUID, INSTANCE_NAME, INTERFACE_NAME,
                    makeAttributes(), Arrays.asList(ADDRESSES));
        }
    }
}
