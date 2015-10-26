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

import java.util.List;

import io.v.android.apps.syncslides.model.Question;

/**
 * Dialog for the presenter to pick a questioner.
 */
public class QuestionDialogFragment extends DialogFragment {
    public static final String QUESTION_ID_KEY = "question_id_key";
    private static final String QUESTIONER_LIST_KEY = "questioner_list_key";

    public static QuestionDialogFragment newInstance(List<Question> questions) {
        QuestionDialogFragment fragment = new QuestionDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArray(QUESTIONER_LIST_KEY, questions.toArray(new Question[0]));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        final Question[] questions = (Question[]) args.getParcelableArray(QUESTIONER_LIST_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] questioners = new String[questions.length];
        for (int i = 0; i < questions.length; i++) {
            questioners[i] = questions[i].getName();
        }
        builder.setTitle(R.string.question_message)
                .setItems(questioners, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(questions[which].getId());
                    }
                });
        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((PresentationActivity) getActivity()).setUiImmersive(true);
    }

    // Send back the question's ID to the NavigateFragment.
    private void sendResult(String id) {
        Intent intent = new Intent();
        intent.putExtra(QUESTION_ID_KEY, id);
        getTargetFragment().onActivityResult(
                getTargetRequestCode(), Activity.RESULT_OK, intent);
    }
}