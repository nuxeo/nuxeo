/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet context listener that sets up the CMIS service factory in the servlet
 * context as expected by
 * {@link org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet}
 * or
 * {@link org.apache.chemistry.opencmis.server.impl.browser.CmisBrowserBindingServlet}
 * or
 * {@link org.apache.chemistry.opencmis.server.impl.webservices.AbstractService}
 * .
 *
 * @see CmisRepositoryContextListener
 */
public class NuxeoCmisContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        RuntimeService runtime = Framework.getRuntime();
        if (runtime == null || !runtime.isStarted()) {
            Framework.addListener(new RuntimeServiceListener() {

                @Override
                public void handleEvent(RuntimeServiceEvent event) {
                    if (event.id != RuntimeServiceEvent.RUNTIME_STARTED) {
                        return;
                    }
                    Framework.removeListener(this);
                    activate(sce);
                }

            });
        } else {
            activate(sce);
        }
    }

    protected void activate(final ServletContextEvent sce) {
        NuxeoCmisServiceFactoryManager manager = Framework
            .getService(NuxeoCmisServiceFactoryManager.class);
        CmisServiceFactory factory = manager.getNuxeoCmisServiceFactory();
        sce.getServletContext().setAttribute(
                CmisRepositoryContextListener.SERVICES_FACTORY, factory);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        CmisServiceFactory factory = (CmisServiceFactory) sce
            .getServletContext().getAttribute(
                    CmisRepositoryContextListener.SERVICES_FACTORY);
        if (factory != null) {
            factory.destroy();
        }
    }

}
