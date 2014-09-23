package com.veyron.projects.namespace;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.naming.MountEntry;

import java.util.List;
 
public class MainActivity extends Activity {
  private static final String TAG = "net.veyron";
  private static final String VEYRON_ACCOUNT_TYPE = "net.veyron";
  private static final String PREF_NAMESPACE_GLOB_ROOT = "pref_namespace_glob_root";
  private static final String DEFAULT_NAMESPACE_GLOB_ROOT = "/proxy.envyor.com:8101";

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
    
    setupAccountActionView();
  }
  
  public void onItemClick(View view) {
    switch (view.getId()) {
      case R.id.object:
        return;
      case R.id.directory:
        handleDirClick(view);
      default:
        android.util.Log.e(
            TAG, String.format("Click on an illegal view with id: %d", view.getId()));
    }
  }
    
  private void handleDirClick(View view) {
    final LinearLayout dirView = (LinearLayout)view;
    dirView.setActivated(!dirView.isActivated());  // toggle
    if (dirView.isActivated()) {
      // Add new views.
      new NamesFetcher(dirView).execute();
      ((TextView)view.findViewById(R.id.name)).setTypeface(Typeface.DEFAULT_BOLD);
    } else {
      // Remove all but the first view.
      if (dirView.getChildCount() > 1) {
        dirView.removeViews(1, dirView.getChildCount() - 1);
      }
      ((TextView)dirView.findViewById(R.id.name)).setTypeface(Typeface.DEFAULT);
    }
    final ImageView arrowView = (ImageView) dirView.findViewById(R.id.arrow);
    arrowView.setRotation(dirView.isActivated() ? 0 : 180);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    // Initialize "account" menu item.
    final AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
    final Account[] veyronAccounts = accountManager.getAccountsByType(VEYRON_ACCOUNT_TYPE);
    final MenuItem accountItem = menu.findItem(R.id.action_account);
    final LinearLayout accountParentView = (LinearLayout) accountItem.getActionView();
    if (veyronAccounts == null || veyronAccounts.length <= 0) {  // No Veyron accounts on device.
      accountParentView.addView(
          getLayoutInflater().inflate(R.layout.action_account_add, null));
      accountParentView.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            Toast.makeText(MainActivity.this, "Add your account", Toast.LENGTH_LONG).show();
          }
      });
    } else {
      final LinearLayout view =
          (LinearLayout) getLayoutInflater().inflate(R.layout.action_account_existing, null);
      ((TextView)view.findViewById(R.id.blessing_name)).setText(veyronAccounts[0].name);
      accountParentView.addView(view);
      accountParentView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          //
        }
      });
    }
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
            accountManager.addAccount(
                VEYRON_ACCOUNT_TYPE, null, null, null, MainActivity.this, null, null);
            //Toast.makeText(MainActivity.this, "Add your account", Toast.LENGTH_LONG).show();
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
          //
        }
      });
    }
    final ActionBar ab = getActionBar();
    ab.setCustomView(accountView);
  }
  
  private class NamesFetcher extends AsyncTask<Void, Void, List<MountEntry>> {
    final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
    final LinearLayout dirView;
    
    NamesFetcher(LinearLayout dirView) {
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
        final String msg = "Error fetching names: " + e.getMessage();
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        return null;
      }
    }
    @Override
    protected void onPostExecute(List<MountEntry> entries) {
      progressDialog.dismiss();
      if (entries == null) return;  // error
      final MountEntry parentEntry = (MountEntry)dirView.getTag();
      for (MountEntry entry : entries) {
        if (!entry.getName().startsWith(parentEntry.getName() + "/")) {
          android.util.Log.e(TAG, String.format("Entry %q doesn't start with parent prefix %q",
              entry.getName(), parentEntry.getName() + "/"));
          continue;
        }
        final String text = entry.getName().substring(parentEntry.getName().length() + 1);
        final LayoutInflater inflater = getLayoutInflater();
        final LinearLayout childView =
            (entry.getServers() == null || entry.getServers().length <= 0)
            ? (LinearLayout)inflater.inflate(R.layout.directory_item, null) // sub-directory
            : (LinearLayout)inflater.inflate(R.layout.object_item, null);   // object

        childView.setTag(entry);
        final TextView nameView = (TextView) childView.findViewById(R.id.name);
        nameView.setText(text);
        dirView.addView(childView);
      }
    }
  }
}