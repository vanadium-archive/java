// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.util.Log;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import java.util.List;

import io.v.android.apps.syncslides.model.Question;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Stream;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Watches for new questions in a given presentation.
 */
class QuestionWatcher {
    private static final String TAG = "QuestionWatcher";
    private final WatcherState mState;
    private final DB.QuestionListener mListener;
    private final List<Question> mQuestions;
    private boolean mIsDiscarded;

    /**
     * Creates a new watcher for the presentation in state.
     *
     * @param state objects necessary to do the watching
     * @param listener notified whenever the set of questions changes
     */
    public QuestionWatcher(WatcherState state, DB.QuestionListener listener) {
        mState = state;
        mListener = listener;
        mQuestions = Lists.newArrayList();
        mState.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                watch();
            }
        });
        mState.thread.start();
        mIsDiscarded = false;
    }

    /**
     * Stops watching the presentation for new questions.
     */
    public void discard() {
        mState.vContext.cancel();  // this will cause the watcher thread to exit
        mState.handler.removeCallbacksAndMessages(null);
        // We've canceled all the pending callbacks, but the handler might be just about
        // to execute put()/get() and those messages wouldn't get canceled.  So we mark
        // the list as discarded and count on put()/get() checking for it.
        mIsDiscarded = true;
    }

    private void watch() {
        try {
            String prefix = NamingUtil.join(
                    mState.deckId, mState.presentationId, SyncbaseDB.QUESTIONS);
            Log.i(TAG, "Watching questions: " + prefix);
            BatchDatabase batch = mState.db.beginBatch(mState.vContext, null);
            Table presentations = batch.getTable(SyncbaseDB.PRESENTATIONS_TABLE);
            Stream<KeyValue> stream = presentations.scan(mState.vContext, RowRange.prefix(prefix));
            for (KeyValue keyValue : stream) {
                VQuestion value = (VQuestion) VomUtil.decode(keyValue.getValue(), VQuestion.class);
                if (value.getAnswered()) {
                    continue;
                }
                final Question question = new Question(
                        lastPart(keyValue.getKey()),
                        value.getQuestioner().getName(),
                        value.getTime());
                mState.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        put(question);
                    }
                });
            }

            Stream<WatchChange> watch = mState.db.watch(
                    mState.vContext, SyncbaseDB.PRESENTATIONS_TABLE, prefix,
                    batch.getResumeMarker(mState.vContext));
            for (WatchChange change : watch) {
                Log.i(TAG, "Found change " + change.getChangeType());
                final String id = lastPart(change.getRowName());
                if (change.getChangeType().equals(ChangeType.PUT_CHANGE)) {
                    VQuestion vQuestion = (VQuestion) VomUtil.decode(
                            change.getVomValue(), VQuestion.class);
                    Log.i(TAG, "Change " + change);
                    if (vQuestion.getAnswered()) {
                        mState.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "Question was answered");
                                delete(id);
                            }
                        });
                        continue;
                    }
                    final Question question = new Question(
                            id,
                            vQuestion.getQuestioner().getName(),
                            vQuestion.getTime());
                    mState.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            put(question);
                        }
                    });
                } else { // ChangeType.DELETE_CHANGE
                    mState.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            delete(id);
                        }
                    });
                }
            }
        } catch (VException e) {
            e.printStackTrace();
        }
    }

    // TODO(kash): Sort the questions?
    private void put(Question question) {
        if (mIsDiscarded) {
            return;
        }
        int i = 0;
        for (; i < mQuestions.size(); i++) {
            int comp = mQuestions.get(i).getId().compareTo(question.getId());
            if (comp == 0) {
                // Existing question with changed state.
                mQuestions.set(i, question);
                break;
            } else if (comp > 0) {
                mQuestions.add(i, question);
                break;
            }
        }
        // None of the previous questions sorted after this one.  Add it to the back.
        if (i == mQuestions.size()) {
            mQuestions.add(question);
        }
        mListener.onChange(Lists.newArrayList(mQuestions));
    }

    private void delete(String id) {
        if (mIsDiscarded) {
            return;
        }
        for (int i = 0; i < mQuestions.size(); i++) {
            if (mQuestions.get(i).getId().equals(id)) {
                mQuestions.remove(i);
                mListener.onChange(Lists.newArrayList(mQuestions));
                return;
            }
        }
        Log.i(TAG, "Could not find question " + id);
    }

    /**
     * Splits the name into parts and returns the last one.
     */
    private static String lastPart(String name) {
        List<String> split = NamingUtil.split(name);
        return split.get(split.size() - 1);
    }
}
