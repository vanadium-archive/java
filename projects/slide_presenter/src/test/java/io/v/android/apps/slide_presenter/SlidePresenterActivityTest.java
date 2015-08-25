package io.v.android.apps.slide_presenter;

import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class SlidePresenterActivityTest
        extends ActivityInstrumentationTestCase2<SlidePresenterActivity> {

    private SlidePresenterActivity mSlidePresenterActivityTest;

    public SlidePresenterActivityTest() {
        super(SlidePresenterActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        setActivityInitialTouchMode(false);
        mSlidePresenterActivityTest =  getActivity();
    }

    @UiThreadTest
    // TODO(afergan): Needs improvement. I don't want to hard code a slide # here in case it's
    // greater than slidelength. Suggestion?
    public void testSetSlideNum() throws Exception {
        mSlidePresenterActivityTest.setSlideNum(mSlidePresenterActivityTest.numSlides() - 1);
        Assert.assertEquals(mSlidePresenterActivityTest.numSlides()-1, mSlidePresenterActivityTest.getSlideNum());
    }

    @UiThreadTest
    public void testSetSlideNumOutOfBounds() throws Exception {
        mSlidePresenterActivityTest.setSlideNum(mSlidePresenterActivityTest.numSlides() + 1);
        Assert.assertEquals(mSlidePresenterActivityTest.numSlides() - 1, mSlidePresenterActivityTest.getSlideNum());
    }

    @UiThreadTest
    public void testNegativeSetSlideNum() throws Exception {
        mSlidePresenterActivityTest.setSlideNum(-3);
        Assert.assertEquals(0, mSlidePresenterActivityTest.getSlideNum());
    }
}

