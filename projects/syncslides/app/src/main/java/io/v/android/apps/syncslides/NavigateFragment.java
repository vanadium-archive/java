// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.model.Slide;

/**
 * Provides both the presenter and audience views for navigating through a presentation.
 * Instantiated by the PresentationActivity along with other views/fragments of the presentation
 * to make transitions between them seamless.
 */
public class NavigateFragment extends Fragment {

    private static final String TAG = "NavigateFragment";
    private static final String DECK_ID_KEY = "deck_id_key";
    private static final String SLIDE_NUM_KEY = "slide_num_key";
    private static final String ROLE_KEY = "role_key";
    private static final String SYNCED_KEY = "synced_key";
    private static final int DIALOG_REQUEST_CODE = 23;

    private String mDeckId;
    private int mSlideNum;
    private ImageView mPrevThumb;
    private ImageView mNextThumb;
    private ImageView mCurrentSlide;
    private ImageView mQuestions;
    private View mFabSync;
    private TextView mQuestionsNum;
    private EditText mNotes;
    private Slide[] mSlides;
    private Role mRole;
    private String[] mQuestionerList;
    private boolean mEditing;
    private int mQuestionerPosition;
    private boolean mSynced;

    public static NavigateFragment newInstance(
            String deckId, int slideNum, Role role, boolean synced) {
        NavigateFragment fragment = new NavigateFragment();
        Bundle args = new Bundle();
        args.putString(DECK_ID_KEY, deckId);
        args.putInt(SLIDE_NUM_KEY, slideNum);
        args.putSerializable(ROLE_KEY, role);
        args.putBoolean(SYNCED_KEY, synced);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args;
        if (savedInstanceState != null) {
            Log.i(TAG, "restoring from savedInstanceState");
            args = savedInstanceState;
        } else {
            args = getArguments();
        }
        mDeckId = args.getString(DECK_ID_KEY);
        mSlideNum = args.getInt(SLIDE_NUM_KEY);
        mRole = (Role) args.get(ROLE_KEY);
        mSynced = args.getBoolean(SYNCED_KEY);

        final View rootView = inflater.inflate(R.layout.fragment_navigate, container, false);
        mFabSync = rootView.findViewById(R.id.audience_sync_fab);
        if (mSynced || mRole != Role.AUDIENCE) {
            mFabSync.setVisibility(View.INVISIBLE);
        } else {
            mFabSync.setVisibility(View.VISIBLE);
        }

        mFabSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO(afergan): Set slide num to the presenter's current slide.
                mSynced = true;
                mFabSync.setVisibility(View.INVISIBLE);
            }
        });
        View.OnClickListener previousSlideListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousSlide();
            }
        };
        View arrowBack = rootView.findViewById(R.id.arrow_back);
        arrowBack.setOnClickListener(previousSlideListener);
        mPrevThumb = (ImageView) rootView.findViewById(R.id.prev_thumb);
        mPrevThumb.setOnClickListener(previousSlideListener);

        View.OnClickListener nextSlideListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSlide();
            }
        };
        // Show either the arrowForward or the FAB but not both.
        View arrowForward = rootView.findViewById(R.id.arrow_forward);
        View fabForward = rootView.findViewById(R.id.primary_navigation_fab);
        if (mRole == Role.PRESENTER) {
            arrowForward.setVisibility(View.INVISIBLE);
            fabForward.setOnClickListener(nextSlideListener);
        } else {
            fabForward.setVisibility(View.INVISIBLE);
            arrowForward.setOnClickListener(nextSlideListener);
        }
        mNextThumb = (ImageView) rootView.findViewById(R.id.next_thumb);
        mNextThumb.setOnClickListener(nextSlideListener);
        mQuestions = (ImageView) rootView.findViewById(R.id.questions);
        // TODO(kash): Hide the mQuestions button if mRole == BROWSER.
        mQuestions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                questionButton();
            }
        });
        mCurrentSlide = (ImageView) rootView.findViewById(R.id.slide_current_medium);
        mCurrentSlide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRole == Role.AUDIENCE || mRole == Role.BROWSER) {
                    ((PresentationActivity) getActivity()).fullscreenSlide(mSlideNum);
                }
            }
        });

        mNotes = (EditText) rootView.findViewById(R.id.notes);
        mNotes.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mEditing = hasFocus;
                getActivity().invalidateOptionsMenu();
                unsync();
            }
        });
        // The parent of mNotes needs to be focusable in order to clear focus
        // from mNotes when done editing.  We set the attributes in code rather
        // than in XML because it is too easy to add an extra level of layout
        // in XML and forget to add these attributes.
        ViewGroup parent = (ViewGroup) mNotes.getParent();
        parent.setFocusable(true);
        parent.setClickable(true);
        parent.setFocusableInTouchMode(true);

        View slideListIcon = rootView.findViewById(R.id.slide_list);
        slideListIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
        mQuestionsNum = (TextView) rootView.findViewById(R.id.questions_num);
        // Start off invisible for everyone.  If there are questions, this
        // will be set to visible in the db.getQuestionerList() callback.
        mQuestionsNum.setVisibility(View.INVISIBLE);

        DB db = DB.Singleton.get(getActivity().getApplicationContext());
        db.getSlides(mDeckId, new DB.Callback<Slide[]>() {
            @Override
            public void done(Slide[] slides) {
                mSlides = slides;
                updateView();
            }
        });
        if (mRole == Role.PRESENTER) {
            db.getQuestionerList(mDeckId, new DB.QuestionerListener() {
                @Override
                public void onChange(String[] questionerList) {
                    mQuestionerList = questionerList;
                    if (mQuestionerList.length > 0) {
                        mQuestionsNum.setVisibility(View.VISIBLE);
                        mQuestionsNum.setText(String.valueOf(mQuestionerList.length));
                    } else {
                        mQuestionsNum.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PresentationActivity) getActivity()).setUiImmersive(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        ((PresentationActivity) getActivity()).setUiImmersive(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DECK_ID_KEY, mDeckId);
        outState.putInt(SLIDE_NUM_KEY, mSlideNum);
        outState.putSerializable(ROLE_KEY, mRole);
        outState.putBoolean(SYNCED_KEY, mSynced);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mEditing) {
            inflater.inflate(R.menu.edit_notes, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                Toast.makeText(getContext(), "Saving notes", Toast.LENGTH_SHORT).show();
                mNotes.clearFocus();
                InputMethodManager inputManager =
                        (InputMethodManager) getContext().
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(
                        getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                ((PresentationActivity) getActivity()).setUiImmersive(true);
                return true;
        }
        return false;
    }

    private void unsync() {
        if (mRole == Role.AUDIENCE && mSynced) {
            mSynced = false;
            mFabSync.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Advances to the next slide, if there is one, and updates the UI.
     */
    private void nextSlide() {
        if (mSlides == null) {
            // Wait until the slides have loaded before letting the user move around.
            return;
        }
        if (mSlideNum < mSlides.length - 1) {
            mSlideNum++;
            updateView();
            unsync();
        }
    }

    /**
     * Goes back to the previous slide, if there is one, and updates the UI.
     */
    private void previousSlide() {
        if (mSlides == null) {
            // Wait until the slides have loaded before letting the user move around.
            return;
        }
        if (mSlideNum > 0) {
            mSlideNum--;
            updateView();
            unsync();
        }
    }

    /**
     * When the user presses the icon, add the user's identity to the presenter's question queue.
     * If presenter presses the button, get a list of users who are asking questions.
     */
    private void questionButton() {
        DB db = DB.Singleton.get(getActivity().getApplicationContext());
        switch (mRole) {
            case AUDIENCE:
                db.askQuestion("Audience member #1");
                Toast toast = Toast.makeText(getActivity().getApplicationContext(),
                        "You have been added to the Q&A queue.", Toast.LENGTH_LONG);
                toast.show();
                break;
            case PRESENTER:
                DialogFragment dialog = QuestionDialogFragment.newInstance(mQuestionerList);
                dialog.setTargetFragment(this, DIALOG_REQUEST_CODE);
                dialog.show(getFragmentManager(), "QuestionerDialogFragment");
                break;
            case BROWSER:
                // Do nothing.
        }
    }

    private void updateView() {
        if (mSlides == null) {
            // We can't do anything until the slides have loaded.
            return;
        }
        if (mSlideNum > 0) {
            setThumbBitmap(mPrevThumb, mSlides[mSlideNum - 1].getImage());
        } else {
            setThumbNull(mPrevThumb);
        }
        mCurrentSlide.setImageBitmap(mSlides[mSlideNum].getImage());
        if (mSlideNum == mSlides.length - 1) {
            setThumbNull(mNextThumb);
        } else {
            setThumbBitmap(mNextThumb, mSlides[mSlideNum + 1].getImage());
        }
        if (!mSlides[mSlideNum].getNotes().equals("")) {
            mNotes.setText(mSlides[mSlideNum].getNotes());
        } else {
            mNotes.getText().clear();
        }
        ((TextView) getView().findViewById(R.id.slide_num_text))
                .setText(String.valueOf(mSlideNum + 1) + " of " + String.valueOf(mSlides.length));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DIALOG_REQUEST_CODE) {
            // TODO(afergan): Using the position is insufficient if the list of questioners changes
            // while the dialog is showing.
            mQuestionerPosition = data.getIntExtra(
                    QuestionDialogFragment.QUESTION_BUNDLE_KEY, 0);
            handoffControl();
        }
    }

    /**
     * Handoff control of the presentation to a questioner. A snackbar displays the status while
     * the audience member is in control, and control ends when the presenter presses the action
     * text.
     */
    private void handoffControl() {
        //TODO(afergan): Change slide presenter to the audience member at mQuestionerPosition.
        View.OnClickListener snackbarClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO(afergan): End handoff, presenter regains control of presentation.
            }
        };

        ((PresentationActivity) getActivity()).setUiImmersive(true);
        Snackbar snack = Snackbar.make(getView(), getResources().getString(
                        R.string.handoff_message) + " " + mQuestionerList[mQuestionerPosition],
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getResources().getString(R.string.end_handoff),
                        snackbarClickListener)
                .setActionTextColor(ContextCompat.getColor(getContext(), R.color.action_orange));

        // Needed to set the location of the snackbar (default is bottom center, which hides buttons
        // in landscape mode).
        View view = snack.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        view.setLayoutParams(params);
        snack.show();
    }

    private void setThumbBitmap(ImageView thumb, Bitmap bitmap) {
        thumb.setImageBitmap(bitmap);
        // In landscape, the height is dependent on the image size.  However, if the
        // image was null, the height is hardcoded to 9/16 of the width in setThumbNull.
        // This resets it to the actual image size.
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup.LayoutParams thumbParams = thumb.getLayoutParams();
            thumbParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    private void setThumbNull(ImageView thumb) {
        thumb.setImageDrawable(null);
        // In landscape, the height is dependent on the image size.  Because we don't have an
        // image, assume all of the images are 16:9.  The width is fixed, so we can calculate
        // the expected height.
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup grandparent = (ViewGroup) thumb.getParent().getParent();
            ViewGroup.LayoutParams thumbParams = thumb.getLayoutParams();
            thumbParams.height = (int) ((9 / 16.0) * grandparent.getMeasuredWidth());
        }
    }
}
