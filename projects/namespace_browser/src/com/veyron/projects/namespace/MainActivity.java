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

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.VRuntime;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.naming.MountEntry;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.Certificate;
import io.veyron.veyron.veyron2.security.Principal;
import io.veyron.veyron.veyron2.security.Security;
import io.veyron.veyron.veyron2.security.WireBlessings;
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

	WireBlessings mSelectedBlessing = null;
	Gson mGson = null;

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

		mSelectedBlessing = null;
		RuntimeFactory.initRuntime(this, new Options());
		mGson = JSONUtil.getGsonBuilder().create();

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
					final WireBlessings blessing = Blessing.getBlessings(resultCode, data);
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
		final Intent intent = Blessing.createIntent(this, accountName);
		startActivityForResult(intent, BLESSING_REQUEST);  // Continues in addBlessing.
	}

	private void addBlessing(WireBlessings blessing) {
		final String name = getBlessingName(blessing);
		final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		final Set<String> blessings = prefs.getStringSet(BLESSINGS_KEY, null);
		final Set<String> newBlessings = new HashSet<String>();
		if (blessings != null) {
			// Remove a blessing (if any) whose name is the same as the new blessing.
			for (String b : blessings) {
				if (!getBlessingName(jsonDecodeBlessing(b)).equals(name)) {
					newBlessings.add(b);
				}
			}
		}
		newBlessings.add(jsonEncodeBlessing(blessing));

		// Update preferences.
		final SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(BLESSINGS_KEY, newBlessings);
		editor.commit();

		Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
		updateBlessingsView();
	}

	private void updateSelectedBlessing(WireBlessings wire) {
		if (wire == null) {
			return;
		}
		try {
			final Blessings blessings = Security.newBlessings(wire);
			final Principal p = RuntimeFactory.defaultRuntime().getPrincipal();
			p.blessingStore().setDefaultBlessings(blessings);
			mSelectedBlessing = wire;
		} catch (VeyronException e) {
			final String msg = String.format(
					"Couldn't set blessing %s: %s", jsonEncodeBlessing(wire), e.getMessage());
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
			if (mSelectedBlessing == null) {
				updateSelectedBlessing(jsonDecodeBlessing(blessings.iterator().next()));
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
		for (String blessingStr : blessings) {
			final WireBlessings blessing = jsonDecodeBlessing(blessingStr);
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

	private static String getBlessingName(WireBlessings blessing) {
		if (blessing == null) {
			return "";
		}
		final List<Certificate> chain = blessing.getCertificateChains().get(0);
		String ret = "";
		// Ignore last name (should be this package name).
		for (int i = 0; i < chain.size() - 1; ++i) {
			ret += chain.get(i).getExtension();
			ret += "/";
		}
		return ret;
	}

	private static String jsonEncodeBlessing(WireBlessings wire) {
		return JSONUtil.getGsonBuilder().create().toJson(wire);
	}

	private static WireBlessings jsonDecodeBlessing(String json) {
		try {
			return (WireBlessings) JSONUtil.getGsonBuilder().create().fromJson(json,
					new TypeToken<WireBlessings>(){}.getType());
		} catch (JsonSyntaxException e) {
			android.util.Log.e(TAG, String.format(
					"Couldn't convert JSON string %s to WireBlessing format: %s",
					json, e.getMessage()));
			return null;
		}
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