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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import javax.servlet.ServletException;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.ecm.webengine.jaxrs.ApplicationManager;
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

    public ServletRegistryComponent() {
    }

    @Override
    public void activate(ComponentContext context) {
        registry = ServletRegistry.getInstance();
    }

    @Override
    public void deactivate(ComponentContext context) {
        ServletRegistry.dispose();
        registry = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_SERVLETS.equals(extensionPoint)) {
            ((ServletDescriptor) contribution).setBundle(contributor.getContext().getBundle());
            try {
                registry.addServlet((ServletDescriptor) contribution);
            } catch (ServletException | NamespaceException e) {
                throw new RuntimeException(e);
            }
        } else if (XP_FILTERS.equals(extensionPoint)) {
            registry.addFilterSet((FilterSetDescriptor) contribution);
        } else if (XP_RESOURCES.equals(extensionPoint)) {
            ResourcesDescriptor rd = (ResourcesDescriptor) contribution;
            rd.setBundle(contributor.getContext().getBundle());
            registry.addResources(rd);
        } else if (XP_SUBRESOURCES.equals(extensionPoint)) {
            ResourceExtension rxt = (ResourceExtension) contribution;
            rxt.setBundle(contributor.getContext().getBundle());
            ApplicationManager.getInstance().getOrCreateApplication(rxt.getApplication()).addExtension(rxt);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_SERVLETS.equals(extensionPoint)) {
            registry.removeServlet((ServletDescriptor) contribution);
        } else if (XP_FILTERS.equals(extensionPoint)) {
            registry.removeFilterSet((FilterSetDescriptor) contribution);
        } else if (XP_RESOURCES.equals(extensionPoint)) {
            ResourcesDescriptor rd = (ResourcesDescriptor) contribution;
            rd.setBundle(contributor.getContext().getBundle());
            registry.removeResources(rd);
        } else if (XP_SUBRESOURCES.equals(extensionPoint)) {
            ResourceExtension rxt = (ResourceExtension) contribution;
            rxt.setBundle(contributor.getContext().getBundle());
            ApplicationManager.getInstance().getOrCreateApplication(rxt.getApplication()).removeExtension(rxt);
        }
    }

}
