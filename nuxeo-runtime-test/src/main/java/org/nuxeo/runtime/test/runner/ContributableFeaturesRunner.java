package org.nuxeo.runtime.test.runner;

import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class ContributableFeaturesRunner extends Suite {

    public ContributableFeaturesRunner(final Class<?> clazz, final RunnerBuilder builder) throws InitializationError {
        super(clazz, new RunnerBuilder() {

            @Override
            public Runner runnerForClass(Class<?> testClass) throws Throwable {
                Runner runner = builder.runnerForClass(testClass);
                if (runner instanceof FeaturesRunner) {
                    ((FeaturesRunner)runner).loadFeatures(clazz);
                }
                return runner;
            }

        });
    }

}
