/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.ecm.webengine.jaxrs.ApplicationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
            ((ServletDescriptor)contribution).setBundle(contributor.getContext().getBundle());
            registry.addServlet((ServletDescriptor)contribution);
        } else if (XP_FILTERS.equals(extensionPoint)) {
            registry.addFilterSet((FilterSetDescriptor)contribution);
        } else if (XP_RESOURCES.equals(extensionPoint)) {
            ResourcesDescriptor rd = (ResourcesDescriptor)contribution;
            rd.setBundle(contributor.getContext().getBundle());
            registry.addResources(rd);
        } else if (XP_SUBRESOURCES.equals(extensionPoint)) {
            ResourceExtension rxt = (ResourceExtension)contribution;
            rxt.setBundle(contributor.getContext().getBundle());
            ApplicationManager.getInstance().getOrCreateApplication(rxt.getApplication()).addExtension(rxt);
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
        } else if (XP_RESOURCES.equals(extensionPoint)) {
            ResourcesDescriptor rd = (ResourcesDescriptor)contribution;
            rd.setBundle(contributor.getContext().getBundle());
            registry.removeResources(rd);
        } else if (XP_SUBRESOURCES.equals(extensionPoint)) {
            ResourceExtension rxt = (ResourceExtension)contribution;
            rxt.setBundle(contributor.getContext().getBundle());
            ApplicationManager.getInstance().getOrCreateApplication(rxt.getApplication()).removeExtension(rxt);
        }
    }

}
