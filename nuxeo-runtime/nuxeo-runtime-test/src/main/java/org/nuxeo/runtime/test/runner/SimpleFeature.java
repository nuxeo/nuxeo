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
public class SimpleFeature implements RunnerFeature {

    public void afterRun(NuxeoRunner runner) throws Exception {
    }

    public void beforeRun(NuxeoRunner runner) throws Exception {
    }

    public void cleanup(NuxeoRunner runner) throws Exception {
    }

    public void configure(NuxeoRunner runner, Binder binder) {
    }

    public void deploy(NuxeoRunner runner) throws Exception {
    }

    public void initialize(NuxeoRunner runner, Class<?> testClass)
            throws Exception {
    }
    
    public void afterMethodRun(NuxeoRunner runner, FrameworkMethod method,
            Object test) throws Exception {
    }
    
    public void beforeMethodRun(NuxeoRunner runner, FrameworkMethod method,
            Object test) throws Exception {
    }

}
