/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.test.runner;

import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;

/**
 * Features runner which integrates with jgiven data provider.
 *
 * @see DataProviderRunner
 * @since 8.4
 */
public class FeaturesRunnerWithParms extends FeaturesRunner {

    public FeaturesRunnerWithParms(Class<?> classToRun) throws InitializationError {
        super(classToRun);
    }

    class Provider extends DataProviderRunner {

        public Provider() throws InitializationError {
            super(getTargetTestClass());
        }

        @Override
        protected List<FrameworkMethod> computeTestMethods() {
            return super.computeTestMethods();
        }

        @Override
        protected void validateTestMethods(List<Throwable> errors) {
            super.validateTestMethods(errors);
        }
    }

    Provider provider;

    Provider provider() {
        if (provider == null) {
            try {
                provider = new Provider();
            } catch (InitializationError cause) {
                throw new AssertionError("Cannot initialize data provider", cause);
            }
        }
        return provider;
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        return provider().computeTestMethods();
    }

    @Override
    protected void validateTestMethods(List<Throwable> errors) {
        provider().validateTestMethods(errors);
    }

}
