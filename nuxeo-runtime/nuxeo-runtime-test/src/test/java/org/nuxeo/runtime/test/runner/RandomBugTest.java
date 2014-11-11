/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.runtime.test.runner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.test.Failures;

/**
 * Tests verifying that test fixtures ({@link Before} and {@link After}) and
 * rules ({@link RandomBug.Repeat}) are properly working together.
 *
 * @since 5.9.5
 */
public class RandomBugTest {

    protected static final String FAILURE_MESSAGE = "FAILURE";

    protected static boolean fail = true;

    protected static boolean isRunningInners;

    protected static class IgnoreInner implements MethodRule {

        @Override
        public Statement apply(Statement base, FrameworkMethod method,
                Object target) {
            Assume.assumeTrue(isRunningInners);
            return base;
        }

    }

    @BeforeClass
    public static void setInner() {
        isRunningInners = true;
    }

    @AfterClass
    public static void resetInner() {
        isRunningInners = false;
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RuntimeFeature.class })
    public static class BeforeWithIgnoredTest {

        @Rule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Before
        public void setup() {
            fail(FAILURE_MESSAGE);
        }

        @Test
        @RandomBug.Repeat(issue = FAILURE_MESSAGE)
        public void test() throws Exception {
            fail("test() should not run");
        }
    }

    @Test
    public void beforeShouldNotRunWhenAllTestsAreIgnored() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.MODE.BYPASS.toString());
        runClassAndVerifyNoFailures(
                BeforeWithIgnoredTest.class,
                "Before method should not have been executed because the test method is ignored");
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RuntimeFeature.class })
    public static class AfterWithIgnoredTest {

        @Rule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Test
        @RandomBug.Repeat(issue = FAILURE_MESSAGE)
        public void test() throws Exception {
            fail("test() should not run");
        }

        @After
        public void tearDown() {
            fail(FAILURE_MESSAGE);
        }
    }

    @Test
    public void afterShouldNotRunWhenAllTestsAreIgnored() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.MODE.BYPASS.toString());
        runClassAndVerifyNoFailures(AfterWithIgnoredTest.class,
                "After method should not have been executed because the test method is ignored");
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RuntimeFeature.class })
    public static class RandomlyFailingTest {

        @Rule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Test
        @RandomBug.Repeat(issue = "testSucceedThenFail")
        public void testSucceedThenFail() throws Exception {
            assertFalse("assert false: " + fail, fail);
        }

        @Test
        @RandomBug.Repeat(issue = "testFailThenSucceed")
        public void testFailThenSucceed() throws Exception {
            assertTrue("assert true: " + fail, fail);
        }

        @After
        public void tearDown() {
            fail = !fail;
        }
    }

    @Test
    public void testBypass() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.MODE.BYPASS.toString());
        runClassAndVerifyNoFailures(RandomlyFailingTest.class,
                "Test should be ignored in BYPASS mode!");
    }

    @Test
    public void testStrict() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.MODE.STRICT.toString());
        Result result = JUnitCore.runClasses(RandomlyFailingTest.class);
        if (result.wasSuccessful() || result.getIgnoreCount() > 0
                || result.getFailureCount() != result.getRunCount()) {
            fail("Unexpected success: STRICT mode expects a failure");
        }
    }

    @Test
    public void testRelax() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.MODE.RELAX.toString());
        Result result = JUnitCore.runClasses(RandomlyFailingTest.class);
        if (!result.wasSuccessful()) {
            Failures failures = new Failures(result.getFailures());
            fail("Unexpected failure: RELAX mode expects a success\n"
                    + failures.toString());
        }
    }

    protected void runClassAndVerifyNoFailures(Class<?> klass,
            String testFailureDescription) {
        Result result = JUnitCore.runClasses(klass);
        analyseResult(result, testFailureDescription);
    }

    protected void analyseResult(Result result, String testFailureDescription) {
        if (!result.wasSuccessful()) {
            new Failures(result).fail(FAILURE_MESSAGE, testFailureDescription);
        }
    }

}
