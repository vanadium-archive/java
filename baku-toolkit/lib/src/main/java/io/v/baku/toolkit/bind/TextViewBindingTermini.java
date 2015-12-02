// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.widget.TextView;

import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.Objects;

import io.v.rx.syncbase.WatchEvent;
import lombok.experimental.UtilityClass;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

@UtilityClass
public class TextViewBindingTermini {
    public static Subscription bindRead(final TextView textView,
                                        final Observable<WatchEvent<String>> downlink,
                                        final Action1<Throwable> onError) {
        return downlink
                .map(WatchEvent::getValue)
                .filter(s -> !Objects.equals(s, Objects.toString(textView.getText(), null)))
                .subscribe(textView::setTextKeepState, onError);
    }

    public static Observable<String> bindWrite(final TextView textView) {
        return RxTextView.afterTextChangeEvents(textView)
                .skip(1) //don't put the initial content
                .map(e -> Objects.toString(e.editable(), null));
    }

    public static Subscription bind(final TextView textView, final TwoWayBinding<String> binding,
                                    final Action1<Throwable> onError) {
        return new CompositeSubscription(
                bindRead(textView, binding.downlink(), onError),
                binding.uplink(bindWrite(textView)));
    }
}
