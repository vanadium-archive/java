// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.discoverysample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.impl.google.lib.discovery.UUIDUtil;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Attributes;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;
import io.v.x.ref.lib.discovery.Advertisement;
import io.v.x.ref.lib.discovery.EncryptionAlgorithm;


public class MainActivity extends Activity {

    private static final int BLESSINGS_REQUEST = 1;
    private Button chooseBlessingsButton;
    private Button scanButton;
    private Button advertiseButton;
    private Blessings blessings;
    private VDiscovery discovery;

    private VContext rootCtx;
    private CancelableVContext scanCtx;
    private CancelableVContext advCtx;

    private boolean isScanning;
    private boolean isAdvertising;

    private ScanHandlerAdapter adapter;

    private Advertisement advertisement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootCtx = V.init(this);
        isScanning = false;
        isAdvertising = false;

        setContentView(R.layout.activity_main);
        chooseBlessingsButton = (Button)findViewById(R.id.blessingsButton);
        chooseBlessingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchBlessings(false);
            }
        });

        scanButton = (Button)findViewById(R.id.scanForService);
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

        byte[] instanceId = {0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15};
        Attributes attrs = new Attributes();
        attrs.put("foo", "bar");
        List<String> addrs = new ArrayList<>();
        addrs.add("localhost:2000");
        String interfaceName = "v.io/x/ref.Interface";
        advertisement = new Advertisement(
                new Service(instanceId, "Android instance",
                        interfaceName, attrs, addrs),
                UUIDUtil.UUIDToUuid(UUIDUtil.UUIDForInterfaceName(interfaceName)),
                new EncryptionAlgorithm(0),
                null,
                false);
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
        if (discovery == null) {
            discovery = V.getDiscovery(rootCtx);
        }

        if (!isAdvertising) {
            isAdvertising = true;

            advCtx = rootCtx.withCancel();
            final Button advButton = advertiseButton;
            discovery.advertise(advCtx, advertisement.getService(), new ArrayList<BlessingPattern>(),
                    new VDiscovery.AdvertiseDoneCallback() {
                        @Override
                        public void done() {
                            isAdvertising = false;
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    advButton.setText("Advertise");
                                }
                            });
                        }
                    });
            advertiseButton.setText("Stop Advertisement");
        } else {
            advCtx.cancel();
        }
    }
    private void flipScan() {
        if (discovery == null) {
            discovery = V.getDiscovery(rootCtx);
        }

        if (!isScanning) {
            scanCtx = rootCtx.withCancel();
            discovery.scan(scanCtx, "v.InterfaceName=\"v.io/x/ref.Interface\"", adapter);
            isScanning = true;
            scanButton.setText("Stop scanning");
        } else {
            isScanning = false;
            scanCtx.cancel();
            scanButton.setText("Start Scan");
        }
    }

    private void fetchBlessings(boolean startScan) {
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

                    // Enable the "start service" button.
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
}
