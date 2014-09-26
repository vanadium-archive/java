package com.veyron.projects.namespace;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.naming.MountEntry;

import java.util.List;

public class MainActivity extends Activity {
  private static final String TAG = "com.veyron.projects.namespace";
  private static final String VEYRON_ACCOUNT_TYPE = "com.veyron";
  private static final String PREF_NAMESPACE_GLOB_ROOT = "pref_namespace_glob_root";
  private static final String DEFAULT_NAMESPACE_GLOB_ROOT = "/proxy.envyor.com:8101";
  private static final String SAVED_VIEW_STATE_KEY = "browser_viewstate";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    RuntimeFactory.init(this, new Options());  // Initializes Veyron Runtime.
    final String root = PreferenceManager.getDefaultSharedPreferences(this).getString(
        PREF_NAMESPACE_GLOB_ROOT, DEFAULT_NAMESPACE_GLOB_ROOT);
    final View dirView = findViewById(R.id.directory);
    dirView.setPadding(  // remove left padding for the root.
        0, dirView.getPaddingTop(), dirView.getPaddingRight(), dirView.getPaddingBottom());
    dirView.setTag(new MountEntry(root, null, null));
    final TextView nameView = (TextView) dirView.findViewById(R.id.name);
    nameView.setText(root);
  }

  @Override
  protected void onStart() {
    super.onStart();
    setupAccountActionView();
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    final Parcel parcel = Parcel.obtain();
    ViewUtil.serializeView(findViewById(R.id.directory), parcel);
    savedInstanceState.putByteArray(SAVED_VIEW_STATE_KEY, parcel.marshall());
    parcel.recycle();
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    final byte[] data = savedInstanceState.getByteArray(SAVED_VIEW_STATE_KEY);
    final Parcel parcel = Parcel.obtain();
    parcel.unmarshall(data, 0, data.length);
    parcel.setDataPosition(0);
    final LinearLayout dirView =
        (LinearLayout) ViewUtil.deserializeView(parcel, getLayoutInflater());
    parcel.recycle();
    // Replace old directory view with the new one.
    final LinearLayout oldDirView = (LinearLayout) findViewById(R.id.directory);
    final ViewGroup parent = (ViewGroup) oldDirView.getParent();
    final int index = parent.indexOfChild(oldDirView);
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
    final LinearLayout dirView = (LinearLayout)view;
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
    final LinearLayout objView = (LinearLayout)view;
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
    final LinearLayout methodView = (LinearLayout)view;
    // Add claiming logic here.
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void setupAccountActionView() {
    final LinearLayout accountView =
        (LinearLayout) getLayoutInflater().inflate(R.layout.action_account, null);
    final AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
    final Account[] veyronAccounts = accountManager.getAccountsByType(VEYRON_ACCOUNT_TYPE);
    if (veyronAccounts == null || veyronAccounts.length <= 0) {  // No Veyron accounts on device.
      accountView.addView(getLayoutInflater().inflate(R.layout.action_account_add, null));
      accountView.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            final Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[]{ VEYRON_ACCOUNT_TYPE });
            startActivity(intent);
          }
      });
    } else {
      final LinearLayout view =
          (LinearLayout) getLayoutInflater().inflate(R.layout.action_account_existing, null);
      ((TextView)view.findViewById(R.id.blessing_name)).setText(veyronAccounts[0].name);
      accountView.addView(view);
      accountView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          final Intent intent = AccountManager.newChooseAccountIntent(
              null, null, new String[]{ VEYRON_ACCOUNT_TYPE }, false, null, null, null, null);
          startActivity(intent);
        }
      });
    }
    final ActionBar ab = getActionBar();
    ab.setCustomView(accountView);
  }

  private class NameFetcher extends AsyncTask<Void, Void, List<MountEntry>> {
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
    protected List<MountEntry> doInBackground(Void... args) {
      final MountEntry entry = (MountEntry)dirView.getTag();
      try {
        return Namespace.glob(entry.getName());
      } catch (VeyronException e) {
        errorMsg = "Error fetching names: " + e.getMessage();
        return null;
      }
    }
    @Override
    protected void onPostExecute(List<MountEntry> entries) {
      progressDialog.dismiss();
      ViewUtil.updateDirectoryView(dirView, true);
      if (entries == null) {
        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
        return;
      }
      final MountEntry parentEntry = (MountEntry)dirView.getTag();
      for (MountEntry entry : entries) {
        if (!entry.getName().startsWith(parentEntry.getName() + "/")) {
          android.util.Log.e(TAG, String.format("Entry %q doesn't start with parent prefix %q",
              entry.getName(), parentEntry.getName() + "/"));
          continue;
        }
        final String text = entry.getName().substring(parentEntry.getName().length() + 1);

        final LinearLayout childView =
            (entry.getServers() == null || entry.getServers().length <= 0)
            ? ViewUtil.createDirectoryView(text, entry, getLayoutInflater()) // sub-directory
            : ViewUtil.createObjectView(text, entry, getLayoutInflater());   // object
        dirView.addView(childView);
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
      final MountEntry entry = (MountEntry)objView.getTag();
      try {
        return Methods.get(entry.getName());
      } catch (VeyronException e) {
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
        return;
      }
      final MountEntry entry = (MountEntry)objView.getTag();
      for (String method : methods) {
        final LinearLayout childView =
            ViewUtil.createMethodView(method, entry, getLayoutInflater());
        objView.addView(childView);
      }
    }
  }
}