// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.common.base.Throwables;

import org.joda.time.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@Slf4j
public class LogCatFragment extends ListFragment {
    private static final int
            INITIAL_BUFFER = 1024,
            MAX_SMOOTH_SCROLL = 64;
    private static final Duration LOGCAT_UPDATE_PERIOD = Duration.millis(100);

    // http://stackoverflow.com/questions/12692103/read-logcat-programmatically-within-application
    private static final Observable<String> RX_LOG_CAT = Observable.<String>create(subscriber -> {
        try {
            final Process process = Runtime.getRuntime().exec("logcat");
            try (final BufferedReader bufferedReader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (!subscriber.isUnsubscribed() && (line = bufferedReader.readLine()) != null) {
                    subscriber.onNext(line);
                }
            }
            subscriber.onCompleted();
        } catch (final IOException e) {
            subscriber.onError(e);
        }
    });

    private Subscription mLogCatSubscription;
    private boolean mFollow = true;
    private final AbsListView.OnScrollListener mScrollListener =
            new AbsListView.OnScrollListener() {
                private int mPrevious;
                @Override
                public void onScrollStateChanged(final AbsListView view, final int scrollState) {
                }

                @Override
                public void onScroll(final AbsListView view, final int firstVisibleItem,
                                     final int visibleItemCount, final int totalItemCount) {
                    final boolean atBottom = visibleItemCount == 0 ||
                            firstVisibleItem + visibleItemCount == totalItemCount;
                    if (firstVisibleItem < mPrevious) {
                        mFollow = false;
                    } else if (atBottom) {
                        mFollow = true;
                    }
                    mPrevious = firstVisibleItem;
                }
            };

    private ListAdapter createAdapter() {
        final ArrayList<String> buffer = new ArrayList<>(INITIAL_BUFFER);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, buffer);
        mLogCatSubscription = RX_LOG_CAT
                .buffer(LOGCAT_UPDATE_PERIOD.getMillis(), TimeUnit.MILLISECONDS)
                .filter(e -> !e.isEmpty())
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> {
                            buffer.addAll(s);
                            adapter.notifyDataSetChanged();
                            if (mFollow) {
                                final ListView listView = getListView();
                                if (buffer.size() - listView.getLastVisiblePosition() >
                                        MAX_SMOOTH_SCROLL) {
                                    listView.setSelection(buffer.size() - 1);
                                } else {
                                    listView.smoothScrollToPosition(buffer.size() - 1);
                                }
                            }
                        },
                        t -> {
                            buffer.add(Throwables.getStackTraceAsString(t));
                            log.error("Error while following logs", t);
                        });
        return adapter;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(createAdapter());
    }

    @Override
    public void onDestroy() {
        mLogCatSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.logcat, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFollow) {
            setSelection(getListAdapter().getCount() - 1);
        }
        getListView().setOnScrollListener(mScrollListener);
    }
}
