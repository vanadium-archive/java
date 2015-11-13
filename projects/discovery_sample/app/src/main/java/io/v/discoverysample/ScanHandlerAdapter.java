// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.discoverysample;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.v.v23.discovery.Service;
import io.v.v23.discovery.Update;
import io.v.v23.discovery.VDiscovery;

public class ScanHandlerAdapter extends BaseAdapter implements VDiscovery.ScanCallback {
    List<Service> knownAdvertisements;

    List<DataSetObserver> observers;

    LayoutInflater inflater;

    Activity activity;

    ScanHandlerAdapter(Activity activity) {
        knownAdvertisements = new ArrayList<>();
        observers = new ArrayList<>();
        this.activity = activity;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return knownAdvertisements.size();
    }

    @Override
    public Object getItem(int i) {
        if (i < knownAdvertisements.size()) {
            return knownAdvertisements.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        if (i < knownAdvertisements.size()) {
            return knownAdvertisements.get(i).hashCode();
        }
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.item, null);
        }
        Service service = knownAdvertisements.get(i);
        TextView displayName = (TextView) view.findViewById(R.id.display_name);
        displayName.setText(service.getInstanceName());
        TextView interfaceName = (TextView) view.findViewById(R.id.interface_name);
        interfaceName.setText(service.getInterfaceName());
        TextView addrs = (TextView) view.findViewById(R.id.addrs);
        addrs.setText(Joiner.on(",").join(service.getAddrs()));
        ListView attrs = (ListView) view.findViewById(R.id.attributes);
        attrs.setAdapter(new AttrAdapter(inflater, service.getAttrs()));
        return view;
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return knownAdvertisements.isEmpty();
    }

    @Override
    public void handleUpdate(Update update) {
        if (update instanceof Update.Found) {
            Update.Found found = (Update.Found) update;
            knownAdvertisements.add(found.getElem().getService());
        } else {
            Update.Lost lost = (Update.Lost) update;
            for (int i = 0; i < knownAdvertisements.size(); i++) {
                if (Arrays.equals(knownAdvertisements.get(i).getInstanceUuid(), lost.getElem().getInstanceUuid())) {
                    knownAdvertisements.remove(i);
                    break;
                }
            }
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }
}
