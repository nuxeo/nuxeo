/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface RunnerFeature {

//    private final Class<T> type;
//
//    @SuppressWarnings("unchecked")
//    protected RunnerFeature() {
//        Type superclass = getClass().getGenericSuperclass();
//        while (superclass instanceof Class<?>) {
//            superclass = ((Class<?>)superclass).getGenericSuperclass();
//        }
//        if (superclass == null) {
//            throw new RuntimeException("Missing type parameter.");
//        }
//        Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
//        if (!(type instanceof Class<?>)) {
//            throw new RuntimeException("Invalid class parameter type. "+type);
//        }
//        this.type = (Class<T>)type;
//    }
//
//    public <R extends FeaturesRunner> boolean acceptRunner(R runner) {
//        return type.isAssignableFrom(runner.getClass());
//    }
//
//    public Class<T> runnerType() {
//        return type;
//    }
//
//    public T castRunner(FeaturesRunner runner) {
//        return null;
//    }
//

    /**
     * Called when preparing to run the test class. Framework is not started at this point.
     * Here is time for the feature to configure the runner from annotations on the test class.
     */
    void initialize(FeaturesRunner runner) throws Exception;

    /**
     * Configures Guice bindings if any is required by the feature.
     * This is called after the framework is started and before Guice module is built.
     * The tests are launched after guice module is built.
     */
    void configure(FeaturesRunner runner, Binder binder);

    /**
     * Before running tests. At this point Guice modules are registered and injector created.
     */
    void beforeRun(FeaturesRunner runner) throws Exception;

    /**
     * After tests were run.
     */
    void afterRun(FeaturesRunner runner) throws Exception;

    /**
     * Features are initialized. Runner is ready to create the injector.
     */
    void start(FeaturesRunner runner) throws Exception;

    /**
     * Before exiting the test.
     */
    void stop(FeaturesRunner runner) throws Exception;

    /**
     * Before a test method is invoked.
     */
    void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception;

    /**
     * After a test method was ran.
     */
    void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception;

}
