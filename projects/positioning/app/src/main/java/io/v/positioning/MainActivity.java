// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

import io.v.positioning.ble.BleActivity;
import io.v.positioning.gae.ServletPostAsyncTask;
import io.v.positioning.tofprotocol.TofProtocolActivity;


public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String mLatitude;
    private String mLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buildGoogleApiClient();
    }

    public void onFindAndRecordDevices(View view) {
        startActivity(new Intent(this, BluetoothPositionActivity.class));
    }

    public void onUltrasound(View view) {
        startActivity(new Intent(this, UltrasoundActivity.class));
    }

    public void onBle(View view) {
        startActivity(new Intent(this, BleActivity.class));
    }

    public void onTofProtocol(View view) {
        startActivity(new Intent(this, TofProtocolActivity.class));
    }

    public void onRecordMyLocation(View view) {
        updateLocation();
        if (mLatitude != null && mLongitude != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("androidId", Settings.Secure.getString(this.getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID));
                data.put("latitude", mLatitude);
                data.put("longitude", mLongitude);
                data.put("deviceTime", System.currentTimeMillis());
                new ServletPostAsyncTask("gps", data).execute(this);
            } catch (JSONException | MalformedURLException e) {
                Log.e(TAG, "Failed to record the location." + e);
            }
        } else {
            Toast.makeText(this, "Latitude and longitude not set.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocation() {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitude = String.valueOf(mLastLocation.getLatitude());
            mLongitude = String.valueOf(mLastLocation.getLongitude());
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        updateLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection to Location services suspended.");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection to Location services failed.", Toast.LENGTH_SHORT).show();
    }

    public String getAndroidId() {
        return Settings.Secure.getString(this.getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
