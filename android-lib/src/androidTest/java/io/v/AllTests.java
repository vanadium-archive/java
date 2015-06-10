// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Enumeration;

import dalvik.system.DexFile;
import io.v.testing.SkipOnAndroid;

@RunWith(org.junit.runners.AllTests.class)
public class AllTests {
    public static TestSuite suite() throws IOException {
        TestSuite suite = new TestSuite();

        Context context = InstrumentationRegistry.getContext();
        DexFile file = new DexFile(context.getPackageCodePath());
        for (Enumeration<String> iter = file.entries(); iter.hasMoreElements();) {
            String name = iter.nextElement();
            if (name.endsWith("Test") && name.startsWith("io.v")) {
                // Load the class.
                try {
                    Class<?> testClass = Class.forName(name);
                    if (testClass.isAnnotationPresent(SkipOnAndroid.class)) {
                        continue;
                    } else if (TestCase.class.isAssignableFrom(testClass)) {
                        // It's a JUnit3 test class.
                        suite.addTestSuite((Class<TestCase>) testClass);
                    } else if (testClass.isAnnotationPresent(RunWith.class)) {
                        // It's a JUnit4 test class.
                        suite.addTest(new JUnit4TestAdapter(testClass));
                    }
                } catch (ClassNotFoundException e) {
                    Log.w(AllTests.class.getName(),
                            "could not load class " + name + " for inspection", e);
                }
            }
        }

        return suite;
    }
}
