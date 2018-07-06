/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import org.junit.runners.model.FrameworkMethod;

import com.google.inject.Binder;

/**
 * These are the states the runner goes through when using runtime feature:
 *
 * <pre>
 * CREATE FRAMEWORK
 * new feature()        --> constructor
 * COLLECT DEFINED DEPLOYMENTS
 * feature.initialize() --> can be used to configure nuxeo home or register JNDI objects
 * START FRAMEWORK
 * feature.start()
 * feature.beforeRun()
 * feature.configure() --> can be used to add guice bindings and to dynamically deploy components using the harness
 * for each test method:
 *   feature.testCreated()
 *   feature.beforeSetup
 *   feature.beforeMethodRun()  --> test method interceptor
 *   testMethod()
 *   feature.afterMethodRun()   --> test method interceptor
 *   feature.afterTeardown()
 * feature.afterRun() --> cleanup that require framework to be started
 * STOP FRAMEWORK
 * feature.stop()  --> destructor
 * </pre>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 10.3, implements directly {@link RunnerFeature}
 */
@Deprecated
public class SimpleFeature implements RunnerFeature {

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
    }

    @Override
    public void testCreated(Object test) throws Exception {
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
    }

    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {

    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {

    }

}
