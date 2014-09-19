package net.veyron;
 
import android.app.Activity;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.veyron2.ipc.VeyronException;

import java.util.ArrayList;
import java.util.List;
 
public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private static final String TAG = "net.veyron"; 

    private final Activity mActivity;
    private final List<String> mNames;
    private final String mDebugName;
 
    public ExpandableListAdapter(Activity activity, List<String> names, String debugName) {
        mActivity = activity;
        mNames = names;
        mDebugName = debugName;
    }
    @Override
    public Object getChild(int groupPosition, int childPosition) {
      android.util.Log.e(TAG, String.format("%s->getChild(%d, %d)", mDebugName, groupPosition, childPosition));
      return null;
    }
    @Override
    public long getChildId(int groupPosition, int childPosition) {
      android.util.Log.e(TAG, String.format("%s->getChildId(%d, %d) = %d", mDebugName, groupPosition, childPosition, childPosition));
      return childPosition;
    }
    @Override
    public View getChildView(final int groupPosition, final int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
      android.util.Log.e(TAG, String.format("%s->getChildView(%d) = %s", mDebugName, groupPosition, String.format("%s[%d]", mDebugName, groupPosition)));

      if (convertView == null) {
        final LayoutInflater inflater = mActivity.getLayoutInflater();
        convertView = inflater.inflate(R.layout.activity_main, null);
      }
      
      /*
      List<String> names = null;
      try {
        android.util.Log.e(TAG, "Globbing at root: " + mNames.get(childPosition));
        names = Namespace.glob(mNames.get(childPosition));
        android.util.Log.e(TAG, "Done globbing for pattern");
      } catch (VeyronException e) {
        names = new ArrayList<String>();
        names.add("Error: " + e.getMessage());
      }*/
      /*
      final ArrayList<String> names = new ArrayList<String>();
      names.add("With");
      names.add("Or");
      names.add("Without");
      names.add("You");
      final ExpandableListView view = (ExpandableListView) convertView.findViewById(R.id.laptop_list);
      final ExpandableListAdapter adapter = new ExpandableListAdapter(
            mActivity, names, String.format("%s[%d]", mDebugName, groupPosition));  
      view.setAdapter(adapter);
       */
      //android.util.Log.e(TAG, String.format("%s->getChildView(%d) wants to be %d high (or %d) but is %d.", mDebugName, groupPosition, convertView.getMeasuredHeight(), view.getMeasuredHeight(), convertView.getHeight()));
      //convertView.setMinimumHeight(100);
 
      return convertView;
    }
    @Override
    public int getChildrenCount(int groupPosition) {
      android.util.Log.e(TAG, String.format("%s->getChildrenCount(%d) = %d", mDebugName, groupPosition, mNames.size()));
      return 1;  // The child is always an ExpandableListView.
    }
    @Override
    public Object getGroup(int groupPosition) {
      android.util.Log.e(TAG, String.format("%s->getGroup(%d)", mDebugName, groupPosition));
      return null;
    }
    @Override
    public int getGroupCount() {
      android.util.Log.e(TAG, String.format("%s->getGroupCount() = %d", mDebugName, mNames.size()));
      return mNames.size();
    }
    @Override
    public long getGroupId(int groupPosition) {
      android.util.Log.e(TAG, String.format("%s->getGroupId(%d) = %d", mDebugName, groupPosition, groupPosition));
        return groupPosition;
    }
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
      return null;
      /*
        final String name = mNames.get(groupPosition);
        android.util.Log.e(TAG, String.format("%s->getGroupView(%d, %s) = %s", mDebugName, groupPosition, isExpanded ? "exp" : "!exp", name));
        if (convertView == null) {
            final LayoutInflater infalInflater = mActivity.getLayoutInflater();
            convertView = infalInflater.inflate(R.layout.group_item, null);
        }
        final TextView item = (TextView) convertView.findViewById(R.id.laptop_group);
        if (isExpanded) {
          item.setTypeface(null, Typeface.BOLD);
        } else {
          item.setTypeface(null, Typeface.NORMAL);
        }
        item.setText(name);

        return convertView;
        */
    }
    @Override
    public boolean hasStableIds() {
      return false;
    }
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
    /*
    private int getHeight() {
      int total = 0;
      for (int i = 0; i < getGroupCount(); i++) {
        total += getGroupView(i, false, null, null).getMeasuredHeight();
        
      }
    }*/         
}