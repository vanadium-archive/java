// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.namespace_browser;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.Duration;

import java.util.List;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.v23.InputChannelCallback;
import io.v.v23.InputChannels;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobError;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.security.Blessings;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String BLESSINGS_KEY = "BlessingsKey";

    private static final String PREF_NAMESPACE_GLOB_ROOT = "pref_namespace_glob_root";
    private static final String DEFAULT_NAMESPACE_GLOB_ROOT = "";
    private static final String SAVED_VIEW_STATE_KEY = "browser_viewstate";

    private VContext mBaseContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBaseContext = V.init(this);
        String root = PreferenceManager.getDefaultSharedPreferences(this).getString(
                PREF_NAMESPACE_GLOB_ROOT, DEFAULT_NAMESPACE_GLOB_ROOT);
        View dirView = findViewById(R.id.directory);
        dirView.setPadding(  // remove left padding for the root.
                0, dirView.getPaddingTop(), dirView.getPaddingRight(), dirView.getPaddingBottom());
        dirView.setTag(new GlobReply.Entry(
                new MountEntry(root, ImmutableList.<MountedServer>of(), true, false)));
        TextView nameView = (TextView) dirView.findViewById(R.id.name);
        nameView.setText("/");
        Drawable d = getResources().getDrawable(R.drawable.ic_account_box_black_36dp);
        d.setColorFilter(new LightingColorFilter(Color.BLACK, Color.GRAY));
        Futures.addCallback(BlessingsManager.getBlessings(mBaseContext, this, BLESSINGS_KEY, true),
                new FutureCallback<Blessings>() {
                    @Override
                    public void onSuccess(Blessings result) {
                        android.util.Log.i(TAG, "Success.");
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        android.util.Log.e(TAG, "Couldn't get blessings: " + t.getMessage());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBaseContext.cancel();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Parcel parcel = Parcel.obtain();
        ViewUtil.serializeView(findViewById(R.id.directory), parcel);
        savedInstanceState.putByteArray(SAVED_VIEW_STATE_KEY, parcel.marshall());
        parcel.recycle();
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        byte[] data = savedInstanceState.getByteArray(SAVED_VIEW_STATE_KEY);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(data, 0, data.length);
        parcel.setDataPosition(0);
        LinearLayout dirView =
                (LinearLayout) ViewUtil.deserializeView(parcel, getLayoutInflater());
        parcel.recycle();
        // Replace old directory view with the new one.
        LinearLayout oldDirView = (LinearLayout) findViewById(R.id.directory);
        ViewGroup parent = (ViewGroup) oldDirView.getParent();
        int index = parent.indexOfChild(oldDirView);
        parent.removeView(oldDirView);
        parent.addView(dirView, index);
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void onItemClick(View view) {
        switch (view.getId()) {
            case R.id.directory:
                handleDirectoryClick(view);
                break;
            case R.id.object:
                handleObjectClick(view);
                break;
            case R.id.method:
                handleMethodClick(view);
                break;
            default:
                android.util.Log.e(
                        TAG, String.format("Click on an illegal view with id: %d", view.getId()));
        }
    }

    private void handleDirectoryClick(View view) {
        LinearLayout dirView = (LinearLayout) view;
        if (!dirView.isActivated()) {
            // Add new views.
            fetchNames(dirView);
        } else {
            // Remove all but the first view.
            if (dirView.getChildCount() > 1) {
                dirView.removeViews(1, dirView.getChildCount() - 1);
            }
            ViewUtil.updateDirectoryView(dirView, false);
        }
    }

    private void handleObjectClick(View view) {
        LinearLayout objView = (LinearLayout) view;
        if (!objView.isActivated()) {
            // Add new views.
            fetchMethods(objView);
        } else {
            // Remove all but the first view.
            if (objView.getChildCount() > 1) {
                objView.removeViews(1, objView.getChildCount() - 1);
            }
            ViewUtil.updateObjectView(objView, false);
        }
    }

    private void handleMethodClick(View view) {
        LinearLayout methodView = (LinearLayout) view;
        // Add claiming logic here.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_account: {
                Futures.addCallback(
                        BlessingsManager.mintBlessings(mBaseContext, this, BLESSINGS_KEY, true),
                        new FutureCallback<Blessings>() {
                            @Override
                            public void onSuccess(Blessings result) {
                                Toast.makeText(
                                        MainActivity.this, "Success.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                Log.e(TAG, "Couldn't get blessings: " + t.getMessage());
                            }
                        });
                return true;
            }
            case R.id.action_settings: {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fetchNames(final LinearLayout dirView) {
        ViewUtil.updateDirectoryView(dirView, true);
        GlobReply entry = (GlobReply) dirView.getTag();
        if (!(entry instanceof GlobReply.Entry)) {
            return;
        }
        final MountEntry dirEntry = ((GlobReply.Entry) entry).getElem();
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Fetching Names...");
        progressDialog.show();
        String root = dirEntry.getName();
        io.v.v23.namespace.Namespace n = V.getNamespace(mBaseContext);
        VContext ctxT = mBaseContext.withTimeout(new Duration(20000));  // 20s
        Futures.addCallback(
                InputChannels.withCallback(n.glob(ctxT, root.isEmpty() ? "*" : root + "/*"),
                        new InputChannelCallback<GlobReply>() {
                    @Override
                    public ListenableFuture<Void> onNext(GlobReply reply) {
                        if (reply instanceof GlobReply.Error) {
                            GlobError error = ((GlobReply.Error) reply).getElem();
                            String msg = String.format(
                                    "Couldn't fetch namespace subtree \"%s\": %s",
                                    error.getName(),
                                    error.getError().getMessage());
                            android.util.Log.e(TAG, msg);
                            return null;
                        }
                        MountEntry entry = ((GlobReply.Entry) reply).getElem();
                        String text = "";
                        if (dirEntry.getName().isEmpty()) {
                            text = entry.getName();
                        } else if (entry.getName().startsWith(dirEntry.getName() + "/")) {
                            text = entry.getName().substring(dirEntry.getName().length() + 1);
                        } else {
                            Log.e(TAG, String.format(
                                    "Entry %s doesn't start with parent prefix %s",
                                    entry.getName(), dirEntry.getName() + "/"));
                            return null;
                        }
                        LinearLayout childView =
                                (entry.getServers() == null || entry.getServers().size() <= 0)
                                        ? ViewUtil.createDirectoryView(
                                                text, reply, getLayoutInflater())
                                        : ViewUtil.createObjectView(
                                                text, reply, getLayoutInflater());
                        dirView.addView(childView);
                        return null;
                    }
                }), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                progressDialog.dismiss();
            }
                    @Override
                    public void onFailure(Throwable t) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error fetching names: " + t.getMessage());
                    }
                });
    }

    private void fetchMethods(final LinearLayout objView) {
        ViewUtil.updateObjectView(objView, true);
        final GlobReply entry = (GlobReply) objView.getTag();
        if (!(entry instanceof GlobReply.Entry)) {
            return;
        }
        final MountEntry objEntry = ((GlobReply.Entry) entry).getElem();
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Fetching Methods...");
        progressDialog.show();
        Futures.addCallback(Methods.get(mBaseContext, objEntry.getName()),
                new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> methods) {
                for (String method : methods) {
                    LinearLayout childView =
                            ViewUtil.createMethodView(method, entry, getLayoutInflater());
                    objView.addView(childView);
                }
                progressDialog.dismiss();
            }
            @Override
            public void onFailure(Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Error fetching methods: " + t.getMessage());
            }
        });
    }
}
