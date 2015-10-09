// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Dialog for the presenter to pick a questioner.
 */
public class QuestionDialogFragment extends DialogFragment {
    public static final String QUESTION_BUNDLE_KEY = "questioner_position";
    private static final String QUESTIONER_LIST_KEY = "questioner_list_key";

    private String[] mQuestionerList;

    public static QuestionDialogFragment newInstance(String[] questionerList) {
        QuestionDialogFragment fragment = new QuestionDialogFragment();
        Bundle args = new Bundle();
        args.putStringArray(QUESTIONER_LIST_KEY, questionerList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mQuestionerList = args.getStringArray(QUESTIONER_LIST_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.question_message)
                .setItems(mQuestionerList, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(which);
                    }
                });
        return builder.create();
    }

    // Send back the position of the questioner to the NavigateFragment.
    private void sendResult(int position) {
        Intent intent = new Intent();
        intent.putExtra(QUESTION_BUNDLE_KEY, position);
        getTargetFragment().onActivityResult(
                getTargetRequestCode(), Activity.RESULT_OK, intent);
    }
}