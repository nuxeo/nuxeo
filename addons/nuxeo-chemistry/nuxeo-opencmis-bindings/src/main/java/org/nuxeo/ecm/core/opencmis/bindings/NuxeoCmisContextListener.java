/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;

/**
 * Servlet context listener that sets up the CMIS service factory in the servlet
 * context as expected by
 * {@link org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet}
 * or
 * {@link org.apache.chemistry.opencmis.server.impl.webservices.AbstractService}
 * .
 *
 * @see CmisRepositoryContextListener
 */
public class NuxeoCmisContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        CmisServiceFactory factory = new NuxeoCmisServiceFactory();
        factory.init(null);
        sce.getServletContext().setAttribute(
                CmisRepositoryContextListener.SERVICES_FACTORY, factory);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        CmisServiceFactory factory = (CmisServiceFactory) sce.getServletContext().getAttribute(
                CmisRepositoryContextListener.SERVICES_FACTORY);
        if (factory != null) {
            factory.destroy();
        }
    }

}
