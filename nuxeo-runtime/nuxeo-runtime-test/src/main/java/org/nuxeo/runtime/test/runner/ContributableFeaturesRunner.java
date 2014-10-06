/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
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
                    ((FeaturesRunner)runner).loader.loadFeatures(clazz);
                }
                return runner;
            }

        });
    }

}
