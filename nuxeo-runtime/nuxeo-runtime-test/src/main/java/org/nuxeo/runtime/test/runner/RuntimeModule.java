/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.runtime.test.runner;

import org.nuxeo.runtime.api.Framework;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * This module is registering all the services deployed in the framework.
 * Services registered after the runner is started (after the injector is created)
 * will not be available for injection.
 * To enforce your services are bound in the injector you must use only {@link Deploy} annotations
 * to declare them, and avoid deploying them using the API.  
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RuntimeModule extends AbstractModule {

    protected NuxeoRunner runner;
    
    public RuntimeModule(NuxeoRunner runner) {
        this.runner = runner;
    }
    
    @Override
    protected void configure() {
        for (String svc : Framework.getRuntime().getComponentManager().getServices()) {
            try {
                Class<?> clazz = Class.forName(svc);
                bind0(clazz);
            } catch (Exception e) {
                throw new RuntimeException("Failed to bind service: "+svc, e);
            }
        }
        bind(NuxeoRunner.class).toInstance(runner);
        bind(RuntimeHarness.class).toInstance(runner.getHarness());
    }

    protected <T> void bind0(Class<T> type) {
        bind(type).toProvider(new ServiceProvider<T>(type)).in(Scopes.SINGLETON);
    }
    
}
