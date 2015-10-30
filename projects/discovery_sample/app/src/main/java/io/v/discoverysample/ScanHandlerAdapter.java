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
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Joiner;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import io.v.impl.google.lib.discovery.ScanHandler;
import io.v.x.ref.lib.discovery.Advertisement;

public class ScanHandlerAdapter extends BaseAdapter implements ScanHandler{

    List<Advertisement> knownAdvertisements;

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
        Advertisement adv = knownAdvertisements.get(i);
        TextView displayName = (TextView)view.findViewById(R.id.display_name);
        displayName.setText(adv.getService().getInstanceName());
        TextView interfaceName = (TextView)view.findViewById(R.id.interface_name);
        interfaceName.setText(adv.getService().getInterfaceName());
        TextView addrs = (TextView)view.findViewById(R.id.addrs);
        addrs.setText(Joiner.on(",").join(adv.getService().getAddrs()));
        ListView attrs = (ListView)view.findViewById(R.id.attributes);
        attrs.setAdapter(new AttrAdapter(inflater, adv.getService().getAttrs()));
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
    public void handleUpdate(Advertisement advertisement) {
        if (!advertisement.getLost()) {
            knownAdvertisements.add(advertisement);
        } else {
            advertisement.setLost(false);
            knownAdvertisements.remove(advertisement);
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }
}
