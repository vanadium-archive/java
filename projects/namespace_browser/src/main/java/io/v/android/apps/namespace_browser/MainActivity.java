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
import android.os.AsyncTask;
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

import java.util.List;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String PREF_NAMESPACE_GLOB_ROOT = "pref_namespace_glob_root";
    private static final String DEFAULT_NAMESPACE_GLOB_ROOT = "";
    private static final String SAVED_VIEW_STATE_KEY = "browser_viewstate";

    private static final int BLESSING_REQUEST = 1;

    private VContext mBaseContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String root = PreferenceManager.getDefaultSharedPreferences(this).getString(
                PREF_NAMESPACE_GLOB_ROOT, DEFAULT_NAMESPACE_GLOB_ROOT);
        View dirView = findViewById(R.id.directory);
        dirView.setPadding(  // remove left padding for the root.
                0, dirView.getPaddingTop(), dirView.getPaddingRight(), dirView.getPaddingBottom());
        dirView.setTag(new GlobReply.Entry(
                new MountEntry(root, ImmutableList.<MountedServer>of(), true, false)));
        TextView nameView = (TextView) dirView.findViewById(R.id.name);
        nameView.setText("/");
        mBaseContext = V.init(this);
        Drawable d = getResources().getDrawable(R.drawable.ic_account_box_black_36dp);
        d.setColorFilter(new LightingColorFilter(Color.BLACK, Color.GRAY));
        getBlessings();
    }

    @Override
    protected void onStart() {
        super.onStart();
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
            new NameFetcher(dirView).execute();
            // dirView will be updated only when the NameFetcher completes.
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
            new MethodFetcher(objView).execute();
            // objView will be updated only when the NameFetcher completes.
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
                refreshBlessings();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESSING_REQUEST:
                try {
                    byte[] blessingVom = BlessingService.extractBlessingReply(resultCode, data);
                    Blessings blessings = (Blessings) VomUtil.decode(blessingVom, Blessings.class);
                    BlessingsManager.addBlessings(this, blessings);
                    Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
                    getBlessings();
                } catch (BlessingCreationException e) {
                    String msg = "Couldn't retrieve blessing from blessing service: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, msg);
                } catch (VException e) {
                    String msg = "Couldn't decode and store blessing: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, msg);
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
            // Update local state with the new blessings.
            VPrincipal p = V.getPrincipal(mBaseContext);
            p.blessingStore().setDefaultBlessings(blessings);
            p.addToRoots(blessings);
        } catch (VException e) {
            String msg = String.format(
                    "Couldn't set local blessing %s: %s", blessings, e.getMessage());
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            android.util.Log.e(TAG, msg);
        }
    }

    private class NameFetcher extends AsyncTask<Void, Void, List<GlobReply>> {
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        final LinearLayout dirView;
        String errorMsg = "";

        NameFetcher(LinearLayout dirView) {
            this.dirView = dirView;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Fetching Names...");
            progressDialog.show();
        }

        @Override
        protected List<GlobReply> doInBackground(Void... args) {
            GlobReply entry = (GlobReply) dirView.getTag();
            if (!(entry instanceof GlobReply.Entry)) {
                return ImmutableList.<GlobReply>of();
            }
            try {
                return Namespace.glob(((GlobReply.Entry) entry).getElem().getName(), mBaseContext);
            } catch (VException e) {
                errorMsg = "Error fetching names: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<GlobReply> replies) {
            progressDialog.dismiss();
            ViewUtil.updateDirectoryView(dirView, true);
            if (replies == null) {
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                android.util.Log.e(TAG, errorMsg);
                return;
            }
            GlobReply.Entry parentEntry = (GlobReply.Entry) dirView.getTag();
            for (GlobReply reply : replies) {
                if (reply instanceof GlobReply.Entry) {
                    MountEntry entry = ((GlobReply.Entry) reply).getElem();
                    String text = "";
                    if (parentEntry.getName().isEmpty()) {
                        text = entry.getName();
                    } else {
                        if (!entry.getName().startsWith(parentEntry.getElem().getName() + "/")) {
                            android.util.Log.e(TAG, String.format(
                                    "Entry %s doesn't start with parent prefix %s",
                                    entry.getName(), parentEntry.getElem().getName() + "/"));
                            // TODO(sjr): figure out the correct name format
                            // continue;
                        }
                        text = entry.getName();
                    }
                    LinearLayout childView =
                            (entry.getServers() == null || entry.getServers().size() <= 0)
                                    ? ViewUtil.createDirectoryView(text, reply, getLayoutInflater())
                                    // sub-dir
                                    : ViewUtil.createObjectView(text, reply,
                                    getLayoutInflater());   // object
                    dirView.addView(childView);
                } else if (reply instanceof GlobReply.Error) {
                    String msg = String.format("Couldn't fetch namespace subtree \"%s\": %s",
                            ((GlobReply.Error) reply).getElem().getName(),
                            ((GlobReply.Error) reply).getElem().getError().getMessage());
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, msg);
                }
            }
        }
    }

    private class MethodFetcher extends AsyncTask<Void, Void, List<String>> {
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        final LinearLayout objView;
        String errorMsg = "";

        MethodFetcher(LinearLayout objView) {
            this.objView = objView;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Fetching Methods...");
            progressDialog.show();
        }

        @Override
        protected List<String> doInBackground(Void... args) {
            GlobReply entry = (GlobReply) objView.getTag();
            if (!(entry instanceof GlobReply.Entry)) {
                return ImmutableList.<String>of();
            }

            try {
                return Methods.get(((GlobReply.Entry) entry).getElem().getName(), mBaseContext);
            } catch (VException e) {
                errorMsg = "Error fetching methods: " + e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<String> methods) {
            progressDialog.dismiss();
            ViewUtil.updateObjectView(objView, true);
            if (methods == null) {
                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                android.util.Log.e(TAG, errorMsg);
                return;
            }
            GlobReply reply = (GlobReply) objView.getTag();
            for (String method : methods) {
                LinearLayout childView =
                        ViewUtil.createMethodView(method, reply, getLayoutInflater());
                objView.addView(childView);
            }
        }
    }
}