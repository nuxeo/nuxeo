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

import org.apache.chemistry.opencmis.commons.api.AclService;
import org.apache.chemistry.opencmis.commons.api.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.api.CmisBinding;
import org.apache.chemistry.opencmis.commons.api.DiscoveryService;
import org.apache.chemistry.opencmis.commons.api.MultiFilingService;
import org.apache.chemistry.opencmis.commons.api.NavigationService;
import org.apache.chemistry.opencmis.commons.api.ObjectService;
import org.apache.chemistry.opencmis.commons.api.PolicyService;
import org.apache.chemistry.opencmis.commons.api.RelationshipService;
import org.apache.chemistry.opencmis.commons.api.RepositoryService;
import org.apache.chemistry.opencmis.commons.api.VersioningService;

/**
 * Local CMIS binding to the services.
 */
public class NuxeoBinding implements CmisBinding {

    private static final long serialVersionUID = 1L;

    private final NuxeoService service;

    public NuxeoBinding(NuxeoService service) {
        this.service = service;
    }

    public void close() {
        service.close();
    }

    public RepositoryService getRepositoryService() {
        return service;
    }

    public NavigationService getNavigationService() {
        return service;
    }

    public ObjectService getObjectService() {
        return service;
    }

    public DiscoveryService getDiscoveryService() {
        return service;
    }

    public RelationshipService getRelationshipService() {
        return service;
    }

    public VersioningService getVersioningService() {
        return service;
    }

    public AclService getAclService() {
        return service;
    }

    public MultiFilingService getMultiFilingService() {
        return service;
    }

    public PolicyService getPolicyService() {
        return service;
    }

    public BindingsObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }

    public void clearAllCaches() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void clearRepositoryCache(String repositoryId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
}
