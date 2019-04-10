/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Servlet context listener that sets up the CMIS service factory in the servlet context as expected by
 * {@link org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet} or
 * {@link org.apache.chemistry.opencmis.server.impl.browser.CmisBrowserBindingServlet}.
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
        NuxeoCmisServiceFactoryManager manager = Framework.getService(NuxeoCmisServiceFactoryManager.class);
        CmisServiceFactory factory = manager.getNuxeoCmisServiceFactory();
        sce.getServletContext().setAttribute(CmisRepositoryContextListener.SERVICES_FACTORY, factory);
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
