package net.veyron;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.veyron2.Options;
import com.veyron2.RuntimeFactory;
 
public class MainActivity extends Activity {
  private static final String TAG = "net.veyron";
  private static final String PREF_NAMESPACE_GLOB_ROOT = "pref_namespace_glob_root";
  private static final String DEFAULT_NAMESPACE_GLOB_ROOT = "/proxy.envyor.com:8101";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    RuntimeFactory.init(this, new Options());
    final String root = PreferenceManager.getDefaultSharedPreferences(this).getString(
            PREF_NAMESPACE_GLOB_ROOT, DEFAULT_NAMESPACE_GLOB_ROOT);
    final TextView nameView = (TextView) findViewById(R.id.name);
    nameView.setText(root);
  }
  
  public void onItemClick(View view) {
    final LinearLayout parent = (LinearLayout)view;
    parent.setActivated(!parent.isActivated());  // toggle
    android.util.Log.e(TAG, "Registered click, status is: " + (parent.isActivated() ? "ACTIVATED" : "NOT ACTIVATED"));
    if (parent.isActivated()) {
      // Add new items.
      final String[] names = {"With", "Or", "Without", "You"};
      for (String name : names) {
        final LayoutInflater inflater = getLayoutInflater();
        final LinearLayout child = (LinearLayout)inflater.inflate(R.layout.name_item, null);
        final TextView nameView = (TextView) child.findViewById(R.id.name);
        nameView.setText(name);
        parent.addView(child);
      }
    } else {
      // Remove all but the first view.
      if (parent.getChildCount() > 1) {
        parent.removeViews(1, parent.getChildCount() - 1);
      }
    }
   final ImageView arrowView = (ImageView) parent.findViewById(R.id.arrow);
   arrowView.setRotation(parent.isActivated() ? 0 : 180);
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
}