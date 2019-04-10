/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.webdav.service;

import org.nuxeo.ecm.webdav.backend.BackendFactory;
import org.nuxeo.ecm.webdav.backend.SearchBackendFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

public class WebDavService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.webdav.service");

    public static String BACKEND_FACTORY_XP = "backendFactory";

    protected BackendFactory backendFactory = new SearchBackendFactory();

    public static WebDavService instance() {
        return (WebDavService) Framework.getRuntime().getComponent(
                WebDavService.NAME);
    }

    public BackendFactory getBackendFactory() {
        return backendFactory;
    }

    // used by tests
    public void setBackendFactory(BackendFactory backendFactory) {
        this.backendFactory = backendFactory;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (BACKEND_FACTORY_XP.equals(extensionPoint)) {
            BackendFactoryDescriptor desc = (BackendFactoryDescriptor) contribution;
            Class<?> factoryClass = desc.getFactoryClass();
            backendFactory = (BackendFactory) factoryClass.newInstance();
        }
    }

}
