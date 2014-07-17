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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;

/**
 * Tests verifying that test fixtures ({@link Before} and {@link After}) and
 * rules ({@link RandomBug.Repeat}) are properly working together.
 *
 * @since 5.9.5
 */
public class RandomBugTest {

    private static final Log log = LogFactory.getLog(RandomBugTest.class);

    private static final String FAILURE_MESSAGE = "FAILURE";

    protected static boolean fail = true;

    @RunWith(FeaturesRunner.class)
    @Features({ RuntimeFeature.class })
    public static class BeforeWithIgnoredTest {
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
            List<Failure> failures = result.getFailures();
            for (Failure failure : failures) {
                log.error(failure);
            }
            fail("Unexpected failure: RELAX mode expects a success");
        }
    }

    private void runClassAndVerifyNoFailures(Class<?> klass,
            String testFailureDescription) {
        Result result = JUnitCore.runClasses(klass);
        analyseResult(result, testFailureDescription);
    }

    private void analyseResult(Result result, String testFailureDescription) {
        List<Failure> failures = result.getFailures();
        if (!failures.isEmpty()) {
            for (Failure failure : failures) {
                analyzeFailure(failure, testFailureDescription);
            }
        }
    }

    private void analyzeFailure(Failure failure, String testFailureDescription) {
        String actualFailureMsg = failure.getMessage();
        if (FAILURE_MESSAGE.equals(actualFailureMsg)) {
            fail(testFailureDescription);
        }
        fail("Unexpected failure : " + actualFailureMsg);
    }
}
