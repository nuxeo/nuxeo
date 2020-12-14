/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.runtime;

import static org.junit.Assert.assertNull;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.google.inject.Binder;

/**
 * @since 11.3
 */
public class RuntimeInitializationPreprocessorErrorFeature implements RunnerFeature {

    protected static final String KEY = "org.nuxeo.runtime.deployment.errors";

    protected void addDeploymentError(String message) {
        System.setProperty(KEY, System.getProperty(KEY, "") + message + "\n");
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        assertNull(System.getProperty(KEY));
        addDeploymentError("Runtime test feature init error");
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        addDeploymentError("Runtime test start error");
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        addDeploymentError("Runtime test configure error");
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        System.clearProperty(KEY);
    }

}
