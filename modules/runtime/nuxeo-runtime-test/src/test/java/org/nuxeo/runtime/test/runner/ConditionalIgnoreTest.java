/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin, Julien Carsique
 *
 */
package org.nuxeo.runtime.test.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Assume;
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

public class ConditionalIgnoreTest {

    protected static boolean isRunningInners;

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

    public static class Always implements ConditionalIgnoreRule.Condition {
        @Override
        public boolean shouldIgnore() {
            return true;
        }
    }

    /** @since 11.1 */
    public static class AlwaysWithClassRule implements ConditionalIgnoreRule.Condition {
        @Override
        public boolean shouldIgnore() {
            return true;
        }

        @Override
        public boolean supportsClassRule() {
            return true;
        }
    }

    public static class Never implements ConditionalIgnoreRule.Condition {
        @Override
        public boolean shouldIgnore() {
            return false;
        }
    }

    /**
     * Expected tests result: 3 run, 1 skip
     */
    @RunWith(FeaturesRunner.class)
    @Features(ConditionalIgnoreRule.Feature.class)
    public static class ShouldIgnoreTest {
        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Test
        @ConditionalIgnoreRule.Ignore(condition = Always.class, cause = "ignored for tests")
        public void ignored() {
            fail("should not be called");
        }

        @Test
        @ConditionalIgnoreRule.Ignore(condition = Never.class, cause = "not ignored for tests")
        public void notIgnored() {
        }

        @Test
        public void ran() {
        }
    }

    /**
     * Expected tests result: 2 run, 2 skip (because ignore condition doesn't support classRule behavior)
     */
    @RunWith(FeaturesRunner.class)
    @Features(ConditionalIgnoreRule.Feature.class)
    @ConditionalIgnoreRule.Ignore(condition = Always.class, cause = "ignored for tests")
    public static class ShouldIgnoreSuite {
        @Test
        public void ignored() {
            fail("should not be called");
        }

        @Test
        public void ran() {
            fail("should not be called");
        }
    }

    /**
     * Expected tests result: 0 run, 1 skip (the whole class)
     *
     * @since 11.1
     */
    @RunWith(FeaturesRunner.class)
    @Features(ConditionalIgnoreRule.Feature.class)
    @ConditionalIgnoreRule.Ignore(condition = AlwaysWithClassRule.class, cause = "ignored for tests")
    public static class ShouldIgnoreSuiteAtClassLevel {
        @Test
        public void ignored() {
            fail("should not be called");
        }

        @Test
        public void ran() {
            fail("should not be called");
        }
    }

    /**
     * Expected tests result: 2 run
     */
    @RunWith(FeaturesRunner.class)
    @Features(ConditionalIgnoreRule.Feature.class)
    @ConditionalIgnoreRule.Ignore(condition = Never.class, cause = "not ignored for tests")
    public static class ShouldNotIgnoreSuite {
        @Test
        public void notIgnored() {
        }

        @Test
        public void ran() {
        }
    }

    @Test
    public void shouldIgnoreTest() {
        runAndAssert(ShouldIgnoreTest.class, 3, 1);
    }

    @Test
    public void shouldIgnoreSuite() {
        runAndAssert(ShouldIgnoreSuite.class, 2, 2);
    }

    @Test
    public void shouldIgnoreSuiteAtClassLevel() {
        runAndAssert(ShouldIgnoreSuiteAtClassLevel.class, 0, 1);
    }

    @Test
    public void shouldNotIgnoreSuite() {
        runAndAssert(ShouldNotIgnoreSuite.class, 2, 0);
    }

    protected void runAndAssert(Class<?> classToRun, int expectedRunCount, int expectedIgnoreCount) {
        Result result = JUnitCore.runClasses(classToRun);
        if (!result.wasSuccessful()) {
            Failures failures = new Failures(result.getFailures());
            fail("Unexpected failure\n" + failures.toString());
        }
        assertEquals(expectedRunCount, result.getRunCount());
        assertEquals(expectedIgnoreCount, result.getIgnoreCount());
    }

}
