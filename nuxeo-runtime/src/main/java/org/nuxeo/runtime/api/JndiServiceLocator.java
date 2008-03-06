/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Properties;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class JndiServiceLocator implements ServiceLocator {

    private static final long serialVersionUID = -8445946234540101788L;

    protected transient InitialContext context;

    /**
     * Initializes and creates the JNDI initial context.
     */
    public void initialize(String host, int port, Properties properties) throws Exception {
        context = new InitialContext();
    }

    public void dispose() {
        context = null;
    }

    /**
     * @return the context.
     */
    public InitialContext getContext() {
        return context;
    }

    public Object lookup(ServiceDescriptor sd) throws Exception {
        String locator = sd.getLocator();
        if (locator == null) {
            locator = createLocator(sd);
            sd.setLocator(locator);
        }
        return lookup(locator);
    }

    public Object lookup(String serviceId) throws Exception {
        return context.lookup(serviceId);
    }

    protected static String createLocator(ServiceDescriptor sd) {
        return sd.getServiceClassName();
    }

}
