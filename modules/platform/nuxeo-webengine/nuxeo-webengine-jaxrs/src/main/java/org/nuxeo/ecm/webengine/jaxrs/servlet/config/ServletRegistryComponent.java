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
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import javax.servlet.ServletException;

import org.nuxeo.ecm.webengine.jaxrs.ApplicationManager;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.service.http.NamespaceException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServletRegistryComponent extends DefaultComponent {

    public static final String XP_SERVLETS = "servlets";

    public static final String XP_FILTERS = "filters";

    public static final String XP_RESOURCES = "resources";

    public static final String XP_SUBRESOURCES = "subresources";

    protected ServletRegistry registry;

    @Override
    public void start(ComponentContext context) {
        registry = ServletRegistry.getInstance();

        this.<FilterSetDescriptor> getRegistryContributions(XP_FILTERS).forEach(registry::addFilterSet);
        this.<ResourcesDescriptor> getRegistryContributions(XP_RESOURCES).forEach(registry::addResources);
        this.<ResourceExtension> getRegistryContributions(XP_SUBRESOURCES)
            .forEach(desc -> ApplicationManager.getInstance()
                                               .getOrCreateApplication(desc.getApplication())
                                               .addExtension(desc));
        // servlets might depend on resources to be already registered
        this.<ServletDescriptor> getRegistryContributions(XP_SERVLETS).forEach(desc -> {
            try {
                registry.addServlet(desc);
            } catch (ServletException | NamespaceException e) {
                throw new RuntimeServiceException(e);
            }
        });

    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        this.<ResourceExtension> getRegistryContributions(XP_SUBRESOURCES)
            .forEach(desc -> ApplicationManager.getInstance()
                                               .getOrCreateApplication(desc.getApplication())
                                               .removeExtension(desc));
        ServletRegistry.dispose();
        registry = null;
    }

}
