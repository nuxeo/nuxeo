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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import javax.inject.Inject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.test.Failures;
import org.nuxeo.runtime.test.runner.RandomBug.Repeat;
import org.nuxeo.runtime.test.runner.RandomBug.RepeatMethodRule;
import org.nuxeo.runtime.test.runner.RandomBug.RepeatTestRule;

/**
 * Tests verifying that test fixtures ({@link Before} and {@link After}) and
 * rules ({@link RandomBug.Repeat}) are properly working together.
 *
 * @since 5.9.5
 */
public class RandomBugTest {

    protected static final String FAILURE_MESSAGE = "FAILURE";

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
    @Features({ RandomBug.Feature.class })
    public static class BeforeWithIgnoredTest {

        @ClassRule
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
                RandomBug.Mode.BYPASS.toString());
        runClassAndVerifyNoFailures(
                BeforeWithIgnoredTest.class,
                "Before method should not have been executed because the test method is ignored");
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class })
    public static class AfterWithIgnoredTest {

        @ClassRule
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
                RandomBug.Mode.BYPASS.toString());
        runClassAndVerifyNoFailures(AfterWithIgnoredTest.class,
                "After method should not have been executed because the test method is ignored");
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class })
    @RandomBug.Repeat(issue = "failingTest")
    public static class FailingTest {

        @Inject
        RepeatTestRule testRule;

        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Test
        @RandomBug.Repeat(issue = "failingTest", bypass = true)
        public void other() throws Exception {

        }

        @Test
        public void failAfterThreeRetry() throws Exception {
            if (testRule.statement.serial < 3) {
                return;
            }
            fail("over");
        }

    }

    @RunWith(FeaturesRunner.class)
    public static class FailingMethod {

        @Inject
        RepeatMethodRule methodRule;

        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Test
        public void other() throws Exception {

        }

        @Test
        @RandomBug.Repeat(issue = "failinMethod")
        public void failAterTenRetry() throws Exception {
            if (methodRule.statement.serial < 10) {
                return;
            }
            fail("over");
        }

    }

    @Test
    public void testBypass() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.Mode.BYPASS.toString());
        runClassAndVerifyNoFailures(FailingTest.class,
                "Test should be ignored in BYPASS mode!");
    }

    @Test
    public void testStrict() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.Mode.STRICT.toString());
        {
            Result result = JUnitCore.runClasses(FailingTest.class);
            assertThat(result.wasSuccessful()).isFalse();
            assertThat(result.getIgnoreCount()).isEqualTo(0);
        }
        {
            Result result = JUnitCore.runClasses(FailingMethod.class);
            assertThat(result.wasSuccessful()).isFalse();
            assertThat(result.getIgnoreCount()).isEqualTo(0);
        }
    }

    @Test
    public void testRelax() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.Mode.RELAX.toString());
        Result result = JUnitCore.runClasses(FailingTest.class);
        if (!result.wasSuccessful()) {
            Failures failures = new Failures(result.getFailures());
            fail("Unexpected failure: RELAX mode expects a success\n"
                    + failures.toString());
        }
    }

    public static class ThisFeature extends SimpleFeature {

        public int repeated;

        @Override
        public void beforeSetup(FeaturesRunner runner) throws Exception {
            repeated++;
        }
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class, ThisFeature.class })
    public static class RepeatFeaturesTest {

        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Inject
        public FeaturesRunner runner;

        public ThisFeature feature;

        @Before
        public void injectFeature() {
            feature = runner.getFeature(ThisFeature.class);
        }

        @Test
        @Repeat(issue = "repeatedTest", onFailure = 5)
        public void repeatedTest() {
            if (feature.repeated < 5) {
                fail("should fail");
            }
        }
    }

    @Test
    public void shouldRepeatFeatures() {
        System.setProperty(RandomBug.MODE_PROPERTY,
                RandomBug.Mode.RELAX.toString());
        Result result = JUnitCore.runClasses(RepeatFeaturesTest.class);
        if (!result.wasSuccessful()) {
            Failures failures = new Failures(result.getFailures());
            fail("Unexpected failure\n" + failures.toString());
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
