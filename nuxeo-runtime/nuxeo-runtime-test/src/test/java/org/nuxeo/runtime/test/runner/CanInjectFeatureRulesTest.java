package org.nuxeo.runtime.test.runner;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(FeaturesRunner.class)
@Features(CanInjectFeatureRulesTest.ThisFeature.class)
public class CanInjectFeatureRulesTest {

    public static boolean typeRuleCallbacked ;

    public static class TypeRule implements TestRule {

        @Override
        public Statement apply(Statement base, Description description) {
            typeRuleCallbacked = true;
            return base;
        }

    }

    public static boolean thisRuleCallbacked;

    public static class ThisRule implements TestRule {
        @Override
        public Statement apply(Statement base, Description description) {
            thisRuleCallbacked = true;
            return base;
        }
    }

    public static class ThisFeature extends SimpleFeature {

        @ClassRule
        public static TestRule myClassRule() {
            return new TypeRule();
        }

        @Rule
        public TestRule myInstanceRule() {
            return new ThisRule();
        }
    }

    @Test
    public void rulesAreLoaded() {
        assertThat(true).isEqualTo(thisRuleCallbacked);
        assertThat(true).isEqualTo(typeRuleCallbacked);
    }
}
