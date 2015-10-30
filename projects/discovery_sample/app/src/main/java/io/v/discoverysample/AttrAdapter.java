// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.discoverysample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.v.v23.discovery.Attributes;

public class AttrAdapter extends BaseAdapter implements ListAdapter {
    private class Entry implements Comparable<Entry> {
        String key;
        String value;

        Entry(Map.Entry<String, String> entry) {
            key = entry.getKey();
            value = entry.getValue();
        }

        @Override
        public int compareTo(Entry entry) {
            return key.compareTo(entry.key);
        }
    }

    List<Entry> entries;
    Context ctx;;
    LayoutInflater inflater;
    public AttrAdapter(LayoutInflater inflater, Attributes attrs) {
        this.inflater = inflater;
        entries = new ArrayList<>(attrs.size());
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            entries.add(new Entry(entry));
        }
        Collections.sort(entries);
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int i) {
        return entries.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.attribute, null);
        }
        Entry e = entries.get(i);
        TextView key = (TextView) view.findViewById(R.id.key);
        key.setText(e.key);
        TextView value = (TextView) view.findViewById(R.id.value);
        value.setText(e.value);
        return view;
    }
}
