package com.veyron.projects.namespace;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import io.veyron.veyron.veyron.runtimes.google.security.Util;
import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.naming.MountEntry;
import io.veyron.veyron.veyron2.security.PrivateID;
import io.veyron.veyron.veyron2.security.PublicID;
import io.veyron.veyron.veyron2.security.wire.Certificate;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.security.wire.ChainPublicIDImpl;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
	private static final String TAG = "com.veyron.projects.namespace";
	private static final String VEYRON_ACCOUNT_TYPE = "com.veyron";
	private static final String PREF_NAMESPACE_GLOB_ROOT = "pref_namespace_glob_root";
	private static final String DEFAULT_NAMESPACE_GLOB_ROOT = "/proxy.envyor.com:8101";
	private static final String SAVED_VIEW_STATE_KEY = "browser_viewstate";
	private static final String BLESSINGS_KEY = "blessings";

	private static final int ACCOUNT_CHOOSING_REQUEST = 1;
	private static final int BLESSING_REQUEST = 2;

	String mSelectedBlessing = "";
	io.veyron.veyron.veyron2.Runtime mRuntime = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final String root = PreferenceManager.getDefaultSharedPreferences(this).getString(
				PREF_NAMESPACE_GLOB_ROOT, DEFAULT_NAMESPACE_GLOB_ROOT);
		final View dirView = findViewById(R.id.directory);
		dirView.setPadding(  // remove left padding for the root.
				0, dirView.getPaddingTop(), dirView.getPaddingRight(), dirView.getPaddingBottom());
		dirView.setTag(new MountEntry(root, null, null));
		final TextView nameView = (TextView) dirView.findViewById(R.id.name);
		nameView.setText(root);

		mSelectedBlessing = "";
		mRuntime = RuntimeFactory.init(this, new Options());

		updateBlessingsView();
	}

	@Override
	protected void onStart() {
		super.onStart();
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case ACCOUNT_CHOOSING_REQUEST:
				if (resultCode != RESULT_OK || data == null || data.getExtras() == null) {
					Toast.makeText(this, "Error selecting account.", Toast.LENGTH_LONG).show();
					return;
				}
				final String accountName = data.getExtras().getString(
						AccountManager.KEY_ACCOUNT_NAME);
				if (accountName == null || accountName.isEmpty()) {
					Toast.makeText(this, "Empty account name.", Toast.LENGTH_LONG).show();
					return;
				}
				deriveAndAddBlessing(accountName);
				return;
			case BLESSING_REQUEST:
				try {
					final String blessing = Blessing.getReply(resultCode, data);
					addBlessing(blessing);
				} catch (VeyronException e) {
					final String msg = "Couldn't derive blessing: " + e.getMessage();
					Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
				}
				return;
		}
	}

	private void createDeriveAndAddBlessing() {
		final Intent intent = AccountManager.newChooseAccountIntent(
				null, null, new String[]{ VEYRON_ACCOUNT_TYPE }, true, null, null, null, null);
		startActivityForResult(intent, ACCOUNT_CHOOSING_REQUEST);
		// Continues in deriveAndAddBlessing.
	}

	private void deriveAndAddBlessing(String accountName) {
		try {
			final Intent intent = Blessing.createIntent(this, accountName);
			startActivityForResult(intent, BLESSING_REQUEST);  // Continues in addBlessing.
		} catch (VeyronException e) {
			final String msg = String.format(
					"Couldn't bless account %q: %s", accountName, e.getMessage());
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
		}
	}

	private void addBlessing(String blessing) {
		final String name = getBlessingName(blessing);
		final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		final Set<String> blessings = prefs.getStringSet(BLESSINGS_KEY, null);
		final Set<String> newBlessings = new HashSet<String>();
		if (blessings != null) {
			// Remove a blessing (if any) whose name is the same as the new blessing.
			for (String b : blessings) {
				if (!getBlessingName(b).equals(name)) {
					newBlessings.add(b);
				}
			}
		}
		newBlessings.add(blessing);

		// Update preferences.
		final SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(BLESSINGS_KEY, newBlessings);
		editor.commit();

		Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
		updateBlessingsView();
	}

	private void updateSelectedBlessing(String blessing) {
		// Decode the blessing and create a new runtime with the decoded blessing.
		try {
			final ChainPublicID[] chains = Util.decodeChains(new String[]{ blessing });
			final PublicID pubID = new ChainPublicIDImpl(chains);
			final PrivateID privID = RuntimeFactory.defaultRuntime().getIdentity();
			final PrivateID derivedPrivID = privID.derive(pubID);
			final Options runtimeOpts = new Options();
			runtimeOpts.set(OptionDefs.RUNTIME_ID, derivedPrivID);
			mRuntime = RuntimeFactory.newRuntime(this, runtimeOpts);
			mSelectedBlessing = blessing;
		} catch (VeyronException e) {
			final String msg = String.format(
					"Couldn't set blessing %s: %s", blessing, e.getMessage());
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
		}
	}

	private void updateBlessingsView() {
		final LinearLayout blessingView =
				(LinearLayout) getLayoutInflater().inflate(R.layout.action_account, null);
		final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		final Set<String> blessings = prefs.getStringSet(BLESSINGS_KEY, null);
		if (blessings == null || blessings.size() <= 0) {  // No blessings for this app.
			blessingView.addView(getLayoutInflater().inflate(R.layout.action_account_add, null));
			blessingView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						createDeriveAndAddBlessing();
					}
			});
		} else {
			if (mSelectedBlessing.isEmpty()) {
				updateSelectedBlessing(blessings.iterator().next());
			}
			final LinearLayout view = (LinearLayout) getLayoutInflater().inflate(
					R.layout.action_account_existing, null);
			((TextView)view.findViewById(R.id.blessing_name)).setText(
					getBlessingName(mSelectedBlessing));
			blessingView.addView(view);
			blessingView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showBlessingsPopup(v);
				}
			});
		}
		getActionBar().setCustomView(blessingView);
	}

	private void showBlessingsPopup(View v) {
		final PopupMenu popup = new PopupMenu(this, v);
		final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		final Set<String> blessings = prefs.getStringSet(BLESSINGS_KEY, null);
		for (final String blessing : blessings) {
			final String itemName = getBlessingName(blessing);
			final MenuItem item = popup.getMenu().add(itemName);
			item.setOnMenuItemClickListener(new OnMenuItemClickListener(){
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					updateSelectedBlessing(blessing);
					updateBlessingsView();
					return true;
				}
			});
		}
		final MenuItem item =
				popup.getMenu().add(getResources().getString(R.string.action_account_add));
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				createDeriveAndAddBlessing();
				return true;
			}
		});
		popup.show();
	}

	private static String getBlessingName(String blessing) {
		String ret = "";
		final Gson gson = JSONUtil.getGsonBuilder().create();
		try {
			final ChainPublicID chain = gson.fromJson(
					blessing, new TypeToken<ChainPublicID>(){}.getType());
			final List<Certificate> certs = chain.getCertificates();
			// Ignore last name (should be this package name).
			for (int i = 0; i < certs.size() - 1; ++i) {
				ret += certs.get(i).getName();
				ret += "/";
			}
		} catch (JsonSyntaxException e) {
			android.util.Log.e(TAG, "Couldn't get name from blessing: " + blessing);
		}
		return ret;
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
				return Namespace.glob(entry.getName(), mRuntime);
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
					android.util.Log.e(TAG, String.format(
							"Entry %q doesn't start with parent prefix %q",
							entry.getName(), parentEntry.getName() + "/"));
					continue;
				}
				final String text = entry.getName().substring(parentEntry.getName().length() + 1);

				final LinearLayout childView =
						(entry.getServers() == null || entry.getServers().length <= 0)
						? ViewUtil.createDirectoryView(text, entry, getLayoutInflater()) // sub-dir
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