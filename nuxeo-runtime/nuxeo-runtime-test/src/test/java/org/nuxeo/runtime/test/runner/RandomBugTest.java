/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.runtime.test.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.nuxeo.runtime.test.Failures;
import org.nuxeo.runtime.test.runner.RandomBug.RepeatRule;

/**
 * Tests verifying that test fixtures ({@link Before} and {@link After}) and rules ({@link RandomBug.Repeat}) are
 * properly working together.
 *
 * @since 5.9.5
 */
public class RandomBugTest {
    private static final Log log = LogFactory.getLog(RandomBugTest.class);

    protected static final String FAILURE_MESSAGE = "FAILURE";

    protected static boolean isRunningInners;

    private String oldProperty;

    protected static class IgnoreInner implements TestRule {
        @Override
        public Statement apply(Statement base, Description description) {
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
    public static class BeforeWithRandomTest {
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
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.BYPASS.toString());
        runClassAndVerify(BeforeWithRandomTest.class,
                "Before method should not have been executed because the test method is ignored", 1, 0, 1);
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class })
    public static class AfterWithRandomTest {
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
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.BYPASS.toString());
        runClassAndVerify(AfterWithRandomTest.class,
                "After method should not have been executed because the test method is ignored", 1, 0, 1);
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class })
    @RandomBug.Repeat(issue = "failingTest")
    public static class FailingTest {
        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Inject
        @Named("test")
        RepeatRule repeatRule;

        @Test
        @RandomBug.Repeat(issue = "failingTest.other", bypass = true)
        public void other() throws Exception {
            log.trace(repeatRule.statement.serial);
            fail("should be bypassed");
        }

        @Test
        public void success() {
            log.trace(repeatRule.statement.serial);
        }

        @Test
        public void failbeforeFiveRetry() throws Exception {
            log.trace(repeatRule.statement.serial);
            if (repeatRule.statement.serial < 5) {
                fail("on retry " + repeatRule.statement.serial);
            }
        }

        @Test
        public void successOnSevenRetry() throws Exception {
            log.trace(repeatRule.statement.serial);
            if (repeatRule.statement.serial != 7) {
                fail("on retry " + repeatRule.statement.serial);
            }
        }

        @Test
        public void failAfterTenRetry() throws Exception {
            log.trace(repeatRule.statement.serial);
            if (repeatRule.statement.serial > 10) {
                fail("on retry " + repeatRule.statement.serial);
            }
        }
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class })
    public static class FailingMethod {
        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Inject
        @Named("method")
        RepeatRule methodRule;

        @Test
        @RandomBug.Repeat(issue = "failingMethod.other", bypass = true)
        public void other() throws Exception {
            log.trace(methodRule.statement.serial);
            fail("should be bypassed");
        }

        @Test
        public void success() {
            log.trace(methodRule.statement);
        }

        @Test
        @RandomBug.Repeat(issue = "failingMethod")
        public void failbeforeFiveRetry() throws Exception {
            log.trace(methodRule.statement.serial);
            if (methodRule.statement.serial < 5) {
                fail("on retry " + methodRule.statement.serial);
            }
        }

        @Test
        @RandomBug.Repeat(issue = "failingMethod")
        public void successOnSevenRetry() throws Exception {
            log.trace(methodRule.statement.serial);
            if (methodRule.statement.serial != 7) {
                fail("on retry " + methodRule.statement.serial);
            }
        }

        @Test
        @RandomBug.Repeat(issue = "failingMethod")
        public void failAfterTenRetry() throws Exception {
            log.trace(methodRule.statement.serial);
            if (methodRule.statement.serial > 10) {
                fail("on retry " + methodRule.statement.serial);
            }
        }
    }

    @Test
    public void testBypassOnClass() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.BYPASS.toString());
        runClassAndVerify(FailingTest.class, "Test should be ignored in BYPASS mode!", 0, 0, 1);
    }

    @Test
    public void testBypassOnMethod() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.BYPASS.toString());
        runClassAndVerify(FailingMethod.class, "Test should be ignored in BYPASS mode!", 5, 0, 4);
    }

    @Test
    public void testStrictOnClass() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.STRICT.toString());
        runClassAndVerify(FailingTest.class, "STRICT mode should reveal failure", 5, 2, 1);
    }

    @Test
    public void testStrictOnMethod() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.STRICT.toString());
        runClassAndVerify(FailingMethod.class, "STRICT mode should reveal failure", 5, 3, 1);
    }

    @Test
    public void testRelaxOnClass() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.RELAX.toString());
        // JC: it remembers the failures: result.wasSuccessful() == false
        runClassAndVerify(FailingTest.class, "RELAX mode expects a success", 35, 10, 7);
    }

    @Test
    public void testRelaxOnMethod() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.RELAX.toString());
        runClassAndVerify(FailingMethod.class, "RELAX mode expects a success", 5, 0, 1);
    }

    public static class ThisFeature implements RunnerFeature {
        public int repeated;

        @Override
        public void beforeSetup(FeaturesRunner runner) throws Exception {
            repeated++;
            log.trace(runner.toString() + " " + repeated);
        }
    }

    public static abstract class AbstractRepeatFeaturesTest {
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
        public void repeatedTest() {
            if (feature.repeated < 5) {
                fail("should fail");
            }
        }
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class, ThisFeature.class })
    @RandomBug.Repeat(issue = "repeatedTest", onFailure = 5)
    public static class RepeatFeaturesTest extends AbstractRepeatFeaturesTest {

    }

    @Test
    public void shouldRepeatFeaturesOnClass() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.RELAX.toString());
        runClassAndVerify(RepeatFeaturesTest.class, "RELAX mode expects a success", 5, 4, 0);
    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class, ThisFeature.class })
    public static class RepeatFeaturesMethod {
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
        @RandomBug.Repeat(issue = "repeatedMethod", onFailure = 5)
        public void repeatedTest() {
            if (feature.repeated < 5) {
                fail("should fail");
            }
        }
    }

    @Test
    public void shouldRepeatFeaturesOnMethod() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.RELAX.toString());
        runClassAndVerify(RepeatFeaturesMethod.class, "RELAX mode expects a success", 1, 0, 0);
    }

    @RandomBug.Repeat(issue = "repeatedTest", onFailure = 5)
    public static class ThisRandomFeature implements RunnerFeature {

    }

    @RunWith(FeaturesRunner.class)
    @Features({ RandomBug.Feature.class, ThisFeature.class, ThisRandomFeature.class })
    public static class RepeatRandomFeaturesTest extends AbstractRepeatFeaturesTest {

    }

    @Test
    public void shouldRepeatFeaturesOnFeature() {
        System.setProperty(RandomBug.MODE_PROPERTY, RandomBug.Mode.RELAX.toString());
        runClassAndVerify(RepeatRandomFeaturesTest.class, "RELAX mode expects a success", 5, 4, 0);
    }

    protected Result runClassAndVerify(Class<?> klass, String testFailureDescription, int runCount, int failureCount,
            int ignoreCount) {
        Result result = JUnitCore.runClasses(klass);
        assertThat(result.getRunCount()).isEqualTo(runCount);
        assertThat(result.getFailureCount()).isEqualTo(failureCount);
        if (failureCount == 0 && !result.wasSuccessful()) {
            new Failures(result).fail(FAILURE_MESSAGE, testFailureDescription);
        }
        assertThat(result.getIgnoreCount()).isEqualTo(ignoreCount);
        return result;
    }

    @Before
    public void storeSystemProperties() {
        oldProperty = System.getProperty(RandomBug.MODE_PROPERTY);
    }

    @After
    public void clearSystemProperties() {
        if (oldProperty != null) {
            System.setProperty(RandomBug.MODE_PROPERTY, oldProperty);
        } else {
            System.clearProperty(RandomBug.MODE_PROPERTY);
        }
    }

}
