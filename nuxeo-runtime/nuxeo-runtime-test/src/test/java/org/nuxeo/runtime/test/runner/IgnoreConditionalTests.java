package org.nuxeo.runtime.test.runner;

import static org.assertj.core.api.Assertions.fail;

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

public class IgnoreConditionalTests {

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

        @ClassRule
        public static final IgnoreInner ignoreInner = new IgnoreInner();

        @Override
        public boolean shouldIgnore() {
            return true;
        }

    }

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
        public void ran() {

        }
    }

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

    @Test
    public void shouldIgnoreTest() {
        Result result = JUnitCore.runClasses(ShouldIgnoreTest.class);
        if (!result.wasSuccessful()) {
            Failures failures = new Failures(result.getFailures());
            fail("Unexpected failure\n" + failures.toString());
        }
    }


    @Test
    public void shouldIgnoreSuite() {
        Result result = JUnitCore.runClasses(ShouldIgnoreSuite.class);
        if (!result.wasSuccessful()) {
            Failures failures = new Failures(result.getFailures());
            fail("Unexpected failure\n" + failures.toString());
        }
    }

    protected void runClassAndVerifyNoFailures(Class<?> klass, String testFailureDescription) {
        Result result = JUnitCore.runClasses(klass);
        analyseResult(result, testFailureDescription);
    }

    protected void analyseResult(Result result, String testFailureDescription) {
        if (!result.wasSuccessful()) {
            new Failures(result).fail("Failure", testFailureDescription);
        }
    }
}
