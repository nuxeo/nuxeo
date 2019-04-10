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

import org.apache.chemistry.opencmis.commons.spi.AclService;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.apache.chemistry.opencmis.commons.spi.DiscoveryService;
import org.apache.chemistry.opencmis.commons.spi.MultiFilingService;
import org.apache.chemistry.opencmis.commons.spi.NavigationService;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.apache.chemistry.opencmis.commons.spi.PolicyService;
import org.apache.chemistry.opencmis.commons.spi.RelationshipService;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.commons.spi.VersioningService;

/**
 * Local CMIS binding to the services.
 */
public class NuxeoBinding implements CmisBinding {

    private static final long serialVersionUID = 1L;

    private final NuxeoService service;

    public NuxeoBinding(NuxeoService service) {
        this.service = service;
    }

    @Override
    public void close() {
        service.close();
    }

    @Override
    public RepositoryService getRepositoryService() {
        return service;
    }

    @Override
    public NavigationService getNavigationService() {
        return service;
    }

    @Override
    public ObjectService getObjectService() {
        return service;
    }

    @Override
    public DiscoveryService getDiscoveryService() {
        return service;
    }

    @Override
    public RelationshipService getRelationshipService() {
        return service;
    }

    @Override
    public VersioningService getVersioningService() {
        return service;
    }

    @Override
    public AclService getAclService() {
        return service;
    }

    @Override
    public MultiFilingService getMultiFilingService() {
        return service;
    }

    @Override
    public PolicyService getPolicyService() {
        return service;
    }

    @Override
    public BindingsObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
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
