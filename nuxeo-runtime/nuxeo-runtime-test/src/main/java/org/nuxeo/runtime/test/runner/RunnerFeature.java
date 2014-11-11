/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
