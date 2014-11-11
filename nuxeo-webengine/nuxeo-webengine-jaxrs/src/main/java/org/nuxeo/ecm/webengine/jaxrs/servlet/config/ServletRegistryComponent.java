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
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServletRegistryComponent extends DefaultComponent {

    public static final String XP_SERVLETS = "servlets";

    public static final String XP_FILTERS = "filters";

    protected ServletRegistry registry;

    public ServletRegistryComponent() {
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        registry = ServletRegistry.getInstance();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        ServletRegistry.dispose();
        registry = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_SERVLETS.equals(extensionPoint)) {
            registry.addServlet((ServletDescriptor)contribution);
        } else if (XP_FILTERS.equals(extensionPoint)) {
            registry.addFilterSet((FilterSetDescriptor)contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_SERVLETS.equals(extensionPoint)) {
            registry.removeServlet((ServletDescriptor)contribution);
        } else if (XP_FILTERS.equals(extensionPoint)) {
            registry.removeFilterSet((FilterSetDescriptor)contribution);
        }
    }

}
