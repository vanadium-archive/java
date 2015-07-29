package io.v.android.apps.slide_presenter;

import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.ImageView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class MainActivityTest
        extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity mMainActivityTest;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        //setActivityInitialTouchMode(true);
        setActivityInitialTouchMode(false);
        mMainActivityTest =  getActivity();
    }

    @UiThreadTest
    //TODO(afergan): Needs improvement. I don't want to hard code a slide # here in case it's
    //greater than slidelength. Suggestion?
    public void testSetSlideNum() throws Exception {
        mMainActivityTest.setSlideNum(mMainActivityTest.numSlides() - 1);
        Assert.assertEquals(mMainActivityTest.numSlides()-1, mMainActivityTest.getSlideNum());
    }

    @UiThreadTest
    public void testSetSlideNumOutOfBounds() throws Exception {
        mMainActivityTest.setSlideNum(mMainActivityTest.numSlides() + 1);
        Assert.assertEquals(mMainActivityTest.numSlides() - 1, mMainActivityTest.getSlideNum());
    }

    @UiThreadTest
    public void testNegativeSetSlideNum() throws Exception {
        mMainActivityTest.setSlideNum(-3);
        Assert.assertEquals(0, mMainActivityTest.getSlideNum());
    }
}

