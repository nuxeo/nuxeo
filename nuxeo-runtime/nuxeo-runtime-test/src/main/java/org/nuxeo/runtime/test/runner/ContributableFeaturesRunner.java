/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
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
                    ((FeaturesRunner) runner).loader.loadFeatures(clazz);
                }
                return runner;
            }

        });
    }

}
