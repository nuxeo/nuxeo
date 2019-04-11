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
package org.nuxeo.ecm.core.opencmis.impl.client;

import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;

/**
 * Local CMIS binding to the services.
 */
public class NuxeoBinding implements CmisBinding {

    private static final long serialVersionUID = 1L;

    private static final BindingsObjectFactory OBJECT_FACTORY = new BindingsObjectFactoryImpl();

    public final CmisService service;

    public NuxeoBinding(CmisService service) {
        this.service = service;
    }

    @Override
    public void close() {
        service.close();
    }

    @Override
    public CmisService getRepositoryService() {
        return service;
    }

    @Override
    public CmisService getNavigationService() {
        return service;
    }

    @Override
    public CmisService getObjectService() {
        return service;
    }

    @Override
    public CmisService getDiscoveryService() {
        return service;
    }

    @Override
    public CmisService getRelationshipService() {
        return service;
    }

    @Override
    public CmisService getVersioningService() {
        return service;
    }

    @Override
    public CmisService getAclService() {
        return service;
    }

    @Override
    public CmisService getMultiFilingService() {
        return service;
    }

    @Override
    public CmisService getPolicyService() {
        return service;
    }

    @Override
    public BindingsObjectFactory getObjectFactory() {
        return OBJECT_FACTORY;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return null; // no provider
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

    @Override
    public String getSessionId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public BindingType getBindingType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CoreSession getCoreSession() {
        NuxeoCmisService nuxeoCmisService = NuxeoCmisService.extractFromCmisService(service);
        return nuxeoCmisService == null ? null : nuxeoCmisService.getCoreSession();
    }

}
