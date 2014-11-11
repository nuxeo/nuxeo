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

    /**
     * Called just after the runner is initialized. Framework is not started at this point.
     * Here is time for the feature to configure the runner from annotations on the test class. 
     * @param runner
     * @param testClass
     * @throws Exception
     */
    public void initialize(NuxeoRunner runner, Class<?> testClass) throws Exception;
    
    /**
     * Deploy artifacts required by this feature.
     * This is called while framework is starting to incrementally deploy bundles.
     * @param runner 
     * @throws Exception
     */
    public void deploy(NuxeoRunner runner) throws Exception;
    
    /**
     * Configure Guice bindings if any is required by the feature.
     * This is called after the framework is started and before guice module is built.
     * The tests are launched after guice module is built. 
     * @param runner
     * @param binder
     */
    public void configure(NuxeoRunner runner, Binder binder);
    
    
    /**
     * Before tests are run. At this point Guice module are registered and framework is started and tests are ready to be launched. 
     * @param runner
     * @throws Exception
     */
    public void beforeRun(NuxeoRunner runner) throws Exception;
    

    /**
     * After tests were run.
     * @param runner
     * @throws Exception
     */
    public void afterRun(NuxeoRunner runner) throws Exception;
    
    
    /**
     * Before stopping the framework.
     * @throws Exception
     */
    public void cleanup(NuxeoRunner runner) throws Exception;
    
    /**
     * Before a test method is invoked
     * @param runner
     * @param method
     * @param test
     * @throws Exception
     */
    public void beforeMethodRun(NuxeoRunner runner, FrameworkMethod method, Object test) throws Exception;
    
    /**
     * After a test method was ran
     * @param runner
     * @param method
     * @param test
     * @throws Exception
     */
    public void afterMethodRun(NuxeoRunner runner, FrameworkMethod method, Object test) throws Exception;
    
}
