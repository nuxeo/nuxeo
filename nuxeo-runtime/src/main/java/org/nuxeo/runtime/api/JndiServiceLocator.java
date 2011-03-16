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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Properties;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JndiServiceLocator implements ServiceLocator {

    private static final long serialVersionUID = -8445946234540101788L;

    protected transient InitialContext context;

    /**
     * Initializes and creates the JNDI initial context.
     */
    public void initialize(String host, int port, Properties properties)
            throws Exception {
        context = new InitialContext();
    }

    @Override
    public void dispose() {
        context = null;
    }

    public InitialContext getContext() {
        return context;
    }

    @Override
    public Object lookup(ServiceDescriptor sd) throws Exception {
        String locator = sd.getLocator();
        if (locator == null) {
            locator = createLocator(sd);
            sd.setLocator(locator);
        }
        return lookup(locator);
    }

    @Override
    public Object lookup(String serviceId) throws Exception {
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
        try {
            return context.lookup(serviceId);
        } finally {
            Thread.currentThread().setContextClassLoader(oldcl);
        }
    }

    protected String createLocator(ServiceDescriptor sd) {
        return sd.getServiceClassName();
    }

}
