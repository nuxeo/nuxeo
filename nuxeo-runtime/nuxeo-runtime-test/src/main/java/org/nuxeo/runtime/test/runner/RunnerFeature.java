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
 */
public interface RunnerFeature {

    /**
     * Called when preparing to run the test class. Framework is not started at this point. Here is time for the feature
     * to configure the runner from annotations on the test class.
     */
    default void initialize(FeaturesRunner runner) throws Exception {
    }

    /**
     * Configures Guice bindings if any is required by the feature. This is called after the framework is started and
     * before Guice module is built. The tests are launched after guice module is built.
     */
    default void configure(FeaturesRunner runner, Binder binder) {
    }

    /**
     * Before running tests. At this point Guice modules are registered and injector created.
     */
    default void beforeRun(FeaturesRunner runner) throws Exception {
    }

    /**
     * After tests were run.
     */
    default void afterRun(FeaturesRunner runner) throws Exception {
    }

    /**
     * Features are initialized. Runner is ready to create the injector.
     */
    default void start(FeaturesRunner runner) throws Exception {
    }

    /**
     * Notification that a test instance was created. Can be used by features to make custom injection or other
     * preparation of the test instance.
     */
    default void testCreated(Object test) throws Exception {
    }

    /**
     * Before exiting the test.
     */
    default void stop(FeaturesRunner runner) throws Exception {
    }

    /**
     * Before entering in the @Before methods
     */
    default void beforeSetup(FeaturesRunner runner) throws Exception {
    }

    /**
     * After the call of the @After methods
     */
    default void afterTeardown(FeaturesRunner runner) throws Exception {
    }

    /**
     * Before a test method is invoked.
     */
    default void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
    }

    /**
     * After a test method was ran.
     */
    default void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
    }

}
