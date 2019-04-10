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
package org.nuxeo.ecm.core.opencmis.impl.client;

import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;

/**
 * Local CMIS binding to the services.
 */
public class NuxeoBinding implements CmisBinding {

    public final NuxeoCmisService service;

    public NuxeoBinding(NuxeoCmisService service) {
        this.service = service;
    }

    @Override
    public void close() {
        service.close();
    }

    @Override
    public NuxeoCmisService getRepositoryService() {
        return service;
    }

    @Override
    public NuxeoCmisService getNavigationService() {
        return service;
    }

    @Override
    public NuxeoCmisService getObjectService() {
        return service;
    }

    @Override
    public NuxeoCmisService getDiscoveryService() {
        return service;
    }

    @Override
    public NuxeoCmisService getRelationshipService() {
        return service;
    }

    @Override
    public NuxeoCmisService getVersioningService() {
        return service;
    }

    @Override
    public NuxeoCmisService getAclService() {
        return service;
    }

    @Override
    public NuxeoCmisService getMultiFilingService() {
        return service;
    }

    @Override
    public NuxeoCmisService getPolicyService() {
        return service;
    }

    @Override
    public BindingsObjectFactory getObjectFactory() {
        return service.getObjectFactory();
    }

    @Override
    public void clearAllCaches() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearRepositoryCache(String repositoryId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
}
