/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.test.runner;

import org.apache.logging.log4j.ThreadContext;
import org.junit.runners.model.FrameworkMethod;

import com.google.inject.Binder;

public class MDCFeature implements RunnerFeature {

    protected static final String F_TEST = "fTest";

    protected static final String F_SUITE = "fSuite";

    protected static final String F_STATE = "fState";

    @Override
    public void initialize(FeaturesRunner runner) {
        ThreadContext.put(F_STATE, "initialize");
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        ThreadContext.put(F_STATE, "configure");
    }

    @Override
    public void beforeRun(FeaturesRunner runner) {
        ThreadContext.put(F_STATE, "beforeRun");
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        ThreadContext.put(F_STATE, "afterRun");
    }

    @Override
    public void start(FeaturesRunner runner) {
        ThreadContext.put(F_STATE, "start");
    }

    @Override
    public void testCreated(Object test) {
        ThreadContext.put(F_STATE, "testCreated");
        ThreadContext.put(F_SUITE, test.getClass().getName());
    }

    @Override
    public void stop(FeaturesRunner runner) {
        ThreadContext.remove(F_STATE);
        ThreadContext.remove(F_SUITE);
        ThreadContext.remove(F_TEST);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) {
        ThreadContext.put(F_STATE, "beforeSetup");
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) {
        ThreadContext.put(F_STATE, "afterTeardown");
    }

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
        ThreadContext.put(F_STATE, "beforeMethodRun");
        ThreadContext.put(F_TEST, method.getMethod().getName());
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
        ThreadContext.put(F_STATE, "afterMethodRun");
        ThreadContext.remove(F_TEST);
    }

}
