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
package org.nuxeo.ecm.webengine;

import java.util.List;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.app.BundledApplication;
import org.nuxeo.ecm.webengine.app.ModuleHandler;
import org.nuxeo.ecm.webengine.app.extensions.ExtensibleResource;
import org.nuxeo.ecm.webengine.app.extensions.ResourceContribution;
import org.nuxeo.ecm.webengine.model.Resource;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ApplicationManager {

    void addApplication(Bundle bundle, Application app);

    void removeApplication(Bundle bundle);

    BundledApplication getApplication(String bundleId);

    BundledApplication[] getApplications();

    ModuleHandler getModuleHandler(String appId);

    ModuleHandler[] getModuleHandlers();

    Object getContribution(Resource target, String key) throws Exception;

    List<ResourceContribution> getContributions(ExtensibleResource target, String category);

    List<ResourceContribution> getContributions(Class<? extends ExtensibleResource> target, String category);

    ModuleHandler getModuleHandlerFor(Class<?> rootResource);

    /**
     * Reload modules - this is useful for hot reload when application classes changes
     */
    void reload();

    /**
     * Deploy the JAX-RS application if any is found in the given bundle.
     * If no JAX-RS application is found return false, otherwise deploy it and return true.
     * @param bundle the bundle that may contain a JAX-RS application
     * @return true if a JAX-RS application was found and deployed, false otherwise
     * @throws Exception
     */
    boolean deployApplication(Bundle bundle) throws Exception;

}
