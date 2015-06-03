// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

@RunWith(org.junit.runners.AllTests.class)
public class AllTests {
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        Reflections reflections = new Reflections(ClasspathHelper.forPackage("io.v"));

        // Grab all the JUnit4 tests
        for (Class<?> type : reflections.getTypesAnnotatedWith(RunWith.class)) {
            if (type.equals(AllTests.class)) {
                // Don't add ourselves!
                continue;
            }
            suite.addTest(new JUnit4TestAdapter(type));
        }

        // Now the JUnit3 tests
        for (Class<?> type : reflections.getSubTypesOf(TestCase.class)) {
            suite.addTest(new JUnit4TestAdapter(type));
        }
        return suite;
    }
}
